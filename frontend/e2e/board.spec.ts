import { test, expect } from '@playwright/test';

const BOARD_URL = '/?devBoard=1';

// Mock data constants (must match BoardPreview.tsx)
const OPPONENT_HAND_SIZE = 5;
const PLAYER_HAND_SIZE = 5;
// card-expensive-1 has manaCost 5, currentMana is 3 — so it is unplayable
const UNPLAYABLE_CARD_ID = 'card-expensive-1';
// card-warrior-1 is the initially selected card
const SELECTED_CARD_ID = 'card-warrior-1';
// Stone Guard on player field has activeBuff "Battle Cry"
const BUFF_NAME = 'Battle Cry';
// Dark Mage on opponent field has activeDebuff "Weaken"
const DEBUFF_NAME = 'Weaken';

test.describe('GameBoard component', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto(BOARD_URL);
  });

  test('opponent hand renders correct number of face-down cards', async ({ page }) => {
    const faceDownCards = page.locator('[data-testid="opponent-hand"] .card--face-down');
    await expect(faceDownCards).toHaveCount(OPPONENT_HAND_SIZE);
  });

  test('player hand renders correct number of face-up cards', async ({ page }) => {
    const faceUpCards = page.locator('[data-testid="player-hand"] .card:not(.card--face-down)');
    await expect(faceUpCards).toHaveCount(PLAYER_HAND_SIZE);
  });

  test('unplayable card (cost > currentMana) has dimmed class', async ({ page }) => {
    const unplayableCard = page.locator(`[data-testid="card-${UNPLAYABLE_CARD_ID}"] .card`);
    await expect(unplayableCard).toHaveClass(/card--unplayable/);
  });

  test('initially selected card has highlighted border class', async ({ page }) => {
    const selectedCard = page.locator(`[data-testid="card-${SELECTED_CARD_ID}"] .card`);
    await expect(selectedCard).toHaveClass(/card--selected/);
  });

  test('buff name peeks above unit card on player field', async ({ page }) => {
    const buffPeek = page.locator('[data-testid="player-field"] [data-testid="buff-peek"]');
    await expect(buffPeek).toBeVisible();
    await expect(buffPeek).toHaveText(BUFF_NAME);
  });

  test('debuff name peeks above unit card on opponent field', async ({ page }) => {
    const debuffPeek = page.locator('[data-testid="opponent-field"] [data-testid="debuff-peek"]');
    await expect(debuffPeek).toBeVisible();
    await expect(debuffPeek).toHaveText(DEBUFF_NAME);
  });

  test('face-down cards reveal no text content', async ({ page }) => {
    const faceDownCards = page.locator('[data-testid="opponent-hand"] .card--face-down');
    const count = await faceDownCards.count();
    for (let i = 0; i < count; i++) {
      const text = await faceDownCards.nth(i).innerText();
      expect(text.trim()).toBe('');
    }
  });

  test('center strip shows correct turn number, mana and phase', async ({ page }) => {
    const centerStrip = page.locator('[data-testid="center-strip"]');
    await expect(centerStrip).toBeVisible();

    await expect(page.locator('[data-testid="turn-number"]')).toHaveText('2');
    await expect(page.locator('[data-testid="current-mana"]')).toHaveText('3');
    await expect(page.locator('[data-testid="phase-badge"]')).toHaveText('ACTION');
  });

  test('clicking a player hand card fires onCardClick with correct cardId', async ({ page }) => {
    const targetCardId = 'card-buff-1';
    const card = page.locator(`[data-testid="card-${targetCardId}"] .card`);
    await card.click();

    const lastClicked = page.locator('[data-testid="last-clicked"]');
    await expect(lastClicked).toHaveText(targetCardId);
  });
});