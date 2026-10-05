package com.projeto.egoodapp.views.dealership;

import com.projeto.egoodapp.data.model.Concessionaria;
import com.projeto.egoodapp.data.nearby.BoundedResponseReader;
import com.projeto.egoodapp.data.nearby.NearbyLocationPolicy;
import com.projeto.egoodapp.data.nearby.OverpassDealerParser;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.CancellationTokenSource;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.projeto.egoodapp.R;
import com.projeto.egoodapp.data.model.AccountProfile;
import com.projeto.egoodapp.data.model.DealerRatingSummary;
import com.projeto.egoodapp.data.local.LocalRepository;
import com.projeto.egoodapp.data.model.Vehicle;
import com.projeto.egoodapp.views.vehicle.VehiclesActivity;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.json.JSONArray;
import org.json.JSONObject;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.FolderOverlay;
import org.osmdroid.views.overlay.Marker;

public class ConcessionariasActivity extends com.projeto.egoodapp.views.common.session.AuthenticatedActivity implements com.projeto.egoodapp.views.privacy.PrivacyUi.LocationConsentHost {
    private static final int MAX_OVERPASS_RESPONSE_BYTES = 2 * 1024 * 1024;
    private static final String TAG = "NearbyDealers";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 100;
    private static final long LOCATION_TIMEOUT_MS = 8000L;
    private static final double DEFAULT_LATITUDE = -23.5015;
    private static final double DEFAULT_LONGITUDE = -47.4581;
    private static final String[] OVERPASS_ENDPOINTS = {
            "https://overpass-api.de/api/interpreter",
            "https://overpass.private.coffee/api/interpreter"
    };

    private final List<Concessionaria> dealerships = new ArrayList<>();
    private final List<Concessionaria> externalDealers = new ArrayList<>();
    private final ExecutorService networkExecutor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final NearbyLocationPolicy.RequestGate locationRequestGate =
            new NearbyLocationPolicy.RequestGate();

    private LocalRepository repository;
    private MapView map;
    private FolderOverlay markerLayer;
    private RecyclerView recyclerView;
    private ConcessionariaAdapter adapter;
    private ChipGroup shortcutGroup;
    private TextView mapCount;
    private ProgressBar mapLoading;
    private View emptyState;
    private FusedLocationProviderClient fusedLocationClient;
    private double userLat = DEFAULT_LATITUDE;
    private double userLon = DEFAULT_LONGITUDE;
    private boolean searchStarted;
    private boolean locationAuthorized;
    private int nearbySearchGeneration;
    private CancellationTokenSource currentLocationCancellation;
    private Runnable locationTimeout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Configuration.getInstance().load(getApplicationContext(),
                androidx.preference.PreferenceManager.getDefaultSharedPreferences(getApplicationContext()));
        Configuration.getInstance().setUserAgentValue(getPackageName());
        setContentView(R.layout.activity_dealership);

        repository = LocalRepository.get(this);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        map = findViewById(R.id.mapView);
        recyclerView = findViewById(R.id.recyclerView);
        shortcutGroup = findViewById(R.id.dealershipShortcutGroup);
        mapCount = findViewById(R.id.dealershipMapCount);
        mapLoading = findViewById(R.id.dealershipMapLoading);
        emptyState = findViewById(R.id.dealershipEmptyState);

