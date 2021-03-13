package com.example.smartpillbox;

import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;

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
import java.util.Arrays;
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
    private ArrayList<HashMap> tempList = new ArrayList<HashMap>();
    FirebaseFirestore db = FirebaseFirestore.getInstance();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View view =  inflater.inflate(R.layout.fragment_schedule, container, false);
        loadFromDatabase();//Load the users medication from firebase
        listItems = new ArrayList<>();
        list = (ListView) view.findViewById(R.id.medSchedule);
        adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_dropdown_item, listItems);
        list.setAdapter(adapter);
        return view;
    }


    /*
    loadFromDatabase loads all of the users previously saved data. This function is called in
    onCreateView so that the users information is displayed when they open the page.
     */
    public void loadFromDatabase(){
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
                                for (QueryDocumentSnapshot document : task.getResult()) {
                                    Log.d(TAG, document.getId() + " => " + document.getData());
                                    Map temp = document.getData();
                                    String tempName = (String) temp.get("medName");
                                    String tempTime = (String) temp.get("time");
                                    HashMap tempMap = new HashMap();
                                    tempMap.put("time", tempTime);
                                    tempMap.put("medName", tempName);
                                    tempList.add(tempMap);
                                    Log.d(TAG, "Successfully loaded data!");
                                }
                            } else {
                                Log.d(TAG, "Error getting documents: ", task.getException());
                            }

                            Log.d(TAG, "Data to be sorted!" + tempList);
                            sortByTime(tempList);
                        }
                    });
        }
    }


    /*
    sortByTime sorts the users pill schedule by time from 00:00 hrs to 23:59 hrs.
    @param schedule : the schedule to be sorted
     */
    public void sortByTime(ArrayList schedule){
        HashMap tempMap;
        int [] intArray = new int[schedule.size()];
        //Get all of the times that medications are taken
        for (int i = 0; i < schedule.size(); i++) {
            tempMap = (HashMap) schedule.get(i);
            intArray[i] = Integer.parseInt((String) tempMap.get("time"));
        }
        //Sort the times
        Arrays.sort(intArray);
        //Add the items to the schedule
        for(int i = 0; i < intArray.length; i++) {
            for (int j = 0; j < schedule.size(); j++){
                tempMap = (HashMap) schedule.get(j);
                if (((String) tempMap.get("time")).equals(String.valueOf(intArray[i]))){
                    listItems.add((String) tempMap.get("time") + " : " + (String) tempMap.get("medName"));
                    adapter.notifyDataSetChanged();
                }
            }
        }
    }
}
