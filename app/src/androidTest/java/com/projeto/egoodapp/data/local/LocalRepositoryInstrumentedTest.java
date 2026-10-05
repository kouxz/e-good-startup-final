package com.projeto.egoodapp.data.local;

import com.projeto.egoodapp.data.model.AccountProfile;
import com.projeto.egoodapp.data.model.Interest;
import com.projeto.egoodapp.data.model.SecurityAuditEvent;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ExifInterface;
import android.net.Uri;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.google.gson.Gson;
import com.projeto.egoodapp.data.model.Vehicle;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** Uses isolated preferences/files so running this test cannot alter existing app data. */
@RunWith(AndroidJUnit4.class)
public class LocalRepositoryInstrumentedTest {
    private Context context;
    private File testFiles;
    private File testCache;
    private LocalRepository repo;
    @Before public void setup() {
        Context target = InstrumentationRegistry.getInstrumentation().getTargetContext();
        String suffix = "-test-" + UUID.randomUUID();
        testFiles = new File(target.getFilesDir(), "repository" + suffix); assertTrue(testFiles.mkdirs());
        testCache = new File(target.getCacheDir(), "repository" + suffix); assertTrue(testCache.mkdirs());
        context = new ContextWrapper(target) {
            @Override public Context getApplicationContext() { return this; }
            @Override public File getFilesDir() { return testFiles; }
            @Override public File getCacheDir() { return testCache; }
            @Override public SharedPreferences getSharedPreferences(String name, int mode) {
                return super.getSharedPreferences(name + suffix, mode);
            }
        };
        repo = new LocalRepository(context);
        repo.saveAccount(profile("dealer-a", "concessionaria"));
        repo.saveAccount(profile("dealer-b", "concessionaria"));
        repo.saveAccount(profile("user", "pessoa"));
    }
    @After public void cleanup() throws Exception {
        if (context != null) {
            context.getSharedPreferences("LocalAppData", 0).edit().clear().commit();
            context.getSharedPreferences("VeiculosPrefs", 0).edit().clear().commit();
        }
        if (testFiles != null && testFiles.exists()) try (var paths = Files.walk(testFiles.toPath())) {
            for (var path : paths.sorted(Comparator.reverseOrder()).collect(java.util.stream.Collectors.toList())) Files.delete(path);
        }
        if (testCache != null && testCache.exists()) try (var paths = Files.walk(testCache.toPath())) {
            for (var path : paths.sorted(Comparator.reverseOrder()).collect(java.util.stream.Collectors.toList())) Files.delete(path);
        }
    }
    @Test public void privacyAuditCooldownAndSessionRemainEncryptedAndArePurgedPerUid() {
        AccountProfile user = repo.account("user");
        com.projeto.egoodapp.security.PrivacyPolicy.acknowledge(user, 100);
        com.projeto.egoodapp.security.PrivacyPolicy.location(user, true, 200);
        repo.saveAccount(user);
        repo.audit(SecurityAuditEvent.Type.PRIVACY_ACKNOWLEDGED, SecurityAuditEvent.Result.SUCCESS, "user");
        repo.audit(SecurityAuditEvent.Type.LOGIN, SecurityAuditEvent.Result.SUCCESS, "dealer-b");
        com.projeto.egoodapp.security.LoginAttemptPolicy attempts = repo.loginAttempts();
        attempts.failure(System.currentTimeMillis(), false, true); repo.saveLoginAttempts(attempts);
        com.projeto.egoodapp.security.SessionDeadline deadline = new com.projeto.egoodapp.security.SessionDeadline();
        deadline.touch("user", 1000, 100, 1); repo.saveSessionDeadline(deadline);
        String encrypted = context.getSharedPreferences(LocalStateStore.PREFERENCES, 0)
                .getString(LocalStateStore.ENCRYPTED_STATE, "");
        assertFalse(encrypted.contains("PRIVACY_ACKNOWLEDGED"));
        assertFalse(encrypted.contains("locationAllowed"));
        repo = new LocalRepository(context);
        assertTrue(com.projeto.egoodapp.security.PrivacyPolicy.locationAllowed(repo.account("user")));
        assertTrue(com.projeto.egoodapp.security.PrivacyPolicy.acknowledged(repo.account("user")));
        assertEquals(1, repo.auditEvents("user").size());
        assertTrue(repo.loginAttempts().remaining(System.currentTimeMillis()) > 0);
        assertEquals("user", repo.sessionDeadline().userId);
        repo.deleteAccount("user"); repo = new LocalRepository(context);
        assertNull(repo.account("user")); assertNull(repo.sessionDeadline());
        assertTrue(repo.auditEvents("user").isEmpty()); assertEquals(1, repo.auditEvents("dealer-b").size());
    }

