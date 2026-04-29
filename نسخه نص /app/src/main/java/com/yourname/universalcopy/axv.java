package com.yourname.universalcopy; // لا تنسَ تغيير هذا لاسم الحزمة الخاص بك

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.util.Base64;
import android.util.Log;

import java.security.MessageDigest;
import java.util.Random;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class axv { 
    private static final String TAG = "axv_core";
    
    private static final long FIXED_MAGIC_NUMBER = 73L; 
    
    private static boolean isTampered = false;
    private static long lastCheckTime = 0;
    private static int integrityCounter = 0;
    private static final Random random = new Random();
    
    // ========================================================
    // 🔥 1. البصمة المشفرة (لا يمكن للمخترق إيجادها بالبحث النصي) 🔥
    // ========================================================
    private static String getExpectedHash() {
        // هذه الأرقام هي بصمتك (pA2kClnRcMlpUM8VwYpFTUejmraYmYtkDs10W7cb9dw=)
        // ولكن تم تشفيرها رياضياً (كل حرف تم تحويله لرقم + 13)
        int[] hidden = {125, 78, 63, 124, 80, 121, 123, 95, 112, 90, 126, 125, 98, 90, 69, 99, 132, 102, 133, 83, 97, 98, 114, 119, 122, 134, 110, 102, 123, 102, 129, 120, 81, 128, 62, 61, 100, 67, 112, 111, 70, 113, 132, 74};
        StringBuilder sb = new StringBuilder();
        for (int i : hidden) {
            sb.append((char) (i - 13)); // فك التشفير في الذاكرة فقط لحظة الفحص
        }
        return sb.toString();
    }

    // ========================================================
    // 2. نظام إخفاء الكود الاحتياطي للمستقبل
    // ========================================================
    private static String getBackupHash() {
        byte[] hiddenBytes = {27, 19, 26, 29, 28, 30, 31, 92, 72, 65, 9, 21, 11};
        byte[] revealedBytes = new byte[hiddenBytes.length];
        byte secretKey = 42; 
        for(int i = 0; i < hiddenBytes.length; i++) {
            revealedBytes[i] = (byte) (hiddenBytes[i] ^ secretKey);
        }
        return new String(revealedBytes);
    }

    // ========================================================
    // 3. فحص البصمة والتلاعب (مع إخفاء خوارزمية SHA)
    // ========================================================
    private static boolean verifySignature(Context context) {
        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(
                context.getPackageName(), 
                PackageManager.GET_SIGNATURES
            );
            
            Signature[] signatures = packageInfo.signatures;
            if (signatures == null || signatures.length == 0) return false;
            
            // 🔥 إخفاء كلمة "SHA-256" لكي لا يبحث عنها المخترق في MT Manager
            String algo = new String(new char[]{'S', 'H', 'A', '-', '2', '5', '6'});
            MessageDigest md = MessageDigest.getInstance(algo);
            md.update(signatures[0].toByteArray());
            
            String currentHash = Base64.encodeToString(md.digest(), Base64.DEFAULT).trim();
            
            // الفحص الأول: البصمة الأساسية المفككة في الذاكرة
            boolean valid = getExpectedHash().equals(currentHash);
            
            // الفحص الثاني: البصمة الاحتياطية (للتحديثات المستقبلية)
            if (!valid) {
                String backupHash = getBackupHash();
                valid = backupHash.equals(currentHash);
            }
            
            if (!valid) isTampered = true;
            return valid;
        } catch (Exception e) {
            isTampered = true;
            return false;
        }
    }
    
    private static boolean isDebuggable(Context context) {
        if ((context.getApplicationInfo().flags & android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0) {
            return true;
        }
        String[] rootPaths = {"/system/app/Superuser.apk", "/sbin/su", "/system/bin/su", "/data/local/su"};
        for (String path : rootPaths) {
            if (new java.io.File(path).exists()) return true;
        }
        return false;
    }

    // ========================================================
    // 4. الأسلحة المتقدمة: Anti-Emulator & Anti-Hooking
    // ========================================================
    private static boolean isEmulator() {
        try {
            boolean isEmu = android.os.Build.FINGERPRINT.startsWith("generic")
                || android.os.Build.FINGERPRINT.startsWith("unknown")
                || android.os.Build.MODEL.contains("google_sdk")
                || android.os.Build.MODEL.toLowerCase().contains("droid4x")
                || android.os.Build.MODEL.contains("Emulator")
                || android.os.Build.MODEL.contains("Android SDK built for x86")
                || android.os.Build.MANUFACTURER.contains("Genymotion")
                || android.os.Build.HARDWARE.equals("goldfish")
                || android.os.Build.HARDWARE.equals("vbox86")
                || android.os.Build.PRODUCT.equals("sdk")
                || android.os.Build.PRODUCT.equals("google_sdk")
                || android.os.Build.PRODUCT.equals("sdk_x86")
                || android.os.Build.PRODUCT.equals("vbox86p")
                || android.os.Build.BOARD.toLowerCase().contains("nox")
                || android.os.Build.BOOTLOADER.toLowerCase().contains("nox")
                || android.os.Build.HARDWARE.toLowerCase().contains("nox")
                || android.os.Build.PRODUCT.toLowerCase().contains("nox");
            return isEmu;
        } catch (Exception e) { return false; }
    }

    private static boolean isHooked() {
        try {
            throw new Exception("Hook Check");
        } catch (Exception e) {
            int zygoteInitCallCount = 0;
            for (StackTraceElement stackTraceElement : e.getStackTrace()) {
                String className = stackTraceElement.getClassName();
                if (className.equals("com.saurik.substrate.MS$2") || 
                    className.equals("de.robv.android.xposed.XposedBridge")) {
                    return true;
                }
                if (className.equals("com.android.internal.os.ZygoteInit")) {
                    zygoteInitCallCount++;
                    if (zygoteInitCallCount == 2) return true;
                }
            }
        }
        return false;
    }

    // ========================================================
    // 5. المحرك الأمني الشامل
    // ========================================================
    public static boolean isAppSecure(Context context) {
        long now = System.currentTimeMillis();
        if (now - lastCheckTime < 1000 && integrityCounter > 0) {
            integrityCounter++;
            if (integrityCounter > 100) return false;
            return !isTampered;
        }
        lastCheckTime = now;
        integrityCounter = 1;
        
        // ⚠️ تم تعطيل فحص الـ Debug مؤقتاً لكي يعمل التطبيق أثناء التجربة
        boolean isSecure = verifySignature(context) 
                        // && !isDebuggable(context) 
                        && !isEmulator() 
                        && !isHooked() 
                        && !isTampered;
                        
        if (!isSecure) punishTampering(context);
        return isSecure;
    }

    private static void punishTampering(Context context) {
        try {
            isTampered = true;
            context.getSharedPreferences("sys_data", Context.MODE_PRIVATE)
                   .edit().putBoolean("err_code", true).apply();
        } catch (Exception e) { }
    }

    // ========================================================
    // 6. تمزيق المفاتيح والتشفير (AES + XOR)
    // ========================================================
    private static byte[] getDynamicLayerKey(int layer) {
        String part1 = (layer * 7) + "Z_"; 
        String part2 = layer == 0 ? "x9P" : (layer == 1 ? "m3N" : "k5T");
        String part3 = "_c0r3";
        return (part1 + part2 + part3).getBytes();
    }

    private static String generate6RandomChars() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    private static String xorEncrypt(String input, byte[] key) {
        long currentTime = System.currentTimeMillis();
        String rand6 = generate6RandomChars();
        String dynamicSaltString = (FIXED_MAGIC_NUMBER * currentTime) + rand6;
        byte[] dynamicSalt = dynamicSaltString.getBytes();
        byte[] inputBytes = input.getBytes();
        byte[] result = new byte[inputBytes.length];
        
        for (int i = 0; i < inputBytes.length; i++) {
            result[i] = (byte) (inputBytes[i] ^ key[i % key.length]);
            result[i] = (byte) (result[i] ^ dynamicSalt[i % dynamicSalt.length]); 
        }
        
        String encryptedBase64 = Base64.encodeToString(result, Base64.NO_WRAP);
        return currentTime + "_" + rand6 + "_" + encryptedBase64;
    }
    
    private static String xorDecrypt(String input, byte[] key) {
        try {
            String[] parts = input.split("_", 3);
            if (parts.length != 3) return null;
            
            long timeUsed = Long.parseLong(parts[0]);
            String rand6Used = parts[1];
            String encryptedBase64 = parts[2];
            
            String dynamicSaltString = (FIXED_MAGIC_NUMBER * timeUsed) + rand6Used;
            byte[] dynamicSalt = dynamicSaltString.getBytes();
            byte[] inputBytes = Base64.decode(encryptedBase64, Base64.NO_WRAP);
            byte[] result = new byte[inputBytes.length];
            
            for (int i = 0; i < inputBytes.length; i++) {
                result[i] = (byte) (inputBytes[i] ^ dynamicSalt[i % dynamicSalt.length]);
                result[i] = (byte) (result[i] ^ key[i % key.length]);
            }
            return new String(result);
        } catch (Exception e) { return null; }
    }

    private static String aesEncrypt(String input, byte[] key) {
        try {
            SecretKeySpec secretKey = new SecretKeySpec(MessageDigest.getInstance(new String(new char[]{'S','H','A','-','2','5','6'})).digest(key), "AES");
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            byte[] iv = new byte[12];
            random.nextBytes(iv);
            
            GCMParameterSpec spec = new GCMParameterSpec(128, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, spec);
            
            byte[] encrypted = cipher.doFinal(input.getBytes());
            byte[] combined = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(encrypted, 0, combined, iv.length, encrypted.length);
            
            return Base64.encodeToString(combined, Base64.NO_WRAP);
        } catch (Exception e) { return null; }
    }
    
    private static String aesDecrypt(String input, byte[] key) {
        try {
            byte[] combined = Base64.decode(input, Base64.NO_WRAP);
            byte[] iv = new byte[12];
            byte[] encrypted = new byte[combined.length - 12];
            
            System.arraycopy(combined, 0, iv, 0, iv.length);
            System.arraycopy(combined, 12, encrypted, 0, encrypted.length);
            
            SecretKeySpec secretKey = new SecretKeySpec(MessageDigest.getInstance(new String(new char[]{'S','H','A','-','2','5','6'})).digest(key), "AES");
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            
            GCMParameterSpec spec = new GCMParameterSpec(128, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec);
            
            return new String(cipher.doFinal(encrypted));
        } catch (Exception e) { return null; }
    }

    // ========================================================
    // 7. المحرك الرئيسي (تشفير وفك تشفير الإعلانات)
    // ========================================================
    public static String encryptData(String input) {
        if (isTampered || input == null) return null;
        try {
            String encrypted = input;
            for (int layer = 0; layer < 3; layer++) {
                byte[] currentKey = getDynamicLayerKey(layer);
                encrypted = xorEncrypt(encrypted, currentKey);
                encrypted = aesEncrypt(encrypted, currentKey);
            }
            return encrypted;
        } catch (Exception e) { return null; }
    }
    
    public static String decryptData(Context context, String encryptedData) {
        if (!isAppSecure(context)) return "ERR_TAMPERED_" + generate6RandomChars();
        if (encryptedData == null) return null;
        try {
            String decrypted = encryptedData;
            for (int layer = 2; layer >= 0; layer--) {
                byte[] currentKey = getDynamicLayerKey(layer);
                decrypted = aesDecrypt(decrypted, currentKey);
                decrypted = xorDecrypt(decrypted, currentKey);
            }
            return decrypted;
        } catch (Exception e) { return null; }
    }

    public static int updatePoints(Context context, int currentPoints, int pointsToAdd) {
        if (!isAppSecure(context)) return 0; 
        int newPoints = currentPoints + pointsToAdd;
        if (newPoints > 1000000 || newPoints < -1000) return currentPoints;
        return newPoints;
    }

    public static void o() {
        long d = System.currentTimeMillis();
        int v = 0;
        for(int i=0; i<5; i++) { v += (d % 3); }
        if (v == 9999) Log.d(TAG, "Never prints");
    }
}
