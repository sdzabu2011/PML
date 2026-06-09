#include "Memory.h"

namespace NyxMem {
    pid_t TargetPID = -1;
    uintptr_t libUE4Base = 0;

    pid_t find_pid(const char* process_name) {
        int id = -1;
        DIR* dir;
        FILE* fp;
        char filename[32];
        char cmdline[256];
        struct dirent* entry;

        if (process_name == NULL) return -1;
        dir = opendir("/proc");
        if (dir == NULL) return -1;

        while ((entry = readdir(dir)) != NULL) {
            id = atoi(entry->d_name);
            if (id != 0) {
                sprintf(filename, "/proc/%d/cmdline", id);
                fp = fopen(filename, "r");
                if (fp) {
                    fgets(cmdline, sizeof(cmdline), fp);
                    fclose(fp);
                    if (strcmp(process_name, cmdline) == 0) {
                        return id;
                    }
                }
            }
        }
        closedir(dir);
        return -1;
    }

    uintptr_t get_module_base(pid_t pid, const char* module_name) {
        FILE* fp;
        uintptr_t addr = 0;
        char filename[32], buffer[1024];

        snprintf(filename, sizeof(filename), "/proc/%d/maps", pid);
        fp = fopen(filename, "rt");
        if (fp != NULL) {
            while (fgets(buffer, sizeof(buffer), fp)) {
                if (strstr(buffer, module_name)) {
                    sscanf(buffer, "%lx-%*lx", &addr);
                    break;
                }
            }
            fclose(fp);
        }
        return addr;
    }

    bool ReadBuffer(uintptr_t address, void* buffer, size_t size) {
        struct iovec local[1];
        struct iovec remote[1];

        local[0].iov_base = buffer;
        local[0].iov_len = size;
        remote[0].iov_base = (void*)address;
        remote[0].iov_len = size;

        ssize_t bytes = process_vm_readv(TargetPID, local, 1, remote, 1, 0);
        return bytes == size;
    }
}
