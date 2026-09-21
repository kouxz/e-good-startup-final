package com.projeto.egoodapp.views.user;

import android.content.Intent;
import android.graphics.Color;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.projeto.egoodapp.R;
import com.projeto.egoodapp.data.local.AccountProfile;
import com.projeto.egoodapp.data.local.LocalRepository;
import com.projeto.egoodapp.data.local.LocalSession;
import com.projeto.egoodapp.views.comparison.ComparisonActivity;
import com.projeto.egoodapp.views.vehicle.SolarActivity;
import com.projeto.egoodapp.views.vehicle.VehiclesActivity;
import java.util.Locale;

public final class UserChrome {
    public enum Section { HOME, VEHICLES, COMPARISON, SOLAR, PROFILE }
    private static final int ACTIVE = Color.rgb(0, 139, 122), INACTIVE = Color.rgb(148, 163, 184);
    private final AppCompatActivity activity;
    private final Section section;
    private final int[] buttons = {R.id.btnHome, R.id.btnVehicles, R.id.btnComparison, R.id.btnSolar, R.id.btnProfile};
    private final int[] icons = {R.id.userNavHomeIcon, R.id.userNavVehiclesIcon, R.id.userNavComparisonIcon, R.id.userNavSolarIcon, R.id.userNavProfileIcon};
    private final int[] labels = {R.id.userNavHomeLabel, R.id.userNavVehiclesLabel, R.id.userNavComparisonLabel, R.id.userNavSolarLabel, R.id.userNavProfileLabel};
    private final int[] dots = {R.id.userNavHomeDot, R.id.userNavVehiclesDot, R.id.userNavComparisonDot, R.id.userNavSolarDot, R.id.userNavProfileDot};
    private final Class<?>[] destinations = {HomeActivity.class, VehiclesActivity.class, ComparisonActivity.class, SolarActivity.class, ProfileActivity.class};

    public UserChrome(AppCompatActivity activity, Section section) {
        this.activity = activity; this.section = section;
        for (int i = 0; i < buttons.length; i++) {
            final int index = i;
            activity.findViewById(buttons[i]).setOnClickListener(view -> open(index));
            boolean selected = section.ordinal() == i;
            ((ImageView) activity.findViewById(icons[i])).setColorFilter(selected ? ACTIVE : INACTIVE);
            ((TextView) activity.findViewById(labels[i])).setTextColor(selected ? ACTIVE : INACTIVE);
            activity.findViewById(dots[i]).setVisibility(selected ? View.VISIBLE : View.INVISIBLE);
        }
        activity.findViewById(R.id.btnUserNotifications).setOnClickListener(view ->
                Toast.makeText(activity, "Nenhuma notificação no momento", Toast.LENGTH_SHORT).show());
        activity.findViewById(R.id.tvUserAvatar).setOnClickListener(view -> open(Section.PROFILE.ordinal()));
        refresh();
    }
    public void refresh() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) { LocalSession.logout(activity); return; }
        AccountProfile profile = LocalRepository.get(activity).account(user.getUid());
        if (profile == null || profile.isDealer()) { LocalSession.logout(activity); return; }
        String name = profile.name == null || profile.name.trim().isEmpty() ? "Usuário" : profile.name.trim();
        String[] words = name.split("\\s+");
        String first = new String(Character.toChars(words[0].codePointAt(0)));
        String last = words.length > 1 ? new String(Character.toChars(words[words.length - 1].codePointAt(0))) : "";
        ((TextView) activity.findViewById(R.id.tvUserAvatar)).setText((first + last).toUpperCase(Locale.ROOT));
    }
    private void open(int index) {
        if (section.ordinal() == index && destinations[index].isInstance(activity)) return;
        activity.startActivity(new Intent(activity, destinations[index])
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP));
    }
}
