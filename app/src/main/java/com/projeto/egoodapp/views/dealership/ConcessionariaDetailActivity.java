package com.projeto.egoodapp.views.dealership;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.projeto.egoodapp.R;
import com.projeto.egoodapp.data.model.AccountProfile;
import com.projeto.egoodapp.data.model.DealerRatingSummary;
import com.projeto.egoodapp.data.local.LocalRepository;
import com.projeto.egoodapp.data.local.LocalSession;
import com.projeto.egoodapp.data.model.Vehicle;
import com.projeto.egoodapp.views.account.LocalProfileForms;
import com.projeto.egoodapp.views.vehicle.VehiclesActivity;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

public class ConcessionariaDetailActivity extends com.projeto.egoodapp.views.common.session.AuthenticatedActivity {
    private String dealerId;
    private String dealerKey;
    private String vehicleId;
    private String address;
    private String dealershipName;
    private double latitude;
    private double longitude;
    private boolean hasLocation;
    private LocalRepository repository;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_dealership_detail);
        repository = LocalRepository.get(this);
        dealerId = getIntent().getStringExtra("dealerId");
        dealerKey = getIntent().getStringExtra("dealerKey");
        if ((dealerKey == null || dealerKey.isEmpty()) && dealerId != null) {
            dealerKey = "local:" + dealerId;
        }
        vehicleId = getIntent().getStringExtra("vehicleId");

        findViewById(R.id.btnVoltar).setOnClickListener(view -> finish());
        findViewById(R.id.btnMaps).setOnClickListener(view -> openMaps());
        findViewById(R.id.btnLigar).setOnClickListener(view -> showInterest());
        findViewById(R.id.btnVerVeiculos).setOnClickListener(view -> {
            if (dealerId != null) {
                startActivity(new Intent(this, VehiclesActivity.class).putExtra("dealerId", dealerId));
            }
        });
        findViewById(R.id.btnDealerRating).setOnClickListener(view -> {
            if (dealerKey != null) {
                DealerRatingDialogs.show(this, dealerKey, dealershipName, this::render);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        render();
    }

    private void render() {
        AccountProfile dealer = dealerId == null ? null : repository.account(dealerId);
        if (dealerId != null && dealer == null) {
            Toast.makeText(this, "Concessionária indisponível", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        dealershipName = dealer == null ? getIntent().getStringExtra("nome") : dealer.name;
        if (dealershipName == null || dealershipName.trim().isEmpty()) dealershipName = "Concessionária";
        address = dealer == null ? getIntent().getStringExtra("endereco") : formatAddress(dealer);
        if (address == null || address.trim().isEmpty()) address = "Endereço não informado";
        String phone = dealer == null ? getIntent().getStringExtra("telefone") : dealer.phone;
        if (phone == null || phone.trim().isEmpty()) phone = "Não informado";

        if (dealer != null && dealer.latitude != null && dealer.longitude != null) {
            latitude = dealer.latitude;
            longitude = dealer.longitude;
            hasLocation = true;
        } else {
            latitude = getIntent().getDoubleExtra("latitude", 0);
            longitude = getIntent().getDoubleExtra("longitude", 0);
            hasLocation = getIntent().getBooleanExtra("hasLocation", false);
        }

        boolean hasDistance = getIntent().hasExtra("distancia") && hasLocation;
        String distance = hasDistance
                ? String.format(Locale.forLanguageTag("pt-BR"), "%.1f km",
                        getIntent().getDoubleExtra("distancia", 0))
                : "Não informada";

        label(R.id.tvNomeConcessionaria, dealershipName);
        label(R.id.tvEndereco, address);
        label(R.id.tvTelefone, phone);
        label(R.id.tvDistancia, hasDistance ? distance : "Distância não informada");
        label(R.id.tvDistanciaCard, distance);
        label(R.id.tvHorario, "Não informado");

        int vehicleCount = dealer == null ? -1 : repository.dealerVehicles(dealerId).size();
        label(R.id.tvVeiculos, vehicleCount < 0 ? "Não informado"
                : vehicleCount + (vehicleCount == 1 ? " modelo" : " modelos"));

        MaterialButton vehicles = findViewById(R.id.btnVerVeiculos);
        vehicles.setVisibility(dealer == null ? View.GONE : View.VISIBLE);
        vehicles.setEnabled(dealer != null);

        MaterialButton interest = findViewById(R.id.btnLigar);
        interest.setEnabled(dealer != null && dealer.hasCompanyData());
        TextView note = findViewById(R.id.tvInterestAvailability);
        note.setText(dealer == null
                ? "Interesse disponível apenas para concessionárias cadastradas neste app."
                : "Seu nome e telefone serão enviados à concessionária.");

        renderBrands(dealer);
        renderRating();
        findViewById(R.id.btnMaps).setEnabled(hasLocation
                || (address != null && !address.equals("Endereço não informado")));
    }

    private void renderBrands(AccountProfile dealer) {
        ChipGroup brands = findViewById(R.id.containerMarcas);
        brands.removeAllViews();
        Set<String> available = new LinkedHashSet<>();
        if (dealer != null) {
            for (Vehicle vehicle : repository.dealerVehicles(dealerId)) {
                if (vehicle.getMarca() != null && !vehicle.getMarca().trim().isEmpty()) {
                    available.add(vehicle.getMarca().trim());
                }
            }
        }
        if (available.isEmpty()) {
            available.add(dealer == null ? "Não informado" : "Nenhum veículo cadastrado");
        }
        for (String brand : available) {
            Chip chip = new Chip(this);
            chip.setText(brand);
            chip.setTextSize(11);
            chip.setTextColor(getColor(R.color.primary_dark));
            chip.setChipBackgroundColor(ColorStateList.valueOf(Color.parseColor("#DDF7F6")));
            chip.setChipStrokeWidth(0);
            chip.setCheckable(false);
            brands.addView(chip);
        }
    }

    private void renderRating() {
        if (dealerKey == null || dealerKey.trim().isEmpty()) return;
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        DealerRatingSummary summary = repository.dealerRating(
                dealerKey, user == null ? null : user.getUid());
        MaterialButton rating = findViewById(R.id.btnDealerRating);
        rating.setText(DealerRatingDialogs.summary(summary.average, summary.count));
        rating.setIconResource(summary.userScore == null
                ? R.drawable.ic_rating_star_outline : R.drawable.ic_rating_star_filled);
        rating.setContentDescription((summary.userScore == null ? "Avaliar " : "Alterar avaliação de ")
                + dealershipName);
    }

    private String formatAddress(AccountProfile dealer) {
        String result = dealer.address == null ? "" : dealer.address.trim();
        String cityState = dealer.city == null ? "" : dealer.city.trim();
        if (dealer.state != null && !dealer.state.trim().isEmpty()) {
            cityState += (cityState.isEmpty() ? "" : ", ") + dealer.state.trim();
        }
        if (!cityState.isEmpty()) result += (result.isEmpty() ? "" : " — ") + cityState;
        return result;
    }

    private void showInterest() {
        if (dealerId == null) return;
        AccountProfile user = LocalSession.current(this, "pessoa");
        if (user == null || user.isDealer()) {
            Toast.makeText(this, "Entre com uma conta de usuário para mostrar interesse",
                    Toast.LENGTH_LONG).show();
            return;
        }
        if (!user.hasContact()) {
            LocalProfileForms.edit(this, user, this::showInterest);
            return;
        }
        try {
            boolean added = repository.addInterest(user.uid, dealerId, vehicleId);
            Toast.makeText(this, added
                    ? "Interesse registrado! A concessionária recebeu seus dados."
                    : "Você já registrou interesse", Toast.LENGTH_LONG).show();
        } catch (IllegalArgumentException | IllegalStateException error) {
            Toast.makeText(this, com.projeto.egoodapp.views.common.feedback.ErrorMessages.safe(error), Toast.LENGTH_LONG).show();
        }
    }

    private void openMaps() {
        Uri uri;
        if (hasLocation) {
            String query = latitude + "," + longitude + "(" + dealershipName + ")";
            uri = Uri.parse("geo:" + latitude + "," + longitude + "?q=" + Uri.encode(query));
        } else if (address != null && !address.trim().isEmpty()) {
            uri = Uri.parse("geo:0,0?q=" + Uri.encode(address));
        } else {
            return;
        }
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, uri));
        } catch (ActivityNotFoundException error) {
            Toast.makeText(this, "Nenhum aplicativo de mapas disponível", Toast.LENGTH_SHORT).show();
        }
    }

    private void label(int id, String value) {
        ((TextView) findViewById(id)).setText(value);
    }
}
