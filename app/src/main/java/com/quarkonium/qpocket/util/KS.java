package com.quarkonium.qpocket.util;

import android.annotation.TargetApi;
import android.content.Context;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.security.keystore.UserNotAuthenticatedException;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;

import com.quarkonium.qpocket.api.entity.ServiceErrorException;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

@TargetApi(23)
public class KS {
    private static final String NAME = "qPocket_ks";

    private synchronized static void putKSFile(Context context, String name, String value) {
        context.getSharedPreferences(NAME, Context.MODE_PRIVATE).edit()
                .putString(name, value).apply();
    }

    private synchronized static String getKSFile(Context context, String name) {
        return context.getSharedPreferences(NAME, Context.MODE_PRIVATE)
                .getString(name, "");
    }

    private synchronized static void removeKSFile(Context context, String name) {
        context.getSharedPreferences(NAME, Context.MODE_PRIVATE).edit()
                .remove(name).apply();
    }

    private static final String TAG = "KS";

    private static final String ANDROID_KEY_STORE = "AndroidKeyStore";
    private static final String BLOCK_MODE = KeyProperties.BLOCK_MODE_CBC;
    private static final String PADDING = KeyProperties.ENCRYPTION_PADDING_PKCS7;
    private static final String CIPHER_ALGORITHM = "AES/CBC/PKCS7Padding";

