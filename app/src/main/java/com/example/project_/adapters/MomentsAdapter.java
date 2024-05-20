package com.example.project_.adapters;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.project_.R;
import com.example.project_.StoryDialog;
import com.example.project_.databinding.ItemContainerUserMomentBinding;
import com.example.project_.models.User;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class MomentsAdapter extends RecyclerView.Adapter<MomentsAdapter.MomentsViewHolder> {
    private final List<User> users;
    private static Context context;

    public MomentsAdapter(List<User> users, Context context) {
        this.users = users;
        this.context = context;
    }

    @NonNull
    @Override
    public MomentsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemContainerUserMomentBinding itemContainerUserMomentBinding = ItemContainerUserMomentBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );
        return new MomentsViewHolder(itemContainerUserMomentBinding);
    }

    @Override
    public void onBindViewHolder(@NonNull MomentsViewHolder holder, int position) {
        holder.setUserData(users.get(position));
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    class MomentsViewHolder extends RecyclerView.ViewHolder {
        ItemContainerUserMomentBinding binding;

        public MomentsViewHolder(ItemContainerUserMomentBinding itemContainerUserMomentBinding) {
            super(itemContainerUserMomentBinding.getRoot());
            binding = itemContainerUserMomentBinding;
        }

        void setUserData(User user) {
            binding.imageProfile.setImageBitmap(getUserImage(user.image));
            checkUserMoments(user.id, new MomentsExistCallback() {
                @Override
                public void onMomentsExist(boolean momentsExist) {
                    if (momentsExist) {
                        binding.imageProfile.setBorderColor(ContextCompat.getColor(context, R.color.primary__color));
                        binding.imageProfile.setBorderWidth(com.intuit.sdp.R.dimen._2sdp);
                    } else {
                        // No moments exist for the user
                        // Do something else
                    }
                }
            });
            LayoutInflater inflater = LayoutInflater.from(context);
            View view = inflater.inflate(R.layout.story_dialog, null);
            Dialog dialog = new StoryDialog(context, user.id);
            dialog.getWindow().getAttributes().windowAnimations = R.style.StoryDialogAnimation;
            dialog.setContentView(view);
            binding.imageProfile.setOnClickListener(v -> {
                checkUserMoments(user.id, new MomentsExistCallback() {
                    @Override
                    public void onMomentsExist(boolean momentsExist) {
                        if (momentsExist && user.id != null) {
                            dialog.show();
                        } else {
                            // No moments exist for the user
                            // Do something else
                        }
                    }
                });
            });
        }
    }

    private Bitmap getUserImage(String encodedImage) {
        byte[] bytes = Base64.decode(encodedImage, Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    }
    public interface MomentsExistCallback {
        void onMomentsExist(boolean momentsExist);
    }

    private void checkUserMoments(String userId, MomentsExistCallback callback) {
        CollectionReference momentsRef = FirebaseFirestore.getInstance().collection("users").document(userId).collection("moments");
        momentsRef.whereGreaterThan("expirationDate", new Date()).get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                QuerySnapshot querySnapshot = task.getResult();
                boolean momentsExist = querySnapshot != null && !querySnapshot.isEmpty();
                callback.onMomentsExist(momentsExist);
            } else {
                callback.onMomentsExist(false); // Assuming moments don't exist if an error occurs
            }
        });
    }


}
