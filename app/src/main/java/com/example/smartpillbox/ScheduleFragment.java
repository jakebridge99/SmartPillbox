package com.example.smartpillbox;

import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ScheduleFragment extends Fragment {

    private AlertDialog.Builder dialogBuilder;
    private AlertDialog dialog;
    private ListView list;
    private ArrayList<String> listItems;
    private ArrayAdapter<String> adapter;

    private ArrayList<HashMap> scheduleList;
    private HashMap scheduleMap = new HashMap();
    FirebaseFirestore db = FirebaseFirestore.getInstance();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View view =  inflater.inflate(R.layout.fragment_schedule, container, false);
        scheduleList = loadFromDatabase();//Load the users medication from firebase
        listItems = new ArrayList<>();
        sortByTime(scheduleList);
        list = (ListView) view.findViewById(R.id.medSchedule);
        adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_dropdown_item, listItems);
        list.setAdapter(adapter);
        return view;
    }


    public ArrayList loadFromDatabase(){
        ArrayList<HashMap> tempList = new ArrayList<HashMap>();
        GoogleSignInAccount acct = com.google.android.gms.auth.api.signin.GoogleSignIn.getLastSignedInAccount(getActivity());   //Get last signed in account
        if (acct != null) {
            //Retrieve all documents in path users->userId->medications
            db.collection("users").document(acct.getId()).collection("medications")
                    .get()
                    .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                        private static final String TAG = "Load Data";

                        @Override
                        public void onComplete(@NonNull Task<QuerySnapshot> task) {
                            if (task.isSuccessful()) {
                                //Adds each document in path to the list of displayed medications
                                int i = 0;
                                for (QueryDocumentSnapshot document : task.getResult()) {
                                    Log.d(TAG, document.getId() + " => " + document.getData());
                                    Map temp = document.getData();
                                    String tempName = (String) temp.get("medName");
                                    String tempTime = (String) temp.get("time");
                                    scheduleMap.put("time", tempTime);
                                    scheduleMap.put("medName", tempName);
                                    tempList.add(i, scheduleMap);
                                    Log.d(TAG, "Successfully loaded data!");
                                    i = i + 1;
                                }
                            } else {
                                Log.d(TAG, "Error getting documents: ", task.getException());
                            }
                        }
                    });
        }
        return tempList;
    }


    public void sortByTime(ArrayList schedule){
        HashMap tempMap = new HashMap();
        Collections.sort(schedule);
        for (int i = 0; i < schedule.size(); i++) {
            tempMap = (HashMap) schedule.get(i);
            String tempName = (String) tempMap.get("medName");
            listItems.add(tempName);
            adapter.notifyDataSetChanged();
        }
    }
}
