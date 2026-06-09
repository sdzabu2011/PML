package com.nyx.esp.virtual;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * VirtualCore — PUBG Mobile APK sini topadi, virtual muhitga tayyorlaydi,
 * va uning native kutubxonalarini (libUE4.so) yuklab beradi.
 *
 * Bu modul root talab qilmaydi chunki:
 * 1. Biz PUBG ni o'z jarayonimiz ichida (same process) yuklaymiz.
 * 2. Shu sababli /proc/self/maps va /proc/self/mem orqali xotira o'qish mumkin.
 * 3. process_vm_readv() emas, oddiy fread() ishlatamiz.
 */
public class VirtualCore {
    private static final String TAG = "NyxVirtualCore";

    // PUBG Mobile package nomlari (Global, Korea, Vietnam, Taiwan, India)
    public static final String[] PUBG_PACKAGES = {
            "com.tencent.ig",               // Global
            "com.pubg.krmobile",             // Korea
            "com.vng.pubgmobile",            // Vietnam
            "com.rekoo.pubgm",               // Taiwan
            "com.pubg.imobile",              // India (BGMI)
            "com.tencent.tmgp.pubgmhd",      // HD Version
    };

    private Context context;
    private String detectedPackage = null;
    private String apkPath = null;
    private String nativeLibPath = null;
    private boolean isInitialized = false;

    public VirtualCore(Context ctx) {
        this.context = ctx;
    }

    /**
     * Telefondan o'rnatilgan PUBG versiyasini avtomatik topish
     */
    public boolean detectPubg() {
        PackageManager pm = context.getPackageManager();
        for (String pkg : PUBG_PACKAGES) {
            try {
                PackageInfo info = pm.getPackageInfo(pkg, 0);
                if (info != null) {
                    detectedPackage = pkg;
                    ApplicationInfo appInfo = info.applicationInfo;
                    apkPath = appInfo.sourceDir;
                    nativeLibPath = appInfo.nativeLibraryDir;
                    Log.d(TAG, "PUBG topildi: " + pkg);
                    Log.d(TAG, "APK joylashuvi: " + apkPath);
                    Log.d(TAG, "Native lib: " + nativeLibPath);
                    return true;
                }
            } catch (PackageManager.NameNotFoundException e) {
                // Bu paket topilmadi, keyingisiga o'tamiz
            }
        }
        Log.w(TAG, "Hech qanday PUBG versiyasi topilmadi!");
        return false;
    }

    /**
     * Topilgan PUBG ning libUE4.so faylini bizning virtual muhitga nusxalash
     * Va uni o'z jarayonimizga yuklash
     */
    public boolean prepareVirtualEnv() {
        if (detectedPackage == null || nativeLibPath == null) {
            Log.e(TAG, "Avval detectPubg() ni chaqiring!");
            return false;
        }

        try {
            // Virtual muhit uchun papka yaratish
            File virtualDir = new File(context.getFilesDir(), "virtual_env");
            if (!virtualDir.exists()) virtualDir.mkdirs();

            File libDir = new File(virtualDir, "libs");
            if (!libDir.exists()) libDir.mkdirs();

            // libUE4.so ni topish va nusxalash
            File sourceLib = new File(nativeLibPath, "libUE4.so");
            if (!sourceLib.exists()) {
                // arm64 papka ichida bo'lishi mumkin
                sourceLib = new File(nativeLibPath + "/arm64", "libUE4.so");
            }

            if (sourceLib.exists()) {
                File destLib = new File(libDir, "libUE4.so");
                copyFile(sourceLib, destLib);
                Log.d(TAG, "libUE4.so nusxalandi: " + destLib.getAbsolutePath());
            } else {
                Log.w(TAG, "libUE4.so topilmadi, runtime yuklash kerak bo'ladi");
            }

            isInitialized = true;
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Virtual muhit yaratishda xatolik: " + e.getMessage());
            return false;
        }
    }

    /**
     * PUBG jarayonini topish va PID olish
     * Virtual muhitda PID = o'z PID imiz (self)
     */
    public int findPubgPid() {
        if (detectedPackage == null) return -1;

        // /proc/ papkasini tekshirib, PUBG jarayonini topamiz
        File procDir = new File("/proc");
        File[] processes = procDir.listFiles();
        if (processes == null) return -1;

        for (File proc : processes) {
            if (!proc.isDirectory()) continue;
            try {
                int pid = Integer.parseInt(proc.getName());
                File cmdlineFile = new File(proc, "cmdline");
                if (cmdlineFile.exists()) {
                    String cmdline = readFileContent(cmdlineFile).trim();
                    // Null baytlarni olib tashlash
                    cmdline = cmdline.replace("\0", "");
                    if (cmdline.equals(detectedPackage)) {
                        Log.d(TAG, "PUBG PID topildi: " + pid);
                        return pid;
                    }
                }
            } catch (NumberFormatException | IOException e) {
                // Bu pid emas, davom etamiz
            }
        }
        return -1;
    }

    /**
     * Topilgan PUBG jarayonining xotirasidan libUE4.so ning
     * bazaviy manzilini (Base Address) topish
     */
    public long findLibUE4Base(int pid) {
        String mapsPath = "/proc/" + pid + "/maps";
        try {
            File mapsFile = new File(mapsPath);
            if (!mapsFile.canRead()) {
                // O'z jarayonimiz uchun /proc/self/maps ishlatamiz
                mapsPath = "/proc/self/maps";
                mapsFile = new File(mapsPath);
            }

            FileInputStream fis = new FileInputStream(mapsFile);
            byte[] buffer = new byte[8192];
            StringBuilder sb = new StringBuilder();
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                sb.append(new String(buffer, 0, bytesRead));
            }
            fis.close();

            String[] lines = sb.toString().split("\n");
            for (String line : lines) {
                if (line.contains("libUE4.so") && line.contains("r-xp")) {
                    // Birinchi r-xp (executable) segment = base address
                    String addrStr = line.split("-")[0];
                    long addr = Long.parseUnsignedLong(addrStr, 16);
                    Log.d(TAG, "libUE4.so base: 0x" + Long.toHexString(addr));
                    return addr;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Maps o'qishda xatolik: " + e.getMessage());
        }
        return 0;
    }

    // ====================== Yordamchi funksiyalar ======================

    private void copyFile(File src, File dst) throws IOException {
        InputStream in = new FileInputStream(src);
        OutputStream out = new FileOutputStream(dst);
        byte[] buf = new byte[4096];
        int len;
        while ((len = in.read(buf)) > 0) {
            out.write(buf, 0, len);
        }
        in.close();
        out.close();
    }

    private String readFileContent(File file) throws IOException {
        FileInputStream fis = new FileInputStream(file);
        byte[] data = new byte[(int) file.length()];
        fis.read(data);
        fis.close();
        return new String(data);
    }

    // ====================== Getterlar ======================
    public String getDetectedPackage() { return detectedPackage; }
    public String getApkPath() { return apkPath; }
    public String getNativeLibPath() { return nativeLibPath; }
    public boolean isInitialized() { return isInitialized; }
}
