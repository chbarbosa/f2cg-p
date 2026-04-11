import { test, expect } from '@playwright/test';

const BOARD_URL = '/?devBoard=1';

test.describe('Card Detail Modal', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto(BOARD_URL);
  });

  test('clicking a face-up hand card opens the modal', async ({ page }) => {
    await page.locator('[data-testid="card-card-warrior-1"] .card').click();
    await expect(page.locator('[data-testid="card-detail-modal"]')).toBeVisible();
  });

  test('modal shows correct card name and stats', async ({ page }) => {
    await page.locator('[data-testid="card-card-mage-1"] .card').click();
    await expect(page.locator('[data-testid="modal-card-name"]')).toHaveText('Arcane Scholar');
    await expect(page.locator('[data-testid="modal-current-atk"]')).toHaveText('6');
    await expect(page.locator('[data-testid="modal-current-def"]')).toHaveText('3');
  });

  test('modal shows current and base ATK/DEF when modified by buff', async ({ page }) => {
    // Stone Guard: base ATK 5, current ATK 7 (buff +2 ATK); DEF 7/7 (unchanged)
    await page.locator('[data-testid="card-field-warrior-1"] .card').click();
    await expect(page.locator('[data-testid="modal-current-atk"]')).toHaveText('7');
    await expect(page.locator('[data-testid="modal-current-def"]')).toHaveText('7');
    // Footer always visible for units
    await expect(page.locator('[data-testid="modal-base-stats"]')).toBeVisible();
    // Only ATK was modified — base ATK shown struck-through
    await expect(page.locator('[data-testid="modal-base-atk"]')).toHaveText('5');
    // DEF not modified — base DEF not rendered
    await expect(page.locator('[data-testid="modal-base-def"]')).not.toBeVisible();
  });

  test('buff block shown with green tint when active', async ({ page }) => {
    // Stone Guard has activeBuff
    await page.locator('[data-testid="card-field-warrior-1"] .card').click();
    const buffBlock = page.locator('[data-testid="modal-buff"]');
    await expect(buffBlock).toBeVisible();
    await expect(buffBlock).toHaveText(/Battle Cry/);
    await expect(buffBlock).toHaveText(/\+2 ATK/);
  });

  test('debuff block shown with red tint when active', async ({ page }) => {
    // Dark Mage (opponent field) has activeDebuff
    await page.locator('[data-testid="card-opp-field-1"] .card').click();
    const debuffBlock = page.locator('[data-testid="modal-debuff"]');
    await expect(debuffBlock).toBeVisible();
    await expect(debuffBlock).toHaveText(/Weaken/);
    await expect(debuffBlock).toHaveText(/-2 ATK/);
  });

  test('abilities list renders with name and description', async ({ page }) => {
    // Iron Knight has Shield Bash + Iron Will
    await page.locator('[data-testid="card-card-warrior-1"] .card').click();
    const abilities = page.locator('[data-testid="modal-abilities"]');
    await expect(abilities).toBeVisible();
    await expect(abilities).toHaveText(/Shield Bash/);
    await expect(abilities).toHaveText(/Reduces enemy DEF by 2/);
    await expect(abilities).toHaveText(/Iron Will/);
    await expect(abilities).toHaveText(/ACTIVE/);
    await expect(abilities).toHaveText(/PASSIVE/);
  });

  test('hasActed banner shows only for field cards that have acted', async ({ page }) => {
    // Tired Paladin has hasActed: true
    await page.locator('[data-testid="card-field-paladin-1"] .card').click();
    await expect(page.locator('[data-testid="modal-acted-banner"]')).toBeVisible();

    // Close and open a hand card — no acted banner
    await page.keyboard.press('Escape');
    await page.locator('[data-testid="card-card-warrior-1"] .card').click();
    await expect(page.locator('[data-testid="modal-acted-banner"]')).not.toBeVisible();
  });

  test('Escape key closes the modal', async ({ page }) => {
    await page.locator('[data-testid="card-card-warrior-1"] .card').click();
    await expect(page.locator('[data-testid="card-detail-modal"]')).toBeVisible();
    await page.keyboard.press('Escape');
    await expect(page.locator('[data-testid="card-detail-modal"]')).not.toBeVisible();
  });

  test('clicking outside the modal closes it', async ({ page }) => {
    await page.locator('[data-testid="card-card-warrior-1"] .card').click();
    await expect(page.locator('[data-testid="card-detail-modal"]')).toBeVisible();
    await page.locator('[data-testid="modal-overlay"]').click({ position: { x: 10, y: 10 } });
    await expect(page.locator('[data-testid="card-detail-modal"]')).not.toBeVisible();
  });

  test('face-down cards do not open modal on click', async ({ page }) => {
    // Opponent hand cards are face-down and non-clickable
    await page.locator('[data-testid="opponent-hand"] .card--face-down').first().click();
    await expect(page.locator('[data-testid="card-detail-modal"]')).not.toBeVisible();
  });
});