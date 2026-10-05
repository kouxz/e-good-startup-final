package com.projeto.egoodapp.views.dealership;

import com.projeto.egoodapp.data.model.Concessionaria;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import com.projeto.egoodapp.R;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ConcessionariaAdapter extends RecyclerView.Adapter<ConcessionariaAdapter.ViewHolder> {
    public interface Listener {
        void onOpen(Concessionaria dealership);
        void onVehicles(Concessionaria dealership);
        void onRate(Concessionaria dealership);
    }

    private final List<Concessionaria> dealerships = new ArrayList<>();
    private final Listener listener;

    public ConcessionariaAdapter(Listener listener) {
        this.listener = listener;
        setHasStableIds(true);
    }

    public void submitList(List<Concessionaria> updated) {
        dealerships.clear();
        dealerships.addAll(updated);
        notifyDataSetChanged();
    }

    @Override
    public long getItemId(int position) {
        String key = dealerships.get(position).getStableKey();
        return key == null ? RecyclerView.NO_ID : key.hashCode();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_dealership, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(dealerships.get(position));
    }

    @Override
    public int getItemCount() {
        return dealerships.size();
    }

    final class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView name;
        private final TextView address;
        private final TextView distance;
        private final TextView rating;
        private final TextView registered;
        private final TextView vehicleCount;
        private final TextView brands;
        private final ImageView vehicleIcon;
        private final ImageButton rate;
        private final MaterialButton vehicles;
        private final MaterialButton detail;

        ViewHolder(View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.txtNome);
            address = itemView.findViewById(R.id.txtEndereco);
            distance = itemView.findViewById(R.id.txtDistancia);
            rating = itemView.findViewById(R.id.txtRating);
            registered = itemView.findViewById(R.id.txtRegistered);
            vehicleCount = itemView.findViewById(R.id.txtVehicleCount);
            brands = itemView.findViewById(R.id.txtBrands);
            vehicleIcon = itemView.findViewById(R.id.iconVehicleCount);
            rate = itemView.findViewById(R.id.btnLike);
            vehicles = itemView.findViewById(R.id.btnDealerVehicles);
            detail = itemView.findViewById(R.id.btnDealerDetail);
        }

        void bind(Concessionaria item) {
            name.setText(item.getNome());
            address.setText(item.getEndereco());
            distance.setText(item.hasLocation()
                    ? String.format(Locale.forLanguageTag("pt-BR"), "%.1f km", item.getDistanciaKm())
                    : "Distância não informada");
            rating.setText(DealerRatingDialogs.summary(item.getRatingAverage(), item.getRatingCount()));
            registered.setVisibility(item.isLocal() ? View.VISIBLE : View.GONE);
            rate.setImageResource(item.getUserRating() == null
                    ? R.drawable.ic_rating_star_outline : R.drawable.ic_rating_star_filled);
            rate.setContentDescription((item.getUserRating() == null ? "Avaliar " : "Alterar avaliação de ")
                    + item.getNome());

            boolean local = item.isLocal();
            vehicleIcon.setVisibility(local ? View.VISIBLE : View.GONE);
            vehicleCount.setVisibility(local ? View.VISIBLE : View.GONE);
            brands.setVisibility(local ? View.VISIBLE : View.GONE);
            vehicles.setVisibility(local ? View.VISIBLE : View.GONE);
            if (local) {
                int count = item.getVehicleCount();
                vehicleCount.setText(count + (count == 1 ? " veículo elétrico" : " veículos elétricos"));
                brands.setText(item.getBrands().isEmpty()
                        ? "Nenhuma marca cadastrada" : String.join(" • ", item.getBrands()));
            }

            itemView.setContentDescription("Abrir concessionária " + item.getNome());
            itemView.setOnClickListener(view -> listener.onOpen(item));
            detail.setOnClickListener(view -> listener.onOpen(item));
            vehicles.setOnClickListener(view -> listener.onVehicles(item));
            rate.setOnClickListener(view -> listener.onRate(item));
        }
    }
}
