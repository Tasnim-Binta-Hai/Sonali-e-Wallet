
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

public class MainActivity extends AppCompatActivity {

    EditText phoneInput, pinInput;
    Button loginButton;
    TextView registerText, togglePinVisibility;

    boolean isPinVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        // Status Bar Color
        getWindow().setStatusBarColor(Color.rgb(42, 155, 210));
        getWindow().setNavigationBarColor(Color.rgb(7, 90, 158));

        // Connect XML Views
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
                    Math.max(0, Math.min(cursorPosition,
                            pinInput.getText().length()))
            );
        });

        // Login Button
        loginButton.setOnClickListener(v -> {

            String phone = phoneInput.getText().toString().trim();
            String pin = pinInput.getText().toString().trim();

            if (phone.isEmpty() || pin.isEmpty()) {

                Toast.makeText(this,
                        "Please enter mobile number and PIN",
                        Toast.LENGTH_SHORT).show();

            } else {

                Toast.makeText(this,
                        "Login Successful! (Demo)",
                        Toast.LENGTH_LONG).show();
            }
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
}
