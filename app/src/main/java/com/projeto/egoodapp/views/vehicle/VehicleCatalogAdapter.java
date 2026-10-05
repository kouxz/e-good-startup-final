package com.projeto.egoodapp.views.vehicle;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.projeto.egoodapp.R;
import com.projeto.egoodapp.data.model.Vehicle;
import com.projeto.egoodapp.views.vehicle.VehiclePhotos;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

final class VehicleCatalogAdapter extends RecyclerView.Adapter<VehicleCatalogAdapter.VehicleViewHolder> {
    interface OnVehicleClickListener {
        void onVehicleClick(Vehicle vehicle);
    }

    private final List<Vehicle> vehicles = new ArrayList<>();
    private final Map<String, Long> stableIds = new HashMap<>();
    private final OnVehicleClickListener listener;
    private final NumberFormat currency = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("pt-BR"));
    private long nextStableId;

    VehicleCatalogAdapter(OnVehicleClickListener listener) {
        this.listener = listener;
        setHasStableIds(true);
    }

    void submitList(List<Vehicle> updated) {
        vehicles.clear();
        vehicles.addAll(updated);
        notifyDataSetChanged();
    }

    @Override
    public long getItemId(int position) {
        String id = vehicles.get(position).getId();
        if (id == null) return RecyclerView.NO_ID;
        Long stableId = stableIds.get(id);
        if (stableId == null) {
            stableId = nextStableId++;
            stableIds.put(id, stableId);
        }
        return stableId;
    }

    @NonNull
    @Override
    public VehicleViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_vehicle_catalog, parent, false);
        return new VehicleViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VehicleViewHolder holder, int position) {
        holder.bind(vehicles.get(position));
    }

    @Override
    public void onViewRecycled(@NonNull VehicleViewHolder holder) {
        holder.clearPhoto();
        super.onViewRecycled(holder);
    }

    @Override
    public int getItemCount() {
        return vehicles.size();
    }

    final class VehicleViewHolder extends RecyclerView.ViewHolder {
        private final ImageView photo;
        private final TextView photoUnavailable;
        private final TextView badge;
        private final TextView name;
        private final TextView price;
        private final TextView autonomy;
        private final TextView battery;
        private final TextView category;

        VehicleViewHolder(@NonNull View itemView) {
            super(itemView);
            photo = itemView.findViewById(R.id.catalogVehiclePhoto);
            photoUnavailable = itemView.findViewById(R.id.vehiclePhotoUnavailable);
            badge = itemView.findViewById(R.id.catalogVehicleBadge);
            name = itemView.findViewById(R.id.catalogVehicleName);
            price = itemView.findViewById(R.id.catalogVehiclePrice);
            autonomy = itemView.findViewById(R.id.catalogVehicleAutonomy);
            battery = itemView.findViewById(R.id.catalogVehicleBattery);
            category = itemView.findViewById(R.id.catalogVehicleCategory);
        }

        void bind(Vehicle vehicle) {
            VehiclePhotos.load(vehicle, photo, photoUnavailable);
            name.setText(vehicle.getNome());
            price.setText(vehicle.getPreco() > 0 ? currency.format(vehicle.getPreco()) : "Em breve");
            autonomy.setText(vehicle.getAutonomia() + " km");
            battery.setText(vehicle.getBateria() + " kWh");
            category.setText(vehicle.getCategoria());
            boolean hasBadge = vehicle.getBadge() != null && !vehicle.getBadge().trim().isEmpty();
            badge.setVisibility(hasBadge ? View.VISIBLE : View.GONE);
            if (hasBadge) badge.setText(vehicle.getBadge());
            itemView.setContentDescription("Abrir detalhes de " + vehicle.getNome());
            itemView.setOnClickListener(v -> listener.onVehicleClick(vehicle));
        }

        void clearPhoto() {
            VehiclePhotos.clear(photo, photoUnavailable);
            itemView.setOnClickListener(null);
        }
    }
}
