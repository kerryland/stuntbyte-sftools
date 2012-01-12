package com.stuntbyte.salesforce.misc;

import javax.crypto.*;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;
import java.security.spec.*;

/**
 */
public class SyncCrypto implements Encrypter, Decrypter {

    byte[] salt = {
            (byte) 0xB2, (byte) 0x8C, (byte) 0xA1, (byte) 0x84,
            (byte) 0x18, (byte) 0x71, (byte) 0xE3, (byte) 0xC3
    };

    int iterationCount = 21;
    private String passPhrase;

    SyncCrypto(String passPhrase) {
        this.passPhrase = passPhrase;
    }

    private Cipher setupCipher(String passPhrase, int mode) throws Exception {
        KeySpec keySpec = new PBEKeySpec(passPhrase.toCharArray(), salt, iterationCount);
        SecretKey key = SecretKeyFactory.getInstance("PBEWithMD5AndDES").generateSecret(keySpec);
        AlgorithmParameterSpec paramSpec = new PBEParameterSpec(salt, iterationCount);

        Cipher cipher = Cipher.getInstance(key.getAlgorithm());
        cipher.init(mode, key, paramSpec);
        return cipher;
    }


    public String encrypt(byte[] utf8) {
        try {
            Cipher cipher = setupCipher(passPhrase, Cipher.ENCRYPT_MODE);

            byte[] enc = cipher.doFinal(utf8);
            return new String(Base64.encodeBytes(enc, Base64.URL_SAFE));

        } catch (Exception e) {
        }
        return null;
    }

    public byte[] decrypt(String str) {
        try {
            byte[] dec = Base64.decode(str, Base64.URL_SAFE);
            Cipher cipher = setupCipher(passPhrase, Cipher.DECRYPT_MODE);
            return cipher.doFinal(dec);
        } catch (Exception e) {
        }
        return null;
    }
}