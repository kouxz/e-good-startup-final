package com.projeto.egoodapp.data.local;

import com.projeto.egoodapp.models.Vehicle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Domain rules are independent of Activities and Android storage. */
public class LocalState {
    public List<AccountProfile> accounts = new ArrayList<>();
    public List<Vehicle> vehicles = new ArrayList<>();
    public List<Interest> interests = new ArrayList<>();
    public List<VehicleView> vehicleViews = new ArrayList<>();
    public boolean legacyImported;
    public boolean legacyAssigned;

    public AccountProfile account(String uid) {
        for (AccountProfile p : accounts) if (Objects.equals(p.uid, uid)) return p;
        return null;
    }
    public void saveAccount(AccountProfile profile) {
        accounts.removeIf(p -> Objects.equals(p.uid, profile.uid));
        accounts.add(profile);
    }
    public AccountProfile confirmAccountType(String uid, String type, String name, String email) {
        if (uid == null || uid.isEmpty() || !AccountAccess.knownType(type)) {
            throw new IllegalArgumentException("Identificação de conta inválida");
        }
        AccountProfile profile = account(uid);
        if (profile != null && AccountAccess.knownType(profile.type)) return profile;
        if (profile == null) {
            profile = new AccountProfile();
            profile.uid = uid;
        }
        profile.type = type;
        if (profile.name == null || profile.name.isEmpty()) profile.name = name == null ? "" : name;
        if (profile.email == null || profile.email.isEmpty()) profile.email = email == null ? "" : email;
        saveAccount(profile);
        return profile;
    }
    public List<Vehicle> dealerVehicles(String uid) {
        List<Vehicle> result = new ArrayList<>();
        for (Vehicle v : vehicles) if (uid != null && uid.equals(v.getConcessionariaId())) result.add(v);
        return result;
    }
    public boolean assignLegacy(String uid) {
        AccountProfile owner = account(uid);
        if (legacyAssigned || owner == null || !owner.isDealer()) return false;
        for (Vehicle v : vehicles) {
            if (v.getConcessionariaId() == null) v.setConcessionariaId(uid);
            if (v.getId() == null) v.setId(UUID.randomUUID().toString());
        }
        legacyAssigned = true;
        return true;
    }
    public boolean deleteVehicle(String actor, String id) {
        AccountProfile owner = account(actor);
        if (owner == null || !owner.isDealer()) return false;
        return vehicles.removeIf(v -> Objects.equals(v.getId(), id) && actor.equals(v.getConcessionariaId()));
    }
    public boolean addInterest(String userId, String dealerId, Vehicle vehicle) {
        AccountProfile user = account(userId), dealer = account(dealerId);
        if (user == null || user.isDealer() || !user.hasContact() || dealer == null
                || !dealer.isDealer() || !dealer.hasCompanyData()) throw new IllegalArgumentException("Complete os dados do perfil");
        if (vehicle != null && (!dealerId.equals(vehicle.getConcessionariaId())
                || vehicles.stream().noneMatch(v -> Objects.equals(v.getId(), vehicle.getId())))) {
            throw new IllegalArgumentException("Veículo indisponível");
        }
        String vehicleId = vehicle == null ? null : vehicle.getId();
        for (Interest i : interests) {
            if (userId.equals(i.userId) && dealerId.equals(i.dealerId) && Objects.equals(vehicleId, i.vehicleId)) return false;
        }
        Interest i = new Interest();
        i.id = UUID.randomUUID().toString(); i.userId = userId; i.dealerId = dealerId;
        i.vehicleId = vehicleId; i.vehicleName = vehicle == null ? "Interesse geral" : vehicle.getNome();
        i.name = user.name; i.phone = user.phone; i.email = user.email; i.createdAt = System.currentTimeMillis();
        interests.add(i);
        return true;
    }
    public List<Interest> dealerInterests(String uid) {
        List<Interest> result = new ArrayList<>();
        for (Interest i : interests) if (uid != null && uid.equals(i.dealerId)) result.add(i);
        result.sort(Comparator.comparingLong((Interest i) -> i.createdAt).reversed());
        return result;
    }
    public boolean updateStatus(String actor, String id, String status) {
        if (!java.util.Arrays.asList("Novo", "Em contato", "Finalizado").contains(status)) return false;
        AccountProfile owner = account(actor);
        if (owner == null || !owner.isDealer()) return false;
        for (Interest i : interests) if (actor.equals(i.dealerId) && id.equals(i.id)) { i.status = status; return true; }
        return false;
    }
    public boolean recordView(String userId, String vehicleId, long createdAt) {
        AccountProfile user = account(userId);
        Vehicle vehicle = null;
        for (Vehicle candidate : vehicles) if (Objects.equals(candidate.getId(), vehicleId)) vehicle = candidate;
        if (user == null || user.isDealer() || vehicle == null || vehicle.getConcessionariaId() == null) return false;
        if (vehicleViews == null) vehicleViews = new ArrayList<>();
        VehicleView view = new VehicleView();
        view.id = UUID.randomUUID().toString(); view.userId = userId; view.vehicleId = vehicleId;
        view.dealerId = vehicle.getConcessionariaId(); view.createdAt = createdAt;
        vehicleViews.add(view); return true;
    }
    public DealerPerformance performance(String dealerId, long monthStart) {
        int views = 0, contacts = 0, completed = 0;
        if (vehicleViews != null) for (VehicleView view : vehicleViews) {
            if (Objects.equals(dealerId, view.dealerId) && view.createdAt >= monthStart) views++;
        }
        for (Interest interest : interests) {
            if (!Objects.equals(dealerId, interest.dealerId) || interest.createdAt < monthStart) continue;
            contacts++;
            if ("Finalizado".equals(interest.status)) completed++;
        }
        int conversion = contacts == 0 ? 0 : (int) Math.round(completed * 100.0 / contacts);
        return new DealerPerformance(views, contacts, conversion);
    }
}
