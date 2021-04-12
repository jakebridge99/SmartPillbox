package com.example.smartpillbox;

import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
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
import android.widget.NumberPicker;
import android.widget.TimePicker;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.auth.User;

import java.sql.Time;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;

public class MedicationFragment extends Fragment {

    private Button addButton;
    private AlertDialog.Builder dialogBuilder;
    private AlertDialog dialog;
    private EditText medName, desc, freq, addInfo;
    private Button addMedButton, cancelButton, saveButton, deleteButton, timeButt;
    private NumberPicker numPicker;
    private ListView list;
    private ArrayList<MedForm> forms;
    private ArrayList<String> listItems;
    private ArrayAdapter<String> adapter;
    private Integer time;
    private String[] pickerVals = {"5", "10", "15", "20", "25", "30"};
    private int valuePicker;

    FirebaseFirestore db = FirebaseFirestore.getInstance();


    /**
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


    /**
     createNewMedDialog gives the user a form to fill out when they press the add new medication
     button. If the user presses the add button in the form the details will be added to firebase.
     If the user presses the cancel button in the form nothing will be saved.
     */
    public void createNewMedDialog(){
        dialogBuilder = new AlertDialog.Builder(this.getContext());
        final View medPopupView = getLayoutInflater().inflate(R.layout.medication_form,null);
        medName = (EditText) medPopupView.findViewById(R.id.med_name);
        desc = (EditText) medPopupView.findViewById(R.id.desc);
        timeButt = (Button) medPopupView.findViewById(R.id.timeButt);
        freq = (EditText) medPopupView.findViewById(R.id.freq);
        addInfo = (EditText) medPopupView.findViewById(R.id.addInfo);
        addMedButton = (Button) medPopupView.findViewById(R.id.addButt);
        cancelButton = (Button) medPopupView.findViewById(R.id.cancel);
        numPicker = (NumberPicker) medPopupView.findViewById(R.id.alarmTime);
        numPicker.setDisplayedValues(pickerVals);
        numPicker.setMaxValue(5);
        numPicker.setMinValue(0);
        numPicker.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                valuePicker = numPicker.getValue();
            }
        });
        dialogBuilder.setView(medPopupView);
        dialog = dialogBuilder.create();
        dialog.show();

        timeButt.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View v) {
                TimePickerDialog timePickerDialog = new TimePickerDialog(getContext(), new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                        time = hourOfDay;
                        timeButt.setText(hourOfDay + " : 00");
                    }
                }, 12, 0, true);
                timePickerDialog.show();
            }
        });

        addMedButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MedForm form = new MedForm(medName.getText().toString(), desc.getText().toString(), time.toString(), freq.getText().toString(), addInfo.getText().toString(), "", Integer.toString((valuePicker + 1) * 5));
                convertToUnixTime(form);
                addToDatabase(form);
                listItems.add(form.toString());
                adapter.notifyDataSetChanged();
                Intent intent = new Intent(getContext(), ReminderBroadcast.class);
                intent.putExtra("Med Name", form.medName);
                intent.putExtra("Extra Info", "Description: " + form.desc + "\n" +  "Frequency: " + form.freq + "\n" + "Additional Info: " + form.addInfo);
                PendingIntent pendingIntent = PendingIntent.getBroadcast(getContext(), 0, intent, 0);
                AlarmManager alarmManager = (AlarmManager) getContext().getSystemService(Context.ALARM_SERVICE);

                Calendar calendar = Calendar.getInstance();
                calendar.set(Calendar.SECOND, 0);
                calendar.set(Calendar.MINUTE, 0);
                calendar.set(Calendar.HOUR_OF_DAY, Integer.parseInt(form.time));
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
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
    @param form the medication info to be saved.
     */
    public void addToDatabase(MedForm form){
        GoogleSignInAccount acct = com.google.android.gms.auth.api.signin.GoogleSignIn.getLastSignedInAccount(getActivity());   //Get last signed in account
        if (acct != null) {
            //Save in users->userId->medications->medName
            db.collection("users").document(acct.getId()).collection("medications").document(form.getMedName())
                    .set(form);
        }
    }


    /**
    convertToUnixTime creates 6 months worth of unix timestamps for when a medication should be
    taken. 6 months = 6 * 30 = 180
    @param form form to add unix timestamps to
     */
    public void convertToUnixTime(MedForm form) {
        GoogleSignInAccount acct = com.google.android.gms.auth.api.signin.GoogleSignIn.getLastSignedInAccount(getActivity());   //Get last signed in account
        if (acct != null) {
            int year = 0;
            int month = 0;
            int day = 0;
            int hour = 0;
            int time_int = Integer.parseInt(form.getTime());
            String times = form.getUnixTimes();

            year = Calendar.getInstance().get(Calendar.YEAR);
            month = Calendar.getInstance().get(Calendar.MONTH);
            day = Calendar.getInstance().get(Calendar.DATE);
            hour = Calendar.getInstance().get(Calendar.HOUR);

            //Time has past on day of, schedule first dosage for next day
            if (hour > time_int) {
                day += 1;
            }

            Date scheduleTime = new Date(year - 1900, month, day, time_int, 0);
            long timeStamp = (scheduleTime.getTime()) / 1000;
            final String TAG = "DATE";
            Log.d(TAG, "Date : " + scheduleTime);

            for (int i = 0; i < 180; i++) {
                //Save in users->userId->medications->medName
                times = times + timeStamp + "\n";
                timeStamp += 86400;
            }
            form.setUnixTimes(times);//Add the time stamps to the form
        }
    }


    /**
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


    /**
    deleteFromDatabase removes a medication and its information from the database
    @param medName the medication to remove
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


    /**
    createNewEditDialogue allows a user to edit information for a medication. When they click a
    medication in the list a form is shown with pre-filled data that they have already entered. If
    the user presses save, the information will be updated in the database. If they press delete,
    the selected entry will be deleted.
    @param name name of medication to edit
    @param position position in list of selected medication
     */
    public void createNewEditDialogue(String name, int position){
        dialogBuilder = new AlertDialog.Builder(this.getContext());
        final View medPopupView = getLayoutInflater().inflate(R.layout.medication_edit_form,null);
        medName = (EditText) medPopupView.findViewById(R.id.med_name);
        desc = (EditText) medPopupView.findViewById(R.id.desc);
        timeButt = (Button) medPopupView.findViewById(R.id.timeButt);
        freq = (EditText) medPopupView.findViewById(R.id.freq);
        addInfo = (EditText) medPopupView.findViewById(R.id.addInfo);
        numPicker = (NumberPicker) medPopupView.findViewById(R.id.alarmTime);
        numPicker.setDisplayedValues(pickerVals);
        numPicker.setMaxValue(5);
        numPicker.setMinValue(0);
        numPicker.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                valuePicker = numPicker.getValue();
            }
        });
        final String[] oldName = new String[1];



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
                                    oldName[0] = tempName;
                                    String tempDesc = (String) temp.get("desc");
                                    String tempTime = (String) temp.get("time");
                                    String tempFreq = (String) temp.get("freq");
                                    String tempInfo = (String) temp.get("addInfo");
                                    String tempAlarm = (String) temp.get("alarmTime");

                                    medName.setText(tempName);
                                    desc.setText(tempDesc);
                                    timeButt.setText(tempTime + " : 00");
                                    freq.setText(tempFreq);
                                    addInfo.setText(tempInfo);
                                    numPicker.setValue(Integer.parseInt(tempAlarm)/5 - 1);

                                    Log.d(TAG, document.getId() + " => " + document.getData());
                                }
                            } else {
                                Log.d(TAG, "Error getting documents: ", task.getException());
                            }
                        }
                    });
            timeButt.setOnClickListener(new View.OnClickListener(){

                @Override
                public void onClick(View v) {
                    TimePickerDialog timePickerDialog = new TimePickerDialog(getContext(), new TimePickerDialog.OnTimeSetListener() {
                        @Override
                        public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                            time = hourOfDay;
                            timeButt.setText(time + " : 00");
                        }
                    }, 12, 0, true);
                    timePickerDialog.show();
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
                deleteFromDatabase(oldName[0]);//Delete old data name so there isn't identical data saved in 2 places
                MedForm updatedForm = new MedForm(medName.getText().toString(), desc.getText().toString(), time.toString(), freq.getText().toString(), addInfo.getText().toString(), "", Integer.toString((valuePicker + 1) * 5));
                convertToUnixTime(updatedForm);
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
                DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog1, int which) {
                        switch (which){
                            case DialogInterface.BUTTON_POSITIVE:
                                listItems.remove(position);
                                deleteFromDatabase(name);
                                adapter.notifyDataSetChanged();
                                dialog1.dismiss();
                                dialog.dismiss();
                                break;

                            case DialogInterface.BUTTON_NEGATIVE:
                                //No button clicked
                                break;
                        }
                    }
                };

                AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext());
                builder.setMessage("Are you sure?").setPositiveButton("Yes", dialogClickListener)
                        .setNegativeButton("No", dialogClickListener).show();

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
        public String unixTimes;
        public String alarmTime;

        public MedForm(String medName, String desc, String time, String freq, String addInfo, String unixTimes, String alarmTime){
            this.medName = medName;
            this.desc = desc;
            this.time = time;
            this.freq = freq;
            this.addInfo = addInfo;
            this.unixTimes = unixTimes;
            this.alarmTime = alarmTime;
        }

        public String getMedName(){
            return this.medName;
        }

        public String getTime(){return this.time;}

        public void setUnixTimes(String unix){this.unixTimes = unix;}

        @Override
        public String toString(){
            return medName;
        }

        public String getUnixTimes() {return this.unixTimes;}
    }
}
