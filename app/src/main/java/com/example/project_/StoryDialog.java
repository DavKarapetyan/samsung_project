package com.example.project_;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Base64;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.example.project_.databinding.StoryDialogBinding;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

public class StoryDialog extends Dialog {
    private String userId;
    private int currentMomentIndex = 0;
    private StoryDialogBinding binding;
    private QuerySnapshot momentSnapshots;

    public StoryDialog(@NonNull Context context, String userId) {
        super(context, android.R.style.Theme_DeviceDefault_Light_NoActionBar_Fullscreen);
        this.userId = userId;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = StoryDialogBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        WindowManager.LayoutParams params = getWindow().getAttributes();
        params.width = WindowManager.LayoutParams.MATCH_PARENT;
        params.height = WindowManager.LayoutParams.MATCH_PARENT;
        getWindow().setAttributes(params);

        // Fetch the moments
        FirebaseFirestore.getInstance().collection("users").document(userId).collection("moments")
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        momentSnapshots = queryDocumentSnapshots;
                        if (!queryDocumentSnapshots.isEmpty()) {
                            showMoment((QueryDocumentSnapshot) queryDocumentSnapshots.getDocuments().get(currentMomentIndex));
                        }
                    }
                });

        // Set touch listener for advancing to next moment
        binding.getRoot().setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN && event.getX() > v.getWidth() * 0.5) {
                    showNextMoment();
                    return true;
                }
                return false;
            }
        });
    }

    private void showNextMoment() {
        currentMomentIndex++;
        if (momentSnapshots != null && currentMomentIndex < momentSnapshots.size()) {
            showMoment((QueryDocumentSnapshot) momentSnapshots.getDocuments().get(currentMomentIndex));
        } else {
            // End of moments
            dismiss();
        }
    }

    private void showMoment(QueryDocumentSnapshot snapshot) {
        Glide.with(getContext())
                .load(snapshot.getString("imageURL"))
                .centerCrop()
                .placeholder(android.R.drawable.alert_dark_frame)
                .into(new CustomTarget<Drawable>() {
                    @Override
                    public void onResourceReady(@NonNull Drawable resource, @Nullable com.bumptech.glide.request.transition.Transition<? super Drawable> transition) {
                        binding.storyBg.setBackground(resource);
                    }

                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {
                        // Handle placeholder cleanup if needed
                    }
                });
    }

    private Bitmap getUserImage(String encodedImage) {
        byte[] bytes = Base64.decode(encodedImage, Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    }
}
