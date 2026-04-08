/* Copyright 2022-2023 John "topjohnwu" Wu
 *
 * Permission to use, copy, modify, and/or distribute this software for any
 * purpose with or without fee is hereby granted.
 *
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES WITH
 * REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF MERCHANTABILITY
 * AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY SPECIAL, DIRECT,
 * INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING FROM
 * LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR
 * OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR
 * PERFORMANCE OF THIS SOFTWARE.
 */

/* Zygisk Module WebUI Template — example implementation
 *
 * Reads config.json written by the WebUI and logs according to per-package settings.
 *
 * Architecture note:
 *   preAppSpecialize() runs inside the app process (app UID), which cannot write to
 *   /data/adb/. Log lines are therefore sent to the companion process (root) via IPC,
 *   and the companion appends them to module.log.
 *
 * Companion protocol (uint8_t opcode first):
 *   OP_READ_CONFIG (0): companion replies with uint32_t len + config bytes.
 *   OP_WRITE_LOG   (1): module sends uint32_t len + log line; companion returns nothing.
 */

#include <unistd.h>
#include <fcntl.h>
#include <android/log.h>
#include <string>
#include <cstdio>
#include <ctime>
#include <sys/stat.h>

#include "zygisk.hpp"
#include "yyjson.h"

using zygisk::Api;
using zygisk::AppSpecializeArgs;
using zygisk::ServerSpecializeArgs;

#define LOG_TAG    "ZygiskWebUI"
#define MODULE_ID  "zygisk_sample"
#define CONFIG_PATH "/data/adb/modules/" MODULE_ID "/config.json"
#define LOG_PATH    "/data/adb/modules/" MODULE_ID "/module.log"

// IPC opcodes
static constexpr uint8_t OP_READ_CONFIG = 0;
static constexpr uint8_t OP_WRITE_LOG   = 1;

// Maximum log file size: rotate when exceeded
static constexpr off_t LOG_MAX_BYTES  = 512 * 1024;
static constexpr off_t LOG_TRIM_BYTES = 256 * 1024;

// ── Companion-side helpers (run as root) ────────────────────────────────────

static void companion_appendLog(const std::string &line) {
    int fd = open(LOG_PATH, O_WRONLY | O_CREAT | O_APPEND, 0644);
    if (fd < 0) return;

    struct stat st{};
    if (fstat(fd, &st) == 0 && st.st_size > LOG_MAX_BYTES) {
        // Keep the newest LOG_TRIM_BYTES
        char *buf = new char[LOG_TRIM_BYTES];
        ssize_t n = pread(fd, buf, LOG_TRIM_BYTES, st.st_size - LOG_TRIM_BYTES);
        if (n > 0) {
            ftruncate(fd, 0);
            lseek(fd, 0, SEEK_SET);
            write(fd, buf, n);
        }
        delete[] buf;
    }

    write(fd, line.data(), line.size());
    close(fd);
}

static void companion_handler(int sock) {
    uint8_t op = OP_READ_CONFIG;
    if (read(sock, &op, 1) != 1) return;

    if (op == OP_READ_CONFIG) {
        std::string config;
        int cfd = open(CONFIG_PATH, O_RDONLY);
        if (cfd >= 0) {
            char buf[4096];
            ssize_t n;
            while ((n = read(cfd, buf, sizeof(buf))) > 0)
                config.append(buf, n);
            close(cfd);
        }
        uint32_t len = (uint32_t)config.size();
        write(sock, &len, sizeof(len));
        if (len > 0) write(sock, config.data(), len);

    } else if (op == OP_WRITE_LOG) {
        uint32_t len = 0;
        if (read(sock, &len, sizeof(len)) != sizeof(len) || len == 0 || len > 65536) return;
        std::string line(len, '\0');
        if (read(sock, &line[0], len) == (ssize_t)len) {
            companion_appendLog(line);
        }
    }
}

// ── Module-side helpers (run in app process) ────────────────────────────────

static char levelChar(int prio) {
    switch (prio) {
        case ANDROID_LOG_VERBOSE: return 'V';
        case ANDROID_LOG_DEBUG:   return 'D';
        case ANDROID_LOG_WARN:    return 'W';
        case ANDROID_LOG_ERROR:   return 'E';
        default:                  return 'I';
    }
}

/**
 * Build a logcat-time-format line and send it to the companion for writing.
 * Also calls __android_log_print so logcat still works normally.
 */
static void remoteLog(Api *api, int prio, const char *tag, const char *msg) {
    // Always log to logcat (works fine from app context)
    __android_log_print(prio, tag, "%s", msg);

    // Build formatted line: "MM-DD HH:MM:SS.mmm  PID  TID  L TAG: msg\n"
    struct timespec ts{};
    clock_gettime(CLOCK_REALTIME, &ts);
    struct tm tm{};
    localtime_r(&ts.tv_sec, &tm);
    int ms = (int)(ts.tv_nsec / 1000000);

    char line[4096];
    int len = snprintf(line, sizeof(line),
        "%02d-%02d %02d:%02d:%02d.%03d %5d %5d %c %-16s: %s\n",
        tm.tm_mon + 1, tm.tm_mday,
        tm.tm_hour, tm.tm_min, tm.tm_sec, ms,
        (int)getpid(), (int)gettid(),
        levelChar(prio), tag, msg);
    if (len <= 0) return;

    // Send to companion (root) via IPC
    int sock = api->connectCompanion();
    if (sock < 0) return;

    uint8_t op = OP_WRITE_LOG;
    uint32_t msgLen = (uint32_t)len;
    write(sock, &op, 1);
    write(sock, &msgLen, sizeof(msgLen));
    write(sock, line, len);
    close(sock);
}

