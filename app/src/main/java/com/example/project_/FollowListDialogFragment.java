package com.example.project_;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.annotation.NonNull;

import com.codebyashish.autoimageslider.ExceptionsClass;
import com.example.project_.activities.PostDetailsActivity;
import com.example.project_.activities.UserProfileActivity;
import com.example.project_.databinding.FragmentCommentListDialogListDialogItemBinding;
import com.example.project_.models.Follow;
import com.example.project_.utilities.PreferenceManager;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.project_.databinding.FragmentItemListDialogListDialogItemBinding;
import com.example.project_.databinding.FragmentItemListDialogListDialogBinding;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>A fragment that shows a list of items as a modal bottom sheet.</p>
 * <p>You can show this modal bottom sheet from your activity like this:</p>
 * <pre>
 *     FollowListDialogFragment.newInstance(30).show(getSupportFragmentManager(), "dialog");
 * </pre>
 */
public class FollowListDialogFragment extends BottomSheetDialogFragment {

    // TODO: Customize parameter argument names
    private static final String ARG_FOLLOW = "follow";
    private static final String ARG_USER_ID = "userId";
    private FragmentItemListDialogListDialogBinding binding;
    private FirebaseFirestore firebaseFirestore;
    private FollowAdapter followAdapter;
    private List<Follow> follows;
    private PreferenceManager preferenceManager;

    // TODO: Customize parameters
    public static FollowListDialogFragment newInstance(String follow, String userId) {
        final FollowListDialogFragment fragment = new FollowListDialogFragment();
        final Bundle args = new Bundle();
        args.putString(ARG_FOLLOW, follow);
        args.putString(ARG_USER_ID, userId);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        firebaseFirestore = FirebaseFirestore.getInstance();
        preferenceManager = new PreferenceManager(getActivity());
        binding = FragmentItemListDialogListDialogBinding.inflate(inflater, container, false);
        return binding.getRoot();

    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        final RecyclerView recyclerView = (RecyclerView) view.findViewById(R.id.list);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        Bundle args = getArguments();
        String follow = args != null ? args.getString(ARG_FOLLOW) : null;
        String userId = args != null ? args.getString(ARG_USER_ID) : null;
        binding.textView.setText(follow.substring(0, 1).toUpperCase() + follow.substring(1));
        follows = new ArrayList<>();
        followAdapter = new FollowAdapter(follows);
        recyclerView.setAdapter(followAdapter);
        loading(true);
        retrieveData(userId, follow);
    }

    private void retrieveData(String userId, String follow) {
        binding.list.setVisibility(View.GONE);
        binding.headerLottie.setVisibility(View.GONE);
        firebaseFirestore.collection("users").document(userId).collection(follow)
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        follows.clear();
                        for (QueryDocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                            Follow follow = new Follow();
                            follow.userId = documentSnapshot.getString("userId");
                            follows.add(follow);
                        }
                        followAdapter.notifyDataSetChanged();
                        if (follows.isEmpty()) {
                            binding.headerLottie.setVisibility(View.VISIBLE);
                        } else {
                            binding.headerLottie.setVisibility(View.GONE);
                        }
                    }
                });
        binding.list.setVisibility(View.VISIBLE);
        loading(false);
    }

    private void loading(boolean isLoading) {
        if (isLoading) {
            binding.loading.setVisibility(View.VISIBLE);
        } else {
            binding.loading.setVisibility(View.GONE);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private class ViewHolder extends RecyclerView.ViewHolder {

        FragmentItemListDialogListDialogItemBinding binding;


        ViewHolder(FragmentItemListDialogListDialogItemBinding fragmentItemListDialogListDialogItemBinding) {
            super(fragmentItemListDialogListDialogItemBinding.getRoot());
            binding = fragmentItemListDialogListDialogItemBinding;
        }

        void setFollowData(Follow followData) throws ExceptionsClass {
            firebaseFirestore.collection("users").document(followData.userId)
                    .get()
                    .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                        @Override
                        public void onSuccess(DocumentSnapshot documentSnapshot) {
                            binding.textName.setText(documentSnapshot.getString("name"));
                            if (documentSnapshot.getString("image") != null) {
                                binding.imageProfile.setImageBitmap(getUserImage(documentSnapshot.getString("image")));
                            }
                            binding.followItem.setOnClickListener(v -> {
                                Intent intent = new Intent(getActivity(), UserProfileActivity.class);
                                intent.putExtra("userId", followData.userId);
                                startActivity(intent);
                            });
                        }
                    });
        }

        private Bitmap getUserImage(String encodedImage) {
            byte[] bytes = Base64.decode(encodedImage, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        }
    }

    private class FollowAdapter extends RecyclerView.Adapter<ViewHolder> {

        private final List<Follow> follows;

        FollowAdapter(List<Follow> follows) {
            this.follows = follows;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

            return new ViewHolder(FragmentItemListDialogListDialogItemBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));

        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            try {
                holder.setFollowData(follows.get(position));
            } catch (ExceptionsClass e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public int getItemCount() {
            return follows.size();
        }

    }
}