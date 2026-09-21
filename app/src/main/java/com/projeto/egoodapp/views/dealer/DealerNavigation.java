package com.projeto.egoodapp.views.dealer;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.Gravity;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import com.projeto.egoodapp.R;
import com.projeto.egoodapp.data.local.AccountProfile;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** Shared dealership chrome; activities retain ownership of their content and destinations. */
public final class DealerNavigation {
    private static final String DRAWER_OPEN = "dealerDrawerOpen";
    private static final int ACTIVE = Color.rgb(0, 139, 122);
    private static final int INACTIVE = Color.rgb(148, 163, 184);
    private final AppCompatActivity activity;
    private final DrawerLayout root;
    private final View drawer;
    private final Supplier<AccountProfile> profile;
    private final String[] destinations = {"dashboard", "vehicles", "add", "contacts", "profile", "settings", "logout"};
    private final int[] bottomIds = {R.id.navDashboard, R.id.navVehicles, R.id.navAdd, R.id.navContacts, R.id.navProfile};
    private final int[] bottomIcons = {R.id.dealerNavDashboardIcon, R.id.dealerNavVehiclesIcon, R.id.dealerNavAddIcon, R.id.dealerNavContactsIcon, R.id.dealerNavProfileIcon};
    private final int[] bottomLabels = {R.id.dealerNavDashboardLabel, R.id.dealerNavVehiclesLabel, R.id.dealerNavAddLabel, R.id.dealerNavContactsLabel, R.id.dealerNavProfileLabel};
    private final int[] bottomDots = {R.id.dealerNavDashboardDot, R.id.dealerNavVehiclesDot, R.id.dealerNavAddDot, R.id.dealerNavContactsDot, R.id.dealerNavProfileDot};
    private final int[] drawerIds = {R.id.nav_dashboard, R.id.nav_vehicles, R.id.nav_add_vehicle, R.id.nav_interested, R.id.nav_profile, R.id.nav_settings, R.id.nav_logout};
    private final int[] drawerIcons = {R.id.dealerDrawerDashboardIcon, R.id.dealerDrawerVehiclesIcon, R.id.dealerDrawerAddIcon, R.id.dealerDrawerContactsIcon, R.id.dealerDrawerProfileIcon, R.id.dealerDrawerSettingsIcon, R.id.dealerDrawerLogoutIcon};
    private final int[] drawerLabels = {R.id.dealerDrawerDashboardLabel, R.id.dealerDrawerVehiclesLabel, R.id.dealerDrawerAddLabel, R.id.dealerDrawerContactsLabel, R.id.dealerDrawerProfileLabel, R.id.dealerDrawerSettingsLabel, R.id.dealerDrawerLogoutLabel};
    private final int[] drawerIndicators = {R.id.dealerDrawerDashboardIndicator, R.id.dealerDrawerVehiclesIndicator, R.id.dealerDrawerAddIndicator, R.id.dealerDrawerContactsIndicator, R.id.dealerDrawerProfileIndicator, R.id.dealerDrawerSettingsIndicator, R.id.dealerDrawerLogoutIndicator};

