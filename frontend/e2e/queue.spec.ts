import { test, expect, type Page } from '@playwright/test';

const uid = () => Date.now().toString(36) + Math.random().toString(36).slice(2, 5);

async function registerAndLogin(page: Page): Promise<string> {
  const username = `queue_${uid()}`;
  await page.goto('/');
  await page.getByRole('button', { name: 'Register' }).first().click();
  await page.getByPlaceholder('Username').fill(username);
  await page.getByPlaceholder('Password').fill('password123');
  await page.getByRole('button', { name: 'Register' }).last().click();
  await expect(page.getByText(`Welcome, ${username}!`)).toBeVisible();
  return username;
}

async function createPlayableDeck(page: Page) {
  await page.getByRole('button', { name: 'My Decks' }).click();
  await page.getByRole('button', { name: '+ New Deck' }).click();
  await page.getByPlaceholder('Deck name…').fill('My Warrior Deck');
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
    if (await card.isVisible()) {
      await card.click();
    }
  }

  const saveBtn = page.getByRole('button', { name: /Save/ });
  await saveBtn.click();
  await expect(page.getByText('My Decks')).toBeVisible();
  await expect(page.getByText('PLAYABLE')).toBeVisible();
}

test.describe('Queue', () => {
  test('Play button is disabled with no playable decks', async ({ page }) => {
    await registerAndLogin(page);
    const playBtn = page.getByRole('button', { name: 'Play' });
    await expect(playBtn).toBeDisabled();
  });

  test('Play button tooltip shown when disabled', async ({ page }) => {
    await registerAndLogin(page);
    const playWrapper = page.locator('[title="You need at least one playable deck to battle"]');
    await expect(playWrapper).toBeVisible();
  });

  test('Play button is enabled after creating a playable deck', async ({ page }) => {
    await registerAndLogin(page);
    await createPlayableDeck(page);
    await page.getByRole('button', { name: '← Back' }).click();
    const playBtn = page.getByRole('button', { name: 'Play' });
    await expect(playBtn).not.toBeDisabled();
  });

  test('deck selector shows only PLAYABLE decks', async ({ page }) => {
    await registerAndLogin(page);

    // Create a DRAFT deck (no cards)
    await page.getByRole('button', { name: 'My Decks' }).click();
    await page.getByRole('button', { name: '+ New Deck' }).click();
    await page.getByPlaceholder('Deck name…').fill('Draft Deck');
    await page.getByRole('button', { name: 'WARRIOR' }).click();
    await page.getByRole('button', { name: /Save/ }).click();
    await expect(page.getByText('My Decks')).toBeVisible();

    await createPlayableDeck(page);
    await page.getByRole('button', { name: '← Back' }).click();

    await page.getByRole('button', { name: 'Play' }).click();
    await expect(page.getByText('Choose Your Deck')).toBeVisible();

    // Only PLAYABLE decks shown — "My Warrior Deck" visible, "Draft Deck" not visible
    await expect(page.getByText('My Warrior Deck')).toBeVisible();
    await expect(page.getByText('Draft Deck')).not.toBeVisible();
  });

  test('Ready button is disabled until a deck is selected', async ({ page }) => {
    await registerAndLogin(page);
    await createPlayableDeck(page);
    await page.getByRole('button', { name: '← Back' }).click();

    await page.getByRole('button', { name: 'Play' }).click();
    await expect(page.getByText('Choose Your Deck')).toBeVisible();

    const readyBtn = page.getByRole('button', { name: 'Ready' });
    await expect(readyBtn).toBeDisabled();

    await page.getByText('My Warrior Deck').click();
    await expect(readyBtn).not.toBeDisabled();
  });

  test('Cancel confirmation modal shows and works', async ({ page }) => {
    await registerAndLogin(page);
    await createPlayableDeck(page);
    await page.getByRole('button', { name: '← Back' }).click();

    await page.getByRole('button', { name: 'Play' }).click();
    await page.getByText('My Warrior Deck').click();
    await page.getByRole('button', { name: 'Ready' }).click();

    await expect(page.getByText('Looking for opponent')).toBeVisible();

    // Click Cancel → modal appears
    await page.getByRole('button', { name: 'Cancel' }).click();
    await expect(page.getByText('Leave the queue?')).toBeVisible();

    // Click Stay → modal disappears, still on waiting screen
    await page.getByRole('button', { name: 'Stay' }).click();
    await expect(page.getByText('Leave the queue?')).not.toBeVisible();
    await expect(page.getByText('Looking for opponent')).toBeVisible();

    // Click Cancel again → Leave → back to home
    await page.getByRole('button', { name: 'Cancel' }).click();
    await page.getByRole('button', { name: 'Leave' }).click();
    await expect(page.getByText(`Welcome,`)).toBeVisible();
  });

  test('beforeunload confirmation triggers on navigation', async ({ page }) => {
    await registerAndLogin(page);
    await createPlayableDeck(page);
    await page.getByRole('button', { name: '← Back' }).click();

    await page.getByRole('button', { name: 'Play' }).click();
    await page.getByText('My Warrior Deck').click();
    await page.getByRole('button', { name: 'Ready' }).click();
    await expect(page.getByText('Looking for opponent')).toBeVisible();

    let dialogTriggered = false;
    page.on('dialog', async dialog => {
      dialogTriggered = true;
      await dialog.dismiss();
    });

    await page.evaluate(() => window.location.href = 'about:blank');
    await page.waitForTimeout(500);

    expect(dialogTriggered).toBe(true);
  });
});