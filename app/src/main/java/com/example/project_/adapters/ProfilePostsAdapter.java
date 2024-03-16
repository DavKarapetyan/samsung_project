package com.example.project_.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.project_.activities.PostDetailsActivity;
import com.example.project_.databinding.ItemContainerPostProfileBinding;
import com.example.project_.models.Post;
import com.squareup.picasso.Picasso;

import java.util.List;

public class ProfilePostsAdapter extends RecyclerView.Adapter<ProfilePostsAdapter.ProfilePostViewHolder> {
    private final List<Post> posts;
    private static Context context;
    public ProfilePostsAdapter(List<Post> posts, Context context) {
        this.posts = posts;
        this.context = context;
    }
    @NonNull
    @Override
    public ProfilePostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemContainerPostProfileBinding itemContainerPostProfileBinding = ItemContainerPostProfileBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );
        return new ProfilePostViewHolder(itemContainerPostProfileBinding);
    }

    @Override
    public void onBindViewHolder(@NonNull ProfilePostViewHolder holder, int position) {
        holder.setPostData(posts.get(position));
    }

    @Override
    public int getItemCount() {
        return posts.size();
    }

    static class ProfilePostViewHolder extends RecyclerView.ViewHolder {
        ItemContainerPostProfileBinding binding;

        public ProfilePostViewHolder(ItemContainerPostProfileBinding itemContainerPostProfileBinding) {
            super(itemContainerPostProfileBinding.getRoot());
            binding = itemContainerPostProfileBinding;
        }

        void setPostData(Post post) {
            if (post.imageUris != null && !post.imageUris.isEmpty()) {
                Picasso.get().load(post.imageUris.get(0)).into(binding.postImage);
            }
            binding.postImage.setOnClickListener(v -> {
                Intent intent = new Intent(context.getApplicationContext(), PostDetailsActivity.class);
                intent.putExtra("postId", post.id);
                context.startActivity(intent);
            });
        }
    }
}
