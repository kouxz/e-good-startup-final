package com.projeto.egoodapp.views.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.projeto.egoodapp.R;
import com.projeto.egoodapp.views.auth.UserRegisterActivity;
import com.projeto.egoodapp.views.auth.DealerRegisterActivity;

public class RegisterActivity extends AppCompatActivity {

    private boolean isPessoa = true;
    private MaterialCardView cardPessoa, cardConcessionaria;
    private ImageView iconPessoa, iconConcessionaria;
    private TextView titlePessoa, descPessoa, titleConcessionaria, descConcessionaria;
    private MaterialButton btnProximo;
    private TextView tvVoltar, tvLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register_selector);

        cardPessoa = findViewById(R.id.cardPessoa);
        cardConcessionaria = findViewById(R.id.cardConcessionaria);
        iconPessoa = findViewById(R.id.iconPessoa);
        iconConcessionaria = findViewById(R.id.iconConcessionaria);
        titlePessoa = findViewById(R.id.titlePessoa);
        descPessoa = findViewById(R.id.descPessoa);
        titleConcessionaria = findViewById(R.id.titleConcessionaria);
        descConcessionaria = findViewById(R.id.descConcessionaria);
        btnProximo = findViewById(R.id.btnProximo);
        tvVoltar = findViewById(R.id.tvVoltar);
        tvLogin = findViewById(R.id.tvLogin);

        cardPessoa.setOnClickListener(v -> setCardState(true));
        cardConcessionaria.setOnClickListener(v -> setCardState(false));

        btnProximo.setOnClickListener(v -> prosseguirComTipo());

        tvVoltar.setOnClickListener(v -> finish());
        tvLogin.setOnClickListener(v -> finish());

        setCardState(savedInstanceState == null || savedInstanceState.getBoolean("isPessoa", true));
    }

    private void setCardState(boolean selecionarPessoa) {
        isPessoa = selecionarPessoa;
        int colorPrimary = ContextCompat.getColor(this, R.color.primary);
        int colorTextDark = ContextCompat.getColor(this, R.color.text_dark);
        int colorTextGray = ContextCompat.getColor(this, R.color.text_gray);
        cardPessoa.setChecked(isPessoa);
        cardConcessionaria.setChecked(!isPessoa);

        if (isPessoa) {
            iconPessoa.setColorFilter(colorPrimary);
            titlePessoa.setTextColor(colorTextDark);
            descPessoa.setTextColor(colorTextGray);

            iconConcessionaria.setColorFilter(colorTextGray);
            titleConcessionaria.setTextColor(colorTextDark);
            descConcessionaria.setTextColor(colorTextGray);
        } else {
            iconConcessionaria.setColorFilter(colorPrimary);
            titleConcessionaria.setTextColor(colorTextDark);
            descConcessionaria.setTextColor(colorTextGray);

            iconPessoa.setColorFilter(colorTextGray);
            titlePessoa.setTextColor(colorTextDark);
            descPessoa.setTextColor(colorTextGray);
        }
    }

    private void prosseguirComTipo() {
        Intent intent;
        if (isPessoa) {
            intent = new Intent(this, UserRegisterActivity.class);
        } else {
            intent = new Intent(this, DealerRegisterActivity.class);
        }
        startActivity(intent);
    }
    @Override protected void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);
        state.putBoolean("isPessoa", isPessoa);
    }
}
