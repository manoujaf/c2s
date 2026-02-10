package c2s.mjk;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Utility class for cryptographic operations.
 * Provides methods for hashing, HMAC generation, digital signatures, Base64 encoding/decoding,
 * and RSA operations. Supports various hash algorithms including MD5, SHA1, SHA256, and
 * HMAC variants.
 */
public class Hash {

    /** Character set with special characters for random string generation */
    private static final String string = "+aZ,b:-@Yc!|0X*dWe'`V1.fUg=Th2-S)i?RjQ(3$kP;l[O4mNn{M5o]L&pKq}6JrIs7_HtG~uF%8vEwD#x9Cy<BzA";
    /** Character set with alphanumeric characters for random string generation */
    private static final String string1 = "abcde0fghij1klmno2pqrst3uvwxy4zABCD5EFGHI6JKLMN7OPQRS8TUVWX9YZ";

    // Hash algorithm constants
    public static final String HMAC_SHA512 = "HmacSHA512";
    public static final String HMAC_SHA384 = "HmacSHA384";
    public static final String HMAC_SHA256 = "HmacSHA256";
    public static final String RSA = "RSA";
    public static final String MD5 = "MD5";
    public static final String SHA1 = "SHA1";
    public static final String SHA256 = "SHA256";

    // Random string generation type constants
    public static final int SPECIAL_CHARS = 0;
    public static final int NORMAL_CHARS = 1;