    public DealerNavigation(AppCompatActivity activity, Supplier<AccountProfile> profile, Consumer<String> navigate) {
        this.activity = activity;
        this.profile = profile;
        root = activity.findViewById(R.id.drawerLayout);
        drawer = activity.findViewById(R.id.navigationView);
        root.setDrawerTitle(Gravity.RIGHT, "Menu da concessionária");
        root.setScrimColor(Color.argb(140, 0, 0, 0));
        WindowCompat.setDecorFitsSystemWindows(activity.getWindow(), false);
        root.setFitsSystemWindows(false);
        ViewCompat.setOnApplyWindowInsetsListener(root, (view, insets) -> {
            androidx.core.graphics.Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.displayCutout());
            // DrawerLayout lays out children using margins rather than parent padding.
            for (int i = 0; i < root.getChildCount(); i++) {
                View child = root.getChildAt(i);
                DrawerLayout.LayoutParams params = (DrawerLayout.LayoutParams) child.getLayoutParams();
                params.setMargins(bars.left, bars.top, bars.right, bars.bottom);
                child.setLayoutParams(params);
            }
            return insets;
        });
        ViewCompat.requestApplyInsets(root);
        root.addOnLayoutChangeListener((view, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
            DrawerLayout.LayoutParams content = (DrawerLayout.LayoutParams) root.getChildAt(0).getLayoutParams();
            int available = right - left - content.leftMargin - content.rightMargin;
            int width = Math.min(dp(320), Math.max(1, available - dp(56)));
            ViewGroup.LayoutParams params = drawer.getLayoutParams();
            if (params.width != width) { params.width = width; drawer.setLayoutParams(params); }
        });
        activity.findViewById(R.id.btnMenuHamburger).setOnClickListener(view -> {
            refreshIdentity();
            if (root.isDrawerVisible(drawer)) root.closeDrawer(drawer);
            else root.openDrawer(drawer);
        });
        for (int i = 0; i < bottomIds.length; i++) {
            final String destination = destinations[i];
            activity.findViewById(bottomIds[i]).setOnClickListener(view -> navigate.accept(destination));
        }
        for (int i = 0; i < drawerIds.length; i++) {
            final String destination = destinations[i];
            activity.findViewById(drawerIds[i]).setOnClickListener(view -> {
                root.closeDrawer(drawer);
                navigate.accept(destination);
            });
        }
        OnBackPressedCallback closeDrawer = new OnBackPressedCallback(false) {
            @Override public void handleOnBackPressed() { root.closeDrawer(drawer); }
        };
        activity.getOnBackPressedDispatcher().addCallback(activity, closeDrawer);
        root.addDrawerListener(new DrawerLayout.SimpleDrawerListener() {
            @Override public void onDrawerSlide(View view, float offset) { closeDrawer.setEnabled(offset > 0); }
            @Override public void onDrawerOpened(View view) { closeDrawer.setEnabled(true); refreshIdentity(); }
            @Override public void onDrawerClosed(View view) { closeDrawer.setEnabled(false); }
        });
        refreshIdentity();
    }

    public void select(String section) {
        for (int i = 0; i < bottomIds.length; i++) {
            boolean selected = destinations[i].equals(section);
            int color = selected ? ACTIVE : INACTIVE;
            activity.findViewById(bottomIds[i]).setSelected(selected);
            ((ImageView) activity.findViewById(bottomIcons[i])).setColorFilter(color);
            ((TextView) activity.findViewById(bottomLabels[i])).setTextColor(color);
            activity.findViewById(bottomDots[i]).setVisibility(selected ? View.VISIBLE : View.INVISIBLE);
        }
        for (int i = 0; i < drawerIds.length; i++) {
            boolean selected = i < destinations.length - 1 && destinations[i].equals(section);
            int color = i == destinations.length - 1 ? Color.rgb(239, 68, 68)
                    : selected ? ACTIVE : Color.rgb(51, 65, 85);
            activity.findViewById(drawerIds[i]).setSelected(selected);
            ((ImageView) activity.findViewById(drawerIcons[i])).setColorFilter(color);
            ((TextView) activity.findViewById(drawerLabels[i])).setTextColor(color);
            activity.findViewById(drawerIndicators[i]).setVisibility(selected ? View.VISIBLE : View.INVISIBLE);
        }
    }

    public void refreshIdentity() {
        AccountProfile current = profile.get();
        String name = current == null || current.name == null || current.name.trim().isEmpty()
                ? "Minha concessionária" : current.name.trim();
        ((TextView) activity.findViewById(R.id.dealerDrawerName)).setText(name);
        String[] words = name.split("\\s+");
        String first = new String(Character.toChars(words[0].codePointAt(0)));
        String last = words.length > 1 ? new String(Character.toChars(words[words.length - 1].codePointAt(0))) : "";
        ((TextView) activity.findViewById(R.id.dealerDrawerAvatar)).setText((first + last).toUpperCase(Locale.ROOT));
    }

    public void save(Bundle state) { state.putBoolean(DRAWER_OPEN, root.isDrawerVisible(drawer)); }
    public void restore(Bundle state) {
        if (state != null && state.getBoolean(DRAWER_OPEN)) root.post(() -> {
            if (!activity.isFinishing() && !activity.isDestroyed()) root.openDrawer(drawer, false);
        });
    }
    private int dp(int value) { return Math.round(value * activity.getResources().getDisplayMetrics().density); }
}