// ── Module class ─────────────────────────────────────────────────────────────

class MyModule : public zygisk::ModuleBase {
public:
    void onLoad(Api *api, JNIEnv *env) override {
        this->api = api;
        this->env = env;
    }

    void preAppSpecialize(AppSpecializeArgs *args) override {
        const char *process = env->GetStringUTFChars(args->nice_name, nullptr);
        preSpecialize(process);
        env->ReleaseStringUTFChars(args->nice_name, process);
    }

    void preServerSpecialize(ServerSpecializeArgs *args) override {
        preSpecialize("system_server");
    }

private:
    Api *api;
    JNIEnv *env;

    void preSpecialize(const char *process) {
        // Read config from companion (OP_READ_CONFIG)
        std::string configJson;
        int sock = api->connectCompanion();
        if (sock >= 0) {
            uint8_t op = OP_READ_CONFIG;
            write(sock, &op, 1);
            uint32_t len = 0;
            if (read(sock, &len, sizeof(len)) == sizeof(len) && len > 0) {
                configJson.resize(len);
                read(sock, &configJson[0], len);
            }
            close(sock);
        }

        if (configJson.empty()) {
            api->setOption(zygisk::Option::DLCLOSE_MODULE_LIBRARY);
            return;
        }

        yyjson_doc *doc = yyjson_read(configJson.data(), configJson.size(), 0);
        if (!doc) {
            api->setOption(zygisk::Option::DLCLOSE_MODULE_LIBRARY);
            return;
        }
        yyjson_val *root = yyjson_doc_get_root(doc);

        // Check enabled flag
        yyjson_val *enabledVal = yyjson_obj_get(root, "enabled");
        if (enabledVal && yyjson_is_bool(enabledVal) && !yyjson_get_bool(enabledVal)) {
            yyjson_doc_free(doc);
            api->setOption(zygisk::Option::DLCLOSE_MODULE_LIBRARY);
            return;
        }

        // Check targetPackages
        bool targeted = false;
        yyjson_val *targets = yyjson_obj_get(root, "targetPackages");
        if (yyjson_is_arr(targets)) {
            size_t idx, max;
            yyjson_val *pkg;
            yyjson_arr_foreach(targets, idx, max, pkg) {
                if (yyjson_is_str(pkg) &&
                    strcmp(yyjson_get_str(pkg), process) == 0) {
                    targeted = true;
                    break;
                }
            }
        }
        if (!targeted) {
            yyjson_doc_free(doc);
            api->setOption(zygisk::Option::DLCLOSE_MODULE_LIBRARY);
            return;
        }

        // Per-package settings
        const char *logLevel = "INFO";
        const char *logTag   = process;
        bool dumpStack       = false;

        yyjson_val *pkgSettings = yyjson_obj_get(root, "packageSettings");
        if (yyjson_is_obj(pkgSettings)) {
            yyjson_val *thisPkg = yyjson_obj_get(pkgSettings, process);
            if (yyjson_is_obj(thisPkg)) {
                yyjson_val *v;
                v = yyjson_obj_get(thisPkg, "logLevel");
                if (yyjson_is_str(v)) logLevel = yyjson_get_str(v);
                v = yyjson_obj_get(thisPkg, "logTag");
                if (yyjson_is_str(v) && yyjson_get_len(v) > 0) logTag = yyjson_get_str(v);
                v = yyjson_obj_get(thisPkg, "dumpStackTrace");
                if (yyjson_is_bool(v)) dumpStack = yyjson_get_bool(v);
            }
        }

        int prio = (strcmp(logLevel, "DEBUG") == 0) ? ANDROID_LOG_DEBUG
                 : (strcmp(logLevel, "WARN")  == 0) ? ANDROID_LOG_WARN
                 :                                     ANDROID_LOG_INFO;

        char msgBuf[2048];
        snprintf(msgBuf, sizeof(msgBuf),
            "[ZygiskWebUI] process=%s logLevel=%s dumpStackTrace=%s",
            process, logLevel, dumpStack ? "true" : "false");
        remoteLog(api, prio, logTag, msgBuf);

        if (dumpStack) {
            snprintf(msgBuf, sizeof(msgBuf),
                "[ZygiskWebUI] stack trace requested for %s (implement hook here)", process);
            remoteLog(api, prio, logTag, msgBuf);
        }

        yyjson_doc_free(doc);
    }
};

REGISTER_ZYGISK_MODULE(MyModule)
REGISTER_ZYGISK_COMPANION(companion_handler)
