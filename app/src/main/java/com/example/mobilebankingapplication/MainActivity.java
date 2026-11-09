
package com.example.mobilebankingapplication;

import android.os.Bundle;
import android.graphics.Color;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    EditText phoneInput, pinInput;
    Button loginButton;
    TextView registerText;

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

        // Login Button Functionality
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

        // Registration Functionality
        registerText.setOnClickListener(v -> {

            Toast.makeText(this,
                    "Registration Successful! (Demo)",
                    Toast.LENGTH_LONG).show();
        });
    }
}
