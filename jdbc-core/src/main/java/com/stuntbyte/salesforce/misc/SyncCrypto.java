/**
 * The MIT License
 * Copyright © 2011-2017 Kerry Sainsbury
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
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
            return Base64.encodeBytes(enc, Base64.URL_SAFE);

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