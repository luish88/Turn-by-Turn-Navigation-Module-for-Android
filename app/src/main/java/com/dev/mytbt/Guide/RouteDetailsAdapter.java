package com.dev.mytbt.Guide;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.dev.mytbt.R;
import com.dev.mytbt.Routing.RoutePoint;

import java.util.List;

/**
 * Created by Luís Henriques for MyTbt.
 * 06-03-2018, 13:18
 */

public class RouteDetailsAdapter extends ArrayAdapter <RouteDetail> {

    public RouteDetailsAdapter(Context context, List<RouteDetail> entries) {
        super(context, 0, entries);
    }

    @SuppressLint("ViewHolder") // we don't really care about smooth scrolling
    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        // Get the data item for this position
        RouteDetail routeDetail = getItem(position);

        // NOTE: reusing a given layout presents layout problems. Do not recycle this view. Smooth scrolling is maintained.
        convertView = LayoutInflater.from(getContext()).inflate(R.layout.entry_route_details, parent, false);

        // Populate the data into the template view using the data object
        if (routeDetail != null) {

            // Lookup view for data population
            TextView tvDistance = convertView.findViewById(R.id.tvRDEntryDistance);
            TextView tvStreetName = convertView.findViewById(R.id.tvRDEntryStreetName);
            TextView tvRecommendedSpeed = convertView.findViewById(R.id.tvRDEntrySpeedIcon);
            ImageView ivIcon = convertView.findViewById(R.id.ivRDEntryIcon);

            tvDistance.setText(String.valueOf(routeDetail.getDistanceText()));
            tvStreetName.setText(routeDetail.getStreetName());
            ivIcon.setImageResource(RoutePoint.getInstructionIconResource(routeDetail.getIcon()));


            if (routeDetail.getSpeedLimit().isEmpty() || routeDetail.getSpeedLimit().equals("0")) {
                tvRecommendedSpeed.setVisibility(View.GONE); // if the current point has no assigned speed limit, we hide the icon
            } else {
                tvRecommendedSpeed.setText(routeDetail.getSpeedLimit());
            }

            if (routeDetail.isPassedBy()) { // if the point was already passed by, we make it a little transparent, so the user knows
                LinearLayout background = convertView.findViewById(R.id.llRDEntryBackground);
                background.setAlpha(0.3f);
                tvDistance.setVisibility(View.GONE);
            }
        }

        // Return the completed view to render on screen
        return convertView;
    }
}
