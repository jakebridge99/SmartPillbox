package com.example.smartpillbox;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.gms.auth.api.signin.GoogleSignInAccount;

public class AccountFragment extends Fragment {

    /**
    onCreateView is used to create the page. The account page displays the users name, email, and
    account id. The account id is unique to each Google account and is used to separate users
    information in the database
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_account, container, false);

        //Find TextView objects from fragment_account.xml that are used for personal information
        TextView nameTV = (TextView) view.findViewById(R.id.name);
        TextView emailTV = (TextView) view.findViewById(R.id.email);
        TextView idTV = (TextView) view.findViewById(R.id.id);

        GoogleSignInAccount acct = com.google.android.gms.auth.api.signin.GoogleSignIn.getLastSignedInAccount(getActivity());   //Get last signed in account
        if (acct != null) {
            // Get values from Google account
            String personName = acct.getDisplayName();
            String personEmail = acct.getEmail();
            String personId = acct.getId();

            //Other potential values to use
                //String personGivenName = acct.getGivenName();
                //String personFamilyName = acct.getFamilyName();

            //Set personal information
            nameTV.setText(personName);
            emailTV.setText(personEmail);
            idTV.setText(personId);
        }
        return view;
    }
}
