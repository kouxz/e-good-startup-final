package com.projeto.egoodapp.data.local;

import com.projeto.egoodapp.data.model.AccountProfile;
import com.projeto.egoodapp.data.model.DealerPerformance;
import com.projeto.egoodapp.data.model.DealerRating;
import com.projeto.egoodapp.data.model.DealerRatingSummary;
import com.projeto.egoodapp.data.model.Interest;
import com.projeto.egoodapp.data.model.SecurityAuditEvent;
import com.projeto.egoodapp.data.model.VehicleView;

import com.projeto.egoodapp.data.model.Vehicle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** Domain rules are independent of Activities and Android storage. */
public class LocalState {
    public List<AccountProfile> accounts = new ArrayList<>();
    public List<Vehicle> vehicles = new ArrayList<>();
    public List<Interest> interests = new ArrayList<>();
    public List<VehicleView> vehicleViews = new ArrayList<>();
    public List<DealerRating> dealerRatings = new ArrayList<>();
    public List<SecurityAuditEvent> securityAudit = new ArrayList<>();
    public com.projeto.egoodapp.security.LoginAttemptPolicy loginAttempts = new com.projeto.egoodapp.security.LoginAttemptPolicy();
    public com.projeto.egoodapp.security.SessionDeadline sessionDeadline;
    public static final int AUDIT_LIMIT = 200;
    public static final long AUDIT_RETENTION_MS = 90L * 24 * 60 * 60 * 1000;
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

    /** Removes every local record that belongs to or identifies the account. */
    public List<String> deleteAccount(String uid) {
        List<String> ownedPhotos = new ArrayList<>();
        if (uid == null || uid.trim().isEmpty()) return ownedPhotos;

        Set<String> ownedVehicleIds = new HashSet<>();
        if (vehicles != null) {
            for (Vehicle vehicle : vehicles) {
                if (!Objects.equals(uid, vehicle.getConcessionariaId())) continue;
                if (vehicle.getId() != null) ownedVehicleIds.add(vehicle.getId());
                if (vehicle.getImagemUrl() != null && !vehicle.getImagemUrl().trim().isEmpty()) {
                    ownedPhotos.add(vehicle.getImagemUrl());
                }
            }
            vehicles.removeIf(vehicle -> Objects.equals(uid, vehicle.getConcessionariaId()));
        }
        if (accounts != null) accounts.removeIf(profile -> Objects.equals(uid, profile.uid));
        if (interests != null) interests.removeIf(interest -> Objects.equals(uid, interest.userId)
                || Objects.equals(uid, interest.dealerId)
                || ownedVehicleIds.contains(interest.vehicleId));
        if (vehicleViews != null) vehicleViews.removeIf(view -> Objects.equals(uid, view.userId)
                || Objects.equals(uid, view.dealerId)
                || ownedVehicleIds.contains(view.vehicleId));
        if (dealerRatings != null) dealerRatings.removeIf(rating -> Objects.equals(uid, rating.userId)
                || Objects.equals("local:" + uid, rating.dealerKey));
        if (securityAudit != null) securityAudit.removeIf(event -> Objects.equals(uid, event.userId));
        if (sessionDeadline != null && Objects.equals(uid, sessionDeadline.userId)) sessionDeadline = null;
        return ownedPhotos;
    }
    public void pruneAudit(long now) {
        if (securityAudit == null) securityAudit = new ArrayList<>();
        securityAudit.removeIf(e -> e == null || e.type == null || e.result == null
                || e.createdAt <= now - AUDIT_RETENTION_MS || e.createdAt > now);
        while (securityAudit.size() > AUDIT_LIMIT) securityAudit.remove(0);
    }
    public void audit(SecurityAuditEvent.Type type, SecurityAuditEvent.Result result, String uid, long now) {
        if (type == null || result == null) throw new IllegalArgumentException("Evento inválido");
        pruneAudit(now);
        securityAudit.add(new SecurityAuditEvent(type, result, uid, now));
        pruneAudit(now);
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
    public boolean migrateLegacyInterests() {
        boolean changed = false;
        if (interests == null) {
            interests = new ArrayList<>();
            return true;
        }
        for (Interest interest : interests) {
            if (InterestWorkflow.migrateLegacy(interest)) changed = true;
        }
        return changed;
    }
    public boolean updateInterest(String actor, String id, String status, String outcome) {
        AccountProfile owner = account(actor);
        if (owner == null || !owner.isDealer()) return false;
        for (Interest interest : interests) {
            if (actor.equals(interest.dealerId) && Objects.equals(id, interest.id)) {
                return InterestWorkflow.update(interest, status, outcome);
            }
        }
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
    public DealerPerformance performance(String dealerId, long periodStart, long periodEnd) {
        int views = 0, contacts = 0, sales = 0;
        if (vehicleViews != null) for (VehicleView view : vehicleViews) {
            if (Objects.equals(dealerId, view.dealerId)
                    && view.createdAt >= periodStart && view.createdAt < periodEnd) views++;
        }
        for (Interest interest : interests) {
            if (!Objects.equals(dealerId, interest.dealerId)
                    || interest.createdAt < periodStart || interest.createdAt >= periodEnd) continue;
            contacts++;
            if (InterestWorkflow.isSale(interest)) sales++;
        }
        int conversion = contacts == 0 ? 0 : (int) Math.round(sales * 100.0 / contacts);
        return new DealerPerformance(views, contacts, sales, conversion);
    }

    public void rateDealer(String userId, String dealerKey, int score, long updatedAt) {
        AccountProfile user = account(userId);
        if (user == null || user.isDealer()) {
            throw new IllegalArgumentException("Apenas usuários Pessoa podem avaliar concessionárias");
        }
        if (dealerKey == null || dealerKey.trim().isEmpty() || score < 1 || score > 5) {
            throw new IllegalArgumentException("Avaliação inválida");
        }
        if (dealerRatings == null) dealerRatings = new ArrayList<>();
        for (DealerRating rating : dealerRatings) {
            if (Objects.equals(userId, rating.userId) && Objects.equals(dealerKey, rating.dealerKey)) {
                rating.score = score;
                rating.updatedAt = updatedAt;
                return;
            }
        }
        DealerRating rating = new DealerRating();
        rating.userId = userId;
        rating.dealerKey = dealerKey;
        rating.score = score;
        rating.updatedAt = updatedAt;
        dealerRatings.add(rating);
    }

    public boolean removeDealerRating(String userId, String dealerKey) {
        if (dealerRatings == null) return false;
        return dealerRatings.removeIf(rating -> Objects.equals(userId, rating.userId)
                && Objects.equals(dealerKey, rating.dealerKey));
    }

    public DealerRatingSummary dealerRating(String dealerKey, String userId) {
        int count = 0;
        int total = 0;
        Integer userScore = null;
        if (dealerRatings != null) {
            for (DealerRating rating : dealerRatings) {
                if (!Objects.equals(dealerKey, rating.dealerKey)) continue;
                total += rating.score;
                count++;
                if (Objects.equals(userId, rating.userId)) userScore = rating.score;
            }
        }
        return new DealerRatingSummary(count == 0 ? 0 : total / (double) count, count, userScore);
    }
}
