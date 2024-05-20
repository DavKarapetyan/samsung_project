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
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class StatisticsActivity extends BaseActivity {
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
//        PieChart pieChart = binding.lineChart;
//        ArrayList<PieEntry> entries = new ArrayList<>();
//        entries.add(new PieEntry(80f, "Maths"));
//        entries.add(new PieEntry(90f, "Science"));
//        entries.add(new PieEntry(75f, "English"));
//        entries.add(new PieEntry(60f, "IT"));
//
//        PieDataSet pieDataSet = new PieDataSet(entries, "Subjects");
//        pieDataSet.setColors(ColorTemplate.MATERIAL_COLORS);
//
//        PieData pieData = new PieData(pieDataSet);
//        pieChart.setData(pieData);
//
//        pieChart.getDescription().setEnabled(true);
//        pieChart.animateY(1000);
//        pieChart.invalidate();
        LineChart lineChart = binding.lineChart;

        // Generate sample data
        List<Entry> entries1 = new ArrayList<>();

        FirebaseFirestore.getInstance().collection("users").document(preferenceManager.getString(Constants.KEY_USER_ID)).collection("moments").whereGreaterThan("publishDate", new Date(1, new Date().getMonth(), new Date().getYear()))
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        entries1.add(new Entry(new Date().getMonth(), queryDocumentSnapshots.size()));
                        LineDataSet dataSet = new LineDataSet(entries1, "Moments per month");
                        dataSet.setColor(R.color.primary__color);
                        dataSet.setCircleColor(R.color.primary__color);
                        dataSet.setDrawCircles(true);
                        dataSet.setDrawValues(false);
                        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
                        dataSet.setDrawFilled(true);
                        dataSet.setValueTextColor(Color.WHITE);
                        //dataSet.setFillAlpha(255); // Set fill transparency (0-255)
                        dataSet.setFillColor(R.color.primary__color);

                        XAxis xAxis = lineChart.getXAxis();
                        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);

                        YAxis leftAxis = lineChart.getAxisLeft();
                        leftAxis.setAxisMinimum(0f);

                        lineChart.getDescription().setEnabled(false);
                        lineChart.setDrawGridBackground(false);

                        LineData lineData = new LineData(dataSet);
                        lineChart.setData(lineData);
                        lineChart.animateY(1000);
                        lineChart.invalidate(); // refresh the chart
                    }
                });

//        FirebaseFirestore.getInstance().collection("users").document(preferenceManager.getString(Constants.KEY_USER_ID)).collection("moments")
//                .get()
//                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
//                    @Override
//                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
//                        int i = 1;
//                        for (QueryDocumentSnapshot queryDocumentSnapshot : queryDocumentSnapshots) {
//                            entries1.add(new Entry(i, queryDocumentSnapshots.size()));
//                            i++;
//                        }
//
//                        LineDataSet dataSet = new LineDataSet(entries1, "Moments per month");
//                        dataSet.setColor(R.color.primary__color);
//                        dataSet.setCircleColor(R.color.primary__color);
//                        dataSet.setDrawCircles(true);
//                        dataSet.setDrawValues(false);
//                        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
//                        dataSet.setDrawFilled(true);
//                        //dataSet.setFillAlpha(255); // Set fill transparency (0-255)
//                        dataSet.setFillColor(R.color.primary__color);
//
//                        XAxis xAxis = lineChart.getXAxis();
//                        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
//
//                        YAxis leftAxis = lineChart.getAxisLeft();
//                        leftAxis.setAxisMinimum(0f);
//
//                        lineChart.getDescription().setEnabled(false);
//                        lineChart.setDrawGridBackground(false);
//
//                        LineData lineData = new LineData(dataSet);
//                        lineChart.setData(lineData);
//                        lineChart.animateY(1000);
//                        lineChart.invalidate(); // refresh the chart
//                    }
//                });
    }
}