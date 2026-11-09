
package com.example.mobilebankingapplication;

import android.content.Intent;
import android.os.Bundle;
import android.graphics.Color;
import android.text.method.PasswordTransformationMethod;
import android.text.method.HideReturnsTransformationMethod;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    EditText phoneInput, pinInput;
    Button loginButton;
    TextView registerText, togglePinVisibility;

    boolean isPinVisible = false;

    private final ExecutorService executor =
            Executors.newSingleThreadExecutor();

    private static final String LOGIN_URL =
            "http://10.0.2.2:5000/api/login";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        getWindow().setStatusBarColor(Color.rgb(42, 155, 210));
        getWindow().setNavigationBarColor(Color.rgb(7, 90, 158));

        phoneInput = findViewById(R.id.phoneInput);
        pinInput = findViewById(R.id.pinInput);
        loginButton = findViewById(R.id.loginButton);
        registerText = findViewById(R.id.registerText);
        togglePinVisibility = findViewById(R.id.togglePinVisibility);

        // PIN Show / Hide
        pinInput.setTransformationMethod(
                PasswordTransformationMethod.getInstance()
        );

        togglePinVisibility.setOnClickListener(v -> {

            int cursorPosition = pinInput.getSelectionStart();

            if (isPinVisible) {

                pinInput.setTransformationMethod(
                        PasswordTransformationMethod.getInstance()
                );

                togglePinVisibility.setText("👁");
                isPinVisible = false;

            } else {

                pinInput.setTransformationMethod(
                        HideReturnsTransformationMethod.getInstance()
                );

                togglePinVisibility.setText("🙈");
                isPinVisible = true;
            }

            pinInput.setSelection(
                    Math.max(0, Math.min(
                            cursorPosition,
                            pinInput.getText().length()
                    ))
            );
        });

        // Real Backend Login
        loginButton.setOnClickListener(v -> {

            String phone = phoneInput.getText().toString().trim();
            String pin = pinInput.getText().toString().trim();

            if (phone.isEmpty() || pin.isEmpty()) {

                Toast.makeText(
                        this,
                        "Please enter mobile number and PIN",
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }

            if (!phone.matches("01[3-9][0-9]{8}") ||
                    !pin.matches("[0-9]{6}")) {

                Toast.makeText(
                        this,
                        "Enter a valid mobile number and 6-digit PIN",
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }

            loginButton.setEnabled(false);
            loginButton.setText("Please wait...");

            executor.execute(() -> {

                HttpURLConnection connection = null;

                try {

                    URL url = new URL(LOGIN_URL);

                    connection = (HttpURLConnection)
                            url.openConnection();

                    connection.setRequestMethod("POST");
                    connection.setConnectTimeout(10000);
                    connection.setReadTimeout(10000);
                    connection.setDoOutput(true);

                    connection.setRequestProperty(
                            "Content-Type",
                            "application/json"
                    );

                    JSONObject requestBody = new JSONObject();

                    requestBody.put("mobile", phone);
                    requestBody.put("pin", pin);

                    try (OutputStream output =
                                 connection.getOutputStream()) {

                        output.write(
                                requestBody.toString().getBytes(
                                        StandardCharsets.UTF_8
                                )
                        );
                    }

                    int statusCode = connection.getResponseCode();

                    InputStream inputStream =
                            statusCode >= 200 && statusCode < 400
                                    ? connection.getInputStream()
                                    : connection.getErrorStream();

                    StringBuilder response = new StringBuilder();

                    try (BufferedReader reader =
                                 new BufferedReader(
                                         new InputStreamReader(
                                                 inputStream,
                                                 StandardCharsets.UTF_8
                                         ))) {

                        String line;

                        while ((line = reader.readLine()) != null) {
                            response.append(line);
                        }
                    }

                    JSONObject jsonResponse =
                            new JSONObject(response.toString());

                    boolean success =
                            jsonResponse.optBoolean("success", false);

                    String message =
                            jsonResponse.optString(
                                    "message",
                                    "Login failed"
                            );

                    runOnUiThread(() -> {

                        loginButton.setEnabled(true);
                        loginButton.setText("LOGIN");

                        if (success) {

                            Toast.makeText(
                                    MainActivity.this,
                                    "Welcome! Login Successful.",
                                    Toast.LENGTH_LONG
                            ).show();

                        } else {

                            Toast.makeText(
                                    MainActivity.this,
                                    message,
                                    Toast.LENGTH_LONG
                            ).show();
                        }
                    });

                } catch (Exception e) {

                    runOnUiThread(() -> {

                        loginButton.setEnabled(true);
                        loginButton.setText("LOGIN");

                        Toast.makeText(
                                MainActivity.this,
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
        });

        // Open Registration Screen
        registerText.setOnClickListener(v -> {

            Intent intent = new Intent(
                    MainActivity.this,
                    RegistrationActivity.class
            );

            startActivity(intent);
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdownNow();
    }
}
