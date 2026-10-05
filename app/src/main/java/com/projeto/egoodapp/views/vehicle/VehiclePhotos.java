package com.projeto.egoodapp.views.vehicle;

import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.projeto.egoodapp.R;
import com.projeto.egoodapp.data.local.DemoCatalog;
import com.projeto.egoodapp.data.model.Vehicle;
import java.util.Objects;

public final class VehiclePhotos {
    private VehiclePhotos() {}

    public static void load(Vehicle vehicle, ImageView image) {
        android.view.ViewGroup parent = (android.view.ViewGroup) image.getParent();
        FrameLayout wrapper;
        TextView unavailable;
        if (parent instanceof FrameLayout && "vehicle-photo".equals(parent.getTag())) {
            wrapper = (FrameLayout) parent;
            unavailable = wrapper.findViewById(R.id.vehiclePhotoUnavailable);
        } else {
            wrapper = new FrameLayout(image.getContext());
            wrapper.setTag("vehicle-photo");
            int index = parent.indexOfChild(image);
            android.view.ViewGroup.LayoutParams params = image.getLayoutParams();
            parent.removeView(image);
            parent.addView(wrapper, index, params);
            wrapper.addView(image, new FrameLayout.LayoutParams(-1, -1));
            unavailable = new TextView(image.getContext());
            unavailable.setId(R.id.vehiclePhotoUnavailable);
            unavailable.setText("Foto indisponível");
            unavailable.setTextColor(ContextCompat.getColor(image.getContext(), R.color.app_text_secondary));
            unavailable.setTextSize(12);
            unavailable.setGravity(Gravity.CENTER);
            unavailable.setBackgroundColor(ContextCompat.getColor(image.getContext(), R.color.app_background));
            wrapper.addView(unavailable, new FrameLayout.LayoutParams(-1, -1));
        }
        load(vehicle, image, unavailable);
    }

    public static void load(Vehicle vehicle, ImageView image, @Nullable TextView unavailable) {
        Glide.with(image).clear(image);
        image.setImageDrawable(null);

        String requestId = vehicle.getId();
        if (requestId == null || requestId.trim().isEmpty()) {
            requestId = vehicle.getImageName() + "|" + vehicle.getImagemUrl();
        }
        image.setTag(R.id.vehicle_photo_bound_id, requestId);
        image.setContentDescription(vehicle.getNome());
        setUnavailableVisible(image, unavailable, requestId, false);

        int demoResource = DemoCatalog.imageResource(vehicle.getId());
        if (demoResource != 0) {
            image.setImageResource(demoResource);
            return;
        }

        Object source = resolveSource(vehicle, image);
        if (source == null) {
            setUnavailableVisible(image, unavailable, requestId, true);
            return;
        }

        final String boundRequestId = requestId;
        Glide.with(image)
                .load(source)
                .centerCrop()
                .dontAnimate()
                .placeholder(R.drawable.ic_egood_logo)
                .listener(new RequestListener<Drawable>() {
                    @Override
                    public boolean onLoadFailed(@Nullable GlideException e, Object model,
                            Target<Drawable> target, boolean first) {
                        setUnavailableVisible(image, unavailable, boundRequestId, true);
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(Drawable resource, Object model,
                            Target<Drawable> target, DataSource dataSource, boolean first) {
                        setUnavailableVisible(image, unavailable, boundRequestId, false);
                        return false;
                    }
                })
                .into(image);
    }

    public static void clear(ImageView image, @Nullable TextView unavailable) {
        Glide.with(image).clear(image);
        image.setTag(R.id.vehicle_photo_bound_id, null);
        image.setImageDrawable(null);
        if (unavailable != null) unavailable.setVisibility(View.GONE);
    }

    @Nullable
    private static Object resolveSource(Vehicle vehicle, ImageView image) {
        if (vehicle.getImageName() != null && !vehicle.getImageName().isEmpty()) {
            int namedResource = image.getResources().getIdentifier(
                    vehicle.getImageName(), "drawable", image.getContext().getPackageName());
            if (namedResource != 0) return namedResource;
        }

        if (vehicle.getImagemUrl() != null && !vehicle.getImagemUrl().isEmpty()) {
            return Uri.parse(vehicle.getImagemUrl());
        }
        return null;
    }

    private static void setUnavailableVisible(ImageView image, @Nullable TextView unavailable,
            String requestId, boolean visible) {
        if (unavailable != null
                && Objects.equals(requestId, image.getTag(R.id.vehicle_photo_bound_id))) {
            unavailable.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
    }
}
