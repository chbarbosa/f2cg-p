import { test, expect, type Page } from '@playwright/test';

const uid = () => Date.now().toString(36) + Math.random().toString(36).slice(2, 5);

async function registerAndLogin(page: Page) {
  const username = `deck_${uid()}`;
  await page.goto('/');
  await page.getByRole('button', { name: 'Register' }).first().click();
  await page.getByPlaceholder('Username').fill(username);
  await page.getByPlaceholder('Password').fill('password123');
  await page.getByRole('button', { name: 'Register' }).last().click();
  await expect(page.getByText(`Welcome, ${username}!`)).toBeVisible();
}

async function goToDeckBuilder(page: Page) {
  await page.getByRole('button', { name: 'My Decks' }).click();
  await expect(page.getByText('My Decks')).toBeVisible();
  await page.getByRole('button', { name: '+ New Deck' }).click();
  await expect(page.getByPlaceholder('Deck name…')).toBeVisible();
}

async function selectThemeAndFirstCard(page: Page) {
  await page.getByRole('button', { name: 'WARRIOR' }).click();
  // Wait for cards to load
  await expect(page.getByText('Iron Knight')).toBeVisible();
  // Click the first card
  await page.getByText('Iron Knight').click();
}

test.describe('Deck Builder', () => {
  test('theme selector locks after first card selection', async ({ page }) => {
    await registerAndLogin(page);
    await goToDeckBuilder(page);

    await page.getByRole('button', { name: 'WARRIOR' }).click();
    await expect(page.getByText('Iron Knight')).toBeVisible();

    // Click a card to lock the theme
    await page.getByText('Iron Knight').click();

    // MAGE button should be disabled
    const mageBtn = page.getByRole('button', { name: 'MAGE' });
    await expect(mageBtn).toBeDisabled();
  });

  test('reset button appears after first card selection', async ({ page }) => {
    await registerAndLogin(page);
    await goToDeckBuilder(page);

    // Reset button should not be visible yet
    await expect(page.getByRole('button', { name: 'Reset' })).not.toBeVisible();

    await selectThemeAndFirstCard(page);

    // Reset button should now be visible
    await expect(page.getByRole('button', { name: 'Reset' })).toBeVisible();
  });

  test('reset modal shows on reset click', async ({ page }) => {
    await registerAndLogin(page);
    await goToDeckBuilder(page);
    await selectThemeAndFirstCard(page);

    await page.getByRole('button', { name: 'Reset' }).click();

    await expect(page.getByText('Reset will clear all card selections. Continue?')).toBeVisible();
  });

  test('reset modal confirm clears selections and unlocks theme', async ({ page }) => {
    await registerAndLogin(page);
    await goToDeckBuilder(page);
    await selectThemeAndFirstCard(page);

    // Verify theme is locked
    await expect(page.getByRole('button', { name: 'MAGE' })).toBeDisabled();

    // Open and confirm reset
    await page.getByRole('button', { name: 'Reset' }).click();
    await page.getByRole('button', { name: 'Confirm' }).click();

    // Theme should be unlocked again
    await expect(page.getByRole('button', { name: 'MAGE' })).not.toBeDisabled();
    // Reset button should disappear
    await expect(page.getByRole('button', { name: 'Reset' })).not.toBeVisible();
  });

  test('reset modal cancel keeps selections intact', async ({ page }) => {
    await registerAndLogin(page);
    await goToDeckBuilder(page);
    await selectThemeAndFirstCard(page);

    await page.getByRole('button', { name: 'Reset' }).click();
    // Use last() to target the modal Cancel (rendered after header Cancel in DOM)
    await page.getByRole('button', { name: 'Cancel' }).last().click();

    // Modal dismissed, theme still locked
    await expect(page.getByText('Reset will clear all card selections. Continue?')).not.toBeVisible();
    await expect(page.getByRole('button', { name: 'MAGE' })).toBeDisabled();
  });

  test('card counter updates correctly', async ({ page }) => {
    await registerAndLogin(page);
    await goToDeckBuilder(page);

    // Initial counter should show 0/20
    await expect(page.getByText('0/20')).toBeVisible();

    await page.getByRole('button', { name: 'WARRIOR' }).click();
    await expect(page.getByText('Iron Knight')).toBeVisible();

    await page.getByText('Iron Knight').click();
    await expect(page.getByText('1/20')).toBeVisible();

    // Click again to deselect
    await page.getByText('Iron Knight').click();
    await expect(page.getByText('0/20')).toBeVisible();
  });

  test('save button is disabled until deck name is filled', async ({ page }) => {
    await registerAndLogin(page);
    await goToDeckBuilder(page);

    // Save button should be disabled when no name
    const saveBtn = page.getByRole('button', { name: /Save/ });
    await expect(saveBtn).toBeDisabled();

    // Type a name
    await page.getByPlaceholder('Deck name…').fill('My Warrior Deck');
    // Still disabled if no theme selected
    await expect(saveBtn).toBeDisabled();

    // Select theme (makes it saveable even without cards)
    await page.getByRole('button', { name: 'WARRIOR' }).click();
    await expect(saveBtn).not.toBeDisabled();
  });

  test('save creates draft deck with fewer than 20 cards', async ({ page }) => {
    await registerAndLogin(page);
    await goToDeckBuilder(page);

    await page.getByPlaceholder('Deck name…').fill('Draft Deck');
    await page.getByRole('button', { name: 'WARRIOR' }).click();
    await expect(page.getByText('Iron Knight')).toBeVisible();
    await page.getByText('Iron Knight').click();

    await page.getByRole('button', { name: /Save/ }).click();

    // Should navigate back to deck list
    await expect(page.getByText('My Decks')).toBeVisible();
    // Deck should appear with DRAFT badge
    await expect(page.getByText('DRAFT', { exact: true })).toBeVisible();
    await expect(page.getByText('Draft Deck')).toBeVisible();
  });

  test('cancel with unsaved changes shows confirmation modal', async ({ page }) => {
    await registerAndLogin(page);
    await goToDeckBuilder(page);

    // Make a change
    await page.getByPlaceholder('Deck name…').fill('Unsaved');

    await page.getByRole('button', { name: 'Cancel' }).click();

    await expect(page.getByText('You have unsaved changes. Discard and go back?')).toBeVisible();
  });

  test('cancel without unsaved changes navigates back immediately', async ({ page }) => {
    await registerAndLogin(page);
    await goToDeckBuilder(page);

    // Do not make any changes
    await page.getByRole('button', { name: 'Cancel' }).click();

    // Should go straight back to deck list
    await expect(page.getByText('My Decks')).toBeVisible();
    await expect(page.getByText('You have unsaved changes')).not.toBeVisible();
  });
});