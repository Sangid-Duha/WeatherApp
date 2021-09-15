package com.ssduha.weather;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Picasso;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class ForecastAdapter extends RecyclerView.Adapter<ForecastAdapter.ViewHolder> {
    private final List<Hour> forecastData;
    private final Context context;

    public ForecastAdapter(List<Hour> forecastData, Context context) {
        this.forecastData = forecastData;
        this.context = context;
    }

    @NonNull
    @Override
    public ForecastAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.forecast_item, parent, false);
        return new ViewHolder(view);
    }

    @SuppressLint({"SetTextI18n", "SimpleDateFormat"})
    @Override
    public void onBindViewHolder(@NonNull ForecastAdapter.ViewHolder holder, int position) {
        final Hour data = forecastData.get(position);

        /*Forecast Weather Time*/
        SimpleDateFormat input = new SimpleDateFormat("yyyy-MM-dd hh:mm");
        SimpleDateFormat output = new SimpleDateFormat("hh:mm aa");
        try {
            Date time = input.parse(data.getTime());
            holder.timeTV.setText(output.format(time));
        } catch (ParseException e) {
            e.printStackTrace();
        }

        /*Forecast Weather Temperature & Wind Speed*/
        holder.tempTV.setText(data.getTemp_c() +"°");
        holder.windTV.setText(data.getWind_kph() +"km/h");

        /*Forecast Weather Image*/
        Picasso.get().load("https:"+ data.getCondition().getIcon()).into(holder.iconIV);
    }

    @Override
    public int getItemCount() {
        return forecastData.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView timeTV, tempTV, windTV;
        private final ImageView iconIV;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            timeTV = itemView.findViewById(R.id.forecastTimeTV);
            tempTV = itemView.findViewById(R.id.forecastTempTV);
            windTV = itemView.findViewById(R.id.forecastWindSpeedTV);
            iconIV = itemView.findViewById(R.id.forecastIconIV);
        }
    }
}
