package com.projeto.egoodapp.data.local;

import com.projeto.egoodapp.data.model.AccountProfile;
import com.projeto.egoodapp.data.model.DealerPerformance;
import com.projeto.egoodapp.data.model.DealerRatingSummary;
import com.projeto.egoodapp.data.model.Interest;
import com.projeto.egoodapp.data.model.SecurityAuditEvent;

import android.content.Context;
import android.net.Uri;
import com.google.firebase.auth.FirebaseUser;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.projeto.egoodapp.data.model.Vehicle;
import java.io.File;
import java.io.IOException;
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
    private final LocalStateStore stateStore;
    private final Gson gson = new Gson();
    public LocalRepository(Context context) {
        this.context = context.getApplicationContext();
        stateStore = new LocalStateStore(this.context);
    }
    private LocalState read() {
        boolean migratePlaintext = stateStore.needsPlaintextMigration();
        LocalState data = gson.fromJson(stateStore.read(), LocalState.class);
        if (data == null) data = new LocalState();
        if (data.vehicleViews == null) data.vehicleViews = new ArrayList<>();
        if (data.dealerRatings == null) data.dealerRatings = new ArrayList<>();
        if (data.loginAttempts == null) data.loginAttempts = new com.projeto.egoodapp.security.LoginAttemptPolicy();
        int previousAuditCount = data.securityAudit == null ? -1 : data.securityAudit.size();
        data.pruneAudit(System.currentTimeMillis());
        boolean changed = migratePlaintext || previousAuditCount != data.securityAudit.size();
        if (data.migrateLegacyInterests()) changed = true;
        if (!data.legacyImported) {
            android.content.SharedPreferences legacy = context.getSharedPreferences(
                    "VeiculosPrefs", Context.MODE_PRIVATE);
            String json = legacy.getString("veiculos_list", "[]");
            List<Vehicle> old = gson.fromJson(json, new TypeToken<List<Vehicle>>(){}.getType());
            if (old != null) data.vehicles.addAll(old);
            for (Vehicle v : data.vehicles) if (v.getId() == null) v.setId(UUID.randomUUID().toString());
            data.legacyImported = true;
            changed = true;
            write(data);
            legacy.edit().remove("veiculos_list").apply();
        } else if (changed) {
            write(data);
        }
        return data;
    }
    private void write(LocalState data) {
        stateStore.write(gson.toJson(data));
    }
    public synchronized AccountProfile ensureAccount(FirebaseUser user, String type) {
        if (user == null || !AccountAccess.knownType(type)) {
            throw new IllegalArgumentException("Identificação de conta inválida");
        }
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
    public synchronized void saveAccount(AccountProfile p) {
        if (p == null || p.uid == null || p.uid.trim().isEmpty()
                || !AccountAccess.knownType(p.type)) {
            throw new IllegalArgumentException("Perfil inválido");
        }
        p.name = com.projeto.egoodapp.security.InputRules.clean(p.name, com.projeto.egoodapp.security.InputRules.NAME);
        p.email = com.projeto.egoodapp.security.InputRules.clean(p.email, com.projeto.egoodapp.security.InputRules.EMAIL);
        p.phone = com.projeto.egoodapp.security.InputRules.clean(p.phone, 15); // Preserve legacy +55 formatting.
        p.cnpj = com.projeto.egoodapp.security.InputRules.clean(p.cnpj, 18); // Preserve legacy formatted CNPJs.
        p.address = com.projeto.egoodapp.security.InputRules.clean(p.address, com.projeto.egoodapp.security.InputRules.ADDRESS);
        p.city = com.projeto.egoodapp.security.InputRules.clean(p.city, com.projeto.egoodapp.security.InputRules.CITY);
        p.state = com.projeto.egoodapp.security.InputRules.clean(p.state, com.projeto.egoodapp.security.InputRules.STATE);
        p.zip = com.projeto.egoodapp.security.InputRules.clean(p.zip, 9);
        p.description = com.projeto.egoodapp.security.InputRules.clean(p.description, com.projeto.egoodapp.security.InputRules.DESCRIPTION);
        LocalState s = read(); s.saveAccount(p); write(s);
    }
    public synchronized void audit(SecurityAuditEvent.Type type, SecurityAuditEvent.Result result, String uid) {
        LocalState state = read(); state.audit(type, result, uid, System.currentTimeMillis()); write(state);
    }
    public synchronized com.projeto.egoodapp.security.LoginAttemptPolicy loginAttempts() { return read().loginAttempts; }
    public synchronized void saveLoginAttempts(com.projeto.egoodapp.security.LoginAttemptPolicy attempts) {
        LocalState state = read(); state.loginAttempts = attempts; write(state);
    }
    public synchronized com.projeto.egoodapp.security.SessionDeadline sessionDeadline() { return read().sessionDeadline; }
    public synchronized void saveSessionDeadline(com.projeto.egoodapp.security.SessionDeadline deadline) {
        LocalState state = read(); state.sessionDeadline = deadline; write(state);
    }
    public synchronized List<SecurityAuditEvent> auditEvents(String uid) {
        List<SecurityAuditEvent> result = new ArrayList<>();
        for (SecurityAuditEvent event : read().securityAudit) if (Objects.equals(uid, event.userId)) result.add(event);
        return result;
    }
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
    public synchronized void publishVehicle(String actor, Vehicle vehicle, String preparedPhoto)
            throws IOException {
        LocalState state = read();
        AccountProfile profile = state.account(actor);
        if (profile == null || !profile.isDealer() || !profile.hasCompanyData()) {
            throw new IllegalArgumentException("Complete o perfil da concessionária antes de publicar");
        }
        String committedPhoto = commitPreparedPhoto(preparedPhoto);
        vehicle.setImagemUrl(committedPhoto);
        vehicle.setConcessionariaId(actor);
        state.vehicles.add(vehicle);
        try {
            write(state);
            discardPreparedPhoto(preparedPhoto);
        } catch (RuntimeException failure) {
            deleteOwnedPhoto(committedPhoto);
            throw failure;
        }
    }
    public synchronized boolean deleteVehicle(String actor, String id) {
        LocalState state = read();
        String photo = null;
        for (Vehicle vehicle : state.vehicles) {
            if (Objects.equals(id, vehicle.getId())
                    && Objects.equals(actor, vehicle.getConcessionariaId())) {
                photo = vehicle.getImagemUrl();
                break;
            }
        }
        boolean removed = state.deleteVehicle(actor, id);
        if (removed) {
            write(state);
            deleteOwnedPhoto(photo);
        }
        return removed;
    }
    public synchronized void deleteAccount(String uid) {
        LocalState state = read();
        List<String> photos = state.deleteAccount(uid);
        write(state);
        for (String photo : photos) deleteOwnedPhoto(photo);
    }
    public synchronized void migrateLegacy(String uid) {
        LocalState s = read(); if (s.assignLegacy(uid)) write(s);
    }
    /** Called on a worker thread; returns a sanitized photo in the app cache. */
    public String preparePhoto(Uri uri) throws IOException {
        return VehiclePhotoImporter.importPhoto(context, uri,
                new File(context.getCacheDir(), "pending_vehicle_photos"));
    }

    /** Called on a worker thread when preserving legacy content URIs. */
    public String copyPhoto(Uri uri) throws IOException {
        File directory = new File(context.getFilesDir(), "vehicle_photos");
        return VehiclePhotoImporter.importPhoto(context, uri, directory);
    }

    public void discardPreparedPhoto(String value) {
        deletePhotoInside(value, new File(context.getCacheDir(), "pending_vehicle_photos"));
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
    public synchronized boolean updateInterest(String owner, String id, String status,
            String outcome) {
        LocalState state = read();
        boolean updated = state.updateInterest(owner, id, status, outcome);
        if (updated) write(state);
        return updated;
    }
    public synchronized boolean recordVehicleView(String user, String vehicle, long createdAt) {
        LocalState s = read(); boolean recorded = s.recordView(user, vehicle, createdAt);
        if (recorded) write(s); return recorded;
    }
    public synchronized DealerPerformance performance(String owner, long periodStart,
            long periodEnd) {
        return read().performance(owner, periodStart, periodEnd);
    }
    public synchronized void rateDealer(String userId, String dealerKey, int score) {
        LocalState state = read();
        state.rateDealer(userId, dealerKey, score, System.currentTimeMillis());
        write(state);
    }
    public synchronized boolean removeDealerRating(String userId, String dealerKey) {
        LocalState state = read();
        boolean removed = state.removeDealerRating(userId, dealerKey);
        if (removed) write(state);
        return removed;
    }
    public synchronized DealerRatingSummary dealerRating(String dealerKey, String userId) {
        return read().dealerRating(dealerKey, userId);
    }

    private void deleteOwnedPhoto(String value) {
        deletePhotoInside(value, new File(context.getFilesDir(), "vehicle_photos"));
    }

    private String commitPreparedPhoto(String value) throws IOException {
        if (value == null || value.trim().isEmpty()) return "";
        if (!value.startsWith("file:")) throw new IOException("Selecione a foto novamente.");
        String path = Uri.parse(value).getPath();
        if (path == null) throw new IOException("Selecione a foto novamente.");

        File pendingDirectory = new File(context.getCacheDir(), "pending_vehicle_photos")
                .getCanonicalFile();
        File source = new File(path).getCanonicalFile();
        if (!isInside(source, pendingDirectory) || !source.isFile()) {
            throw new IOException("Selecione a foto novamente.");
        }

        File photoDirectory = new File(context.getFilesDir(), "vehicle_photos");
        if (!photoDirectory.isDirectory() && !photoDirectory.mkdirs()) {
            throw new IOException("Não foi possível salvar a foto.");
        }
        File destination = new File(photoDirectory, UUID.randomUUID() + ".jpg");
        try {
            Files.copy(source.toPath(), destination.toPath(), StandardCopyOption.REPLACE_EXISTING);
            return Uri.fromFile(destination).toString();
        } catch (IOException | SecurityException error) {
            destination.delete();
            throw error;
        }
    }

    private void deletePhotoInside(String value, File allowedDirectory) {
        if (value == null || !value.startsWith("file:")) return;
        try {
            String path = Uri.parse(value).getPath();
            if (path == null) return;
            File photoDirectory = allowedDirectory.getCanonicalFile();
            File target = new File(path).getCanonicalFile();
            if (isInside(target, photoDirectory)) Files.deleteIfExists(target.toPath());
        } catch (IOException | SecurityException ignored) {
            // The state no longer references the file; a failed best-effort cleanup is harmless.
        }
    }

    private static boolean isInside(File target, File directory) {
        return target.getPath().startsWith(directory.getPath() + File.separator);
    }
}
