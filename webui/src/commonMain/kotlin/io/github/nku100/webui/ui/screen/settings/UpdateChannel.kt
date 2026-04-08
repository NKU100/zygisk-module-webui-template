package io.github.nku100.webui.ui.screen.settings

/**
 * Update channel that controls which update.json URL is written into module.prop.
 * The module manager (Magisk / KernelSU) reads `updateJson` from module.prop
 * to check for updates, so switching channels is simply rewriting that field.
 */
enum class UpdateChannel {
    STABLE,
    BETA,
}
