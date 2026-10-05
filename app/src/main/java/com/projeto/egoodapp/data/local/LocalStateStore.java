package com.projeto.egoodapp.data.local;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/** Encrypts the local repository snapshot and authenticates it before parsing. */
final class LocalStateStore {
    static final String PREFERENCES = "LocalAppData";
    static final String LEGACY_STATE = "state";
    static final String ENCRYPTED_STATE = "state_encrypted_v1";

    private static final String KEY_ALIAS = "egood_local_state_v1";
    private static final String FORMAT = "v1";
    private static final byte[] ASSOCIATED_DATA =
            "com.projeto.egoodapp.local-state.v1".getBytes(StandardCharsets.UTF_8);
    private static final Object KEY_LOCK = new Object();

    private final SharedPreferences preferences;

    LocalStateStore(Context context) {
        preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
    }

    String read() {
        String encrypted = preferences.getString(ENCRYPTED_STATE, null);
        if (encrypted != null) return decrypt(encrypted);
        return preferences.getString(LEGACY_STATE, "{}");
    }

    boolean needsPlaintextMigration() {
        return !preferences.contains(ENCRYPTED_STATE) && preferences.contains(LEGACY_STATE);
    }

    void write(String serialized) {
        String encrypted = encrypt(serialized == null ? "{}" : serialized);
        boolean saved = preferences.edit()
                .putString(ENCRYPTED_STATE, encrypted)
                .remove(LEGACY_STATE)
                .commit();
        if (!saved) throw new IllegalStateException("Não foi possível salvar os dados locais");
    }

    private static String encrypt(String plaintext) {
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key());
            cipher.updateAAD(ASSOCIATED_DATA);
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            return FORMAT + "." + Base64.encodeToString(cipher.getIV(), Base64.NO_WRAP)
                    + "." + Base64.encodeToString(ciphertext, Base64.NO_WRAP);
        } catch (GeneralSecurityException failure) {
            throw new IllegalStateException("Não foi possível proteger os dados locais", failure);
        }
    }

    private static String decrypt(String encoded) {
        try {
            String[] parts = encoded.split("\\.", 3);
            if (parts.length != 3 || !FORMAT.equals(parts[0])) {
                throw new GeneralSecurityException("Unknown encrypted state format");
            }
            byte[] iv = Base64.decode(parts[1], Base64.NO_WRAP);
            byte[] ciphertext = Base64.decode(parts[2], Base64.NO_WRAP);
            if (iv.length != 12 || ciphertext.length < 16) {
                throw new GeneralSecurityException("Invalid encrypted state payload");
            }
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key(), new GCMParameterSpec(128, iv));
            cipher.updateAAD(ASSOCIATED_DATA);
            return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException failure) {
            throw new IllegalStateException(
                    "Os dados locais não puderam ser autenticados. Entre novamente.", failure);
        }
    }

    private static SecretKey key() throws GeneralSecurityException {
        synchronized (KEY_LOCK) {
            KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
            try {
                keyStore.load(null);
            } catch (java.io.IOException failure) {
                throw new GeneralSecurityException(failure);
            }
            java.security.Key existing = keyStore.getKey(KEY_ALIAS, null);
            if (existing instanceof SecretKey) return (SecretKey) existing;

            KeyGenerator generator = KeyGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
            generator.init(new KeyGenParameterSpec.Builder(KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setRandomizedEncryptionRequired(true)
                    .setKeySize(256)
                    .build());
            return generator.generateKey();
        }
    }
}
