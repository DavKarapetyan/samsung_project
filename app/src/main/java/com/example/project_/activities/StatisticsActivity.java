package com.example.project_.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Base64;

import com.example.project_.R;
import com.example.project_.databinding.ActivityStatisticsBinding;
import com.example.project_.utilities.Constants;
import com.example.project_.utilities.PreferenceManager;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class StatisticsActivity extends AppCompatActivity {
    ActivityStatisticsBinding binding;
    PreferenceManager preferenceManager;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityStatisticsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        preferenceManager = new PreferenceManager(getApplicationContext());

        FirebaseFirestore.getInstance().collection("users").document(preferenceManager.getString(Constants.KEY_USER_ID))
                .get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        binding.name.setText(documentSnapshot.getString("name"));
                        binding.nickName.setText(documentSnapshot.getString("nickName"));
                        byte[] bytes = Base64.decode(documentSnapshot.getString("image"), Base64.DEFAULT);
                        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                        binding.imageProfile.setImageBitmap(bitmap);
                    }
                });
        PieChart pieChart = binding.chart;
        ArrayList<PieEntry> entries = new ArrayList<>();
        entries.add(new PieEntry(80f, "Maths"));
        entries.add(new PieEntry(90f, "Science"));
        entries.add(new PieEntry(75f, "English"));
        entries.add(new PieEntry(60f, "IT"));

        PieDataSet pieDataSet = new PieDataSet(entries, "Subjects");
        pieDataSet.setColors(ColorTemplate.MATERIAL_COLORS);

        PieData pieData = new PieData(pieDataSet);
        pieChart.setData(pieData);

        pieChart.getDescription().setEnabled(true);
        pieChart.animateY(1000);
        pieChart.invalidate();
        LineChart lineChart = binding.lineChart;

        // Generate sample data
        List<Entry> entries1 = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            entries1.add(new Entry(i, (float) Math.random() * 100));
        }

        // Create a dataset with sample data
        LineDataSet dataSet = new LineDataSet(entries1, "Followers per month");
        dataSet.setColor(R.color.primary__color);
        dataSet.setCircleColor(R.color.primary__color);
        dataSet.setDrawCircles(true);
        dataSet.setDrawValues(false);
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        dataSet.setDrawFilled(true); // Enable filling
        //dataSet.setFillAlpha(255); // Set fill transparency (0-255)
        dataSet.setFillColor(R.color.primary__color); // Set fill color


        // Customize X axis
        XAxis xAxis = lineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);

        // Customize Y axis
        YAxis leftAxis = lineChart.getAxisLeft();
        leftAxis.setAxisMinimum(0f);

        // Customize chart appearance
        lineChart.getDescription().setEnabled(false);
        lineChart.setDrawGridBackground(false);

        // Add data to the chart
        LineData lineData = new LineData(dataSet);
        lineChart.setData(lineData);
        lineChart.animateY(1000);
        lineChart.invalidate(); // refresh the chart
    }
}