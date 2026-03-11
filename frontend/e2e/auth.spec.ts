import { test, expect, type Page } from '@playwright/test';

// Unique suffix per test run to avoid username collisions across runs
const uid = () => Date.now().toString(36) + Math.random().toString(36).slice(2, 5);

async function goToRegisterTab(page: Page) {
  await page.getByRole('button', { name: 'Register' }).first().click();
}

test.describe('Auth flows', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/');
  });

  test('register new user shows welcome screen', async ({ page }) => {
    const username = `user_${uid()}`;

    await goToRegisterTab(page);
    await page.getByPlaceholder('Username').fill(username);
    await page.getByPlaceholder('Password').fill('password123');
    await page.getByRole('button', { name: 'Register' }).last().click();

    await expect(page.getByText(`Welcome, ${username}!`)).toBeVisible();
  });

  test('register duplicate username shows error', async ({ page }) => {
    const username = `user_${uid()}`;

    // First registration
    await goToRegisterTab(page);
    await page.getByPlaceholder('Username').fill(username);
    await page.getByPlaceholder('Password').fill('password123');
    await page.getByRole('button', { name: 'Register' }).last().click();
    await expect(page.getByText(`Welcome, ${username}!`)).toBeVisible();

    // Logout and try registering the same username again
    await page.getByRole('button', { name: 'Logout' }).click();
    await goToRegisterTab(page);
    await page.getByPlaceholder('Username').fill(username);
    await page.getByPlaceholder('Password').fill('password123');
    await page.getByRole('button', { name: 'Register' }).last().click();

    await expect(page.getByText('Username already taken')).toBeVisible();
  });

  test('login with valid credentials shows welcome screen', async ({ page }) => {
    const username = `user_${uid()}`;

    // Register first
    await goToRegisterTab(page);
    await page.getByPlaceholder('Username').fill(username);
    await page.getByPlaceholder('Password').fill('mypassword');
    await page.getByRole('button', { name: 'Register' }).last().click();
    await expect(page.getByText(`Welcome, ${username}!`)).toBeVisible();

    // Logout and login
    await page.getByRole('button', { name: 'Logout' }).click();
    await page.getByPlaceholder('Username').fill(username);
    await page.getByPlaceholder('Password').fill('mypassword');
    await page.getByRole('button', { name: 'Login' }).last().click();

    await expect(page.getByText(`Welcome, ${username}!`)).toBeVisible();
  });

  test('login with wrong password shows error', async ({ page }) => {
    const username = `user_${uid()}`;

    // Register first
    await goToRegisterTab(page);
    await page.getByPlaceholder('Username').fill(username);
    await page.getByPlaceholder('Password').fill('correct');
    await page.getByRole('button', { name: 'Register' }).last().click();
    await expect(page.getByText(`Welcome, ${username}!`)).toBeVisible();

    // Logout and login with wrong password
    await page.getByRole('button', { name: 'Logout' }).click();
    await page.getByPlaceholder('Username').fill(username);
    await page.getByPlaceholder('Password').fill('wrong');
    await page.getByRole('button', { name: 'Login' }).last().click();

    await expect(page.getByText('Invalid credentials')).toBeVisible();
  });

  test('logout returns to auth form', async ({ page }) => {
    const username = `user_${uid()}`;

    await goToRegisterTab(page);
    await page.getByPlaceholder('Username').fill(username);
    await page.getByPlaceholder('Password').fill('password123');
    await page.getByRole('button', { name: 'Register' }).last().click();
    await expect(page.getByText(`Welcome, ${username}!`)).toBeVisible();

    await page.getByRole('button', { name: 'Logout' }).click();

    await expect(page.getByRole('heading', { name: 'F2CG' })).toBeVisible();
    await expect(page.getByPlaceholder('Username')).toBeVisible();
  });
});