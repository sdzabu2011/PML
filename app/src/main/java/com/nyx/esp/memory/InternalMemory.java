package com.nyx.esp.memory;

import android.util.Log;

import java.io.FileInputStream;
import java.io.RandomAccessFile;

/**
 * InternalMemory — Root'siz xotira o'qish tizimi.
 *
 * /proc/self/mem yoki /proc/{pid}/mem orqali xotirani o'qiydi.
 * Bu usulda biz PUBG bilan bir xil jarayonda (process) ishlashimiz kerak
 * yoki boshqa jarayonni ptrace qilishimiz kerak.
 *
 * Virtual Space yondashuvi:
 * - PUBG bizning ilovamiz ichida ishga tushiriladi
 * - Natijada /proc/self/mem orqali uning xotirasini o'qish mumkin
 * - Root talab qilinmaydi
 */
public class InternalMemory {
    private static final String TAG = "NyxMemory";
    
    private RandomAccessFile memFile = null;
    private int targetPid = -1;
    private long libBase = 0;
    private boolean isAttached = false;

    /**
     * Xotira faylini ochish
     * Avval o'z jarayonimizni sinab ko'ramiz, keyin target PID ni
     */
    public boolean attach(int pid) {
        this.targetPid = pid;
        
        // /proc/self/mem dan boshlaymiz (virtual space rejimida ishlaydi)
        String[] memPaths = {
            "/proc/self/mem",
            "/proc/" + pid + "/mem",
        };

        for (String path : memPaths) {
            try {
                memFile = new RandomAccessFile(path, "r");
                isAttached = true;
                Log.d(TAG, "Xotiraga ulandi: " + path);
                return true;
            } catch (Exception e) {
                Log.w(TAG, "Ulanib bo'lmadi: " + path + " -> " + e.getMessage());
            }
        }

        Log.e(TAG, "Hech qanday xotira fayliga ulanib bo'lmadi!");
        return false;
    }

    /**
     * Xotiradan baytlarni o'qish
     */
    public byte[] readBytes(long address, int size) {
        if (!isAttached || memFile == null) return null;

        byte[] buffer = new byte[size];
        try {
            memFile.seek(address);
            int read = memFile.read(buffer);
            if (read == size) return buffer;
        } catch (Exception e) {
            // Xotira himoyalangan bo'lishi mumkin
        }
        return null;
    }

    /**
     * int (4 bayt) o'qish
     */
    public int readInt(long address) {
        byte[] data = readBytes(address, 4);
        if (data == null) return 0;
        return (data[0] & 0xFF) 
             | ((data[1] & 0xFF) << 8) 
             | ((data[2] & 0xFF) << 16) 
             | ((data[3] & 0xFF) << 24);
    }

    /**
     * long (8 bayt, pointer) o'qish — 64-bit PUBG uchun
     */
    public long readLong(long address) {
        byte[] data = readBytes(address, 8);
        if (data == null) return 0;
        return (long)(data[0] & 0xFF)
             | ((long)(data[1] & 0xFF) << 8)
             | ((long)(data[2] & 0xFF) << 16)
             | ((long)(data[3] & 0xFF) << 24)
             | ((long)(data[4] & 0xFF) << 32)
             | ((long)(data[5] & 0xFF) << 40)
             | ((long)(data[6] & 0xFF) << 48)
             | ((long)(data[7] & 0xFF) << 56);
    }

    /**
     * float (4 bayt) o'qish — koordinatalar uchun
     */
    public float readFloat(long address) {
        int bits = readInt(address);
        return Float.intBitsToFloat(bits);
    }

    /**
     * 3D Vektor (X, Y, Z) ni o'qish
     */
    public float[] readVector3(long address) {
        float[] vec = new float[3];
        vec[0] = readFloat(address);
        vec[1] = readFloat(address + 4);
        vec[2] = readFloat(address + 8);
        return vec;
    }

    /**
     * ViewMatrix (4x4 = 16 float) ni o'qish — WorldToScreen uchun
     */
    public float[] readMatrix(long address) {
        float[] matrix = new float[16];
        byte[] data = readBytes(address, 64); // 16 * 4 bayt
        if (data == null) return matrix;
        
        for (int i = 0; i < 16; i++) {
            int offset = i * 4;
            int bits = (data[offset] & 0xFF)
                     | ((data[offset + 1] & 0xFF) << 8)
                     | ((data[offset + 2] & 0xFF) << 16)
                     | ((data[offset + 3] & 0xFF) << 24);
            matrix[i] = Float.intBitsToFloat(bits);
        }
        return matrix;
    }

    /**
     * Xotirani yopish
     */
    public void detach() {
        if (memFile != null) {
            try { memFile.close(); } catch (Exception e) {}
            memFile = null;
        }
        isAttached = false;
    }

    public boolean isAttached() { return isAttached; }
    public int getTargetPid() { return targetPid; }
}
