package com.projeto.egoodapp.data.local;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.google.gson.Gson;
import com.projeto.egoodapp.models.Vehicle;
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
    private LocalRepository repo;
    @Before public void setup() {
        Context target = InstrumentationRegistry.getInstrumentation().getTargetContext();
        String suffix = "-test-" + UUID.randomUUID();
        testFiles = new File(target.getFilesDir(), "repository" + suffix); assertTrue(testFiles.mkdirs());
        context = new ContextWrapper(target) {
            @Override public Context getApplicationContext() { return this; }
            @Override public File getFilesDir() { return testFiles; }
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
    }
    @Test public void publishedPhotoAndInterestSurviveRepositoryRecreationAndVehicleDeletion() throws Exception {
        File source = new File(testFiles, "source.png");
        Bitmap bitmap = Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888);
        try (FileOutputStream output = new FileOutputStream(source)) { assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)); }
        bitmap.recycle();
        Vehicle v = vehicle(); v.setImagemUrl(repo.copyPhoto(Uri.fromFile(source)));
        repo.addVehicle("dealer-a", v); assertTrue(source.delete());
        repo = new LocalRepository(context);
        Vehicle loaded = repo.vehicle(v.getId()); assertNotNull(loaded);
        Bitmap copied = BitmapFactory.decodeFile(Uri.parse(loaded.getImagemUrl()).getPath());
        assertNotNull(copied); assertEquals(2, copied.getWidth()); copied.recycle();
        assertTrue(repo.addInterest("user", "dealer-a", v.getId()));
        assertFalse(repo.addInterest("user", "dealer-a", v.getId()));
        Interest lead = repo.interests("dealer-a").get(0); repo.updateStatus("dealer-a", lead.id, "Finalizado");
        assertFalse(repo.deleteVehicle("dealer-b", v.getId())); assertTrue(repo.deleteVehicle("dealer-a", v.getId()));
        repo = new LocalRepository(context);
        assertNull(repo.vehicle(v.getId())); assertTrue(repo.dealerVehicles("dealer-a").isEmpty());
        assertEquals("BYD Seal", repo.interests("dealer-a").get(0).vehicleName);
        assertEquals("Finalizado", repo.interests("dealer-a").get(0).status);
        assertTrue(repo.interests("dealer-b").isEmpty());
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
    }
    private static Vehicle vehicle() { return new Vehicle("BYD", "Seal", 2024, 0, 200000, "Luxo", 60, 450, "Branco", "Descrição", ""); }
    private static AccountProfile profile(String uid, String type) {
        AccountProfile p = new AccountProfile(); p.uid = uid; p.type = type; p.name = uid; p.phone = "11999991234";
        p.email = uid + "@example.com"; p.cnpj = "12345678000199"; p.address = "Rua 1"; p.city = "Sorocaba"; p.state = "SP"; return p;
    }
}
