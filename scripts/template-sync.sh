#!/usr/bin/env bash
set -euo pipefail

repository_root=$(git rev-parse --show-toplevel)
cd "$repository_root"

template_repository=${TEMPLATE_REPOSITORY:-https://github.com/NKU100/zygisk-module-webui-template.git}
template_ref=${TEMPLATE_REF:-master}
template_remote=${TEMPLATE_REMOTE:-template}

fail() {
  echo "template-sync: $*" >&2
  exit 1
}

[[ -z "$(git status --porcelain)" ]] \
  || fail "working tree is not clean; commit or stash local changes before synchronizing"

if ! git remote get-url "$template_remote" >/dev/null 2>&1; then
  git remote add "$template_remote" "$template_repository"
fi

remote_ref=$template_ref
case "$remote_ref" in
  refs/heads/*)
    remote_ref=${remote_ref#refs/heads/}
    ;;
esac

git fetch --no-tags "$template_remote" \
  "+refs/heads/$remote_ref:refs/remotes/$template_remote/$remote_ref" \
  || fail "could not fetch template ref '$template_ref' from '$template_repository'"

template_commit_ref="$template_remote/$remote_ref"
git rev-parse --verify "$template_commit_ref^{commit}" >/dev/null 2>&1 \
  || fail "template ref '$template_ref' was not found"

target_tree=$(git rev-parse HEAD^{tree})
template_tree=$(git rev-parse "$template_commit_ref^{tree}")
if [[ "$target_tree" == "$template_tree" ]]; then
  echo "template-sync: no content changes"
  exit 0
fi

if git merge-base HEAD "$template_commit_ref" >/dev/null 2>&1; then
  git merge --no-edit "$template_commit_ref"
  exit $?
fi

target_root=$(git rev-list --max-parents=0 --reverse HEAD | tail -n 1)
target_root_tree=$(git show -s --format=%T "$target_root")
template_seed=

while IFS= read -r commit; do
  commit_tree=$(git show -s --format=%T "$commit")
  if [[ "$commit_tree" == "$target_root_tree" ]]; then
    template_seed=$commit
  fi
done < <(git rev-list --reverse "$template_commit_ref")

[[ -n "$template_seed" ]] \
  || fail "could not find a template seed with tree $target_root_tree; the initial snapshot may have changed"

echo "template-sync: using seed $template_seed for target root $target_root"
git replace --graft "$target_root" "$template_seed"

cleanup_replace_ref() {
  git replace -d "$target_root" >/dev/null 2>&1 || true
}
trap cleanup_replace_ref EXIT

git merge --no-edit "$template_commit_ref"
