package com.projeto.egoodapp.views.dealership;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.projeto.egoodapp.R;

import java.util.List;
import java.util.Locale;

public class ConcessionariaAdapter extends RecyclerView.Adapter<ConcessionariaAdapter.ViewHolder> {

    private List<Concessionaria> lista;
    private Context context;

    public ConcessionariaAdapter(List<Concessionaria> lista) {
        this.lista = lista;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        this.context = parent.getContext();
        View view = LayoutInflater.from(context).inflate(R.layout.item_dealership, parent, false);
        return new ViewHolder(view, context);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Concessionaria item = lista.get(position);

        holder.txtNome.setText(item.getNome());
        holder.txtEndereco.setText(item.getEndereco());
        holder.txtDistancia.setText(item.hasLocation() ? String.format(Locale.getDefault(), "%.1f km", item.getDistanciaKm()) : "Cadastrada no app");

        // Define o ícone de curtida com base no estado
        atualizarIconeLike(holder.btnLike, item.isCurtida());

        // Lógica de clique no Like
        holder.btnLike.setOnClickListener(v -> {
            item.setCurtida(!item.isCurtida());
            atualizarIconeLike(holder.btnLike, item.isCurtida());
        });

        // Clique na concessionária para abrir detalhes
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, ConcessionariaDetailActivity.class);
            intent.putExtra("nome", item.getNome());
            intent.putExtra("endereco", item.getEndereco());
            intent.putExtra("distancia", item.getDistanciaKm());
            intent.putExtra("telefone", item.getTelefone());
            intent.putExtra("dealerId", item.getDealerId());
            intent.putExtra("hasLocation", item.hasLocation());
            context.startActivity(intent);
        });
    }

    private void atualizarIconeLike(ImageButton btn, boolean isCurtida) {
        if (isCurtida) {
            btn.setImageResource(android.R.drawable.btn_star_big_on);
        } else {
            btn.setImageResource(android.R.drawable.btn_star_big_off);
        }
    }

    @Override
    public int getItemCount() {
        return lista.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtNome, txtEndereco, txtDistancia;
        ImageButton btnLike;

        public ViewHolder(View itemView, Context context) {
            super(itemView);
            txtNome = itemView.findViewById(R.id.txtNome);
            txtEndereco = itemView.findViewById(R.id.txtEndereco);
            txtDistancia = itemView.findViewById(R.id.txtDistancia);
            btnLike = itemView.findViewById(R.id.btnLike);
        }
    }
}
