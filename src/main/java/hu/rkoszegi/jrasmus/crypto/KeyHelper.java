package hu.rkoszegi.jrasmus.crypto;

import javax.crypto.*;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;

/**
 * Created by rkoszegi on 02/12/2016.
 */
public class KeyHelper {

    public static SecretKey generateSecretKeyFromPassword(char[] password, byte[] salt) {
        SecretKey key = null;
        int desiredKeyLen = 128;
        int iterations = 65536;
        try {
            SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            //key = secretKeyFactory.generateSecret(new PBEKeySpec(password,salt,iterations,desiredKeyLen));
            KeySpec spec = new PBEKeySpec(password, salt, iterations, desiredKeyLen);
            SecretKey tmp = secretKeyFactory.generateSecret(spec);
            key = new SecretKeySpec(tmp.getEncoded(), "AES");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (InvalidKeySpecException e) {
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

    private static void encryptFile(SecretKey key) {
        Cipher cipher = null;
        try {
            cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, key);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        }

        try (FileInputStream fis = new FileInputStream("szakirod-kezeles.pdf");
             FileOutputStream fos = new FileOutputStream("encoded.ric");
             CipherOutputStream cos = new CipherOutputStream(fos,cipher)){

            byte[] b = new byte[8];
            int i = fis.read(b);
            while (i != -1) {
                cos.write(b, 0, i);
                i = fis.read(b);
            }
            cos.flush();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void decryptFile(SecretKey key) {
        Cipher cipher = null;
        try {
            cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, key);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        }
        try (FileInputStream fis = new FileInputStream("encoded.ric");
             CipherInputStream  cis = new CipherInputStream(fis, cipher);
             FileOutputStream fos = new FileOutputStream("decoded.pdf")) {

            byte[] b = new byte[10];
            int i = cis.read(b);
            while (i != -1) {
                fos.write(b, 0, i);
                i = cis.read(b);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
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
