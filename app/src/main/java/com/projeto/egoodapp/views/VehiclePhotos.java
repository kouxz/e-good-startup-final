package com.projeto.egoodapp.views;

import android.graphics.Color;
import android.net.Uri;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.Nullable;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import android.graphics.drawable.Drawable;
import com.projeto.egoodapp.R;
import com.projeto.egoodapp.models.Vehicle;

public final class VehiclePhotos {
    private VehiclePhotos() {}
    public static void load(Vehicle vehicle, ImageView image) {
        android.view.ViewGroup parent = (android.view.ViewGroup) image.getParent();
        FrameLayout wrapper;
        TextView unavailable;
        if (parent instanceof FrameLayout && "vehicle-photo".equals(parent.getTag())) {
            wrapper = (FrameLayout) parent;
            unavailable = (TextView) wrapper.getChildAt(1);
        } else {
            wrapper = new FrameLayout(image.getContext()); wrapper.setTag("vehicle-photo");
            int index = parent.indexOfChild(image);
            android.view.ViewGroup.LayoutParams params = image.getLayoutParams();
            parent.removeView(image); parent.addView(wrapper, index, params);
            wrapper.addView(image, new FrameLayout.LayoutParams(-1, -1));
            unavailable = new TextView(image.getContext());
            unavailable.setText("Foto indisponível"); unavailable.setTextColor(Color.parseColor("#64748B"));
            unavailable.setTextSize(12); unavailable.setGravity(Gravity.CENTER);
            unavailable.setBackgroundColor(Color.parseColor("#F8F9FA"));
            wrapper.addView(unavailable, new FrameLayout.LayoutParams(-1, -1));
        }
        image.setContentDescription(vehicle.getNome());
        unavailable.setVisibility(View.GONE);
        TextView message = unavailable;
        Object source = null;
        if (vehicle.getImageName() != null) {
            source = image.getResources().getIdentifier(vehicle.getImageName(), "drawable", image.getContext().getPackageName());
        } else if (vehicle.getImagemUrl() != null && !vehicle.getImagemUrl().isEmpty()) source = Uri.parse(vehicle.getImagemUrl());
        if (source == null) { unavailable.setVisibility(View.VISIBLE); return; }
        Glide.with(image).load(source).centerCrop().placeholder(R.drawable.ic_egood_logo)
                .listener(new RequestListener<Drawable>() {
                    @Override public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean first) {
                        message.setVisibility(View.VISIBLE); return false;
                    }
                    @Override public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource ds, boolean first) {
                        message.setVisibility(View.GONE); return false;
                    }
                }).into(image);
    }
}
