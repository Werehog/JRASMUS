package hu.rkoszegi.jrasmus.crypto;

import javax.crypto.*;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;

/**
 * Created by rkoszegi on 02/12/2016.
 */
public class KeyHelper {

    private static SecretKey fileNameKey;

    public static SecretKey getFileNameKey() {
        char[] password = "TestPassword".toCharArray();
        if(fileNameKey != null) {
            return fileNameKey;
        }

        try(InputStream keystoreInputStream = KeyHelper.class.getResourceAsStream("/keystore.ks")) {
            KeyStore keyStore = KeyStore.getInstance("JCEKS");
            keyStore.load(keystoreInputStream, password);
            fileNameKey = (SecretKey) keyStore.getKey("SecretKey", password);
        } catch (IOException | CertificateException | NoSuchAlgorithmException | UnrecoverableKeyException | KeyStoreException e) {
            e.printStackTrace();
        }

        return fileNameKey;
    }

    public static SecretKey generateSecretKeyFromPassword(char[] password, byte[] salt) {
        SecretKey key = null;
        int desiredKeyLen = 128;
        int iterations = 65536;
        try {
            String algorithm = "PBKDF2WithHmacSHA256";
            SecretKeyFactory keyFactory = SecretKeyFactory.getInstance(algorithm);
            KeySpec spec = new PBEKeySpec(password, salt, iterations, desiredKeyLen);
            SecretKey tmp = keyFactory.generateSecret(spec);
            key = new SecretKeySpec(tmp.getEncoded(), "AES");
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            e.printStackTrace();
        }

        return key;
    }

    public static byte[] generatePasswordHash(SecretKey key) {
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        md.update(key.getEncoded());
        return md.digest();
    }

    public static byte[] generateSalt() {
        int saltLen = 32;

        byte[] salt = null;

        try {
            salt = SecureRandom.getInstance("SHA1PRNG").generateSeed(saltLen);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return salt;
    }
}
