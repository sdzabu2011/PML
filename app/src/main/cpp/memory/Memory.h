#ifndef MEMORY_H
#define MEMORY_H

#include <stdint.h>
#include <unistd.h>
#include <sys/uio.h>
#include <sys/types.h>
#include <dirent.h>
#include <stdlib.h>
#include <string.h>
#include <stdio.h>

namespace NyxMem {
    extern pid_t TargetPID;
    extern uintptr_t libUE4Base;

    pid_t find_pid(const char* process_name);
    uintptr_t get_module_base(pid_t pid, const char* module_name);

    // RPM Engine (process_vm_readv)
    template <typename T>
    T Read(uintptr_t address) {
        T buffer;
        struct iovec local[1];
        struct iovec remote[1];

        local[0].iov_base = &buffer;
        local[0].iov_len = sizeof(T);
        remote[0].iov_base = (void*)address;
        remote[0].iov_len = sizeof(T);

        // process_vm_readv requires ROOT or Kernel access to bypass Anti-Cheat protection.
        if (TargetPID > 0) {
            process_vm_readv(TargetPID, local, 1, remote, 1, 0);
        }
        return buffer;
    }

    bool ReadBuffer(uintptr_t address, void* buffer, size_t size);
}

#endif
