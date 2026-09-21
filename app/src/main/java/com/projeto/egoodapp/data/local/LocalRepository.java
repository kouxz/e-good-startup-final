package com.projeto.egoodapp.data.local;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import com.google.firebase.auth.FirebaseUser;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.projeto.egoodapp.models.Vehicle;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class LocalRepository {
    private static LocalRepository instance;
    public static synchronized LocalRepository get(Context context) {
        if (instance == null) instance = new LocalRepository(context);
        return instance;
    }
    private final Context context;
    private final SharedPreferences prefs;
    private final Gson gson = new Gson();
    public LocalRepository(Context context) {
        this.context = context.getApplicationContext();
        prefs = this.context.getSharedPreferences("LocalAppData", Context.MODE_PRIVATE);
    }
    private LocalState read() {
        LocalState data = gson.fromJson(prefs.getString("state", "{}"), LocalState.class);
        if (data == null) data = new LocalState();
        if (data.vehicleViews == null) data.vehicleViews = new ArrayList<>();
        if (!data.legacyImported) {
            String json = context.getSharedPreferences("VeiculosPrefs", Context.MODE_PRIVATE).getString("veiculos_list", "[]");
            List<Vehicle> old = gson.fromJson(json, new TypeToken<List<Vehicle>>(){}.getType());
            if (old != null) data.vehicles.addAll(old);
            for (Vehicle v : data.vehicles) if (v.getId() == null) v.setId(UUID.randomUUID().toString());
            data.legacyImported = true;
            write(data);
        }
        return data;
    }
    private void write(LocalState data) {
        if (!prefs.edit().putString("state", gson.toJson(data)).commit()) throw new IllegalStateException("Não foi possível salvar os dados");
    }
    public synchronized AccountProfile ensureAccount(FirebaseUser user, String type) {
        LocalState data = read();
        AccountProfile p = data.account(user.getUid());
        if (p == null) {
            p = new AccountProfile(); p.uid = user.getUid(); p.type = type;
            p.name = user.getDisplayName() == null ? "" : user.getDisplayName();
            p.email = user.getEmail() == null ? "" : user.getEmail();
            data.saveAccount(p); write(data);
        }
        return p;
    }
    public synchronized AccountProfile account(String uid) { return read().account(uid); }
    public synchronized AccountProfile confirmAccountType(FirebaseUser user, String type) {
        LocalState state = read();
        AccountProfile profile = state.confirmAccountType(user.getUid(), type, user.getDisplayName(), user.getEmail());
        write(state);
        return profile;
    }
    public synchronized void saveAccount(AccountProfile p) { LocalState s = read(); s.saveAccount(p); write(s); }
    public synchronized List<AccountProfile> dealers() {
        List<AccountProfile> result = new ArrayList<>();
        for (AccountProfile p : read().accounts) if (p.isDealer() && p.hasCompanyData()) result.add(p);
        return result;
    }
    public synchronized List<Vehicle> dealerVehicles(String uid) { return read().dealerVehicles(uid); }
    public synchronized List<Vehicle> catalog() {
        List<Vehicle> result = new ArrayList<>(DemoCatalog.vehicles()); result.addAll(read().vehicles); return result;
    }
    public synchronized Vehicle vehicle(String id) {
        for (Vehicle v : catalog()) if (Objects.equals(id, v.getId())) return v;
        return null;
    }
    public synchronized void addVehicle(String actor, Vehicle vehicle) {
        LocalState s = read(); AccountProfile p = s.account(actor);
        if (p == null || !p.isDealer() || !p.hasCompanyData()) throw new IllegalArgumentException("Complete o perfil da concessionária antes de publicar");
        vehicle.setConcessionariaId(actor); s.vehicles.add(vehicle); write(s);
    }
    public synchronized boolean deleteVehicle(String actor, String id) {
        LocalState s = read(); boolean removed = s.deleteVehicle(actor, id); if (removed) write(s); return removed;
    }
    public synchronized void migrateLegacy(String uid) {
        LocalState s = read(); if (s.assignLegacy(uid)) write(s);
    }
    /** Called on a worker thread; source documents are copied into app-owned storage. */
    public String copyPhoto(Uri uri) throws IOException {
        File directory = new File(context.getFilesDir(), "vehicle_photos");
        if (!directory.isDirectory() && !directory.mkdirs()) throw new IOException("Não foi possível salvar a foto");
        File destination = new File(directory, UUID.randomUUID() + ".img");
        try (InputStream input = context.getContentResolver().openInputStream(uri)) {
            if (input == null) throw new IOException("Foto indisponível");
            Files.copy(input, destination.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException | SecurityException e) {
            destination.delete(); throw e;
        }
        return Uri.fromFile(destination).toString();
    }
    public void preserveLegacyPhotos(String owner) {
        // Never overwrite a newer snapshot while copying a document in the background.
        for (Vehicle v : dealerVehicles(owner)) {
            String source = v.getImagemUrl();
            if (source == null || !source.startsWith("content:")) continue;
            try {
                String local = copyPhoto(Uri.parse(source));
                synchronized (this) {
                    LocalState s = read();
                    for (Vehicle current : s.vehicles) if (v.getId().equals(current.getId())
                            && source.equals(current.getImagemUrl())) current.setImagemUrl(local);
                    write(s);
                }
            } catch (IOException | SecurityException ignored) { /* Keep original URI and display unavailable if needed. */ }
        }
    }
    public synchronized boolean addInterest(String user, String dealer, String vehicleId) {
        LocalState s = read(); Vehicle v = null;
        if (vehicleId != null) {
            for (Vehicle candidate : s.vehicles) if (vehicleId.equals(candidate.getId())) v = candidate;
            if (v == null) throw new IllegalArgumentException("Veículo indisponível");
        }
        boolean added = s.addInterest(user, dealer, v); if (added) write(s); return added;
    }
    public synchronized List<Interest> interests(String owner) { return read().dealerInterests(owner); }
    public synchronized void updateStatus(String owner, String id, String status) {
        LocalState s = read(); if (s.updateStatus(owner, id, status)) write(s);
    }
    public synchronized boolean recordVehicleView(String user, String vehicle, long createdAt) {
        LocalState s = read(); boolean recorded = s.recordView(user, vehicle, createdAt);
        if (recorded) write(s); return recorded;
    }
    public synchronized DealerPerformance performance(String owner, long monthStart) {
        return read().performance(owner, monthStart);
    }
}
