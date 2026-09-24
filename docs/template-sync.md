# Template synchronization

This repository includes a workflow for receiving updates from the template repository through a reviewed Pull Request. It uses Git's three-way merge; it does not copy template files over the module repository.

## Normal operation

`.github/workflows/sync-template.yml` runs every Monday at 04:17 UTC and can also be started from the Actions page with **Run workflow**. It fetches complete histories, merges the configured template ref, and asks `peter-evans/create-pull-request` to create or update the fixed `chore/template-sync` branch and Pull Request.

Review and merge the Pull Request instead of allowing the workflow to update the default branch directly. A merge commit keeps the template parent relationship for later synchronizations. Squash merging is functionally supported, but a later run may need to perform the delayed seed graft again.

The defaults are defined in the workflow environment:

```yaml
TEMPLATE_REPOSITORY: https://github.com/NKU100/zygisk-module-webui-template.git
TEMPLATE_REF: master
TEMPLATE_SYNC_BRANCH: chore/template-sync
```

Change these values when the template repository or its branch changes.

## First synchronization

Repositories created with **Use this template** do not need an immediate manual graft. The workflow compares the target repository's initial root tree with the template history. If it finds the matching template seed, it creates a temporary local replace ref and lets Git perform a normal three-way merge. The replace ref is removed after both successful and failed merges; a successful merge commit stores the real history relationship.

This also handles a repository that has accumulated target-only commits after creation. Those commits remain in the merge result and are not overwritten.

If the initial tree cannot be found in the template history, the workflow fails without changing the target branch. This usually means the initial snapshot or template history was rewritten, or that the repository was changed during creation. Preserve a backup branch, inspect the workflow log, and perform the first migration manually before enabling later automatic synchronization.

## Conflicts

When the template and module both modify the same file, the synchronization uses Git's normal conflict behavior. It does not select the template side automatically and does not silently exclude module files.

The workflow stops before creating a Pull Request when Git reports a conflict. To resolve it locally:

1. Start from a clean branch based on the module's default branch.
2. Run `scripts/template-sync.sh` with the same `TEMPLATE_REPOSITORY`, `TEMPLATE_REF`, and `TEMPLATE_REMOTE` values.
3. Resolve the conflict, run `git add` on the resolved files, and commit the merge.
4. Push the synchronization branch and open or update the Pull Request.

Do not use a blanket template-wins merge option; it would discard module-specific changes.

## Token permissions

The workflow falls back to `GITHUB_TOKEN`. For ordinary source-file updates, the workflow-level `contents: write` and `pull-requests: write` permissions are sufficient when the repository's Actions settings allow workflows to create Pull Requests.

Set the `TEMPLATE_SYNC_TOKEN` repository secret when the synchronization Pull Request must modify workflow files under `.github/workflows/`, or when repository policy does not grant the default token the required permissions. The replacement token must have write access to repository contents, Pull Requests, and workflow files according to its token type.

The script only fetches Git history and performs Git merges. It does not execute code from the template repository or import its secrets.
