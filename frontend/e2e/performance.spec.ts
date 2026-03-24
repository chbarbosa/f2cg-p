import { test, expect, type Page } from '@playwright/test';

// Inject a fake auth state into localStorage so we appear logged in
// without needing a real backend login/verify flow
async function setAuthState(page: Page) {
  await page.goto('/');
  await page.evaluate(() => {
    const authState = {
      state: {
        playerId: 'test-player-1',
        token: 'fake-jwt-token',
        username: 'test@example.com',
        nickname: 'Tester',
        country: 'BR',
        pendingEmail: null,
      },
      version: 0,
    };
    localStorage.setItem('auth', JSON.stringify(authState));
  });
  await page.reload();
  // Wait until the home menu is visible
  await expect(page.getByRole('button', { name: 'Performance' })).toBeVisible({ timeout: 10000 });
}

const SEASON = {
  id: 's-2026-1',
  year: 2026,
  seasonNumber: 1,
  name: null,
  startDate: '2026-01-01',
  endDate: '2026-02-28',
  phase2StartDate: '2026-02-01',
};

function makeCurrentResponse(overrides: object) {
  return {
    season: SEASON,
    currentPhase: 'FREE',
    rank: 'PENDING',
    highestRank: 'PENDING',
    totalMatches: 10,
    victories: 6,
    defeats: 4,
    matchesThisWeek: 10,
    ...overrides,
  };
}

const SEASON_LIST = [
  {
    id: 's-2025-6',
    year: 2025,
    seasonNumber: 6,
    name: null,
    startDate: '2025-11-01',
    endDate: '2025-12-31',
    phase2StartDate: '2025-12-01',
  },
  {
    id: 's-2025-5',
    year: 2025,
    seasonNumber: 5,
    name: null,
    startDate: '2025-09-01',
    endDate: '2025-10-31',
    phase2StartDate: '2025-10-01',
  },
];

const PAST_SEASON_RESPONSE = {
  season: SEASON_LIST[0],
  currentPhase: null,
  rank: 'ROOKIE',
  highestRank: 'INTERMEDIATE',
  totalMatches: 22,
  victories: 10,
  defeats: 12,
  matchesThisWeek: 0,
};

async function goToPerformance(page: Page, currentResponse: object, seasonList = '[]') {
  await page.route('**/api/performance/current', route =>
    route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(currentResponse) })
  );
  await page.route('**/api/performance/seasons', route =>
    route.fulfill({ status: 200, contentType: 'application/json', body: seasonList })
  );
  await page.getByRole('button', { name: 'Performance' }).click();
  await expect(page.getByTestId('stats-panel')).toBeVisible({ timeout: 10000 });
}

