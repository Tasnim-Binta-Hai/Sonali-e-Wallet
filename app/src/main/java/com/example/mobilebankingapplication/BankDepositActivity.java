package com.example.mobilebankingapplication;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BankDepositActivity extends Activity {

    private TextView backButton;
    private TextView balanceText;

    private EditText amountInput;
    private EditText pinInput;

    private Button submitButton;

    private ExecutorService executorService;

    private int customerId;

    // Only Wallet to Bank is allowed
    private static final String TRANSFER_TYPE = "WALLET_TO_BANK";

    // Demo account IDs
    private static final int BANK_ACCOUNT_ID = 1;
    private static final int WALLET_ACCOUNT_ID = 1;

    private static final String API_URL =
            "http://10.0.2.2:5000/api/add-money";

    private static final String DASHBOARD_URL =
            "http://10.0.2.2:5000/api/dashboard/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_bank_deposit);

        // CONNECT XML VIEWS
        backButton = findViewById(R.id.bankDepositBackButton);

        balanceText = findViewById(
                R.id.depositUsableBalanceText
        );

        amountInput = findViewById(
                R.id.depositAmountInput
        );

        pinInput = findViewById(
                R.id.depositPinInput
        );

        submitButton = findViewById(
                R.id.bankDepositSubmitButton
        );

        executorService = Executors.newSingleThreadExecutor();

        customerId = getIntent().getIntExtra(
                "CUSTOMER_ID",
                -1
        );

        // BACK BUTTON
        backButton.setOnClickListener(v -> finish());

        // CUSTOMER VALIDATION
        if (customerId <= 0) {

            Toast.makeText(
                    this,
                    "Customer information missing. Please login again.",
                    Toast.LENGTH_LONG
            ).show();

            finish();
            return;
        }

        // SUBMIT BUTTON
        submitButton.setOnClickListener(v -> validateDeposit());

        // LOAD OWN WALLET BALANCE
        loadWalletBalance();
    }

    // ==========================================
    // LOAD WALLET BALANCE
    // ==========================================

    private void loadWalletBalance() {

        balanceText.setText("৳ Loading...");

        executorService.execute(() -> {

            HttpURLConnection connection = null;

            try {

                URL url = new URL(
                        DASHBOARD_URL + customerId
                );

                connection = (HttpURLConnection) url.openConnection();

                connection.setRequestMethod("GET");
                connection.setConnectTimeout(15000);
                connection.setReadTimeout(15000);

                int responseCode = connection.getResponseCode();

                InputStream inputStream =
                        responseCode >= 200 && responseCode < 300
                                ? connection.getInputStream()
                                : connection.getErrorStream();

                if (inputStream == null) {
                    throw new Exception("Empty server response");
                }

                JSONObject response = new JSONObject(
                        readResponse(inputStream)
                );

                if (responseCode < 200 ||
                        responseCode >= 300 ||
                        !response.optBoolean("success", false)) {

                    throw new Exception(
                            response.optString(
                                    "message",
                                    "Unable to load wallet balance"
                            )
                    );
                }

                double walletBalance = response.optDouble(
                        "walletBalance",
                        0
                );

                String formatted = formatBalance(walletBalance);

                runOnUiThread(() -> {

                    if (!isFinishing() && !isDestroyed()) {
                        balanceText.setText(formatted);
                    }

                });

            } catch (Exception e) {

                android.util.Log.e(
                        "BankDeposit",
                        "Wallet balance loading failed",
                        e
                );

                runOnUiThread(() -> {

                    if (!isFinishing() && !isDestroyed()) {

                        balanceText.setText("৳ Unavailable");

                        Toast.makeText(
                                BankDepositActivity.this,
                                "Unable to load wallet balance",
                                Toast.LENGTH_LONG
                        ).show();
                    }

                });

            } finally {

                if (connection != null) {
                    connection.disconnect();
                }

            }

        });
    }

    // ==========================================
    // VALIDATE DEPOSIT
    // ==========================================

    private void validateDeposit() {

        String amount = amountInput.getText().toString().trim();
        String pin = pinInput.getText().toString().trim();

        if (amount.isEmpty()) {

            amountInput.setError("Enter deposit amount");
            amountInput.requestFocus();
            return;
        }

        try {

            BigDecimal value = new BigDecimal(amount);

            if (value.compareTo(BigDecimal.ZERO) <= 0 ||
                    value.scale() > 2) {

                amountInput.setError("Enter a valid amount");
                amountInput.requestFocus();
                return;
            }

        } catch (NumberFormatException e) {

            amountInput.setError("Enter a valid amount");
            amountInput.requestFocus();
            return;
        }

        if (!pin.matches("\\d{6}")) {

            pinInput.setError("Enter your 6-digit PIN");
            pinInput.requestFocus();
            return;
        }

        showConfirmation(amount, pin);
    }

    // ==========================================
    // CONFIRMATION DIALOG
    // ==========================================

    private void showConfirmation(String amount, String pin) {

        new AlertDialog.Builder(this)
                .setTitle("Confirm Bank Deposit")
                .setMessage(
                        "Transaction: Wallet to Bank" +
                                "\nFrom: Your Sonali e-Wallet" +
                                "\nTo: Your Bank Account" +
                                "\nAmount: ৳ " + amount +
                                "\n\nDo you want to continue?"
                )
                .setNegativeButton(
                        "Cancel",
                        (dialog, which) -> dialog.dismiss()
                )
                .setPositiveButton(
                        "Confirm",
                        (dialog, which) -> performDeposit(amount, pin)
                )
                .show();
    }

    // ==========================================
    // PERFORM DEPOSIT API
    // ==========================================

    private void performDeposit(String amount, String pin) {

        submitButton.setEnabled(false);
        submitButton.setText("PROCESSING...");

        executorService.execute(() -> {

            HttpURLConnection connection = null;

            try {

                JSONObject requestData = new JSONObject();

                requestData.put("customerId", customerId);

                // Always WALLET_TO_BANK
                requestData.put("transferType", TRANSFER_TYPE);

                requestData.put("bankAccountId", BANK_ACCOUNT_ID);
                requestData.put("walletAccountId", WALLET_ACCOUNT_ID);

                requestData.put("amount", amount);
                requestData.put("pin", pin);

                requestData.put(
                        "description",
                        "Sonali e-Wallet Bank Deposit"
                );

                URL url = new URL(API_URL);

                connection = (HttpURLConnection) url.openConnection();

                connection.setRequestMethod("POST");
                connection.setConnectTimeout(15000);
                connection.setReadTimeout(15000);
                connection.setDoOutput(true);

                connection.setRequestProperty(
                        "Content-Type",
                        "application/json; charset=UTF-8"
                );

                byte[] requestBytes = requestData.toString()
                        .getBytes(StandardCharsets.UTF_8);

                try (OutputStream outputStream =
                             connection.getOutputStream()) {

                    outputStream.write(requestBytes);
                    outputStream.flush();
                }

                int responseCode = connection.getResponseCode();

                InputStream inputStream =
                        responseCode >= 200 && responseCode < 300
                                ? connection.getInputStream()
                                : connection.getErrorStream();

                String responseText = inputStream == null
                        ? "{}"
                        : readResponse(inputStream);

                JSONObject response = new JSONObject(responseText);

                boolean success =
                        responseCode >= 200 &&
                                responseCode < 300 &&
                                response.optBoolean("success", false);

                String message = response.optString(
                        "message",
                        success
                                ? "Bank deposit completed successfully"
                                : "Bank deposit failed"
                );

                runOnUiThread(() -> {

                    if (isFinishing() || isDestroyed()) {
                        return;
                    }

                    submitButton.setEnabled(true);
                    submitButton.setText("SUBMIT");

                    if (success) {

                        new AlertDialog.Builder(BankDepositActivity.this)
                                .setTitle("Deposit Successful")
                                .setMessage(message)
                                .setPositiveButton(
                                        "OK",
                                        (dialog, which) -> {
                                            setResult(RESULT_OK);
                                            finish();
                                        }
                                )
                                .setCancelable(false)
                                .show();

                    } else {

                        Toast.makeText(
                                BankDepositActivity.this,
                                message,
                                Toast.LENGTH_LONG
                        ).show();
                    }

                });

            } catch (Exception e) {

                android.util.Log.e(
                        "BankDeposit",
                        "Deposit transaction failed",
                        e
                );

                runOnUiThread(() -> {

                    if (isFinishing() || isDestroyed()) {
                        return;
                    }

                    submitButton.setEnabled(true);
                    submitButton.setText("SUBMIT");

                    Toast.makeText(
                            BankDepositActivity.this,
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

    // ==========================================
    // READ HTTP RESPONSE
    // ==========================================

    private String readResponse(InputStream inputStream)
            throws Exception {

        StringBuilder response = new StringBuilder();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(
                        inputStream,
                        StandardCharsets.UTF_8
                ))) {

            String line;

            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
        }

        return response.toString();
    }

    // ==========================================
    // FORMAT BALANCE
    // ==========================================

    private String formatBalance(double balance) {

        NumberFormat formatter =
                NumberFormat.getNumberInstance(Locale.US);

        formatter.setMinimumFractionDigits(2);
        formatter.setMaximumFractionDigits(2);

        return "৳ " + formatter.format(balance);
    }

    // ==========================================
    // SYSTEM BACK BUTTON
    // ==========================================

    @Override
    public void onBackPressed() {
        finish();
    }

    // ==========================================
    // DESTROY
    // ==========================================

    @Override
    protected void onDestroy() {

        if (executorService != null) {
            executorService.shutdown();
        }

        super.onDestroy();
    }
}
