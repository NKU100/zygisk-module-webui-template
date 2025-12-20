val moduleId: String by rootProject.extra

val installDependencyTask = tasks.register<Exec>("prepare") {
    group = "webui"
    if (org.apache.tools.ant.taskdefs.condition.Os.isFamily(org.apache.tools.ant.taskdefs.condition.Os.FAMILY_WINDOWS)) {
        commandLine("cmd", "/c", "npm", "install")
    } else {
        commandLine("npm", "install")
    }
}

val buildWebUITask = tasks.register<Exec>("build") {
    group = "webui"
    dependsOn(installDependencyTask)
    if (org.apache.tools.ant.taskdefs.condition.Os.isFamily(org.apache.tools.ant.taskdefs.condition.Os.FAMILY_WINDOWS)) {
        commandLine("cmd", "/c", "npm", "run", "build")
    } else {
        commandLine("npm", "run", "build")
    }
}

val pushWebUITask = tasks.register<Exec>("push") {
    group = "webui"
    dependsOn(buildWebUITask)
    commandLine("adb", "push", "dist", "/data/local/tmp")
}

val removeWebUITask = tasks.register<Exec>("remove") {
    group = "webui"
    dependsOn(pushWebUITask)
    commandLine("adb", "shell", "su", "-c", "rm -rf /data/adb/modules/$moduleId/webroot")
}

tasks.register<Exec>("install") {
    group = "webui"
    dependsOn(removeWebUITask)
    commandLine("adb", "shell", "su", "-c", "mv /data/local/tmp/dist /data/adb/modules/$moduleId/webroot")
}