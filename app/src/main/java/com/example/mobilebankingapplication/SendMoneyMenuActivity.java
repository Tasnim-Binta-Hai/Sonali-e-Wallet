package com.example.mobilebankingapplication;

import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class SendMoneyMenuActivity extends AppCompatActivity {

    private int customerId;

    private LinearLayout optionBankToBank;
    private LinearLayout optionWalletToBank;
    private LinearLayout optionBankToWallet;
    private LinearLayout optionWalletToWallet;

    private TextView closeMenuButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_send_money_menu);

        customerId = getIntent().getIntExtra("CUSTOMER_ID", -1);

        if (customerId <= 0) {
            Toast.makeText(
                    this,
                    "Customer ID not found. Please login again.",
                    Toast.LENGTH_LONG
            ).show();

            finish();
            return;
        }

        initializeViews();
        setupClickListeners();
    }

    private void initializeViews() {
        optionBankToBank = findViewById(R.id.optionBankToBank);
        optionWalletToBank = findViewById(R.id.optionWalletToBank);
        optionBankToWallet = findViewById(R.id.optionBankToWallet);
        optionWalletToWallet = findViewById(R.id.optionWalletToWallet);
        closeMenuButton = findViewById(R.id.closeMenuButton);
    }

    private void setupClickListeners() {

        optionBankToBank.setOnClickListener(v ->
                openTransferForm("BANK_TO_BANK"));

        optionWalletToBank.setOnClickListener(v ->
                openTransferForm("WALLET_TO_BANK"));

        optionBankToWallet.setOnClickListener(v ->
                openTransferForm("BANK_TO_WALLET"));

        optionWalletToWallet.setOnClickListener(v ->
                openTransferForm("WALLET_TO_WALLET"));

        // CLOSE BUTTON: go back to the dashboard
        closeMenuButton.setOnClickListener(v -> finish());
    }

    private void openTransferForm(String selectedTransferType) {

        Intent intent = new Intent(
                SendMoneyMenuActivity.this,
                SendMoneyActivity.class
        );

        intent.putExtra("CUSTOMER_ID", customerId);
        intent.putExtra("TRANSFER_TYPE", selectedTransferType);

        startActivity(intent);
    }
}