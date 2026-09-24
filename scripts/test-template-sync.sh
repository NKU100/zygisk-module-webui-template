#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd -P)
SYNC_SCRIPT="$SCRIPT_DIR/template-sync.sh"
TEST_ROOT=$(mktemp -d "${TMPDIR:-/tmp}/template-sync-test.XXXXXX")

cleanup() {
  rm -rf "$TEST_ROOT"
}
trap cleanup EXIT

fail() {
  echo "FAIL: $*" >&2
  exit 1
}

assert_equal() {
  local expected=$1
  local actual=$2
  local message=$3
  if [[ "$expected" != "$actual" ]]; then
    fail "$message: expected '$expected', got '$actual'"
  fi
}

assert_file() {
  local path=$1
  [[ -f "$path" ]] || fail "expected file: $path"
}

assert_no_file() {
  local path=$1
  [[ ! -e "$path" ]] || fail "did not expect file: $path"
}

init_repo() {
  local repo=$1
  git init -q -b master "$repo"
  git -C "$repo" config user.name "Template Sync Test"
  git -C "$repo" config user.email "template-sync-test@example.invalid"
}

commit_all() {
  local repo=$1
  local message=$2
  git -C "$repo" add --all
  git -C "$repo" commit -qm "$message"
}

run_sync() {
  local repo=$1
  (
    cd "$repo"
    TEMPLATE_REPOSITORY="$TEMPLATE_REPOSITORY" \
      TEMPLATE_REF=master \
      TEMPLATE_REMOTE=template \
      bash "$SYNC_SCRIPT"
  )
}

TEMPLATE_REPOSITORY="$TEST_ROOT/template"
TARGET_REPOSITORY="$TEST_ROOT/target"
CONFLICT_REPOSITORY="$TEST_ROOT/conflict"
MISSING_SEED_REPOSITORY="$TEST_ROOT/missing-seed"

init_repo "$TEMPLATE_REPOSITORY"
printf 'seed\n' > "$TEMPLATE_REPOSITORY/shared.txt"
printf 'remove me\n' > "$TEMPLATE_REPOSITORY/deleted.txt"
printf 'rename me\n' > "$TEMPLATE_REPOSITORY/rename-old.txt"
commit_all "$TEMPLATE_REPOSITORY" "template: create seed"
TEMPLATE_SEED=$(git -C "$TEMPLATE_REPOSITORY" rev-parse HEAD)

mkdir "$TARGET_REPOSITORY"
git -C "$TEMPLATE_REPOSITORY" archive "$TEMPLATE_SEED" | tar -x -C "$TARGET_REPOSITORY"
init_repo "$TARGET_REPOSITORY"
commit_all "$TARGET_REPOSITORY" "module: import template snapshot"
printf 'target only\n' > "$TARGET_REPOSITORY/target-only.txt"
commit_all "$TARGET_REPOSITORY" "module: add target file"
printf 'target history\n' > "$TARGET_REPOSITORY/target-history.txt"
commit_all "$TARGET_REPOSITORY" "module: add another target commit"
printf 'target history\r\n' > "$TARGET_REPOSITORY/target-history.txt"

rm "$TEMPLATE_REPOSITORY/deleted.txt"
mv "$TEMPLATE_REPOSITORY/rename-old.txt" "$TEMPLATE_REPOSITORY/rename-new.txt"
printf 'template update\n' >> "$TEMPLATE_REPOSITORY/shared.txt"
printf 'template only\n' > "$TEMPLATE_REPOSITORY/template-only.txt"
commit_all "$TEMPLATE_REPOSITORY" "template: update structure"
TEMPLATE_HEAD=$(git -C "$TEMPLATE_REPOSITORY" rev-parse HEAD)

run_sync "$TARGET_REPOSITORY"
assert_equal "$TEMPLATE_HEAD" "$(git -C "$TARGET_REPOSITORY" merge-base HEAD template/master)" "delayed graft merge base"
assert_file "$TARGET_REPOSITORY/target-only.txt"
assert_file "$TARGET_REPOSITORY/target-history.txt"
assert_file "$TARGET_REPOSITORY/rename-new.txt"
assert_no_file "$TARGET_REPOSITORY/deleted.txt"
assert_file "$TARGET_REPOSITORY/template-only.txt"
assert_equal "" "$(git -C "$TARGET_REPOSITORY" replace -l)" "replace refs after delayed graft"

printf 'follow-up\n' > "$TEMPLATE_REPOSITORY/template-follow-up.txt"
commit_all "$TEMPLATE_REPOSITORY" "template: add follow-up file"
TEMPLATE_HEAD=$(git -C "$TEMPLATE_REPOSITORY" rev-parse HEAD)
run_sync "$TARGET_REPOSITORY"
assert_equal "$TEMPLATE_HEAD" "$(git -C "$TARGET_REPOSITORY" merge-base HEAD template/master)" "ordinary follow-up merge base"
assert_file "$TARGET_REPOSITORY/template-follow-up.txt"
assert_file "$TARGET_REPOSITORY/target-only.txt"
assert_equal "" "$(git -C "$TARGET_REPOSITORY" replace -l)" "replace refs after ordinary merge"

TARGET_HEAD_BEFORE_NO_CHANGE=$(git -C "$TARGET_REPOSITORY" rev-parse HEAD)
run_sync "$TARGET_REPOSITORY"
assert_equal "$TARGET_HEAD_BEFORE_NO_CHANGE" "$(git -C "$TARGET_REPOSITORY" rev-parse HEAD)" "no-change synchronization"

printf 'template conflict\n' > "$TEMPLATE_REPOSITORY/shared.txt"
commit_all "$TEMPLATE_REPOSITORY" "template: change shared file"

mkdir "$CONFLICT_REPOSITORY"
git -C "$TEMPLATE_REPOSITORY" archive "$TEMPLATE_SEED" | tar -x -C "$CONFLICT_REPOSITORY"
init_repo "$CONFLICT_REPOSITORY"
commit_all "$CONFLICT_REPOSITORY" "module: import template snapshot"
printf 'target conflict\n' > "$CONFLICT_REPOSITORY/shared.txt"
commit_all "$CONFLICT_REPOSITORY" "module: change shared file"

if run_sync "$CONFLICT_REPOSITORY" > "$TEST_ROOT/conflict.log" 2>&1; then
  fail "expected synchronization conflict"
fi
git -C "$CONFLICT_REPOSITORY" status --short | grep -q '^UU shared.txt$' \
  || fail "expected shared.txt to remain conflicted"
grep -q '^<<<<<<< ' "$CONFLICT_REPOSITORY/shared.txt" \
  || fail "expected conflict markers in shared.txt"
assert_equal "" "$(git -C "$CONFLICT_REPOSITORY" replace -l)" "replace refs after conflict"

init_repo "$MISSING_SEED_REPOSITORY"
printf 'unrelated\n' > "$MISSING_SEED_REPOSITORY/unrelated.txt"
commit_all "$MISSING_SEED_REPOSITORY" "module: create unrelated history"
MISSING_SEED_HEAD=$(git -C "$MISSING_SEED_REPOSITORY" rev-parse HEAD)
if run_sync "$MISSING_SEED_REPOSITORY" > "$TEST_ROOT/missing-seed.log" 2>&1; then
  fail "expected missing seed synchronization to fail"
fi
assert_equal "$MISSING_SEED_HEAD" "$(git -C "$MISSING_SEED_REPOSITORY" rev-parse HEAD)" "missing seed keeps HEAD"
assert_equal "" "$(git -C "$MISSING_SEED_REPOSITORY" replace -l)" "replace refs after missing seed"

echo "template sync integration tests passed"
