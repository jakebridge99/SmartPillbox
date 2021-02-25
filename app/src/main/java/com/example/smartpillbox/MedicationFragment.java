package com.example.smartpillbox;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;

public class MedicationFragment extends Fragment {

    private Button addButton;
    private AlertDialog.Builder dialogBuilder;
    private AlertDialog dialog;
    private EditText medName, desc, time, freq, addInfo;
    private Button addMedButton, cancelButton;
    private ListView list;
    private ArrayList<String> listItems;
    private ArrayAdapter<String> adapter;



    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view =  inflater.inflate(R.layout.fragment_medication, container, false);

        list = (ListView) view.findViewById(R.id.medList);
        listItems = new ArrayList<>();
        addButton = (Button) view.findViewById(R.id.addBtn);
        adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_dropdown_item, listItems);
        list.setAdapter(adapter);
        addButton.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View v) {
                createNewMedDialog();
            }
        });

        return view;
    }

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
                listItems.add(medName.getText().toString());
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

}
