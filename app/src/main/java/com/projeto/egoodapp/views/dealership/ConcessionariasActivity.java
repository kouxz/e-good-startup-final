package com.projeto.egoodapp.views.dealership;

import com.projeto.egoodapp.data.local.LocalRepository;
import com.projeto.egoodapp.data.local.AccountProfile;
import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.projeto.egoodapp.R;

import org.json.JSONArray;
import org.json.JSONObject;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ConcessionariasActivity extends AppCompatActivity {

    private MapView map;
    private RecyclerView recyclerView;
    private ConcessionariaAdapter adapter;
    private List<Concessionaria> listaConcessionarias = new ArrayList<>();
    private final List<Concessionaria> externalDealers = new ArrayList<>();
    
    private FusedLocationProviderClient fusedLocationClient;
    private double userLat = -23.5015; // Padrão: Sorocaba
    private double userLon = -47.4581;
    
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Context ctx = getApplicationContext();
        Configuration.getInstance().load(ctx, androidx.preference.PreferenceManager.getDefaultSharedPreferences(ctx));

        setContentView(R.layout.activity_dealership);

        // Inicializar Google Play Services Location
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Configurar Mapa
        map = findViewById(R.id.mapView);
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setMultiTouchControls(true);

        // Configurar Lista
        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ConcessionariaAdapter(listaConcessionarias);
        recyclerView.setAdapter(adapter);

        // Botão Voltar
        findViewById(R.id.btnVoltar).setOnClickListener(v -> finish());

        // Solicitar localização
        requestUserLocation();
    }

    private void requestUserLocation() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        LOCATION_PERMISSION_REQUEST_CODE);
                return;
            }
        }

        // Permissão já concedida, obter localização
        obterLocalizacaoDoUsuario();
    }

    private void obterLocalizacaoDoUsuario() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        fusedLocationClient.getLastLocation()
            .addOnSuccessListener(location -> {
                if (location != null) {
                    userLat = location.getLatitude();
                    userLon = location.getLongitude();
                    Toast.makeText(ConcessionariasActivity.this, "Localização obtida: " + userLat + ", " + userLon, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(ConcessionariasActivity.this, "Usando localização padrão", Toast.LENGTH_SHORT).show();
                }
                inicializarMapa();
            })
            .addOnFailureListener(e -> {
                Toast.makeText(ConcessionariasActivity.this, "Erro ao obter localização", Toast.LENGTH_SHORT).show();
                inicializarMapa();
            });
    }

    private void inicializarMapa() {
        GeoPoint userPoint = new GeoPoint(userLat, userLon);
        map.getController().setZoom(14.0);
        map.getController().setCenter(userPoint);

        // Adicionar pino do usuário
        Marker userMarker = new Marker(map);
        userMarker.setPosition(userPoint);
        userMarker.setTitle("Você está aqui");
        userMarker.setIcon(getResources().getDrawable(android.R.drawable.ic_menu_mylocation));
        map.getOverlays().add(userMarker);

        buscarDadosAPI();
    }

    private void buscarDadosAPI() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            String queryUrl = "https://overpass-api.de/api/interpreter?data=[out:json];node[\"shop\"=\"car\"](around:5000," + userLat + "," + userLon + ");out;";

            try {
                URL url = new URL(queryUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");

                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) response.append(line);
                reader.close();

                JSONObject jsonObject = new JSONObject(response.toString());
                JSONArray elements = jsonObject.getJSONArray("elements");

                GeoPoint geoUser = new GeoPoint(userLat, userLon);

                List<Concessionaria> lojasEncontradas = new ArrayList<>();

                for (int i = 0; i < elements.length(); i++) {
                    JSONObject node = elements.getJSONObject(i);
                    double lat = node.getDouble("lat");
                    double lon = node.getDouble("lon");

                    String nome = "Concessionária";
                    String endereco = "Endereço não cadastrado";
                    String telefone = "";

                    if (node.has("tags")) {
                        JSONObject tags = node.getJSONObject("tags");
                        if (tags.has("name")) nome = tags.getString("name");

                        String rua = tags.has("addr:street") ? tags.getString("addr:street") : "";
                        String numero = tags.has("addr:housenumber") ? tags.getString("addr:housenumber") : "";

                        if (!rua.isEmpty()) {
                            endereco = rua + (numero.isEmpty() ? "" : ", " + numero);
                        }

                        // Extrair telefone
                        if (tags.has("phone")) {
                            telefone = tags.getString("phone");
                        } else if (tags.has("contact:phone")) {
                            telefone = tags.getString("contact:phone");
                        } else if (tags.has("phone:work")) {
                            telefone = tags.getString("phone:work");
                        }
                    }

                    GeoPoint geoLoja = new GeoPoint(lat, lon);
                    double distanciaMetros = geoUser.distanceToAsDouble(geoLoja);
                    double distanciaKm = distanciaMetros / 1000.0;

                    Concessionaria conc = new Concessionaria(nome, endereco, distanciaKm, lat, lon);
                    conc.setTelefone(telefone);
                    lojasEncontradas.add(conc);
                }

                Collections.sort(lojasEncontradas, (c1, c2) -> Double.compare(c1.getDistanciaKm(), c2.getDistanciaKm()));

                handler.post(() -> {
                    externalDealers.clear(); externalDealers.addAll(lojasEncontradas);
                    refreshLocalDealers();

                    // Adiciona pinos no mapa
                    for (Concessionaria c : listaConcessionarias) {
                        if (!c.hasLocation()) continue;
                        Marker marker = new Marker(map);
                        marker.setPosition(new GeoPoint(c.getLat(), c.getLon()));
                        marker.setTitle(c.getNome());
                        map.getOverlays().add(marker);
                    }
                    map.invalidate();
                });

            } catch (Exception e) {
                e.printStackTrace();
                handler.post(() -> Toast.makeText(this, "Erro ao buscar concessionárias", Toast.LENGTH_SHORT).show());
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                obterLocalizacaoDoUsuario();
            } else {
                Toast.makeText(this, "Permissão de localização negada", Toast.LENGTH_SHORT).show();
                inicializarMapa(); // Usar localização padrão
            }
        }
    }

    private void refreshLocalDealers() {
        if (adapter == null) return;
        listaConcessionarias.clear();
        for (AccountProfile profile : LocalRepository.get(this).dealers()) {
            boolean located = profile.latitude != null && profile.longitude != null;
            double latitude = located ? profile.latitude : 0, longitude = located ? profile.longitude : 0;
            double distance = located ? new GeoPoint(userLat, userLon).distanceToAsDouble(new GeoPoint(latitude, longitude)) / 1000 : 0;
            Concessionaria local = new Concessionaria(profile.name, profile.address + " — " + profile.city + ", " + profile.state, distance, latitude, longitude);
            local.setDealerId(profile.uid); local.setTelefone(profile.phone); local.setHasLocation(located);
            listaConcessionarias.add(local);
        }
        listaConcessionarias.addAll(externalDealers); adapter.notifyDataSetChanged();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (map != null) map.onResume();
        refreshLocalDealers();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (map != null) map.onPause();
    }
}