    @Test public void publishedPhotoAndInterestSurviveRepositoryRecreationAndVehicleDeletion() throws Exception {
        File source = new File(testFiles, "source.png");
        Bitmap bitmap = Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888);
        try (FileOutputStream output = new FileOutputStream(source)) { assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)); }
        bitmap.recycle();
        Vehicle v = vehicle();
        String pending = repo.preparePhoto(Uri.fromFile(source));
        File pendingFile = new File(Uri.parse(pending).getPath());
        assertTrue(pendingFile.isFile());
        BitmapFactory.Options pendingBounds = new BitmapFactory.Options();
        pendingBounds.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(pendingFile.getAbsolutePath(), pendingBounds);
        assertEquals("image/jpeg", pendingBounds.outMimeType);
        assertNull(new ExifInterface(pendingFile.getAbsolutePath())
                .getAttribute(ExifInterface.TAG_GPS_LATITUDE));
        repo.publishVehicle("dealer-a", v, pending);
        assertFalse(pendingFile.exists());
        assertTrue(source.delete());
        repo = new LocalRepository(context);
        Vehicle loaded = repo.vehicle(v.getId()); assertNotNull(loaded);
        Bitmap copied = BitmapFactory.decodeFile(Uri.parse(loaded.getImagemUrl()).getPath());
        assertNotNull(copied); assertEquals(2, copied.getWidth()); copied.recycle();
        assertTrue(repo.addInterest("user", "dealer-a", v.getId()));
        assertFalse(repo.addInterest("user", "dealer-a", v.getId()));
        Interest lead = repo.interests("dealer-a").get(0);
        assertTrue(repo.updateInterest("dealer-a", lead.id,
                InterestWorkflow.STATUS_FINISHED, InterestWorkflow.OUTCOME_SALE));
        assertFalse(repo.deleteVehicle("dealer-b", v.getId())); assertTrue(repo.deleteVehicle("dealer-a", v.getId()));
        repo = new LocalRepository(context);
        assertNull(repo.vehicle(v.getId())); assertTrue(repo.dealerVehicles("dealer-a").isEmpty());
        assertEquals("BYD Seal", repo.interests("dealer-a").get(0).vehicleName);
        assertEquals("Finalizado", repo.interests("dealer-a").get(0).status);
        assertEquals(InterestWorkflow.OUTCOME_SALE,
                repo.interests("dealer-a").get(0).outcome);
        assertTrue(repo.interests("dealer-b").isEmpty());
    }
    @Test public void localStateIsEncryptedAndSurvivesRepositoryRecreation() {
        SharedPreferences preferences = context.getSharedPreferences(
                LocalStateStore.PREFERENCES, Context.MODE_PRIVATE);
        assertFalse(preferences.contains(LocalStateStore.LEGACY_STATE));
        String encrypted = preferences.getString(LocalStateStore.ENCRYPTED_STATE, null);
        assertNotNull(encrypted);
        assertTrue(encrypted.startsWith("v1."));
        assertFalse(encrypted.contains("dealer-a@example.com"));
        assertFalse(encrypted.contains("11999991234"));

        repo = new LocalRepository(context);
        assertEquals("dealer-a@example.com", repo.account("dealer-a").email);
    }
    @Test public void existingPlaintextStateIsMigratedWithoutLosingTheProfile() {
        SharedPreferences preferences = context.getSharedPreferences(
                LocalStateStore.PREFERENCES, Context.MODE_PRIVATE);
        preferences.edit().clear().putString(LocalStateStore.LEGACY_STATE,
                "{\"legacyImported\":true,\"legacyAssigned\":false,"
                        + "\"accounts\":[{\"uid\":\"legacy-user\",\"type\":\"pessoa\","
                        + "\"name\":\"Perfil legado\",\"email\":\"legacy@example.com\"}],"
                        + "\"vehicles\":[],\"interests\":[],\"vehicleViews\":[],"
                        + "\"dealerRatings\":[]}").commit();

        repo = new LocalRepository(context);
        assertEquals("Perfil legado", repo.account("legacy-user").name);
        assertFalse(preferences.contains(LocalStateStore.LEGACY_STATE));
        String encrypted = preferences.getString(LocalStateStore.ENCRYPTED_STATE, null);
        assertNotNull(encrypted);
        assertFalse(encrypted.contains("legacy@example.com"));
    }
    @Test public void modifiedCiphertextIsRejectedInsteadOfBeingParsed() {
        SharedPreferences preferences = context.getSharedPreferences(
                LocalStateStore.PREFERENCES, Context.MODE_PRIVATE);
        String encrypted = preferences.getString(LocalStateStore.ENCRYPTED_STATE, null);
        assertNotNull(encrypted);
        int index = encrypted.length() - 2;
        char replacement = encrypted.charAt(index) == 'A' ? 'B' : 'A';
        String modified = encrypted.substring(0, index) + replacement
                + encrypted.substring(index + 1);
        preferences.edit().putString(LocalStateStore.ENCRYPTED_STATE, modified).commit();

        try {
            new LocalRepository(context).account("dealer-a");
            fail("Modified encrypted state must not be accepted");
        } catch (IllegalStateException expected) {
            assertTrue(expected.getMessage().contains("autenticados"));
        }
    }
    @Test public void disguisedDocumentIsRejectedWithoutLeavingPendingFiles() throws Exception {
        File source = new File(testFiles, "fake.jpg");
        Files.writeString(source.toPath(), "%PDF-1.7 not an image");
        try {
            repo.preparePhoto(Uri.fromFile(source));
            fail("A non-image document must be rejected");
        } catch (java.io.IOException expected) {
            assertTrue(expected.getMessage().contains("JPEG"));
        }
        File pendingDirectory = new File(testCache, "pending_vehicle_photos");
        File[] leftovers = pendingDirectory.listFiles();
        assertTrue(leftovers == null || leftovers.length == 0);
    }
    @Test public void legacyPreferencesAreImportedAndAssignedOnlyOnce() {
        // Begin with a fresh isolated store, then simulate the pre-update vehicle list.
        context.getSharedPreferences("LocalAppData", 0).edit().clear().commit();
        Vehicle old = vehicle();
        context.getSharedPreferences("VeiculosPrefs", 0).edit().putString("veiculos_list", new Gson().toJson(java.util.Collections.singletonList(old))).commit();
        repo = new LocalRepository(context); repo.saveAccount(profile("dealer-a", "concessionaria"));
        repo.migrateLegacy("dealer-a"); repo = new LocalRepository(context);
        repo.saveAccount(profile("dealer-b", "concessionaria")); repo.migrateLegacy("dealer-b");
        assertEquals(1, repo.dealerVehicles("dealer-a").size()); assertTrue(repo.dealerVehicles("dealer-b").isEmpty());
        assertEquals(old.getId(), repo.dealerVehicles("dealer-a").get(0).getId());
        assertEquals(7, repo.catalog().size());
        assertFalse(context.getSharedPreferences("VeiculosPrefs", 0)
                .contains("veiculos_list"));
        assertFalse(context.getSharedPreferences("LocalAppData", 0)
                .contains(LocalStateStore.LEGACY_STATE));
    }
    @Test public void deletingAccountRemovesOnlyAppOwnedPhotos() throws Exception {
        File source = new File(testFiles, "original.png");
        try (FileOutputStream output = new FileOutputStream(source)) {
            Bitmap bitmap = Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888);
            assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output));
            bitmap.recycle();
        }
        Vehicle owned = vehicle();
        owned.setImagemUrl(repo.copyPhoto(Uri.fromFile(source)));
        repo.addVehicle("dealer-a", owned);
        File copied = new File(Uri.parse(owned.getImagemUrl()).getPath());
        assertTrue(copied.isFile());

        Vehicle external = vehicle();
        external.setImagemUrl(Uri.fromFile(source).toString());
        repo.addVehicle("dealer-b", external);

        repo.deleteAccount("dealer-a");
        assertNull(repo.account("dealer-a"));
        assertNull(repo.vehicle(owned.getId()));
        assertFalse(copied.exists());
        assertTrue(source.exists());

        repo.deleteAccount("dealer-b");
        assertTrue(source.exists());
    }
    private static Vehicle vehicle() { return new Vehicle("BYD", "Seal", 2024, 0, 200000, "Luxo", 60, 450, "Branco", "Descrição", ""); }
    private static AccountProfile profile(String uid, String type) {
        AccountProfile p = new AccountProfile(); p.uid = uid; p.type = type; p.name = uid; p.phone = "11999991234";
        p.email = uid + "@example.com"; p.cnpj = "12345678000199"; p.address = "Rua 1"; p.city = "Sorocaba"; p.state = "SP"; return p;
    }
}
