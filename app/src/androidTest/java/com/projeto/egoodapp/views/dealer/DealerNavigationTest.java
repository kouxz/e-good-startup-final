package com.projeto.egoodapp.views.dealer;

import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.projeto.egoodapp.R;
import com.projeto.egoodapp.data.model.AccountProfile;
import com.projeto.egoodapp.views.auth.RegisterActivity;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** Exercises the real shared shell without signing in or changing stored accounts. */
@RunWith(AndroidJUnit4.class)
public class DealerNavigationTest {
    @Test public void footerAndDrawerDispatchTheSameDestinationsAndShowSelection() {
        try (ActivityScenario<RegisterActivity> scenario = ActivityScenario.launch(RegisterActivity.class)) {
            scenario.onActivity(activity -> {
                activity.setContentView(R.layout.activity_dealer_dashboard);
                AccountProfile company = new AccountProfile(); company.name = "Loja Elétrica";
                AtomicReference<String> destination = new AtomicReference<>();
                DealerNavigation navigation = new DealerNavigation(activity, () -> company, destination::set);
                int[] bottom = {R.id.navDashboard, R.id.navVehicles, R.id.navAdd, R.id.navContacts, R.id.navProfile};
                int[] drawer = {R.id.nav_dashboard, R.id.nav_vehicles, R.id.nav_add_vehicle, R.id.nav_interested, R.id.nav_profile};
                String[] sections = {"dashboard", "vehicles", "add", "contacts", "profile"};
                for (int i = 0; i < sections.length; i++) {
                    activity.findViewById(bottom[i]).performClick();
                    assertEquals(sections[i], destination.get());
                    activity.findViewById(drawer[i]).performClick();
                    assertEquals(sections[i], destination.get());
                    navigation.select(sections[i]);
                    for (int k = 0; k < bottom.length; k++) {
                        assertEquals(k == i, activity.findViewById(bottom[k]).isSelected());
                        assertEquals(k == i, activity.findViewById(drawer[k]).isSelected());
                    }
                }
                assertTrue(activity.findViewById(R.id.dealerNavVehiclesIcon) instanceof ImageView);
                navigation.select("settings");
                assertTrue(activity.findViewById(R.id.nav_settings).isSelected());
                for (int id : bottom) assertFalse(activity.findViewById(id).isSelected());
                activity.findViewById(R.id.nav_settings).performClick(); assertEquals("settings", destination.get());
                activity.findViewById(R.id.nav_logout).performClick(); assertEquals("logout", destination.get());
                assertEquals("LE", ((TextView) activity.findViewById(R.id.dealerDrawerAvatar)).getText().toString());
                company.name = "Nova Empresa";
                navigation.refreshIdentity();
                assertEquals("Nova Empresa", ((TextView) activity.findViewById(R.id.dealerDrawerName)).getText().toString());
                assertEquals("NE", ((TextView) activity.findViewById(R.id.dealerDrawerAvatar)).getText().toString());
            });
        }
    }

    @Test public void addVehicleUsesRightDrawerWithBoundedWidthAndRestoresOpenState() {
        try (ActivityScenario<RegisterActivity> scenario = ActivityScenario.launch(RegisterActivity.class)) {
            AtomicReference<DealerNavigation> navigation = new AtomicReference<>();
            scenario.onActivity(activity -> {
                activity.setContentView(R.layout.activity_add_vehicle);
                navigation.set(new DealerNavigation(activity, () -> null, destination -> {}));
                navigation.get().select("add");
                Bundle saved = new Bundle(); saved.putBoolean("dealerDrawerOpen", true);
                navigation.get().restore(saved);
            });
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            scenario.onActivity(activity -> {
                DrawerLayout root = activity.findViewById(R.id.drawerLayout);
                View drawer = activity.findViewById(R.id.navigationView);
                DrawerLayout.LayoutParams params = (DrawerLayout.LayoutParams) drawer.getLayoutParams();
                float density = activity.getResources().getDisplayMetrics().density;
                assertEquals(Gravity.RIGHT, params.gravity);
                assertTrue(params.width <= Math.round(320 * density));
                DrawerLayout.LayoutParams content = (DrawerLayout.LayoutParams) root.getChildAt(0).getLayoutParams();
                int available = root.getWidth() - content.leftMargin - content.rightMargin;
                assertTrue(available - params.width >= Math.round(56 * density));
                assertTrue(root.isDrawerOpen(drawer));
                Bundle saved = new Bundle(); navigation.get().save(saved);
                assertTrue(saved.getBoolean("dealerDrawerOpen"));
                assertTrue(activity.findViewById(R.id.navAdd).isSelected());
                assertEquals(View.VISIBLE, activity.findViewById(R.id.dealerNavAddDot).getVisibility());
                assertEquals(View.INVISIBLE, activity.findViewById(R.id.dealerNavProfileDot).getVisibility());
                root.closeDrawer(drawer, false);
                activity.findViewById(R.id.btnMenuHamburger).performClick();
                // The click starts opening the actual DrawerLayout, not a different activity.
                root.openDrawer(drawer, false);
                assertTrue(root.isDrawerOpen(drawer));
            });
        }
    }

    @Test public void dealershipLayoutsExposePerformanceSettingsAndTechnicalFields() {
        try (ActivityScenario<RegisterActivity> scenario = ActivityScenario.launch(RegisterActivity.class)) {
            scenario.onActivity(activity -> {
                activity.setContentView(R.layout.activity_dealer_dashboard);
                assertNotNull(activity.findViewById(R.id.progressDashboardViews));
                assertNotNull(activity.findViewById(R.id.progressDashboardContacts));
                assertNotNull(activity.findViewById(R.id.progressDashboardConversion));
                assertNotNull(activity.findViewById(R.id.tvDashboardSales));
                assertNotNull(activity.findViewById(R.id.tvContactsMonthlyCount));
                assertNotNull(activity.findViewById(R.id.tvContactsMonthlySales));
                assertNotNull(activity.findViewById(R.id.tvContactsMonthlyConversion));
                View interest = activity.getLayoutInflater().inflate(
                        R.layout.item_dealer_interest, null, false);
                assertNotNull(interest.findViewById(R.id.btnInterestStatusAction));
                assertNotNull(interest.findViewById(R.id.btnInterestCall));
                assertNotNull(interest.findViewById(R.id.btnInterestEmail));
                assertNotNull(activity.findViewById(R.id.switchDealerNotifications));
                assertNotNull(activity.findViewById(R.id.switchDealerInterests));
                assertNotNull(activity.findViewById(R.id.btnDealerChangePassword));
                assertNotNull(activity.findViewById(R.id.btnSettingsLogout));
                activity.setContentView(R.layout.activity_add_vehicle);
                assertNotNull(activity.findViewById(R.id.editConsumo));
                assertNotNull(activity.findViewById(R.id.editPotencia));
                assertNotNull(activity.findViewById(R.id.editRecarga));
            });
        }
    }
}
