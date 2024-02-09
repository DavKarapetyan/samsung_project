    package com.example.project_.adapters;

    import android.graphics.Bitmap;
    import android.graphics.BitmapFactory;
    import android.net.Uri;
    import android.util.Base64;
    import android.view.LayoutInflater;
    import android.view.ViewGroup;

    import androidx.annotation.NonNull;
    import androidx.recyclerview.widget.RecyclerView;

    import com.codebyashish.autoimageslider.Enums.ImageScaleType;
    import com.codebyashish.autoimageslider.ExceptionsClass;
    import com.codebyashish.autoimageslider.Models.ImageSlidesModel;
    import com.example.project_.databinding.ItemContainerPostBinding;
    import com.example.project_.models.Post;
    import com.squareup.picasso.Picasso;

    import java.util.ArrayList;
    import java.util.List;

    public class PostAdapter extends RecyclerView.Adapter<PostAdapter.PostViewHolder> {
        private final List<Post> posts;
        public PostAdapter(List<Post> posts) {
            this.posts = posts;
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
            }

        }
        private Bitmap getUserImage(String encodedImage) {
            byte[] bytes = Base64.decode(encodedImage, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        }
    }
