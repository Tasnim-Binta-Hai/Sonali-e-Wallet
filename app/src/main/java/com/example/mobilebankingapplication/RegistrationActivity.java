package com.example.mobilebankingapplication;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Calendar;
import java.util.Locale;

public class RegistrationActivity extends AppCompatActivity {

    EditText nameInput, mobileInput, nidInput, dobInput;
    EditText createPinInput, confirmPinInput;
    Button registerButton;
    TextView backToLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration);

        // Connect XML Views

        nameInput = findViewById(R.id.nameInput);
        mobileInput = findViewById(R.id.mobileInput);
        nidInput = findViewById(R.id.nidInput);
        dobInput = findViewById(R.id.dobInput);

        createPinInput = findViewById(R.id.createPinInput);
        confirmPinInput = findViewById(R.id.confirmPinInput);

        registerButton = findViewById(R.id.registerButton);
        backToLogin = findViewById(R.id.backToLogin);

        // Date of Birth Picker

        dobInput.setOnClickListener(v -> {

            Calendar calendar = Calendar.getInstance();

            DatePickerDialog datePicker = new DatePickerDialog(
                    RegistrationActivity.this,
                    (view, year, month, day) -> {

                        String date = String.format(
                                Locale.getDefault(),
                                "%02d/%02d/%04d",
                                day, month + 1, year
                        );

                        dobInput.setText(date);
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
            );

            datePicker.getDatePicker().setMaxDate(
                    System.currentTimeMillis()
            );

            datePicker.show();
        });

        // Registration Button

        registerButton.setOnClickListener(v -> {

            String name = nameInput.getText().toString().trim();
            String mobile = mobileInput.getText().toString().trim();
            String nid = nidInput.getText().toString().trim();
            String dob = dobInput.getText().toString().trim();

            String pin = createPinInput.getText().toString().trim();
            String confirmPin = confirmPinInput.getText().toString().trim();

            // Name Validation

            if (name.isEmpty()) {
                nameInput.setError("Please enter your full name");
                nameInput.requestFocus();
                return;
            }

            // Mobile Validation

            if (!mobile.matches("01[3-9][0-9]{8}")) {
                mobileInput.setError("Enter a valid Bangladesh mobile number");
                mobileInput.requestFocus();
                return;
            }

            // NID Validation

            if (!nid.matches("([0-9]{10}|[0-9]{13}|[0-9]{17})")) {
                nidInput.setError("Enter a valid NID number");
                nidInput.requestFocus();
                return;
            }

            // Date Validation

            if (dob.isEmpty()) {
                dobInput.setError("Please select your date of birth");
                return;
            }

            // PIN Validation

            if (!pin.matches("[0-9]{6}")) {
                createPinInput.setError("PIN must contain exactly 6 digits");
                createPinInput.requestFocus();
                return;
            }

            // Confirm PIN Validation

            if (!pin.equals(confirmPin)) {
                confirmPinInput.setError("PIN does not match");
                confirmPinInput.requestFocus();
                return;
            }

            // Demo Registration Success

            Toast.makeText(
                    RegistrationActivity.this,
                    "Registration Successful! (Demo)",
                    Toast.LENGTH_LONG
            ).show();

            finish();
        });

        // Back to Login

        backToLogin.setOnClickListener(v -> finish());
    }
}
