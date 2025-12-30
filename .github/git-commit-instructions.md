Generate commit messages following the **Conventional Commits** specification.

# Format
`<type>(<scope>): <short description>`

# Allowed Types
- **feat**: A new feature (e.g., new endpoint, new React component).
- **fix**: A bug fix.
- **docs**: Documentation only changes (README, JavaDoc).
- **style**: Formatting, semi-colons, white-space (no code change).
- **refactor**: Code change that neither fixes a bug nor adds a feature.
- **perf**: A code change that improves performance (especially for Timefold Solver).
- **test**: Adding or correcting tests.
- **chore**: Build process, dependencies (Maven, npm), Flyway migrations.

# Scopes (Optional)
Detect the scope based on the file path:
- **solver**: Changes in `com.dancescheduler.solver`.
- **backend**: Changes in Java files (excluding solver).
- **frontend**: Changes in React/TypeScript files.
- **db**: Changes in Flyway migrations (`db/migration`).
- **infra**: Docker, GitHub Actions, AWS config.

# Rules
1. **Language**: Always write in **English**.
2. **Imperative Mood**: Use "Add" instead of "Added", "Fix" instead of "Fixed".
3. **Length**: Keep the subject line under 72 characters.
4. **Body**: If the change is complex, generate a bulleted list in the body explaining *why* the change was made.