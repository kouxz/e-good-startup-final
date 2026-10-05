package com.projeto.egoodapp.views.dealership;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Intent;
import android.view.View;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.ChipGroup;
import com.projeto.egoodapp.R;
import com.projeto.egoodapp.views.dealership.ConcessionariaDetailActivity;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class DealershipScreensTest {
    @Test
    public void externalDealerDetailUsesProfessionalControlsAndHonestEmptyData() {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(),
                ConcessionariaDetailActivity.class)
                .putExtra("dealerKey", "osm:node:42")
                .putExtra("nome", "Eco Motors")
                .putExtra("endereco", "Avenida Central, 100")
                .putExtra("telefone", "(15) 3333-4444")
                .putExtra("hasLocation", true)
                .putExtra("latitude", -23.5)
                .putExtra("longitude", -47.4)
                .putExtra("distancia", 2.3);

        try (ActivityScenario<ConcessionariaDetailActivity> scenario = ActivityScenario.launch(intent)) {
            scenario.onActivity(activity -> {
                assertEquals("Eco Motors", activity.<android.widget.TextView>findViewById(
                        R.id.tvNomeConcessionaria).getText().toString());
                assertEquals("Não informado", activity.<android.widget.TextView>findViewById(
                        R.id.tvVeiculos).getText().toString());
                assertTrue(activity.findViewById(R.id.btnDealerRating) instanceof MaterialButton);
                assertTrue(activity.findViewById(R.id.btnMaps) instanceof MaterialButton);
                assertTrue(activity.findViewById(R.id.containerMarcas) instanceof ChipGroup);
                assertEquals(View.GONE, activity.findViewById(R.id.btnVerVeiculos).getVisibility());
                assertFalse(activity.findViewById(R.id.btnLigar).isEnabled());
            });
        }
    }
}
