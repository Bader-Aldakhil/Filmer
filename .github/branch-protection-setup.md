# Branch Protection Setup (Required)

These settings must be configured in GitHub repository settings for `main` (or `master`).

## Required checks before merge
1. Enable branch protection rule for the default branch.
2. Enable `Require status checks to pass before merging`.
3. Mark these checks as required:
   - `frontend-tests`
   - `frontend-e2e`
   - `backend-tests`
   - `backend-integration-stress-robustness`
   - `secret-scan`
   - `approval-gate`

## Required approvals
1. Enable `Require a pull request before merging`.
2. Enable `Require approvals` and set required approvals to `2`.
3. Enable `Dismiss stale pull request approvals when new commits are pushed`.

## Recommended hardening
- Enable `Require conversation resolution before merging`.
- Enable `Restrict who can push to matching branches`.
- Disable force pushes.
