
package com.example.mobilebankingapplication;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Calendar;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RegistrationActivity extends AppCompatActivity {

    EditText nameInput, mobileInput, nidInput, dobInput;
    EditText createPinInput, confirmPinInput;
    Button registerButton;
    TextView backToLogin;

    private final ExecutorService executor =
            Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration);

        nameInput = findViewById(R.id.nameInput);
        mobileInput = findViewById(R.id.mobileInput);
        nidInput = findViewById(R.id.nidInput);
        dobInput = findViewById(R.id.dobInput);

        createPinInput = findViewById(R.id.createPinInput);
        confirmPinInput = findViewById(R.id.confirmPinInput);

        registerButton = findViewById(R.id.registerButton);
        backToLogin = findViewById(R.id.backToLogin);

        // Date Picker
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

            if (name.isEmpty()) {
                nameInput.setError("Please enter your full name");
                nameInput.requestFocus();
                return;
            }

            if (!mobile.matches("01[3-9][0-9]{8}")) {
                mobileInput.setError("Enter a valid Bangladesh mobile number");
                mobileInput.requestFocus();
                return;
            }

            if (!nid.matches("([0-9]{10}|[0-9]{13}|[0-9]{17})")) {
                nidInput.setError("Enter a valid NID number");
                nidInput.requestFocus();
                return;
            }

            if (dob.isEmpty()) {
                dobInput.setError("Please select your date of birth");
                dobInput.requestFocus();
                return;
            }

            if (!pin.matches("[0-9]{6}")) {
                createPinInput.setError("PIN must contain exactly 6 digits");
                createPinInput.requestFocus();
                return;
            }

            if (!pin.equals(confirmPin)) {
                confirmPinInput.setError("PIN does not match");
                confirmPinInput.requestFocus();
                return;
            }

            registerButton.setEnabled(false);
            registerButton.setText("Registering...");

            sendRegistrationRequest(name, mobile, nid, dob, pin);
        });

        backToLogin.setOnClickListener(v -> finish());
    }

    private void sendRegistrationRequest(
            String name,
            String mobile,
            String nid,
            String dob,
            String pin
    ) {

        executor.execute(() -> {

            HttpURLConnection connection = null;

            try {
                URL url = new URL(
                        "http://10.0.2.2:5000/api/register"
                );

                connection = (HttpURLConnection) url.openConnection();

                connection.setRequestMethod("POST");
                connection.setRequestProperty(
                        "Content-Type", "application/json; charset=UTF-8"
                );
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(10000);
                connection.setDoOutput(true);

                JSONObject requestData = new JSONObject();

                requestData.put("fullName", name);
                requestData.put("mobile", mobile);
                requestData.put("nid", nid);
                requestData.put("dob", dob);
                requestData.put("pin", pin);

                try (OutputStream outputStream =
                             connection.getOutputStream()) {

                    outputStream.write(
                            requestData.toString().getBytes(
                                    StandardCharsets.UTF_8
                            )
                    );
                }

                int responseCode = connection.getResponseCode();

                InputStream inputStream =
                        (responseCode >= 200 && responseCode < 300)
                                ? connection.getInputStream()
                                : connection.getErrorStream();

                StringBuilder response = new StringBuilder();

                if (inputStream != null) {
                    try (BufferedReader reader = new BufferedReader(
                            new InputStreamReader(
                                    inputStream, StandardCharsets.UTF_8
                            ))) {

                        String line;

                        while ((line = reader.readLine()) != null) {
                            response.append(line);
                        }
                    }
                }

                JSONObject jsonResponse =
                        new JSONObject(response.toString());

                boolean success =
                        jsonResponse.optBoolean("success", false);

                String message = jsonResponse.optString(
                        "message", "Registration failed"
                );

                runOnUiThread(() -> {

                    registerButton.setEnabled(true);
                    registerButton.setText("Register");

                    Toast.makeText(
                            RegistrationActivity.this,
                            message,
                            Toast.LENGTH_LONG
                    ).show();

                    if (success) {
                        finish();
                    }
                });

            } catch (Exception e) {

                runOnUiThread(() -> {

                    registerButton.setEnabled(true);
                    registerButton.setText("Register");

                    Toast.makeText(
                            RegistrationActivity.this,
                            "Connection Error: " + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });

            } finally {

                if (connection != null) {
                    connection.disconnect();
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}
