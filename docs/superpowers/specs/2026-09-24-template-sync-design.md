# 模板仓库同步设计

状态：已确认

## 目标

让通过模板创建的模块仓库能够在模板更新后，通过 Pull Request 接收更新，同时保留 Git 的三方合并能力。

同步必须支持两种目标仓库：

- 使用 `Use this template` 创建、与模板没有共同历史的仓库；
- 通过 clone 或后续同步已经建立共同历史的仓库。

同步不得直接覆盖目标仓库的默认分支，也不得使用模板优先策略静默覆盖模块代码。

## 核心方案

模板仓库提供一个同步 workflow 和一个 Git 同步脚本。

```text
定时或手动触发
        ↓
完整获取目标仓库和模板历史
        ↓
检查是否已有共同祖先
        ├─ 有：普通 git merge
        └─ 无：寻找模板 seed，临时 graft 后 merge
        ↓
推送同步分支
        ↓
创建或更新 Pull Request
```

同步使用 Git 的三方合并，不复制文件。模板新增、删除、重命名和目标仓库的独立提交由 Git 处理；同一文件两边都修改时保留冲突，让 Pull Request 进入人工处理流程。

## Seed 检测与 graft

目标仓库的 seed 是其初始根提交。脚本执行以下步骤：

1. 获取目标仓库默认分支的完整历史。
2. 获取模板仓库指定分支的完整历史。
3. 先执行 `git merge-base`。如果已经存在共同祖先，直接进入普通 merge。
4. 如果没有共同祖先，读取目标根提交的 tree 对象。
5. 在模板历史中查找 tree 完全相同的提交作为模板 seed。
6. 找到 seed 后执行：

   ```bash
   git replace --graft <target-root> <template-seed>
   git merge --no-edit <template-ref>
   ```

7. 无论 merge 成功或失败，都删除本地 replace ref。成功的 merge commit 会永久保存真实的父提交关系。

如果找不到 tree 相同的模板提交，脚本失败并说明可能原因，不执行无共同祖先的强制 merge。目标根提交被改写、模板历史被重写，或模板创建后自动修改了文件时，需要人工完成一次迁移。

如果模板自 seed 以来没有内容变化，merge 可能没有可提交的内容；这种情况下保留现状，下一次模板产生实际内容变化时再次 graft。不能提交只有历史关系、但对目标分支没有内容差异的空 PR。

## Workflow

新增 `.github/workflows/sync-template.yml`，支持：

- 每周一次的 `schedule`，避开整点；
- `workflow_dispatch` 手动触发；
- `fetch-depth: 0`，确保 seed 和 merge-base 可用；
- `contents: write` 与 `pull-requests: write`；
- 固定的同步分支前缀，例如 `chore/template-sync`；
- 无差异时不创建 PR；
- 已有同步 PR 时更新原分支，避免重复 PR；
- merge 冲突时失败并输出处理提示，不自动选择任一侧。

模板仓库地址和分支写入 workflow 的默认值，同时允许通过环境变量或 workflow 配置覆盖，以便未来迁移模板仓库。

PR 创建使用成熟的 PR Action，Git 合并逻辑由仓库内脚本负责。这样可以独立验证核心同步算法，不把模板内容合并行为交给黑盒 Action。

## 权限与安全

普通 `GITHUB_TOKEN` 可以读取公开模板并创建目标仓库分支和 PR，但修改 `.github/workflows` 中已有文件通常还需要额外的 workflow 写权限。

Workflow 支持优先使用使用者仓库中的 `TEMPLATE_SYNC_TOKEN`，未配置时回退到 `GITHUB_TOKEN`。文档说明：如果希望自动同步模板 workflow，使用者必须配置具有目标仓库内容写权限和 workflow 写权限的 GitHub App 或 Token。

同步脚本只对当前 checkout 和模板公开地址执行 Git 操作，不执行模板仓库中的脚本，不把模板仓库的 secrets 带入目标仓库。

## 文件范围

首版新增：

- `scripts/template-sync.sh`：seed 检测、graft、merge 和错误处理；
- `.github/workflows/sync-template.yml`：定时触发、权限、checkout 和 PR 编排；
- `docs/template-sync.md`：使用者配置 Token、手动运行、冲突处理和首次同步说明。

不新增 `.templatesyncignore`。首版依赖 Git 的三方合并保留模块专属文件；如果模板和模块同时修改同一文件，必须显式解决冲突，而不是通过静默排除规则隐藏差异。

## 验证

至少验证以下场景：

1. 模板提交更新，目标仓库没有本地改动，生成同步 PR。
2. 目标仓库在模板 seed 后有多次独立提交，延迟执行首次 graft 仍能合并。
3. 模板修改、删除、重命名文件，普通 merge 正确反映这些变化。
4. 模板和目标仓库修改同一文件，workflow 失败并留下可人工处理的冲突信息，不覆盖目标内容。
5. 首次同步完成后，模板再次更新可以直接普通 merge。
6. 没有模板差异时不创建空 PR。
7. 修改模板 workflow 时，使用 `TEMPLATE_SYNC_TOKEN` 可以生成包含 workflow 变化的 PR。

当前仓库已有未提交的其他设计文档和工作区改动不属于本功能，不纳入本次提交。
