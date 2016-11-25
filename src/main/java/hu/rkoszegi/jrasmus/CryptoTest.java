package hu.rkoszegi.jrasmus;

import hu.rkoszegi.jrasmus.crypto.KeyManager;

import javax.crypto.*;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;

/**
 * Created by rkoszegi on 06/11/2016.
 */
public class CryptoTest {

    public void TestIt(char[] passwd) {
        //SecretKey key = generateAesKeyFromPassword(passwd);
        SecretKey key = KeyManager.getKey();
        encryptFile(key);
        decryptFile(key);
    }

    //Passwd azert char[] mert String globalis valtozo, nem lehet nullazni/torolni
    private SecretKey generateAesKeyFromPassword(char[] password) {
        if(password == null || password.length == 0) {
            throw new IllegalArgumentException("Password must not be empty!");
        }

        int saltLen = 32;
        int desiredKeyLen = 128;
        byte[] salt = null;
        int iterations = 65536;
        try {
            salt = SecureRandom.getInstance("SHA1PRNG").generateSeed(saltLen);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        SecretKey key = null;
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

    private void encryptFile(SecretKey key) {
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

        try (FileInputStream fis = new FileInputStream("mon.txt");
             FileOutputStream fos = new FileOutputStream("mon.ric");
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

    private void decryptFile(SecretKey key) {
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
        try (FileInputStream fis = new FileInputStream("mon.ric");
             CipherInputStream  cis = new CipherInputStream(fis, cipher);
             FileOutputStream fos = new FileOutputStream("monencoded.txt")) {

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

    public static void decryptSuchFile(SecretKey key, File file) {
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

        int blockNr =0;

        try (FileInputStream fis = new FileInputStream(file);
             //CipherInputStream  cis = new CipherInputStream(fis, cipher);
            CipherOutputStream cos = new CipherOutputStream(new FileOutputStream(new File("dec_" +  file.getName())), cipher);){

            long fileSize = file.length();

            while(fileSize - 16 >= 0) {
                byte[] b = new byte[16];
                int i = fis.read(b);
                cos.write(b);

                blockNr++;
                fileSize -= 16;
            }

            System.out.println("Remained: " + (fileSize % 16));

            if(fileSize != 0) {

                int remained = Math.toIntExact(fileSize % 16);
                byte[] b = new byte[remained];
                int i = fis.read(b);
                cos.write(b);


            }

            cos.flush();
            cos.close();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println(blockNr);

    }
}
