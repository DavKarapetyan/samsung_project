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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

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


        AtomicInteger atomicInteger = new AtomicInteger();
        FirebaseFirestore.getInstance().collection("posts").whereEqualTo("userId", preferenceManager.getString(Constants.KEY_USER_ID)).get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        for (QueryDocumentSnapshot queryDocumentSnapshot : queryDocumentSnapshots) {
                            List<String> list = (List<String>) queryDocumentSnapshot.get("likedUsers");
                            atomicInteger.addAndGet((int) list.stream().count());
                        }
                        binding.likesCount.setText("Total Likes Count: " +  atomicInteger.toString());
                    }
                });
        LineChart lineChart = binding.lineChart;
        fetchDataAndDisplayChart(lineChart);

//        // Generate sample data
//        List<Entry> entries1 = new ArrayList<>();
//
//        FirebaseFirestore.getInstance().collection("users").document(preferenceManager.getString(Constants.KEY_USER_ID)).collection("moments")
//                .get()
//                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
//                    @Override
//                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
//                        entries1.add(new Entry(new Date().getDay(), queryDocumentSnapshots.size()));
//                        LineDataSet dataSet = new LineDataSet(entries1, "Moments per month");
//                        dataSet.setColor(R.color.primary__color);
//                        dataSet.setCircleColor(R.color.primary__color);
//                        dataSet.setDrawCircles(true);
//                        dataSet.setDrawValues(false);
//                        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
//                        dataSet.setDrawFilled(true);
//                        dataSet.setValueTextColor(Color.WHITE);
//                        //dataSet.setFillAlpha(255); // Set fill transparency (0-255)
//                        dataSet.setFillColor(R.color.primary__color);
//                        dataSet.setColor(Color.WHITE);
//
//                        XAxis xAxis = lineChart.getXAxis();
//                        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
//                        xAxis.setTextColor(Color.WHITE);
//
//                        YAxis leftAxis = lineChart.getAxisLeft();
//                        leftAxis.setAxisMinimum(0f);
//                        leftAxis.setTextColor(Color.WHITE);
//
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

    private void fetchDataAndDisplayChart(LineChart lineChart) {
        FirebaseFirestore.getInstance().collection("users")
                .document(preferenceManager.getString(Constants.KEY_USER_ID))
                .collection("moments")
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        Map<String, Integer> momentsPerDay = new HashMap<>();
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                        for (DocumentSnapshot documentSnapshot : queryDocumentSnapshots.getDocuments()) {
                            Date momentDate = documentSnapshot.getDate("publishDate"); // Assuming the date field is named "date"
                            String day = sdf.format(momentDate);
                            momentsPerDay.put(day, momentsPerDay.getOrDefault(day, 0) + 1);
                        }

                        List<Entry> entries = new ArrayList<>();
                        int index = 0;
                        for (Map.Entry<String, Integer> entry : momentsPerDay.entrySet()) {
                            // Convert date string back to Date object for proper sorting
                            Date date = null;
                            try {
                                date = sdf.parse(entry.getKey());
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            if (date != null) {
                                entries.add(new Entry(date.getTime(), entry.getValue()));
                            }
                        }

                        LineDataSet dataSet = new LineDataSet(entries, "Moments per Day");
                        dataSet.setColor(getResources().getColor(R.color.primary__color));
                        dataSet.setCircleColor(getResources().getColor(R.color.primary__color));
                        dataSet.setFillColor(getResources().getColor(R.color.primary__color));
                        dataSet.setValueTextColor(getResources().getColor(R.color.primary__color));

                        XAxis xAxis = lineChart.getXAxis();
                        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
                        xAxis.setValueFormatter(new DateFormatter()); // Custom X-axis value formatter
                        xAxis.setTextColor(Color.WHITE);

                        YAxis yAxis = lineChart.getAxisLeft();
                        yAxis.setTextColor(Color.WHITE);

                        LineData lineData = new LineData(dataSet);
                        lineChart.setData(lineData);
                        lineChart.invalidate();
                    }
                });
    }

    private class DateFormatter extends com.github.mikephil.charting.formatter.ValueFormatter {
        private final SimpleDateFormat mFormat = new SimpleDateFormat("dd-MM", Locale.getDefault());

        @Override
        public String getFormattedValue(float value) {
            return mFormat.format(new Date((long) value));
        }
    }
}