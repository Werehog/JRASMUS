package hu.rkoszegi.jrasmus.crypto;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.io.*;
import java.security.*;
import java.security.KeyStore.ProtectionParameter;
import java.security.cert.CertificateException;

/**
 * Created by rkoszegi on 06/11/2016.
 */
public class KeyManager {

    private static SecretKey key;

    public static SecretKey getKey() {
        char[] password = "TestPassword".toCharArray();
        if(key != null) {
            return  key;
        }

        File keystoreFile = new File("keystore.ks");
        if(keystoreFile.exists()) {
            try (FileInputStream fis = new FileInputStream(keystoreFile);){
                KeyStore keyStore = KeyStore.getInstance("JCEKS");
                keyStore.load(fis, password);
                key = (SecretKey) keyStore.getKey("SecretKey", password);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (CertificateException e) {
                e.printStackTrace();
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            } catch (UnrecoverableKeyException e) {
                e.printStackTrace();
            } catch (KeyStoreException e) {
                e.printStackTrace();
            }

        } else {
            try {
                KeyStore keyStore = KeyStore.getInstance("JCEKS");
                //Ha null-lal inicializalom, akkor ujat hoz letre
                keyStore.load(null);
                key = generateAesKey();
                ProtectionParameter protectionParameter = new KeyStore.PasswordProtection(password);
                KeyStore.SecretKeyEntry secretKeyEntry = new KeyStore.SecretKeyEntry(key);
                keyStore.setEntry("SecretKey", secretKeyEntry, protectionParameter);

                try(FileOutputStream fos = new FileOutputStream("keystore.ks")) {
                    keyStore.store(fos, password);
                }

            } catch (KeyStoreException e) {
                e.printStackTrace();
            } catch (CertificateException e) {
                e.printStackTrace();
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return key;
    }

    private static SecretKey generateAesKey() {
        KeyGenerator generator = null;
        SecretKey key = null;
        try {
            generator = KeyGenerator.getInstance("AES");
            generator.init(128);
            key = generator.generateKey();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        return key;
    }
}
