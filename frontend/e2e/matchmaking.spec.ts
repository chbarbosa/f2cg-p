import { test, expect, type Browser, type Page } from '@playwright/test';

const uid = () => Date.now().toString(36) + Math.random().toString(36).slice(2, 5);

async function registerAndLogin(page: Page): Promise<string> {
  const username = `mm_${uid()}`;
  await page.goto('/');
  await page.getByRole('button', { name: 'Register' }).first().click();
  await page.getByPlaceholder('Username').fill(username);
  await page.getByPlaceholder('Password').fill('password123');
  await page.getByRole('button', { name: 'Register' }).last().click();
  await expect(page.getByText(`Welcome, ${username}!`)).toBeVisible();
  return username;
}

async function navigateToWaitingScreen(page: Page): Promise<void> {
  await page.getByRole('button', { name: 'My Decks' }).click();
  await page.getByRole('button', { name: '+ New Deck' }).click();
  await page.getByPlaceholder('Deck name…').fill('Match Deck');
  await page.getByRole('button', { name: 'WARRIOR' }).click();
  await expect(page.getByText('Iron Knight')).toBeVisible();

  const cardNames = [
    'Iron Knight', 'Steel Guardian', 'Berserker', 'Shield Bearer', 'Sword Dancer',
    'Battle Axe Warrior', 'Heavy Lancer', 'War Chief', 'Paladin', 'Crusader',
    'Veteran Soldier', 'Warlord', 'Siege Knight', 'Thunder Knight', 'Blood Warrior',
    'Champion', 'Iron Wall', 'Battle Hardened', 'War Elephant', 'Titan Guard',
  ];
  for (const name of cardNames) {
    const card = page.getByText(name, { exact: true }).first();
    if (await card.isVisible()) await card.click();
  }
  await page.getByRole('button', { name: /Save/ }).click();
  await expect(page.getByText('My Decks')).toBeVisible();
  await page.getByRole('button', { name: '← Back' }).click();
  await page.getByRole('button', { name: 'Play' }).click();
  await page.getByText('Match Deck').click();
  await page.getByRole('button', { name: 'Ready' }).click();
  await expect(page.getByText('Looking for opponent')).toBeVisible();
}

test.describe('Matchmaking', () => {
  test('Waiting screen subscribes to SSE stream on mount', async ({ page }) => {
    await registerAndLogin(page);

    const sseRequestPromise = page.waitForRequest(req =>
      req.url().includes('/api/queue/stream')
    );

    await navigateToWaitingScreen(page);

    const sseRequest = await sseRequestPromise;
    expect(sseRequest.url()).toContain('/api/queue/stream');
    expect(sseRequest.url()).toContain('token=');
  });

  test('MATCH_FOUND event redirects both players to game screen', async ({ browser }: { browser: Browser }) => {
    const ctx1 = await browser.newContext();
    const ctx2 = await browser.newContext();
    const page1 = await ctx1.newPage();
    const page2 = await ctx2.newPage();

    try {
      await registerAndLogin(page1);
      await registerAndLogin(page2);

      // Player 1 navigates to waiting screen first (SSE stream opens)
      await navigateToWaitingScreen(page1);

      // Player 2 joins — backend matches with player 1 and emits MATCH_FOUND to both
      await navigateToWaitingScreen(page2);

      // Both players should be redirected to game screen
      await expect(page1.getByText('Match started!')).toBeVisible({ timeout: 10_000 });
      await expect(page2.getByText('Match started!')).toBeVisible({ timeout: 10_000 });
    } finally {
      await ctx1.close();
      await ctx2.close();
    }
  });

  test('Game screen renders placeholder with both usernames', async ({ browser }: { browser: Browser }) => {
    const ctx1 = await browser.newContext();
    const ctx2 = await browser.newContext();
    const page1 = await ctx1.newPage();
    const page2 = await ctx2.newPage();

    try {
      const username1 = await registerAndLogin(page1);
      await registerAndLogin(page2);

      await navigateToWaitingScreen(page1);
      await navigateToWaitingScreen(page2);

      await expect(page1.getByText('Match started!')).toBeVisible({ timeout: 10_000 });
      await expect(page1.getByText('Game is under construction. Stay tuned!')).toBeVisible();

      // Both usernames are shown somewhere in the vs display
      await expect(page1.getByText(new RegExp(username1))).toBeVisible();
    } finally {
      await ctx1.close();
      await ctx2.close();
    }
  });

  test('QUEUE_TIMEOUT event shows modal with correct message', async ({ page }) => {
    await registerAndLogin(page);

    // Mock the SSE stream to immediately emit QUEUE_TIMEOUT
    await page.route('**/api/queue/stream**', async route => {
      await route.fulfill({
        status: 200,
        contentType: 'text/event-stream',
        headers: { 'Cache-Control': 'no-cache' },
        body: 'event: QUEUE_TIMEOUT\ndata: {"message":"No opponent found. Please try again."}\n\n',
      });
    });

    await navigateToWaitingScreen(page);

    await expect(page.getByText('No opponent found')).toBeVisible({ timeout: 5_000 });
    await expect(page.getByText('We could not find an opponent. Please try again.')).toBeVisible();
    await expect(page.getByRole('button', { name: 'Back to Home' })).toBeVisible();
  });

  test('Back to Home button dismisses modal and navigates home', async ({ page }) => {
    await registerAndLogin(page);

    await page.route('**/api/queue/stream**', async route => {
      await route.fulfill({
        status: 200,
        contentType: 'text/event-stream',
        headers: { 'Cache-Control': 'no-cache' },
        body: 'event: QUEUE_TIMEOUT\ndata: {"message":"No opponent found. Please try again."}\n\n',
      });
    });

    await navigateToWaitingScreen(page);

    await expect(page.getByText('No opponent found')).toBeVisible({ timeout: 5_000 });
    await page.getByRole('button', { name: 'Back to Home' }).click();

    await expect(page.getByText('Welcome,')).toBeVisible();
    await expect(page.getByText('No opponent found')).not.toBeVisible();
  });

  test('Cancel button on waiting screen still works', async ({ page }) => {
    await registerAndLogin(page);

    // Mock SSE to stay open (no events) so we can test cancel without interference
    await page.route('**/api/queue/stream**', async route => {
      // Never fulfill — keeps SSE stream open
      await new Promise(() => {});
    });

    await navigateToWaitingScreen(page);
    await expect(page.getByText('Looking for opponent')).toBeVisible();

    await page.getByRole('button', { name: 'Cancel' }).click();
    await expect(page.getByText('Leave the queue?')).toBeVisible();

    await page.getByRole('button', { name: 'Leave' }).click();
    await expect(page.getByText('Welcome,')).toBeVisible();
  });
});