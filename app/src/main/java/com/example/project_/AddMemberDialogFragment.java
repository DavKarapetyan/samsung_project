package com.example.project_;

import android.content.res.ColorStateList;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.annotation.NonNull;

import com.example.project_.activities.AddGroupChatActivity;
import com.example.project_.utilities.PreferenceManager;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.example.project_.databinding.FragmentAddMemberDialogListDialogBinding;
import com.google.android.material.chip.Chip;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>A fragment that shows a list of items as a modal bottom sheet.</p>
 * <p>You can show this modal bottom sheet from your activity like this:</p>
 * <pre>
 *     AddMemberDialogFragment.newInstance(30).show(getSupportFragmentManager(), "dialog");
 * </pre>
 */
public class AddMemberDialogFragment extends BottomSheetDialogFragment {

    private static final String ARG_GROUP_CHAT_ID = "groupChatId";
    private FragmentAddMemberDialogListDialogBinding binding;
    FirebaseFirestore firebaseFirestore;
    PreferenceManager preferenceManager;
    List<String> members;

    public static AddMemberDialogFragment newInstance(String groupChatId) {
        final AddMemberDialogFragment fragment = new AddMemberDialogFragment();
        final Bundle args = new Bundle();
        args.putString(ARG_GROUP_CHAT_ID, groupChatId);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentAddMemberDialogListDialogBinding.inflate(inflater, container, false);
        preferenceManager = new PreferenceManager(getActivity());
        firebaseFirestore = FirebaseFirestore.getInstance();
        members = new ArrayList<>();
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        Bundle args = getArguments();
        String groupChatId = args != null ? args.getString(ARG_GROUP_CHAT_ID) : null;
        binding.fab.setOnClickListener(v -> {
            firebaseFirestore.collection("users").whereEqualTo("nickName", binding.member.getText().toString().trim())
                    .get()
                    .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<QuerySnapshot> task) {
                            if (task.isSuccessful()) {
                                if (task.getResult().size() > 0) {
                                    for (QueryDocumentSnapshot queryDocumentSnapshot : task.getResult()) {
                                        String nickname = queryDocumentSnapshot.getString("nickName");

                                        // Create a new Chip
                                        Chip chip = new Chip(getActivity());
                                        chip.setText(nickname);
                                        chip.setTextSize(14);
                                        chip.setTextColor(getResources().getColor(R.color.primary__color));
                                        chip.setChipStrokeColorResource(R.color.primary__color);
                                        chip.setCloseIconVisible(true);
                                        chip.setCloseIconResource(R.drawable.delete);
                                        chip.setCloseIconTint(ColorStateList.valueOf(getResources().getColor(R.color.primary__color)));

                                        // Set OnClickListener to delete the Chip
                                        chip.setOnCloseIconClickListener(new View.OnClickListener() {
                                            @Override
                                            public void onClick(View v) {
                                                binding.members.removeView(chip); // Remove the Chip from the layout
                                                members.remove(nickname); // Remove the member from the list
                                            }
                                        });

                                        // Add the Chip to the layout
                                        binding.members.addView(chip);
                                        binding.member.setText(null);
                                        members.add(queryDocumentSnapshot.getId());
                                    }
                                } else {
                                    Toast.makeText(getActivity(), "No user with this nickName", Toast.LENGTH_SHORT).show();
                                }
                            } else {
                                Toast.makeText(getActivity(), "Failed to fetch user with this nickName", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
        });

        binding.save.setOnClickListener(v -> {
            for (String a : members) {
                firebaseFirestore.collection("groupChats").document(groupChatId).update("members", FieldValue.arrayUnion(a))
                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void unused) {
                                Toast.makeText(getActivity(), "Ok", Toast.LENGTH_SHORT).show();
                            }
                        });
            }
            getActivity().finish();
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

}