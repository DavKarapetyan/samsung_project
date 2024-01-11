package com.example.project_;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.project_.utilities.Constants;
import com.example.project_.utilities.PreferenceManager;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

public class BaseFragment extends Fragment {
    private DocumentReference documentReference;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PreferenceManager preferenceManager = new PreferenceManager(getActivity());
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        documentReference = database.collection(Constants.KEY_COLLECTION_USERS)
                .document(preferenceManager.getString(Constants.KEY_USER_ID));
    }

    @Override
    public void onPause() {
        super.onPause();
        documentReference.update(Constants.KEY_AVAILABILITY, 0);
    }

    @Override
    public void onResume() {
        super.onResume();
        documentReference.update(Constants.KEY_AVAILABILITY, 1);
    }
}