    private synchronized static void setData(
            Context context,
            byte[] data,
            String alias,
            String aliasFile,
            String aliasIV) throws ServiceErrorException {
        if (data == null) {
            throw new ServiceErrorException(
                    ServiceErrorException.INVALID_DATA, "keystore insert data is null");
        }
        KeyStore keyStore;
        try {
            //1、获取keystore
            keyStore = KeyStore.getInstance(ANDROID_KEY_STORE);
            keyStore.load(null);
            // Create the keys if necessary
            if (!keyStore.containsAlias(alias)) {
                //初始化
                KeyGenerator keyGenerator = KeyGenerator.getInstance(
                        KeyProperties.KEY_ALGORITHM_AES,
                        ANDROID_KEY_STORE);

                // Set the alias of the entry in Android KeyStore where the key will appear
                // and the constrains (purposes) in the constructor of the Builder
                keyGenerator.init(new KeyGenParameterSpec.Builder(
                        alias,
                        KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                        .setBlockModes(BLOCK_MODE)
                        .setKeySize(256)
                        .setUserAuthenticationRequired(false)
                        .setRandomizedEncryptionRequired(true)
                        .setEncryptionPaddings(PADDING)
                        .build());
                keyGenerator.generateKey();
            }

            //2、获取加密钥匙
            SecretKey secret = (SecretKey) keyStore.getKey(alias, null);
            if (secret == null) {
                throw new ServiceErrorException(
                        ServiceErrorException.KEY_STORE_SECRET,
                        "secret is null on setData: " + alias);
            }
            Cipher inCipher = Cipher.getInstance(CIPHER_ALGORITHM);
            inCipher.init(Cipher.ENCRYPT_MODE, secret);
            byte[] iv = inCipher.getIV();
            // 將iv轉為base64的string編碼
            String ivStr = new String(Base64.encode(iv, Base64.DEFAULT));
            putKSFile(context, aliasIV, ivStr);
            if (TextUtils.isEmpty(getKSFile(context, aliasIV))) {
                keyStore.deleteEntry(alias);
                throw new ServiceErrorException(
                        ServiceErrorException.FAIL_TO_SAVE_IV_FILE,
                        "Failed to save the iv file for: " + alias);
            }

            //3、加密
            // 加密過後的byte
            byte[] encryptedBytes = inCipher.doFinal(data);
            // 將byte轉為base64的string編碼
            String objectStr = new String(Base64.encode(encryptedBytes, Base64.DEFAULT));
            putKSFile(context, aliasFile, objectStr);
            if (TextUtils.isEmpty(getKSFile(context, aliasFile))) {
                keyStore.deleteEntry(alias);
                throw new ServiceErrorException(
                        ServiceErrorException.KEY_STORE_ERROR,
                        "Failed to save the file for: " + alias);
            }
        } catch (ServiceErrorException ex) {
            Log.d(TAG, "Key store error", ex);
            throw ex;
        } catch (Exception ex) {
            Log.d(TAG, "Key store error", ex);
            throw new ServiceErrorException(ServiceErrorException.KEY_STORE_ERROR);
        }
    }

    private synchronized static byte[] getNewData(
            final Context context,
            String alias,
            String aliasFile,
            String aliasIV)
            throws ServiceErrorException {
        KeyStore keyStore;
        try {
            //1、初始化
            keyStore = KeyStore.getInstance(ANDROID_KEY_STORE);
            keyStore.load(null);
            SecretKey secretKey = (SecretKey) keyStore.getKey(alias, null);
            if (secretKey == null) {
                /* no such key, the key is just simply not there */
                boolean fileExists = !TextUtils.isEmpty(getKSFile(context, aliasFile));
                if (!fileExists) {
                    return null;/* file also not there, fine then */
                }
                throw new ServiceErrorException(
                        ServiceErrorException.KEY_IS_GONE,
                        "file is present but the key is gone: " + alias);
            }

            //1、判定数据
            String ivExists = getKSFile(context, aliasIV);
            String aliasExists = getKSFile(context, aliasFile);
            if (TextUtils.isEmpty(ivExists) || TextUtils.isEmpty(aliasExists)) {
                //report it if one exists and not the other.
                if (TextUtils.isEmpty(ivExists) != TextUtils.isEmpty(aliasFile)) {
                    throw new ServiceErrorException(
                            ServiceErrorException.IV_OR_ALIAS_NO_ON_DISK,
                            "file is present but the key is gone: " + alias);
                } else {
                    throw new ServiceErrorException(
                            ServiceErrorException.IV_OR_ALIAS_NO_ON_DISK,
                            "!ivExists && !aliasExists: " + alias);
                }
            }

            //2、解密
            byte[] iv = Base64.decode(ivExists, Base64.DEFAULT);
            if (iv == null || iv.length == 0) {
                throw new NullPointerException("iv is missing for " + alias);
            }
            Cipher outCipher = Cipher.getInstance(CIPHER_ALGORITHM);
            outCipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(iv));
            byte[] aliasData = Base64.decode(aliasExists, Base64.DEFAULT);
            return outCipher.doFinal(aliasData);
        } catch (InvalidKeyException e) {
            if (e instanceof UserNotAuthenticatedException) {
                throw new ServiceErrorException(ServiceErrorException.USER_NOT_AUTHENTICATED);
            } else {
                throw new ServiceErrorException(ServiceErrorException.INVALID_KEY);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new ServiceErrorException(ServiceErrorException.KEY_STORE_ERROR);
        }
    }

    private synchronized static String getFilePath(Context context, String fileName) {
        File file = getWalletFilePath(context, fileName);
        if (file.exists()) {
            return file.getAbsolutePath();
        }
        return new File(context.getFilesDir(), fileName).getAbsolutePath();
    }

    private synchronized static File getWalletFilePath(Context context, String fileName) {
        File file = context.getDir("qpocket", Context.MODE_PRIVATE);
        return new File(file, fileName);
    }

    private synchronized static void removeAlias(String alias) {
        KeyStore keyStore;
        try {
            keyStore = KeyStore.getInstance(ANDROID_KEY_STORE);
            keyStore.load(null);
            keyStore.deleteEntry(alias);
        } catch (KeyStoreException | CertificateException | NoSuchAlgorithmException | IOException e) {
            e.printStackTrace();
        }
    }

    private static byte[] readBytesFromStream(InputStream in) {
        // this dynamically extends to take the bytes you read
        ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
        // this is storage overwritten on each iteration with bytes
        int bufferSize = 1024;
        byte[] buffer = new byte[bufferSize];
        // we need to know how may bytes were read to write them to the byteBuffer
        int len;
        try {
            while ((len = in.read(buffer)) != -1) {
                byteBuffer.write(buffer, 0, len);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                byteBuffer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (in != null) try {
                in.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        // and then we can return your byte array.
        return byteBuffer.toByteArray();
    }

    private static byte[] readBytesFromFile(String path) {
        byte[] bytes = null;
        FileInputStream fin;
        try {
            File file = new File(path);
            fin = new FileInputStream(file);
            bytes = readBytesFromStream(fin);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return bytes;
    }

    public static void put(Context context, String address, String phrase) throws ServiceErrorException {
        setData(context, phrase.getBytes(), address + "ph", address + "ph", address + "phIV");
    }

    public static void putPD(Context context, String address, String password) throws ServiceErrorException {
        setData(context, password.getBytes(), address + "pd", address + "pd", address + "pdIV");
    }

    //存放keystore
    public static void putKeystore(Context context, String address, String keyStore) throws ServiceErrorException {
        setData(context, keyStore.getBytes(), address + "ks", address + "ks", address + "ksIV");
    }

    //存放keystore
    public static void putPrivateKey(Context context, String address, String pv) throws ServiceErrorException {
        setData(context, pv.getBytes(), address + "pv", address + "pv", address + "pvIV");
    }

    public static byte[] get(Context context, String address) throws ServiceErrorException {
        byte[] data = getNewData(context, address + "ph", address + "ph", address + "phIV");
        if (data == null) {
            data = getOldData(context, address, address, address + "iv", null);
            if (data != null) {
                //用新格式存储数据phra
                put(context, address, new String(data));
            }
        }
        return data;
    }

    public static byte[] getPD(Context context, String address) throws ServiceErrorException {
        byte[] data = getNewData(context, address + "pd", address + "pd", address + "pdIV");
        if (data == null) {
            data = getOldData(context, address + "password", address + "password", address + "passwordIV", null);
            if (data != null) {
                //用新格式存储数据pd
                putPD(context, address, new String(data));
            }
        }
        return data;
    }

    public static byte[] getKeystore(Context context, String address, String password) throws ServiceErrorException {
        byte[] data = getNewData(context, address + "ks", address + "ks", address + "ksIV");
        if (data == null) {
            data = getOldData(context, address + "keystore", address + "keystore", address + "keystoreIV", password);
            if (data != null) {
                //用新格式存储数据pd
                putKeystore(context, address, new String(data));
            }
        }
        return data;
    }

    public static byte[] getPrivateKey(Context context, String address, String password) throws ServiceErrorException {
        byte[] data = getNewData(context, address + "pv", address + "pv", address + "pvIV");
        if (data == null) {
            data = getOldData(context, address + "privateKey", address + "privateKey", address + "privateKeyIV", password);
            if (data != null) {
                //用新格式存储数据pd
                putPrivateKey(context, address, new String(data));
            }
        }
        return data;
    }

    public static void removeKeystoreWallet(Context context, String address) {
        //删除ks
        String alias = address + "ks";
        String aliasIV = address + "ks";
        String aliasFile = address + "ksIV";
        removeAlias(alias);
        removeKSFile(context, aliasIV);
        removeKSFile(context, aliasFile);
        //删除pv
        alias = address + "pv";
        aliasIV = address + "pv";
        aliasFile = address + "pvIV";
        removeAlias(alias);
        removeKSFile(context, aliasIV);
        removeKSFile(context, aliasFile);
    }

    private synchronized static byte[] getOldData(
            final Context context,
            String alias,
            String aliasFile,
            String aliasIV,
            String password)
            throws ServiceErrorException {
        KeyStore keyStore;
        String encryptedDataFilePath = getFilePath(context, aliasFile);
        try {
            keyStore = KeyStore.getInstance(ANDROID_KEY_STORE);
            keyStore.load(null);
            SecretKey secretKey = (SecretKey) keyStore.getKey(alias, TextUtils.isEmpty(password) ? null : password.toCharArray());
            if (secretKey == null) {
                /* no such key, the key is just simply not there */
                boolean fileExists = new File(encryptedDataFilePath).exists();
                if (!fileExists) {
                    return null;/* file also not there, fine then */
                }
                throw new ServiceErrorException(
                        ServiceErrorException.KEY_IS_GONE,
                        "file is present but the key is gone: " + alias);
            }

            boolean ivExists = new File(getFilePath(context, aliasIV)).exists();
            boolean aliasExists = new File(getFilePath(context, aliasFile)).exists();
            if (!ivExists || !aliasExists) {
                //report it if one exists and not the other.
                if (ivExists != aliasExists) {
                    throw new ServiceErrorException(
                            ServiceErrorException.IV_OR_ALIAS_NO_ON_DISK,
                            "file is present but the key is gone: " + alias);
                } else {
                    throw new ServiceErrorException(
                            ServiceErrorException.IV_OR_ALIAS_NO_ON_DISK,
                            "!ivExists && !aliasExists: " + alias);
                }
            }

            byte[] iv = readBytesFromFile(getFilePath(context, aliasIV));
            if (iv == null || iv.length == 0) {
                throw new NullPointerException("iv is missing for " + alias);
            }
            Cipher outCipher = Cipher.getInstance(CIPHER_ALGORITHM);
            outCipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(iv));
            CipherInputStream cipherInputStream = new CipherInputStream(new FileInputStream(encryptedDataFilePath), outCipher);
            return readBytesFromStream(cipherInputStream);
        } catch (InvalidKeyException e) {
            if (e instanceof UserNotAuthenticatedException) {
//				showAuthenticationScreen(context, requestCode);
                throw new ServiceErrorException(ServiceErrorException.USER_NOT_AUTHENTICATED);
            } else {
                throw new ServiceErrorException(ServiceErrorException.INVALID_KEY);
            }
        } catch (IOException | CertificateException | KeyStoreException | UnrecoverableKeyException | NoSuchAlgorithmException | NoSuchPaddingException | InvalidAlgorithmParameterException e) {
            throw new ServiceErrorException(ServiceErrorException.KEY_STORE_ERROR);
        }
    }
}
