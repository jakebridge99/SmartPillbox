package com.example.smartpillbox;

import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Map;

public class MedicationFragment extends Fragment {

    private Button addButton;
    private AlertDialog.Builder dialogBuilder;
    private AlertDialog dialog;
    private EditText medName, desc, time, freq, addInfo;
    private Button addMedButton, cancelButton, saveButton, deleteButton;
    private ListView list;
    private ArrayList<MedForm> forms;
    private ArrayList<String> listItems;
    private ArrayAdapter<String> adapter;

    FirebaseFirestore db = FirebaseFirestore.getInstance();


    /*
     onCreateView creates the page layout when the page is opened
     @param inflater
     @param container
     @param savedInstanceState
     @return view
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view =  inflater.inflate(R.layout.fragment_medication, container, false);
        loadFromDatabase();//Load the users medication from firebase
        list = (ListView) view.findViewById(R.id.medList);
        listItems = new ArrayList<>();
        forms = new ArrayList<>();
        addButton = (Button) view.findViewById(R.id.addBtn);
        adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_dropdown_item, listItems);
        list.setAdapter(adapter);

        //Allows items to be selected to edit/delete
        list.setOnItemClickListener(new AdapterView.OnItemClickListener(){
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String item = (String) list.getItemAtPosition(position);
                createNewEditDialogue(item, position);
            }
        });

        //Button to add new medications
        addButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                createNewMedDialog();
            }
        });
        return view;
    }

    /*
     createNewMedDialog gives the user a form to fill out when they press the add new medication
     button. If the user presses the add button in the form the details will be added to firebase.
     If the user presses the cancel button in the form nothing will be saved.
     */
    public void createNewMedDialog(){
        dialogBuilder = new AlertDialog.Builder(this.getContext());
        final View medPopupView = getLayoutInflater().inflate(R.layout.medication_form,null);
        medName = (EditText) medPopupView.findViewById(R.id.med_name);
        desc = (EditText) medPopupView.findViewById(R.id.desc);
        time = (EditText) medPopupView.findViewById(R.id.time);
        freq = (EditText) medPopupView.findViewById(R.id.freq);
        addInfo = (EditText) medPopupView.findViewById(R.id.addInfo);
        addMedButton = (Button) medPopupView.findViewById(R.id.addButt);
        cancelButton = (Button) medPopupView.findViewById(R.id.cancel);
        dialogBuilder.setView(medPopupView);
        dialog = dialogBuilder.create();
        dialog.show();

        addMedButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MedForm form = new MedForm(medName.getText().toString(), desc.getText().toString(), time.getText().toString(), freq.getText().toString(), addInfo.getText().toString());
                addToDatabase(form);
                listItems.add(form.toString());
                adapter.notifyDataSetChanged();
                dialog.dismiss();
                }

        });

        cancelButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
    }


    /*
    addToDatabase adds medication info to the users medication list in firebase. If the medication
    already exists it will overwrite the data previously stored for the medication. If it is a new
    medication it will automatically create a new document for the medication.
    @param form : the medication info to be saved.
     */
    //Adds a newly created medication to the database
    public void addToDatabase(MedForm form){
        GoogleSignInAccount acct = com.google.android.gms.auth.api.signin.GoogleSignIn.getLastSignedInAccount(getActivity());   //Get last signed in account
        if (acct != null) {
            //Save in users->userId->medications->medName
            db.collection("users").document(acct.getId()).collection("medications").document(form.getMedName())
                    .set(form);
        }
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
                                //Adds each document in path to the list of displayed medications
                                for (QueryDocumentSnapshot document : task.getResult()) {
                                    Log.d(TAG, document.getId() + " => " + document.getData());
                                    Map temp = document.getData();
                                    String tempName = (String) temp.get("medName");
                                    listItems.add(tempName);
                                    adapter.notifyDataSetChanged();
                                    Log.d(TAG, "Successfully loaded data!");
                                }
                            } else {
                                Log.d(TAG, "Error getting documents: ", task.getException());
                            }
                        }
                    });
        }
    }


    /*
    deleteFromDatabase removes a medication and its information from the database
    @param medName : the medication to remove
     */
    public void deleteFromDatabase(String medName){
        GoogleSignInAccount acct = com.google.android.gms.auth.api.signin.GoogleSignIn.getLastSignedInAccount(getActivity());   //Get last signed in account
        if (acct != null) {
            //Delete path users->userId->medications->medName
            db.collection("users").document(acct.getId()).collection("medications").document(medName)
                    .delete()
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        private static final String TAG = "Delete Data: ";

                        @Override
                        public void onSuccess(Void aVoid) {
                            Log.d(TAG, "DocumentSnapshot successfully deleted!");
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        private static final String TAG = "Delete Data: ";

                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.w(TAG, "Error deleting document", e);
                        }
                    });
        }
    }


    public void createEvent(MedForm form){

    }


    /*
    createNewEditDialogue allows a user to edit information for a medication. When they click a
    medication in the list a form is shown with pre-filled data that they have already entered. If
    the user presses save, the information will be updated in the database. If they press delete,
    the selected entry will be deleted.
    @param name : name of medication to edit
    @param position : position in list of selected medication
     */
    public void createNewEditDialogue(String name, int position){
        dialogBuilder = new AlertDialog.Builder(this.getContext());
        final View medPopupView = getLayoutInflater().inflate(R.layout.medication_edit_form,null);
        medName = (EditText) medPopupView.findViewById(R.id.med_name);
        desc = (EditText) medPopupView.findViewById(R.id.desc);
        time = (EditText) medPopupView.findViewById(R.id.time);
        freq = (EditText) medPopupView.findViewById(R.id.freq);
        addInfo = (EditText) medPopupView.findViewById(R.id.addInfo);

        GoogleSignInAccount acct = com.google.android.gms.auth.api.signin.GoogleSignIn.getLastSignedInAccount(getActivity());   //Get last signed in account
        if (acct != null) {
            //Populates form with information found at path users->userId->medications->name
            db.collection("users").document(acct.getId()).collection("medications")
                    .whereEqualTo("medName", name)
                    .get()
                    .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                        private static final String TAG = "Data to Edit";

                        @Override
                        public void onComplete(@NonNull Task<QuerySnapshot> task) {
                            if (task.isSuccessful()) {
                                for (QueryDocumentSnapshot document : task.getResult()) {

                                    Map temp = document.getData();
                                    String tempName = (String) temp.get("medName");
                                    String tempDesc = (String) temp.get("desc");
                                    String tempTime = (String) temp.get("time");
                                    String tempFreq = (String) temp.get("freq");
                                    String tempInfo = (String) temp.get("addInfo");

                                    medName.setText(tempName);
                                    desc.setText(tempDesc);
                                    time.setText(tempTime);
                                    freq.setText(tempFreq);
                                    addInfo.setText(tempInfo);

                                    Log.d(TAG, document.getId() + " => " + document.getData());
                                }
                            } else {
                                Log.d(TAG, "Error getting documents: ", task.getException());
                            }
                        }
                    });
        }

        saveButton = (Button) medPopupView.findViewById(R.id.saveButt);
        deleteButton = (Button) medPopupView.findViewById(R.id.delButt);
        dialogBuilder.setView(medPopupView);
        dialog = dialogBuilder.create();
        dialog.show();

        //Save button
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MedForm updatedForm = new MedForm(medName.getText().toString(), desc.getText().toString(), time.getText().toString(), freq.getText().toString(), addInfo.getText().toString());
                addToDatabase(updatedForm);
                listItems.set(position, medName.getText().toString());
                adapter.notifyDataSetChanged();
                dialog.dismiss();
            }
        });

        //Delete button
        deleteButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                listItems.remove(position);
                deleteFromDatabase(name);
                adapter.notifyDataSetChanged();
                dialog.dismiss();
            }
        });
    }


    //Class for medication forms
    public class MedForm {
        public String medName;
        public String desc;
        public String time;
        public String freq;
        public String addInfo;

        public MedForm(String medName, String desc, String time, String freq, String addInfo){
            this.medName = medName;
            this.desc = desc;
            this.time = time;
            this.freq = freq;
            this.addInfo = addInfo;
        }

        public String getMedName(){
            return this.medName;
        }

        @Override
        public String toString(){
            return medName;
        }
    }
}
