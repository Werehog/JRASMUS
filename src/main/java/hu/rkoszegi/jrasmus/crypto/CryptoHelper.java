package hu.rkoszegi.jrasmus.crypto;

import javax.crypto.*;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

/**
 * Created by rkoszegi on 02/12/2016.
 */
public class CryptoHelper {


    private static Cipher getCipher(SecretKey key, int cipherMode) {
        Cipher cipher = null;
        try {
            cipher = Cipher.getInstance("AES");
            cipher.init(cipherMode, key);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        }
        return cipher;
    }

    public static void encrypt(SecretKey key, InputStream in, OutputStream out) {
        Cipher cipher = getCipher(key, Cipher.ENCRYPT_MODE);

        try (CipherOutputStream cos = new CipherOutputStream(out,cipher)){

            byte[] b = new byte[256];
            int i = in.read(b);
            while (i != -1) {
                cos.write(b, 0, i);
                i = in.read(b);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void decrypt(SecretKey key, InputStream in, OutputStream out) {
        Cipher cipher = getCipher(key, Cipher.DECRYPT_MODE);

        try (CipherInputStream  cis = new CipherInputStream(in, cipher)) {

            byte[] b = new byte[256];
            int i = cis.read(b);
            while (i != -1) {
                out.write(b, 0, i);
                i = cis.read(b);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
