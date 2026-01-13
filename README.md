# Beads Viewer - IntelliJ Plugin

Please note, this repo is in the early development state. The plugin can still be used to display in multiple views or querying with filters, but I give no guarantees. Also, if you find it useful, you can write me some feedback `ellesterate@gmail.com`. 

An IntelliJ Platform plugin for **Beads**, a git-backed issue tracker designed for AI-supervised coding workflows.

## About Beads

Beads is a distributed, git-integrated issue tracking system that stores issues as JSONL in a `.beads/` directory within your repository. This plugin provides IntelliJ IDE integration to explore and manage your project's Beads issues.

## Plugin Features (In Development)

- Browse issues from your Beads repository
- Visualize issue dependencies and relationships
- Quick status and priority overview
- Integration with IntelliJ's project structure
- **Real-time updates**: Automatically reflects changes from `bd` CLI commands
- **"Local" badges**: Visual indicators for issues with unsaved changes

## How It Works

### Data Loading Strategy

The plugin uses a **SQLite-first loading strategy** with automatic fallback to ensure reliability:

1. **Primary Source: SQLite Database** (`.beads/beads.db`)
   - Fast querying and real-time updates
   - Shows latest changes from `bd` CLI commands **before** they're synced to git
   - Tracks "dirty" issues (modified but not yet exported to JSONL)

2. **Fallback Source: JSONL File** (`.beads/issues.jsonl`)
   - Used if SQLite database is unavailable, corrupted, or locked
   - Guaranteed to work even if database has issues
   - Represents the git-committed state of your issues

### "Local" Badge Indicator

Issues with unsaved changes display an amber **"Local"** badge, indicating:
- The issue was modified via `bd` CLI commands
- Changes are in the SQLite database but not yet exported to `issues.jsonl`
- You haven't run `bd sync` yet to commit these changes to git

This helps you track which issues have uncommitted modifications.

### Real-Time File Watching

The plugin watches for changes to:
- `.beads/beads.db` - SQLite database (100ms debounce for fast updates)
- `.beads/beads.db-wal` - Write-Ahead Log file (indicates database writes)
- `.beads/issues.jsonl` - JSONL file (300ms debounce for manual edits)

Changes are automatically reflected in the UI without manual refresh.

### Edge Case Handling

The plugin gracefully handles:
- **Missing database**: Automatically falls back to JSONL
- **Database locked** by another process: 5-second timeout, then fallback to JSONL
- **Legacy databases**: Works with older databases missing optional tables
- **Empty projects**: Shows friendly "No issues found" message
- **Concurrent access**: Multiple processes can read the database simultaneously

### Troubleshooting

**Q: Why do some issues show a "Local" badge?**
A: These issues have been modified via `bd` CLI but not yet synced to git. Run `bd sync` to export them to `issues.jsonl` and commit.

**Q: Plugin shows "Failed to load issues" error**
A: Check that your project has a `.beads/` directory with either `beads.db` or `issues.jsonl`. If both fail to load, check the IntelliJ logs for details.

**Q: Changes from `bd` CLI don't appear immediately**
A: The plugin watches for file changes with a short debounce (100-300ms). If issues persist, click the refresh button in the toolbar.

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
