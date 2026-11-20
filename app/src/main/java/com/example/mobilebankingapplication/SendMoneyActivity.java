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
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SendMoneyActivity extends Activity {

    private TextView transferTitle;
    private TextView transferSubtitle;
    private TextView sourceBalanceText;
    private TextView destinationLabel;
    private TextView backButton;

    private EditText destinationNumberInput;
    private EditText amountInput;
    private EditText pinInput;

    private Button continueTransferButton;

    private ExecutorService executorService;

    private int customerId;
    private String transferType;

    // Demo account IDs
    private static final int BANK_ACCOUNT_ID = 1;
    private static final int WALLET_ACCOUNT_ID = 1;

    private static final String API_URL =
            "http://10.0.2.2:5000/api/transfer";

    private static final String DASHBOARD_URL =
            "http://10.0.2.2:5000/api/dashboard/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_send_money);

        // Initialize views
        transferTitle = findViewById(R.id.transferTitle);
        transferSubtitle = findViewById(R.id.transferSubtitle);
        sourceBalanceText = findViewById(R.id.sourceBalanceText);
        destinationLabel = findViewById(R.id.destinationLabel);
        backButton = findViewById(R.id.backButton);

        destinationNumberInput =
                findViewById(R.id.destinationNumberInput);

        amountInput =
                findViewById(R.id.amountInput);

        pinInput =
                findViewById(R.id.pinInput);

        continueTransferButton =
                findViewById(R.id.continueTransferButton);

        executorService = Executors.newSingleThreadExecutor();

        // Get customer ID
        customerId = getIntent().getIntExtra(
                "CUSTOMER_ID",
                -1
        );

        // Get selected transfer type
        transferType = getIntent().getStringExtra(
                "TRANSFER_TYPE"
        );

        // BACK BUTTON - SINGLE CLICK
        backButton.setClickable(true);
        backButton.setFocusable(true);

        backButton.setOnClickListener(v -> finish());

        // Validate transfer information
        if (customerId <= 0 || transferType == null) {

            Toast.makeText(
                    this,
                    "Transfer information missing. Please try again.",
                    Toast.LENGTH_LONG
            ).show();

            finish();
            return;
        }

        configureTransferForm();

        continueTransferButton.setOnClickListener(
                v -> validateTransfer()
        );

        // Load source balance
        loadSourceBalance();
    }

    // ==========================================
    // CONFIGURE TRANSFER FORM
    // ==========================================

    private void configureTransferForm() {

        switch (transferType) {

            case "BANK_TO_BANK":

                transferTitle.setText(
                        "Bank Account to SBL Bank Account"
                );

                transferSubtitle.setText(
                        "Transfer from your bank account"
                );

                destinationLabel.setText(
                        "Receiver Account Number"
                );

                destinationNumberInput.setHint(
                        "Enter receiver bank account number"
                );

                break;

            case "WALLET_TO_BANK":

                transferTitle.setText(
                        "Wallet to SBL Bank Account"
                );

                transferSubtitle.setText(
                        "Transfer from your Sonali e-Wallet"
                );

                destinationLabel.setText(
                        "Receiver Account Number"
                );

                destinationNumberInput.setHint(
                        "Enter receiver bank account number"
                );

                break;

            case "BANK_TO_WALLET":

                transferTitle.setText(
                        "Bank Account to Sonali e-Wallet"
                );

                transferSubtitle.setText(
                        "Transfer from your bank account"
                );

                destinationLabel.setText(
                        "Receiver Wallet Number"
                );

                destinationNumberInput.setHint(
                        "Enter receiver wallet number"
                );

                break;

            case "WALLET_TO_WALLET":

                transferTitle.setText(
                        "Wallet to Sonali e-Wallet"
                );

                transferSubtitle.setText(
                        "Transfer from your Sonali e-Wallet"
                );

                destinationLabel.setText(
                        "Receiver Wallet Number"
                );

                destinationNumberInput.setHint(
                        "Enter receiver wallet number"
                );

                break;

            default:

                Toast.makeText(
                        this,
                        "Invalid transfer type.",
                        Toast.LENGTH_LONG
                ).show();

                finish();
                break;
        }
    }

    // ==========================================
    // LOAD SOURCE BALANCE
    // ==========================================

    private void loadSourceBalance() {

        sourceBalanceText.setText("৳ Loading...");

        executorService.execute(() -> {

            HttpURLConnection connection = null;

            try {

                URL url = new URL(
                        DASHBOARD_URL + customerId
                );

                connection = (HttpURLConnection)
                        url.openConnection();

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

                String responseText = readResponse(inputStream);

                JSONObject response =
                        new JSONObject(responseText);

                if (responseCode < 200 ||
                        responseCode >= 300 ||
                        !response.optBoolean("success", false)) {

                    throw new Exception(
                            response.optString(
                                    "message",
                                    "Unable to load balance"
                            )
                    );
                }

                double balance;

                if (transferType.equals("BANK_TO_BANK") ||
                        transferType.equals("BANK_TO_WALLET")) {

                    balance = response.optDouble(
                            "bankBalance",
                            0.0
                    );

                } else {

                    balance = response.optDouble(
                            "walletBalance",
                            0.0
                    );
                }

                String formattedBalance =
                        formatBalance(balance);

                runOnUiThread(() ->
                        sourceBalanceText.setText(formattedBalance)
                );

            } catch (Exception e) {

                android.util.Log.e(
                        "SendMoney",
                        "Balance loading failed",
                        e
                );

                runOnUiThread(() -> {

                    if (isFinishing() || isDestroyed()) {
                        return;
                    }

                    sourceBalanceText.setText("৳ Unavailable");

                    Toast.makeText(
                            SendMoneyActivity.this,
                            "Unable to load source balance.",
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
    // VALIDATE TRANSFER
    // ==========================================

    private void validateTransfer() {

        String destination =
                destinationNumberInput.getText()
                        .toString().trim();

        String amount =
                amountInput.getText()
                        .toString().trim();

        String pin =
                pinInput.getText()
                        .toString().trim();

        if (destination.isEmpty()) {

            destinationNumberInput.setError(
                    "Enter receiver number"
            );

            destinationNumberInput.requestFocus();
            return;
        }

        if (amount.isEmpty()) {

            amountInput.setError(
                    "Enter amount"
            );

            amountInput.requestFocus();
            return;
        }

        try {

            double amountValue = Double.parseDouble(amount);

            if (!Double.isFinite(amountValue) ||
                    amountValue <= 0) {

                amountInput.setError(
                        "Enter a valid amount"
                );

                amountInput.requestFocus();
                return;
            }

        } catch (NumberFormatException e) {

            amountInput.setError(
                    "Enter a valid amount"
            );

            amountInput.requestFocus();
            return;
        }

        if (!pin.matches("\\d{6}")) {

            pinInput.setError(
                    "PIN must contain exactly 6 digits"
            );

            pinInput.requestFocus();
            return;
        }

        showConfirmation(
                destination,
                amount,
                pin
        );
    }

    // ==========================================
    // TRANSFER CONFIRMATION
    // ==========================================

    private void showConfirmation(
            String destination,
            String amount,
            String pin
    ) {

        new AlertDialog.Builder(this)
                .setTitle("Confirm Transfer")
                .setMessage(
                        "Transfer: " +
                                transferTitle.getText().toString()
                                + "\nReceiver: " + destination
                                + "\nAmount: ৳ " + amount
                                + "\n\nDo you want to submit?"
                )
                .setNegativeButton(
                        "Cancel",
                        (dialog, which) -> dialog.dismiss()
                )
                .setPositiveButton(
                        "Confirm",
                        (dialog, which) ->
                                performTransfer(
                                        destination,
                                        amount,
                                        pin
                                )
                )
                .show();
    }

    // ==========================================
    // PERFORM TRANSFER API
    // ==========================================

    private void performTransfer(
            String destination,
            String amount,
            String pin
    ) {

        continueTransferButton.setEnabled(false);
        continueTransferButton.setText("PROCESSING...");

        executorService.execute(() -> {

            HttpURLConnection connection = null;

            try {

                JSONObject requestData = new JSONObject();

                requestData.put(
                        "senderCustomerId",
                        customerId
                );

                requestData.put(
                        "transferType",
                        transferType
                );

                int sourceAccountId;

                switch (transferType) {

                    case "BANK_TO_BANK":

                        sourceAccountId = BANK_ACCOUNT_ID;

                        requestData.put(
                                "destinationAccountNumber",
                                destination
                        );

                        break;

                    case "WALLET_TO_BANK":

                        sourceAccountId = WALLET_ACCOUNT_ID;

                        requestData.put(
                                "destinationAccountNumber",
                                destination
                        );

                        break;

                    case "BANK_TO_WALLET":

                        sourceAccountId = BANK_ACCOUNT_ID;

                        requestData.put(
                                "destinationWalletNumber",
                                destination
                        );

                        break;

                    case "WALLET_TO_WALLET":

                        sourceAccountId = WALLET_ACCOUNT_ID;

                        requestData.put(
                                "destinationWalletNumber",
                                destination
                        );

                        break;

                    default:

                        throw new Exception(
                                "Invalid transfer type"
                        );
                }

                requestData.put(
                        "sourceAccountId",
                        sourceAccountId
                );

                requestData.put("amount", amount);

                requestData.put("pin", pin);

                requestData.put(
                        "description",
                        "Sonali e-Wallet Transfer"
                );

                URL url = new URL(API_URL);

                connection = (HttpURLConnection)
                        url.openConnection();

                connection.setRequestMethod("POST");
                connection.setConnectTimeout(15000);
                connection.setReadTimeout(15000);
                connection.setDoOutput(true);

                connection.setRequestProperty(
                        "Content-Type",
                        "application/json; charset=UTF-8"
                );

                byte[] requestBytes =
                        requestData.toString()
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

                JSONObject response =
                        new JSONObject(responseText);

                boolean success =
                        responseCode >= 200 &&
                                responseCode < 300 &&
                                response.optBoolean("success", false);

                String message = response.optString(
                        "message",
                        success
                                ? "Transfer completed successfully."
                                : "Transfer failed."
                );

                runOnUiThread(() -> {

                    if (isFinishing() || isDestroyed()) {
                        return;
                    }

                    continueTransferButton.setEnabled(true);
                    continueTransferButton.setText("SUBMIT");

                    if (success) {

                        new AlertDialog.Builder(
                                SendMoneyActivity.this
                        )
                                .setTitle("Transfer Successful")
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
                                SendMoneyActivity.this,
                                message,
                                Toast.LENGTH_LONG
                        ).show();
                    }
                });

            } catch (Exception e) {

                android.util.Log.e(
                        "SendMoney",
                        "Transfer failed",
                        e
                );

                runOnUiThread(() -> {

                    if (isFinishing() || isDestroyed()) {
                        return;
                    }

                    continueTransferButton.setEnabled(true);
                    continueTransferButton.setText("SUBMIT");

                    Toast.makeText(
                            SendMoneyActivity.this,
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
    // ANDROID SYSTEM BACK
    // ==========================================

    @Override
    public void onBackPressed() {
        finish();
    }

    // ==========================================
    // ACTIVITY DESTROY
    // ==========================================

    @Override
    protected void onDestroy() {

        if (executorService != null) {
            executorService.shutdown();
        }

        super.onDestroy();
    }
}
