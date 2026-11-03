package com.zcshou.gogogo;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.GeneralSecurityException;
import java.util.Arrays;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

final class LicenseManager {
    private static final String PREFS_NAME = "local_license";
    private static final String KEY_AUTHORIZED = "authorized";
    private static final String KEY_VERIFIER = "verifier";

    private LicenseManager() {
    }

    static boolean isAuthorized(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(
                PREFS_NAME, Context.MODE_PRIVATE);
        String savedVerifier = preferences.getString(KEY_VERIFIER, "");
        return preferences.getBoolean(KEY_AUTHORIZED, false)
                && constantTimeEquals(PasswordConfig.PASSWORD_VERIFIER, savedVerifier);
    }

    static boolean verifyPassword(String password) {
        if (password == null || password.isEmpty()) {
            return false;
        }

        char[] passwordChars = password.toCharArray();
        byte[] derivedKey = null;
        PBEKeySpec keySpec = null;
        try {
            keySpec = new PBEKeySpec(
                    passwordChars,
                    PasswordConfig.PASSWORD_SALT.getBytes(StandardCharsets.UTF_8),
                    PasswordConfig.PBKDF2_ITERATIONS,
                    256);
            derivedKey = SecretKeyFactory
                    .getInstance("PBKDF2WithHmacSHA256")
                    .generateSecret(keySpec)
                    .getEncoded();
            return MessageDigest.isEqual(
                    hexToBytes(PasswordConfig.PASSWORD_VERIFIER),
                    derivedKey);
        } catch (GeneralSecurityException | IllegalArgumentException ignored) {
            return false;
        } finally {
            Arrays.fill(passwordChars, '\0');
            if (keySpec != null) {
                keySpec.clearPassword();
            }
            if (derivedKey != null) {
                Arrays.fill(derivedKey, (byte) 0);
            }
        }
    }

    static boolean persistAuthorization(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_AUTHORIZED, true)
                .putString(KEY_VERIFIER, PasswordConfig.PASSWORD_VERIFIER)
                .commit();
    }

    static boolean requireAuthorization(Activity activity) {
        if (isAuthorized(activity)) {
            return true;
        }

        Intent intent = new Intent(activity, AuthActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        activity.startActivity(intent);
        activity.finish();
        return false;
    }

    private static boolean constantTimeEquals(String expected, String actual) {
        if (actual == null) {
            return false;
        }
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.US_ASCII),
                actual.getBytes(StandardCharsets.US_ASCII));
    }

    private static byte[] hexToBytes(String hex) {
        if ((hex.length() & 1) != 0) {
            throw new IllegalArgumentException("Invalid verifier");
        }

        byte[] bytes = new byte[hex.length() / 2];
        for (int i = 0; i < hex.length(); i += 2) {
            int high = Character.digit(hex.charAt(i), 16);
            int low = Character.digit(hex.charAt(i + 1), 16);
            if (high < 0 || low < 0) {
                throw new IllegalArgumentException("Invalid verifier");
            }
            bytes[i / 2] = (byte) ((high << 4) | low);
        }
        return bytes;
    }
}