    /**
     * Computes a hash of the given text using the specified algorithm.
     * Supports algorithms like MD5, SHA1, SHA256, etc.
     *
     * @param txt the text to hash
     * @param hashType the hash algorithm type (e.g., "SHA256", "MD5")
     * @return the hexadecimal string representation of the hash, or empty string on error
     */
    public static String getHash(String txt, String hashType) {
        try {
            final MessageDigest md = MessageDigest.getInstance(hashType);
            final byte[] array = md.digest(txt.getBytes());
            final StringBuilder sb = new StringBuilder();
            for (byte anArray : array) {
                sb.append(Integer.toHexString((anArray & 0xFF) | 0x100).substring(1, 3));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return "";
    }

    /**
     * Generates a random string of specified length.
     * Can use either normal alphanumeric characters or special characters.
     *
     * @param length the length of the random string to generate
     * @param charType SPECIAL_CHARS (0) for special characters, NORMAL_CHARS (1) for alphanumeric
     * @return a randomly generated string of the specified length
     */
    public static String generateRandomString(int length, int charType) {

        final int size;
        final String _string;

        if (charType == SPECIAL_CHARS) {
            size = 89;
            _string = string;
        } else {
            size = 62;
            _string = string1;
        }

        final StringBuilder hash = new StringBuilder();
        for (int i = 0; i < length; i++) {
            int _i = (int) ((Math.random() * size));
            hash.append(_string.charAt(_i));
        }
        return hash.toString();
    }

    /**
     *
     * @param data text or data
     * @param secret password or sth...
     * @param algorithm like SHA-256
     * @return hash string
     */
    public static String hmacDigest(String data, String secret, String algorithm) {
        String digest = null;

        boolean b = (data != null) && (secret != null);
        if (!b) {
            return null;
        }

        try {
            final SecretKeySpec key = new SecretKeySpec((secret).getBytes(UTF_8), algorithm);
            final Mac mac = Mac.getInstance(algorithm);
            mac.init(key);
            byte[] bytes = mac.doFinal(data.getBytes(UTF_8));
            final StringBuilder hash = new StringBuilder();
            for (byte aByte : bytes) {
                String hex = Integer.toHexString(0xFF & aByte);
                if (hex.length() == 1) {
                    hash.append('0');
                }
                hash.append(hex);
            }
            digest = hash.toString().toLowerCase();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return digest;
    }

    /**
     *
     * @param data
     * @param secret
     * @param algorithm
     * @return
     * @throws Exception
     */
    public static String hmacDigestBinary(String data, String secret, String algorithm) throws Exception {

        Mac sha256_HMAC = Mac.getInstance(algorithm);
        SecretKeySpec secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.ISO_8859_1), algorithm);
        sha256_HMAC.init(secretKey);

        byte[] rawHmac = sha256_HMAC.doFinal(data.getBytes(StandardCharsets.ISO_8859_1));

        // Convert byte[] to String using ISO-8859-1 to preserve bytes
        String hmacString = new String(rawHmac, StandardCharsets.ISO_8859_1);

        return hmacString;
    }

    /**
     *
     * @param filePath
     * @return
     */
    /**
     * Computes the MD5 hash of a file's contents.
     * Reads the file in chunks to handle large files efficiently.
     *
     * @param filePath the path to the file to hash
     * @return the hexadecimal string representation of the MD5 hash, or empty string on error
     */
    public static String calculateFileMd5(String filePath) {

        StringBuilder returnVal = new StringBuilder();

        try {

            final InputStream input = new FileInputStream(filePath);
            final byte[] buffer = new byte[1024];
            final MessageDigest md5Hash = MessageDigest.getInstance("MD5");
            int numRead = 0;
            while (numRead != -1)  {
                numRead = input.read(buffer);
                if (numRead > 0) {
                    md5Hash.update(buffer, 0, numRead);
                }
            }
            input.close();

            byte [] md5Bytes = md5Hash.digest();
            for (byte md5Byte : md5Bytes) {
                returnVal.append(Integer.toString((md5Byte & 0xff) + 0x100, 16).substring(1));
            }
        }

        catch(Throwable t) {
            t.printStackTrace();
        }
        return returnVal.toString().toLowerCase();

    }

    /**
     * Encodes the given string to Base64 using URL-safe encoding.
     * URL-safe encoding uses '-' and '_' instead of '+' and '/'.
     *
     * @param data the string to encode
     * @return the URL-safe Base64 encoded string
     */
    public static String toBase64(String data)
    {
        return Base64.getUrlEncoder().encodeToString(data.getBytes());
    }

    /**
     * Decodes a URL-safe Base64 encoded string.
     * Reverses the encoding performed by toBase64().
     *
     * @param data the URL-safe Base64 encoded string to decode
     * @return the decoded string
     */
    public static String fromBase64(String data) {
        return new String(Base64.getUrlDecoder().decode(data));
    }


    /**
     * Signs a string using RSA with SHA256 algorithm.
     * Uses a Base64-encoded PKCS#8 private key to create a digital signature.
     *
     * @param preparedStr the string to sign
     * @param privateKeyStr the Base64-encoded PKCS#8 private key
     * @return the Base64-encoded signature (without padding)
     * @throws RuntimeException if signing fails
     */
    public static String signWithRSA(String preparedStr, String privateKeyStr) {
        try {
            // Decode the Base64 private key
            byte[] keyBytes = Base64.getDecoder().decode(privateKeyStr);
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PrivateKey privateKey = keyFactory.generatePrivate(keySpec);

            // Sign with SHA256withRSA
            Signature signature = Signature.getInstance("SHA256WithRSA");
            signature.initSign(privateKey);
            signature.update(preparedStr.getBytes(StandardCharsets.UTF_8));
            byte[] signedBytes = signature.sign();

            // Return Base64 encoded signature
            return Base64.getEncoder().withoutPadding().encodeToString(signedBytes);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    /**
     * Detects the type of a secret key (RSA or HMAC).
     * Analyzes the key format and length to determine its intended use.
     *
     * @param secretKey the secret key string to analyze
     * @return "RSA" for RSA private keys, "HmacSHA256" for HMAC keys, "INVALID" for null/empty, or "UNKNOWN" for unrecognized formats
     */
    public static String detectSecretKeyType(String secretKey) {
        if (secretKey == null || secretKey.trim().isEmpty()) {
            return "INVALID";
        }

        String trimmedKey = secretKey.trim();

        // Check if it's an RSA private key
        // RSA keys are typically long Base64 strings (usually 1000+ characters)
        // and start with specific patterns when decoded
        if (trimmedKey.length() > 200) {
            try {
                // Try to decode as Base64
                byte[] keyBytes = Base64.getDecoder().decode(trimmedKey);

                // Check if it starts with the ASN.1 DER encoding for PKCS#8 private key
                // PKCS#8 private keys typically start with: 0x30 0x82 (or 0x30 0x81)
                if (keyBytes.length > 10 && keyBytes[0] == 0x30 &&
                        (keyBytes[1] == (byte)0x82 || keyBytes[1] == (byte)0x81)) {

                    // Try to actually parse it as RSA key to confirm
                    try {
                        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
                        KeyFactory keyFactory = KeyFactory.getInstance(RSA);
                        keyFactory.generatePrivate(keySpec);
                        return RSA;
                    } catch (Exception e) {
                        // Not a valid RSA key
                    }
                }
            } catch (Exception e) {
                // Not valid Base64 or not an RSA key
            }
        }

        // If it's not RSA, assume it's for HMAC
        // HMAC keys are typically shorter strings (32-128 characters)
        if (trimmedKey.length() >= 16 && trimmedKey.length() <= 256) {
            return HMAC_SHA256;
        }

        return "UNKNOWN";
    }

}