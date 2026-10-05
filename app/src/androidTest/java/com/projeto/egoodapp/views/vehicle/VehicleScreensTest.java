package com.projeto.egoodapp.views.vehicle;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Intent;
import android.widget.ImageView;
import androidx.recyclerview.widget.RecyclerView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.projeto.egoodapp.R;
import com.projeto.egoodapp.views.vehicle.VehicleDetailActivity;
import com.projeto.egoodapp.views.vehicle.VehiclesActivity;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class VehicleScreensTest {
    @Test
    public void catalogUsesMaterialSearchFiltersAndRecyclerView() {
        try (ActivityScenario<VehiclesActivity> scenario = ActivityScenario.launch(VehiclesActivity.class)) {
            scenario.onActivity(activity -> {
                assertTrue(activity.findViewById(R.id.etSearch) instanceof TextInputEditText);
                assertTrue(activity.findViewById(R.id.vehicleRecycler) instanceof RecyclerView);
                MaterialButton all = activity.findViewById(R.id.btnFilterAll);
                MaterialButton hatch = activity.findViewById(R.id.btnFilterHatch);
                assertTrue(all.isChecked());
                hatch.performClick();
                assertTrue(hatch.isChecked());
            });
        }
    }

    @Test
    public void dolphinDetailUsesTheVehicleBoundToTheStableId() {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), VehicleDetailActivity.class)
                .putExtra("vehicleId", "demo-mini");
        try (ActivityScenario<VehicleDetailActivity> scenario = ActivityScenario.launch(intent)) {
            scenario.onActivity(activity -> {
                assertEquals("BYD", activity.<android.widget.TextView>findViewById(R.id.tvManufacturer)
                        .getText().toString());
                assertEquals("BYD Dolphin Mini", activity.<android.widget.TextView>findViewById(R.id.tvVehicleName)
                        .getText().toString());
                assertEquals("BYD Dolphin Mini", activity.<android.widget.ImageView>findViewById(R.id.ivVehicleImage)
                        .getContentDescription().toString());
                assertTrue(activity.findViewById(R.id.btnFindDealer) instanceof MaterialButton);
            });
        }
    }

    @Test
    public void catalogKeepsDemoPhotosBoundToTheirVehicleAfterRecycling() {
        try (ActivityScenario<VehiclesActivity> scenario = ActivityScenario.launch(VehiclesActivity.class)) {
            scenario.onActivity(activity ->
                    activity.<RecyclerView>findViewById(R.id.vehicleRecycler).scrollToPosition(5));
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            scenario.onActivity(activity -> assertPhotoBinding(activity, 5, "demo-bolt"));

            scenario.onActivity(activity ->
                    activity.<RecyclerView>findViewById(R.id.vehicleRecycler).scrollToPosition(0));
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            scenario.onActivity(activity -> assertPhotoBinding(activity, 0, "demo-mini"));

            scenario.onActivity(activity ->
                    activity.<RecyclerView>findViewById(R.id.vehicleRecycler).scrollToPosition(1));
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            scenario.onActivity(activity -> assertPhotoBinding(activity, 1, "demo-dolphin"));
        }
    }

    private static void assertPhotoBinding(VehiclesActivity activity, int position, String vehicleId) {
        RecyclerView recycler = activity.findViewById(R.id.vehicleRecycler);
        RecyclerView.ViewHolder holder = recycler.findViewHolderForAdapterPosition(position);
        assertNotNull(holder);
        ImageView photo = holder.itemView.findViewById(R.id.catalogVehiclePhoto);
        assertEquals(vehicleId, photo.getTag(R.id.vehicle_photo_bound_id));
    }
}
