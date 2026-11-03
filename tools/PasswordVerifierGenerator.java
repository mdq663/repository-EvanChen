import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Usage:
 *   javac tools/PasswordVerifierGenerator.java
 *   java tools/PasswordVerifierGenerator.java "new-password"
 *
 * Put the printed verifier into PasswordConfig.PASSWORD_VERIFIER.
 */
public final class PasswordVerifierGenerator {
    private static final String SALT = "GoGoGo.LocalLicense.v1";
    private static final int ITERATIONS = 120_000;

    private PasswordVerifierGenerator() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 1 || args[0].isEmpty()) {
            System.err.println("Usage: java tools/PasswordVerifierGenerator.java \"new-password\"");
            System.exit(1);
        }

        char[] password = args[0].toCharArray();
        PBEKeySpec keySpec = new PBEKeySpec(
                password,
                SALT.getBytes(StandardCharsets.UTF_8),
                ITERATIONS,
                256);
        try {
            byte[] verifier = SecretKeyFactory
                    .getInstance("PBKDF2WithHmacSHA256")
                    .generateSecret(keySpec)
                    .getEncoded();
            System.out.println(HexFormat.of().withUpperCase().formatHex(verifier));
        } finally {
            keySpec.clearPassword();
        }
    }
}
