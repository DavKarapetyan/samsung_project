package com.example.project_.adapters;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.RecyclerView;

import com.codebyashish.autoimageslider.Enums.ImageScaleType;
import com.codebyashish.autoimageslider.ExceptionsClass;
import com.codebyashish.autoimageslider.Models.ImageSlidesModel;
import com.example.project_.CommentListDialogFragment;
import com.example.project_.databinding.ItemContainerPostBinding;
import com.example.project_.models.Post;
import com.example.project_.utilities.Constants;
import com.example.project_.utilities.PreferenceManager;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class PostAdapter extends RecyclerView.Adapter<PostAdapter.PostViewHolder> {
    private final List<Post> posts;
    private FragmentManager fragmentManager;
    private PreferenceManager preferenceManager;

    public PostAdapter(List<Post> posts, FragmentManager fragmentManager, PreferenceManager preferenceManager) {
        this.posts = posts;
        this.fragmentManager = fragmentManager;
        this.preferenceManager = preferenceManager;
    }

    @NonNull
    @Override
    public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemContainerPostBinding itemContainerPostBinding = ItemContainerPostBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );
        return new PostViewHolder(itemContainerPostBinding);
    }

    @Override
    public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {
        try {
            holder.setPostData(posts.get(position));
        } catch (ExceptionsClass e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int getItemCount() {
        return posts.size();
    }

    class PostViewHolder extends RecyclerView.ViewHolder {
        ItemContainerPostBinding binding;

        PostViewHolder(ItemContainerPostBinding itemContainerPostBinding) {
            super(itemContainerPostBinding.getRoot()); // Use the root view of the binding object
            binding = itemContainerPostBinding;
        }

        void setPostData(Post post) throws ExceptionsClass {
            if (post.imageUris != null && !post.imageUris.isEmpty()) {
                //Picasso.get().load(post.imageUris.get(0)).into(binding.postImage);
                ArrayList<ImageSlidesModel> autoImageList = new ArrayList<>();
                for (String str : post.imageUris) {
                    autoImageList.add(new ImageSlidesModel(str, ImageScaleType.CENTER_CROP));
                }
                binding.postImages.setImageList(autoImageList);
                binding.postImages.setDefaultAnimation();
            }
            binding.fullName.setText(post.userName);
            binding.content.setText(post.content);
            if (post.userImage != null) {
                binding.profileImage.setImageBitmap(getUserImage(post.userImage));
            }
            binding.showComments.setOnClickListener(v -> {
                CommentListDialogFragment commentListDialogFragment = CommentListDialogFragment.newInstance(post.id, post.userImage);
                commentListDialogFragment.show(fragmentManager, "TAG");
            });
//            if (isUserExistsInLiked(post.id)) {
//                binding.likeButton.setLiked(true);
//            }
            binding.likeButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
//                    if (!binding.likeButton.isLiked())
//                    {
//                        addLike(post.id);
//                        binding.likeButton.setLiked(true);
//                    } else if (binding.likeButton.isLiked()) {
//                        removeLike(post.id);
//                        binding.likeButton.setLiked(false);
//                    }
                    addLike(post.id);
                    binding.likeButton.setLiked(true);
                }
            });
        }

    }

    private Bitmap getUserImage(String encodedImage) {
        byte[] bytes = Base64.decode(encodedImage, Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    }

    private boolean isUserExistsInLiked(String postId) {
        final boolean[] a = new boolean[1];
        FirebaseFirestore.getInstance().collection("posts").document(postId)
                .get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        List<String> likedUsers = (List<String>) documentSnapshot.get("likedUsers");
                        a[0] = likedUsers.contains(preferenceManager.getString(Constants.KEY_USER_ID));
                    }
                });
        return a[0];
    }

    private void addLike(String postId) {
        DocumentReference docRef = FirebaseFirestore.getInstance().collection("posts").document(postId);
        docRef
                .get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        if (documentSnapshot.exists()) {
                            List<String> likedUsers = documentSnapshot.get("likedUsers", List.class);
                            if (likedUsers == null) {
                                likedUsers = new ArrayList<>();
                            }

                            likedUsers.add(preferenceManager.getString(Constants.KEY_USER_ID));

                            docRef.update("likedUsers", likedUsers);
                        } else {
                            System.out.println("Error");
                        }
                    }
                });
    }

    private void removeLike(String postId) {
        DocumentReference docRef = FirebaseFirestore.getInstance().collection("posts").document(postId);
        docRef
                .get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        if (documentSnapshot.exists()) {
                            List<String> likedUsers = documentSnapshot.get("likedUsers", List.class);
                            if (likedUsers != null) {
                                likedUsers.remove(preferenceManager.getString(Constants.KEY_USER_ID));

                                docRef.update("likedUsers", likedUsers);
                            }
                        } else {
                            System.out.println("Error");
                        }
                    }
                });
    }
}