        configureMap();
        configureList();
        mapCount.setOnClickListener(view -> {
            if (!searchStarted) startNearbySearch();
        });
        findViewById(R.id.btnVoltar).setOnClickListener(view -> finish());
        refreshDealers();
        requestUserLocation();
    }

    private void configureMap() {
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setMultiTouchControls(true);
        map.setBuiltInZoomControls(false);
        map.getController().setZoom(13.5);
        map.getController().setCenter(new GeoPoint(userLat, userLon));
        markerLayer = new FolderOverlay();
        map.getOverlays().add(markerLayer);
    }

    private void configureList() {
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ConcessionariaAdapter(new ConcessionariaAdapter.Listener() {
            @Override public void onOpen(Concessionaria dealership) { openDealership(dealership); }
            @Override public void onVehicles(Concessionaria dealership) {
                if (dealership.isLocal()) {
                    startActivity(new Intent(ConcessionariasActivity.this, VehiclesActivity.class)
                            .putExtra("dealerId", dealership.getDealerId()));
                }
            }
            @Override public void onRate(Concessionaria dealership) {
                DealerRatingDialogs.show(ConcessionariasActivity.this,
                        dealership.getStableKey(), dealership.getNome(),
                        ConcessionariasActivity.this::refreshDealers);
            }
        });
        recyclerView.setAdapter(adapter);
    }

    private void requestUserLocation() {
        com.projeto.egoodapp.views.privacy.PrivacyUi.requestLocation(this);
    }
    @Override public void onLocationConsentResult(boolean allowed) {
        locationAuthorized = allowed;
        if (!allowed) { useDefaultLocationAndSearch(); return; }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && !hasLocationPermission()) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
            return;
        }
        obtainUserLocation();
    }

    @SuppressLint("MissingPermission")
    private void obtainUserLocation() {
        if (!com.projeto.egoodapp.views.privacy.PrivacyUi.locationAllowed(this) || !hasLocationPermission()) {
            useDefaultLocationAndSearch();
            return;
        }
        int requestGeneration = locationRequestGate.begin();
        mapLoading.setVisibility(View.VISIBLE);
        mapCount.setClickable(false);
        mapCount.setText("Obtendo localização");
        locationTimeout = () -> completeLocationResolution(requestGeneration, null);
        mainHandler.postDelayed(locationTimeout, LOCATION_TIMEOUT_MS);

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (!isLocationRequestActive(requestGeneration)) return;
                    if (location != null && NearbyLocationPolicy.canUseCached(
                            location.getLatitude(), location.getLongitude(), location.getTime(),
                            System.currentTimeMillis())) {
                        completeLocationResolution(requestGeneration, location);
                    } else {
                        requestCurrentLocation(requestGeneration);
                    }
                })
                .addOnFailureListener(error -> requestCurrentLocation(requestGeneration));
    }

    @SuppressLint("MissingPermission")
    private void requestCurrentLocation(int requestGeneration) {
        if (!isLocationRequestActive(requestGeneration)) return;
        if (!com.projeto.egoodapp.views.privacy.PrivacyUi.locationAllowed(this) || !hasLocationPermission()) {
            completeLocationResolution(requestGeneration, null);
            return;
        }
        if (currentLocationCancellation != null) currentLocationCancellation.cancel();
        currentLocationCancellation = new CancellationTokenSource();
        fusedLocationClient.getCurrentLocation(
                        Priority.PRIORITY_HIGH_ACCURACY,
                        currentLocationCancellation.getToken())
                .addOnSuccessListener(location ->
                        completeLocationResolution(requestGeneration, location))
                .addOnFailureListener(error ->
                        completeLocationResolution(requestGeneration, null));
    }

    private boolean isLocationRequestActive(int requestGeneration) {
        return locationRequestGate.isActive(requestGeneration)
                && com.projeto.egoodapp.views.privacy.PrivacyUi.locationAllowed(this)
                && !isFinishing() && !isDestroyed();
    }

    private void completeLocationResolution(int requestGeneration, Location location) {
        if (isFinishing() || isDestroyed()
                || !locationRequestGate.complete(requestGeneration)) return;
        if (locationTimeout != null) {
            mainHandler.removeCallbacks(locationTimeout);
            locationTimeout = null;
        }
        if (currentLocationCancellation != null) {
            currentLocationCancellation.cancel();
            currentLocationCancellation = null;
        }
        if (com.projeto.egoodapp.views.privacy.PrivacyUi.locationAllowed(this) && location != null && NearbyLocationPolicy.hasValidCoordinates(
                location.getLatitude(), location.getLongitude())) {
            applyLocationAndSearch(location);
        } else {
            useDefaultLocationAndSearch();
        }
    }

    private void applyLocationAndSearch(Location location) {
        userLat = location.getLatitude();
        userLon = location.getLongitude();
        map.getController().setCenter(new GeoPoint(userLat, userLon));
        refreshDealers();
        startNearbySearch();
    }

    private void useDefaultLocationAndSearch() {
        userLat = DEFAULT_LATITUDE;
        userLon = DEFAULT_LONGITUDE;
        map.getController().setCenter(new GeoPoint(userLat, userLon));
        refreshDealers();
        startNearbySearch();
    }

    private boolean hasLocationPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void startNearbySearch() {
        if (searchStarted) return;
        searchStarted = true;
        mapLoading.setVisibility(View.VISIBLE);
        mapCount.setClickable(false);
        mapCount.setText("Buscando concessionárias próximas");
        fetchExternalDealers();
    }

    private void fetchExternalDealers() {
        int generation = ++nearbySearchGeneration;
        double latitude = userLat, longitude = userLon;
        networkExecutor.execute(() -> {
            try {
                String query = "[out:json][timeout:15];node[\"shop\"=\"car\"](around:5000,"
                        + latitude + "," + longitude + ");out body;";
                String response = executeOverpass(query);
                JSONArray elements = new JSONObject(response).getJSONArray("elements");
                List<Concessionaria> found = OverpassDealerParser.parse(
                        elements, latitude, longitude);
                mainHandler.post(() -> {
                    if (isFinishing() || isDestroyed() || generation != nearbySearchGeneration) return;
                    externalDealers.clear();
                    externalDealers.addAll(found);
                    searchStarted = true;
                    mapLoading.setVisibility(View.GONE);
                    mapCount.setClickable(false);
                    refreshDealers();
                });
            } catch (Exception error) {
                Log.w(TAG, "All Overpass endpoints failed", error);
                mainHandler.post(() -> {
                    if (isFinishing() || isDestroyed() || generation != nearbySearchGeneration) return;
                    searchStarted = false;
                    mapLoading.setVisibility(View.GONE);
                    refreshDealers();
                    mapCount.setClickable(true);
                    mapCount.setContentDescription("Tentar novamente a busca de concessionárias próximas");
                    mapCount.setText(dealerships.isEmpty()
                            ? "Locais indisponíveis · toque para tentar novamente"
                            : "Mostrando cadastros do app · toque para atualizar");
                });
            }
        });
    }

    private String executeOverpass(String query) throws Exception {
        Exception lastError = null;
        for (String endpoint : OVERPASS_ENDPOINTS) {
            HttpURLConnection connection = null;
            try {
                connection = (HttpURLConnection) new URL(endpoint).openConnection();
                connection.setRequestMethod("POST");
                connection.setDoOutput(true);
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(18000);
                connection.setInstanceFollowRedirects(false);
                connection.setRequestProperty("User-Agent", getPackageName() + "/1.0");
                connection.setRequestProperty("Accept", "application/json");
                connection.setRequestProperty("Content-Type",
                        "application/x-www-form-urlencoded; charset=UTF-8");
                try (OutputStreamWriter writer = new OutputStreamWriter(
                        connection.getOutputStream(), StandardCharsets.UTF_8)) {
                    writer.write("data=" + URLEncoder.encode(
                            query, StandardCharsets.UTF_8.name()));
                }
                int status = connection.getResponseCode();
                if (status != HttpURLConnection.HTTP_OK) {
                    throw new IllegalStateException(endpoint + " returned HTTP " + status);
                }
                long announcedLength = connection.getContentLengthLong();
                if (announcedLength > MAX_OVERPASS_RESPONSE_BYTES) {
                    throw new IllegalStateException("Overpass response exceeds size limit");
                }
                String body;
                try (java.io.InputStream input = connection.getInputStream()) {
                    body = BoundedResponseReader.readUtf8(input, MAX_OVERPASS_RESPONSE_BYTES);
                }
                new JSONObject(body).getJSONArray("elements");
                return body;
            } catch (Exception error) {
                lastError = error;
                Log.w(TAG, "Overpass request failed at " + endpoint, error);
            } finally {
                if (connection != null) connection.disconnect();
            }
        }
        throw lastError == null ? new IllegalStateException("No Overpass endpoint available") : lastError;
    }

    private void refreshDealers() {
        dealerships.clear();
        for (AccountProfile profile : repository.dealers()) {
            boolean located = profile.latitude != null && profile.longitude != null;
            double latitude = located ? profile.latitude : 0;
            double longitude = located ? profile.longitude : 0;
            double distance = located
                    ? new GeoPoint(userLat, userLon)
                            .distanceToAsDouble(new GeoPoint(latitude, longitude)) / 1000d
                    : 0;
            Concessionaria local = new Concessionaria(profile.name,
                    formatLocalAddress(profile), distance, latitude, longitude);
            local.setDealerId(profile.uid);
            local.setStableKey("local:" + profile.uid);
            local.setTelefone(profile.phone);
            local.setHasLocation(located);

            List<Vehicle> stock = repository.dealerVehicles(profile.uid);
            local.setVehicleCount(stock.size());
            Set<String> brands = new LinkedHashSet<>();
            for (Vehicle vehicle : stock) {
                if (vehicle.getMarca() != null && !vehicle.getMarca().trim().isEmpty()) {
                    brands.add(vehicle.getMarca().trim());
                }
            }
            local.setBrands(new ArrayList<>(brands));
            dealerships.add(local);
        }
        dealerships.addAll(externalDealers);
        dealerships.sort(Comparator
                .comparing((Concessionaria dealership) -> !dealership.isLocal())
                .thenComparing(dealership -> !dealership.hasLocation())
                .thenComparingDouble(Concessionaria::getDistanciaKm)
                .thenComparing(Concessionaria::getNome, String.CASE_INSENSITIVE_ORDER));
        applyRatings();
        adapter.submitList(dealerships);
        emptyState.setVisibility(dealerships.isEmpty() ? View.VISIBLE : View.GONE);
        renderShortcuts();
        renderMarkers();
    }

    private String formatLocalAddress(AccountProfile profile) {
        List<String> parts = new ArrayList<>();
        if (profile.address != null && !profile.address.trim().isEmpty()) parts.add(profile.address.trim());
        String cityState = "";
        if (profile.city != null && !profile.city.trim().isEmpty()) cityState = profile.city.trim();
        if (profile.state != null && !profile.state.trim().isEmpty()) {
            cityState += (cityState.isEmpty() ? "" : ", ") + profile.state.trim();
        }
        if (!cityState.isEmpty()) parts.add(cityState);
        return parts.isEmpty() ? "Endereço não informado" : String.join(" — ", parts);
    }

    private void applyRatings() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        String userId = user == null ? null : user.getUid();
        for (Concessionaria dealership : dealerships) {
            DealerRatingSummary summary = repository.dealerRating(dealership.getStableKey(), userId);
            dealership.setRating(summary.average, summary.count, summary.userScore);
        }
    }

    private void renderShortcuts() {
        shortcutGroup.removeAllViews();
        int added = 0;
        for (Concessionaria dealership : dealerships) {
            if (!dealership.hasLocation()) continue;
            Chip chip = new Chip(this);
            chip.setText(dealership.getNome() + " · "
                    + String.format(Locale.forLanguageTag("pt-BR"), "%.1f km", dealership.getDistanciaKm()));
            chip.setTextSize(10);
            chip.setTextColor(ContextCompat.getColor(this, R.color.app_text_secondary));
            chip.setChipBackgroundColor(ColorStateList.valueOf(
                    ContextCompat.getColor(this, R.color.app_surface)));
            chip.setChipStrokeColor(ColorStateList.valueOf(
                    ContextCompat.getColor(this, R.color.app_stroke)));
            chip.setChipStrokeWidth(getResources().getDisplayMetrics().density);
            chip.setChipIconResource(R.drawable.ic_user_dealership);
            chip.setChipIconTint(ColorStateList.valueOf(getColor(R.color.primary_dark)));
            chip.setChipIconVisible(true);
            chip.setCheckable(false);
            chip.setOnClickListener(view -> focusDealership(dealership));
            shortcutGroup.addView(chip);
            if (++added == 3) break;
        }
        findViewById(R.id.dealershipShortcutScroll)
                .setVisibility(added == 0 ? View.GONE : View.VISIBLE);
    }

    private void renderMarkers() {
        markerLayer.getItems().clear();
        Marker userMarker = new Marker(map);
        userMarker.setPosition(new GeoPoint(userLat, userLon));
        userMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
        userMarker.setTitle("Você está aqui");
        userMarker.setIcon(ContextCompat.getDrawable(this, R.drawable.ic_map_user_marker));
        markerLayer.add(userMarker);

        int located = 0;
        for (Concessionaria dealership : dealerships) {
            if (!dealership.hasLocation()) continue;
            located++;
            Marker marker = new Marker(map);
            marker.setPosition(new GeoPoint(dealership.getLat(), dealership.getLon()));
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            marker.setTitle(dealership.getNome());
            marker.setSnippet(dealership.getEndereco());
            marker.setIcon(ContextCompat.getDrawable(this, R.drawable.ic_map_dealer_marker));
            marker.setOnMarkerClickListener((selected, mapView) -> {
                focusDealership(dealership);
                selected.showInfoWindow();
                return true;
            });
            markerLayer.add(marker);
        }
        mapCount.setText(located + (located == 1
                ? " concessionária próxima" : " concessionárias próximas"));
        map.invalidate();
    }

    private void focusDealership(Concessionaria dealership) {
        if (dealership.hasLocation()) {
            map.getController().animateTo(new GeoPoint(dealership.getLat(), dealership.getLon()));
            if (map.getZoomLevelDouble() < 15) map.getController().setZoom(15.0);
        }
        int position = dealerships.indexOf(dealership);
        if (position >= 0) recyclerView.smoothScrollToPosition(position);
    }

    private void openDealership(Concessionaria dealership) {
        Intent intent = new Intent(this, ConcessionariaDetailActivity.class)
                .putExtra("nome", dealership.getNome())
                .putExtra("endereco", dealership.getEndereco())
                .putExtra("distancia", dealership.getDistanciaKm())
                .putExtra("telefone", dealership.getTelefone())
                .putExtra("dealerId", dealership.getDealerId())
                .putExtra("dealerKey", dealership.getStableKey())
                .putExtra("hasLocation", dealership.hasLocation())
                .putExtra("latitude", dealership.getLat())
                .putExtra("longitude", dealership.getLon());
        startActivity(intent);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != LOCATION_PERMISSION_REQUEST_CODE) return;
        if (com.projeto.egoodapp.views.privacy.PrivacyUi.locationAllowed(this) && hasLocationPermission()) {
            obtainUserLocation();
        } else {
            Toast.makeText(this, "Usando a localização padrão de Sorocaba", Toast.LENGTH_SHORT).show();
            useDefaultLocationAndSearch();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        map.onResume();
        boolean authorized = com.projeto.egoodapp.views.privacy.PrivacyUi.locationAllowed(this);
        if (!authorized && (locationAuthorized || userLat != DEFAULT_LATITUDE || userLon != DEFAULT_LONGITUDE)) {
            locationRequestGate.cancel();
            if (locationTimeout != null) mainHandler.removeCallbacks(locationTimeout);
            if (currentLocationCancellation != null) currentLocationCancellation.cancel();
            searchStarted = false; externalDealers.clear(); useDefaultLocationAndSearch();
        }
        if (authorized && !locationAuthorized) {
            searchStarted = false; onLocationConsentResult(true);
        }
        locationAuthorized = authorized;
        refreshDealers();
    }

    @Override
    protected void onPause() {
        map.onPause();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        locationRequestGate.cancel();
        if (locationTimeout != null) mainHandler.removeCallbacks(locationTimeout);
        if (currentLocationCancellation != null) currentLocationCancellation.cancel();
        networkExecutor.shutdownNow();
        map.onDetach();
        super.onDestroy();
    }
}
