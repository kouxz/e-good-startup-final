package com.projeto.egoodapp.views.dealership;

import android.content.Intent;
import android.content.ActivityNotFoundException;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.projeto.egoodapp.R;
import com.projeto.egoodapp.models.Vehicle;
import com.projeto.egoodapp.data.local.AccountProfile;
import com.projeto.egoodapp.data.local.LocalRepository;
import com.projeto.egoodapp.data.local.LocalSession;
import com.projeto.egoodapp.views.LocalProfileForms;
import com.projeto.egoodapp.views.vehicle.VehiclesActivity;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

public class ConcessionariaDetailActivity extends AppCompatActivity {
    private String dealerId, vehicleId, address;
    private LocalRepository repo;
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state); setContentView(R.layout.activity_dealership_detail);
        repo = LocalRepository.get(this); dealerId = getIntent().getStringExtra("dealerId");
        vehicleId = getIntent().getStringExtra("vehicleId");
        findViewById(R.id.btnVoltar).setOnClickListener(v -> finish());
        findViewById(R.id.btnMaps).setOnClickListener(v -> openMaps());
        findViewById(R.id.btnLigar).setOnClickListener(v -> showInterest());
        findViewById(R.id.btnVerVeiculos).setOnClickListener(v -> {
            if (dealerId != null) startActivity(new Intent(this, VehiclesActivity.class).putExtra("dealerId", dealerId));
        });
    }
    @Override protected void onResume() { super.onResume(); render(); }
    private void render() {
        AccountProfile dealer = dealerId == null ? null : repo.account(dealerId);
        if (dealerId != null && dealer == null) {
            Toast.makeText(this, "Concessionária indisponível", Toast.LENGTH_LONG).show(); finish(); return;
        }
        String name = dealer == null ? getIntent().getStringExtra("nome") : dealer.name;
        address = dealer == null ? getIntent().getStringExtra("endereco") : dealer.address + " — " + dealer.city + ", " + dealer.state;
        String phone = dealer == null ? getIntent().getStringExtra("telefone") : dealer.phone;
        label(R.id.tvNomeConcessionaria, name == null || name.isEmpty() ? "Concessionária" : name);
        label(R.id.tvEndereco, address == null ? "Endereço não informado" : address);
        label(R.id.tvTelefone, phone == null || phone.isEmpty() ? "Não informado" : phone);
        boolean locationKnown = getIntent().hasExtra("distancia") && getIntent().getBooleanExtra("hasLocation", true);
        String distance = locationKnown ? String.format(Locale.forLanguageTag("pt-BR"), "%.1f km", getIntent().getDoubleExtra("distancia", 0)) : "Não informado";
        label(R.id.tvDistancia, locationKnown ? "📍 " + distance : "Cadastrada no app");
        label(R.id.tvDistanciaCard, distance);
        label(R.id.tvVeiculos, dealer == null ? "Não informado" : repo.dealerVehicles(dealerId).size() + " modelos");
        label(R.id.tvHorario, "Não informado");
        Button interest = findViewById(R.id.btnLigar);
        interest.setText("Mostrar interesse");
        interest.setEnabled(dealer != null && dealer.hasCompanyData());
        findViewById(R.id.btnVerVeiculos).setEnabled(dealer != null);
        LinearLayout brands = findViewById(R.id.containerMarcas); brands.removeAllViews();
        Set<String> available = new LinkedHashSet<>();
        if (dealer != null) for (Vehicle vehicle : repo.dealerVehicles(dealerId)) available.add(vehicle.getMarca());
        if (available.isEmpty()) available.add(dealer == null ? "Não informado" : "Nenhum veículo cadastrado");
        for (String brand : available) {
            TextView badge = new TextView(this); badge.setText(brand); badge.setTextSize(12);
            badge.setTextColor(getColor(R.color.primary_dark)); badge.setPadding(16, 8, 16, 8);
            badge.setBackgroundResource(R.drawable.bg_badge_teal); brands.addView(badge);
        }
        TextView note = findViewById(R.id.tvInterestAvailability);
        note.setText(dealer == null ? "Interesse disponível apenas para concessionárias cadastradas neste app." : "Seu nome e telefone serão enviados à concessionária.");
    }
    private void showInterest() {
        if (dealerId == null) return;
        AccountProfile user = LocalSession.current(this, "pessoa");
        if (user == null || user.isDealer()) {
            Toast.makeText(this, "Entre com uma conta de usuário para mostrar interesse", Toast.LENGTH_LONG).show(); return;
        }
        if (!user.hasContact()) { LocalProfileForms.edit(this, user, this::showInterest); return; }
        try {
            boolean added = repo.addInterest(user.uid, dealerId, vehicleId);
            Toast.makeText(this, added ? "Interesse registrado! A concessionária recebeu seus dados." : "Você já registrou interesse", Toast.LENGTH_LONG).show();
        } catch (IllegalArgumentException | IllegalStateException e) { Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show(); }
    }
    private void label(int id, String value) { ((TextView) findViewById(id)).setText(value); }
    private void openMaps() {
        if (address == null || address.isEmpty()) return;
        try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=" + Uri.encode(address)))); }
        catch (ActivityNotFoundException e) { Toast.makeText(this, "Nenhum aplicativo de mapas disponível", Toast.LENGTH_SHORT).show(); }
    }
}
