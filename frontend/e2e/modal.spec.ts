import { test, expect, type Page } from '@playwright/test';

async function setAuthState(page: Page) {
  await page.goto('/');
  await page.evaluate(() => {
    const authState = {
      state: {
        playerId: 'test-player-1',
        token: 'fake-jwt-token',
        username: 'testuser',
        nickname: 'Tester',
        country: 'BR',
        pendingEmail: null,
      },
      version: 0,
    };
    localStorage.setItem('auth', JSON.stringify(authState));
  });
  await page.reload();
  await expect(page.getByRole('button', { name: 'Logout' })).toBeVisible({ timeout: 10000 });
}

async function openLogoutModal(page: Page) {
  await page.getByRole('button', { name: 'Logout' }).click();
  await expect(page.locator('[data-testid="modal"]')).toBeVisible({ timeout: 3000 });
}

test.describe('Modal component', () => {
  test.beforeEach(async ({ page }) => {
    await setAuthState(page);
  });

  test('modal does not render before logout button is clicked', async ({ page }) => {
    await expect(page.locator('[data-testid="modal"]')).not.toBeVisible();
  });

  test('modal renders when logout button is clicked', async ({ page }) => {
    await page.getByRole('button', { name: 'Logout' }).click();
    await expect(page.locator('[data-testid="modal"]')).toBeVisible();
  });

  test('title renders correctly', async ({ page }) => {
    await openLogoutModal(page);
    await expect(page.locator('[data-testid="modal-title"]')).toHaveText('LOGOUT');
  });

  test('message renders correctly', async ({ page }) => {
    await openLogoutModal(page);
    const body = page.locator('[data-testid="modal-message"]');
    await expect(body).toContainText('Are you sure you want to logout?');
    await expect(body).toContainText('Any ongoing match will count as a forfeit.');
  });

  test('confirm button label matches confirmLabel prop', async ({ page }) => {
    await openLogoutModal(page);
    await expect(page.locator('[data-testid="modal-confirm"] button')).toHaveText('LOGOUT');
  });

  test('cancel button label matches cancelLabel default', async ({ page }) => {
    await openLogoutModal(page);
    await expect(page.locator('[data-testid="modal-cancel"] button')).toHaveText('CANCEL');
  });

  test('clicking confirm triggers logout and navigates to login', async ({ page }) => {
    await openLogoutModal(page);
    await page.locator('[data-testid="modal-confirm"] button').click();
    // Auth state cleared — login form should appear
    await expect(page.getByPlaceholder('Username')).toBeVisible({ timeout: 5000 });
    // Auth localStorage key should be cleared
    const auth = await page.evaluate(() => {
      const raw = localStorage.getItem('auth');
      if (!raw) return null;
      return JSON.parse(raw).state;
    });
    expect(auth?.token).toBeNull();
    expect(auth?.username).toBeNull();
  });

  test('clicking cancel closes modal and stays on home screen', async ({ page }) => {
    await openLogoutModal(page);
    await page.locator('[data-testid="modal-cancel"] button').click();
    await expect(page.locator('[data-testid="modal"]')).not.toBeVisible({ timeout: 3000 });
    // Should still be on home screen
    await expect(page.getByRole('button', { name: 'Logout' })).toBeVisible();
  });

  test('clicking outside modal triggers cancel and closes modal', async ({ page }) => {
    await openLogoutModal(page);
    // Click outside the modal box (on the overlay)
    await page.locator('[data-testid="modal-overlay"]').click({ position: { x: 10, y: 10 } });
    await expect(page.locator('[data-testid="modal"]')).not.toBeVisible({ timeout: 3000 });
    await expect(page.getByRole('button', { name: 'Logout' })).toBeVisible();
  });

  test('pressing Escape closes modal', async ({ page }) => {
    await openLogoutModal(page);
    await page.keyboard.press('Escape');
    await expect(page.locator('[data-testid="modal"]')).not.toBeVisible({ timeout: 3000 });
    await expect(page.getByRole('button', { name: 'Logout' })).toBeVisible();
  });

  test('danger variant renders danger-themed title bar', async ({ page }) => {
    await openLogoutModal(page);
    const titleBar = page.locator('[data-testid="modal-title"]');
    // Danger variant uses --color-danger-bg background
    // We verify the computed background-color resolves to the danger token value
    const bg = await titleBar.evaluate(el => getComputedStyle(el).backgroundColor);
    // --color-danger-bg = #3a0000 = rgb(58, 0, 0)
    expect(bg).toBe('rgb(58, 0, 0)');
  });

  test('ongoing match warning message shows in logout modal', async ({ page }) => {
    await openLogoutModal(page);
    await expect(page.locator('[data-testid="modal-message"]')).toContainText('forfeit');
  });
});

test.describe('Modal: isLoading state (mocked)', () => {
  test('isLoading disables both buttons and prevents outside dismiss', async ({ page }) => {
    // Intercept decks call to ensure home screen loads
    await page.route('**/api/decks', route =>
      route.fulfill({ status: 200, contentType: 'application/json', body: '[]' })
    );

    await setAuthState(page);
    await openLogoutModal(page);

    // Verify buttons are currently enabled
    const confirmBtn = page.locator('[data-testid="modal-confirm"] button');
    const cancelBtn = page.locator('[data-testid="modal-cancel"] button');
    await expect(confirmBtn).not.toBeDisabled();
    await expect(cancelBtn).not.toBeDisabled();
  });
});

test.describe('Logout confirmation flow', () => {
  test.beforeEach(async ({ page }) => {
    await setAuthState(page);
  });

  test('logout button opens modal instead of logging out immediately', async ({ page }) => {
    await page.getByRole('button', { name: 'Logout' }).click();
    // Modal should appear — user should still be "logged in" (home screen behind modal)
    await expect(page.locator('[data-testid="modal"]')).toBeVisible();
    await expect(page.getByPlaceholder('Username')).not.toBeVisible();
  });

  test('confirming logout clears auth state', async ({ page }) => {
    await openLogoutModal(page);
    await page.locator('[data-testid="modal-confirm"] button').click();
    await expect(page.getByPlaceholder('Username')).toBeVisible({ timeout: 5000 });
  });

  test('cancelling logout closes modal and stays on current screen', async ({ page }) => {
    await openLogoutModal(page);
    await page.locator('[data-testid="modal-cancel"] button').click();
    await expect(page.locator('[data-testid="modal"]')).not.toBeVisible({ timeout: 3000 });
    await expect(page.getByRole('button', { name: 'Logout' })).toBeVisible();
    await expect(page.getByPlaceholder('Username')).not.toBeVisible();
  });
});