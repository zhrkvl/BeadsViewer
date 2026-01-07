# Beads Viewer - IntelliJ Plugin

An IntelliJ Platform plugin for **Beads**, a git-backed issue tracker designed for AI-supervised coding workflows.

## About Beads

Beads is a distributed, git-integrated issue tracking system that stores issues as JSONL in a `.beads/` directory within your repository. This plugin provides IntelliJ IDE integration to explore and manage your project's Beads issues.

## Plugin Features (In Development)

- Browse issues from your Beads repository
- Visualize issue dependencies and relationships
- Quick status and priority overview
- Integration with IntelliJ's project structure

## Project Structure

### references/

This directory contains example Beads projects for reference and development:

- **`references/beads/`** - The Beads project itself (~980 issues)
  - A real-world example of a large project using Beads
  - Demonstrates the `.beads/` directory structure and data format
  - Contains issues with dependencies, comments, and various statuses

- **`references/MeshCoreQt/`** - MeshCoreQt project (~134 issues)
  - Another example project showing typical Beads usage
  - Smaller scale, useful for understanding basic patterns
  - Reference for plugin testing and UI development

Both directories include `.beads/issues.jsonl` files with real issue data in JSONL format (one JSON object per line).

### Beads Data Format

Issues are stored in `.beads/issues.jsonl`:

```json
{
  "id": "bd-00u3",
  "title": "Issue title here",
  "description": "Detailed description",
  "status": "open",
  "priority": 2,
  "issue_type": "task",
  "created_at": "2025-12-25T00:59:03.667211-08:00",
  "dependencies": [...],
  "comments": [...]
}
```

## Development

### Build the Plugin
```bash
./gradlew build
```

### Run the Plugin (Test IDE)
```bash
./gradlew runIde
```

### Run Tests
```bash
./gradlew test
```

## Plugin Information

- **Vendor:** Zakhar Kaval
- **Compatibility:** IntelliJ IDEA 2025.3+
- **Language:** Kotlin + Jetpack Compose
- **UI Framework:** JetBrains Jewel

## Links

- [IntelliJ Platform SDK](https://plugins.jetbrains.com/docs/intellij/)
- [JetBrains Marketplace](https://plugins.jetbrains.com)