test.describe('Performance screen', () => {
  test.beforeEach(async ({ page }) => {
    await setAuthState(page);
  });

  test('PENDING state renders correct message', async ({ page }) => {
    await goToPerformance(page, makeCurrentResponse({}));

    await expect(page.getByTestId('rank-badge-rank')).toHaveText('PENDING');
    await expect(page.getByTestId('pending-message')).toBeVisible();
    await expect(page.getByTestId('pending-message')).toHaveText('Rank pending — play at least 15 matches');
  });

  test('ELITE rank badge renders with gold color', async ({ page }) => {
    await goToPerformance(page, makeCurrentResponse({ rank: 'ELITE', highestRank: 'ELITE' }));

    const badge = page.getByTestId('rank-badge-rank');
    await expect(badge).toHaveText('ELITE');
    await expect(badge).toHaveCSS('background-color', 'rgb(249, 226, 175)');
  });

  test('INTERMEDIATE rank badge renders with blue color', async ({ page }) => {
    await goToPerformance(page, makeCurrentResponse({ rank: 'INTERMEDIATE', highestRank: 'INTERMEDIATE' }));

    const badge = page.getByTestId('rank-badge-rank');
    await expect(badge).toHaveText('INTERMEDIATE');
    await expect(badge).toHaveCSS('background-color', 'rgb(137, 180, 250)');
  });

  test('FREE phase badge renders correct text and color', async ({ page }) => {
    await goToPerformance(page, makeCurrentResponse({ currentPhase: 'FREE' }));

    const badge = page.getByTestId('phase-badge');
    await expect(badge).toHaveText('Free Season — open matchmaking');
    await expect(badge).toHaveCSS('background-color', 'rgb(137, 180, 250)');
  });

  test('RANKED phase badge renders correct text and color', async ({ page }) => {
    await goToPerformance(page, makeCurrentResponse({ currentPhase: 'RANKED', matchesThisWeek: 15 }));

    const badge = page.getByTestId('phase-badge');
    await expect(badge).toHaveText('Ranked Season — same rank only');
    await expect(badge).toHaveCSS('background-color', 'rgb(203, 166, 247)');
  });

  test('peak rank badge renders correctly', async ({ page }) => {
    await goToPerformance(page, makeCurrentResponse({ rank: 'ADVANCED', highestRank: 'ELITE' }));

    await expect(page.getByTestId('rank-badge-peak-rank')).toHaveText('ELITE');
  });

  test('season selector populates with participated seasons', async ({ page }) => {
    await goToPerformance(page, makeCurrentResponse({}), JSON.stringify(SEASON_LIST));

    const selector = page.getByTestId('season-selector');
    await expect(selector).toBeVisible();
    // 2 past seasons + "Current season" default option = 3 options
    const options = selector.locator('option');
    await expect(options).toHaveCount(3);
  });

  test('selecting a past season displays correct historical stats', async ({ page }) => {
    await page.route('**/api/performance?seasonId=s-2025-6', route =>
      route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(PAST_SEASON_RESPONSE) })
    );
    await goToPerformance(page, makeCurrentResponse({}), JSON.stringify(SEASON_LIST));

    await page.getByTestId('season-selector').selectOption('s-2025-6');
    await expect(page.getByTestId('rank-badge-rank')).toHaveText('ROOKIE');
    await expect(page.getByTestId('stats-row')).toContainText('22');
  });

  test('weekly activity counter is hidden for past seasons', async ({ page }) => {
    await page.route('**/api/performance?seasonId=s-2025-6', route =>
      route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(PAST_SEASON_RESPONSE) })
    );
    await goToPerformance(
      page,
      makeCurrentResponse({ currentPhase: 'RANKED', matchesThisWeek: 15 }),
      JSON.stringify(SEASON_LIST)
    );

    // Current season (RANKED) shows weekly counter
    await expect(page.getByTestId('weekly-activity')).toBeVisible();

    // Select past season — weekly counter disappears
    await page.getByTestId('season-selector').selectOption('s-2025-6');
    await expect(page.getByTestId('weekly-activity')).not.toBeVisible();
  });

  test('green activity indicator renders when matchesThisWeek >= 15', async ({ page }) => {
    await goToPerformance(page, makeCurrentResponse({ currentPhase: 'RANKED', matchesThisWeek: 15 }));

    await expect(page.getByTestId('activity-met')).toBeVisible();
    await expect(page.getByTestId('activity-met')).toHaveText('Activity requirement met');
  });

  test('yellow warning renders when matchesThisWeek < 15', async ({ page }) => {
    await goToPerformance(page, makeCurrentResponse({ currentPhase: 'RANKED', matchesThisWeek: 8 }));

    await expect(page.getByTestId('activity-warning')).toBeVisible();
    await expect(page.getByTestId('activity-warning')).toHaveText('Play 7 more matches to avoid demotion');
  });

  test('weekly activity counter is hidden in FREE phase', async ({ page }) => {
    await goToPerformance(page, makeCurrentResponse({ currentPhase: 'FREE' }));

    await expect(page.getByTestId('weekly-activity')).not.toBeVisible();
  });
});
