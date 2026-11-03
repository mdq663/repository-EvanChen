package com.zcshou.gogogo;

/**
 * Local license-password configuration.
 *
 * <p>Only a PBKDF2 verifier is compiled into the APK. To change the password,
 * generate a new verifier with tools/PasswordVerifierGenerator.java and replace
 * PASSWORD_VERIFIER below. Keep PASSWORD_SALT and PBKDF2_ITERATIONS unchanged
 * unless the generator output explicitly includes matching values.</p>
 */
final class PasswordConfig {
    static final String PASSWORD_SALT = "GoGoGo.LocalLicense.v1";
    static final int PBKDF2_ITERATIONS = 120_000;
    static final String PASSWORD_VERIFIER =
            "885BEE66DFBB167B837DDCEBDC464502D6B65C1C66A78C3F5A20E7C970A2F7CF";

    private PasswordConfig() {
    }
}
