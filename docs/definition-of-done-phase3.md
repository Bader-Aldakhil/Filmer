# Phase 3 Definition of Done

A feature/PR is considered done only if all items below are complete:

1. Automated tests
- Frontend unit tests pass.
- Frontend E2E tests pass.
- Backend unit tests pass.
- Backend integration tests pass, including stress/robustness scenarios.

2. CI quality gate
- CI runs automatically on pull requests.
- All required checks are green before merge.

3. Robustness and stress
- Concurrent-request stress test is included and passing.
- Failure simulation tests (DB failure/timeout-like behavior via mocks) are included and passing.

4. Security and secrets hygiene
- No secrets/API keys are committed in the repository.
- Configuration uses environment variables and safe defaults.

5. PR governance
- PR has clear summary and validation evidence.
- Minimum 2 approvals are collected before merge.
- Merge blocked unless CI and approvals conditions are met.
