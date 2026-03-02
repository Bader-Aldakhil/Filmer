import { expect, test } from '@playwright/test';

test.describe('Filmer frontend e2e', () => {
  test('smoke: app loads and route navigation works', async ({ page }) => {
    await page.goto('/');

    await expect(page.getByRole('heading', { name: 'Filmer - Movie Rental System' })).toBeVisible();
    await expect(page.getByRole('heading', { name: 'Welcome to Filmer' })).toBeVisible();

    await page.getByRole('link', { name: 'Connection Test' }).click();
    await expect(page).toHaveURL(/\/connection-test$/);
    await expect(page.getByRole('heading', { name: 'End-to-End Connectivity Test' })).toBeVisible();

    await page.getByRole('link', { name: 'Home' }).click();
    await expect(page).toHaveURL('/');
  });

  test('browse/search flow equivalent: from home to details-like connectivity result', async ({ page }) => {
    await page.goto('/');

    await page.getByRole('button', { name: 'Test Connection' }).click();
    await expect(page).toHaveURL(/\/connection-test$/);

    await page.route('**/api/v1/health/db', async route => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          success: true,
          data: {
            database_status: 'UP',
            result: 1,
            message: 'Database connection successful'
          }
        })
      });
    });

    await page.getByRole('button', { name: 'Test Connection' }).click();

    await expect(page.getByRole('heading', { name: 'Connection Successful!' })).toBeVisible();
    await expect(page.getByText('Database Status:')).toBeVisible();
    await expect(page.locator('.status-grid .status-value').first()).toHaveText('UP');
  });

  test('negative flow: API failure shows error UI', async ({ page }) => {
    await page.goto('/connection-test');

    await page.route('**/api/v1/health/db', async route => {
      await route.fulfill({
        status: 503,
        contentType: 'application/json',
        body: JSON.stringify({
          success: false,
          error: {
            code: 'DB_CONNECTION_ERROR',
            message: 'Database connection failed'
          }
        })
      });
    });

    await page.getByRole('button', { name: 'Test Connection' }).click();

    await expect(page.getByRole('heading', { name: 'Connection Failed' })).toBeVisible();
    await expect(page.getByText('Database connection failed. Is PostgreSQL running?')).toBeVisible();
  });
});
