package com.example.mobilebankingapplication;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class SendMoneyMenuActivity extends Activity {

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

        customerId = getIntent().getIntExtra(
                "CUSTOMER_ID",
                -1
        );

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

    // INITIALIZE MENU VIEWS
    private void initializeViews() {

        optionBankToBank = findViewById(
                R.id.optionBankToBank
        );

        optionWalletToBank = findViewById(
                R.id.optionWalletToBank
        );

        optionBankToWallet = findViewById(
                R.id.optionBankToWallet
        );

        optionWalletToWallet = findViewById(
                R.id.optionWalletToWallet
        );

        closeMenuButton = findViewById(
                R.id.closeMenuButton
        );
    }

    // SETUP CLICK LISTENERS
    private void setupClickListeners() {

        optionBankToBank.setOnClickListener(v ->
                openTransferForm("BANK_TO_BANK")
        );

        optionWalletToBank.setOnClickListener(v ->
                openTransferForm("WALLET_TO_BANK")
        );

        optionBankToWallet.setOnClickListener(v ->
                openTransferForm("BANK_TO_WALLET")
        );

        optionWalletToWallet.setOnClickListener(v ->
                openTransferForm("WALLET_TO_WALLET")
        );

        // CLOSE BUTTON
        closeMenuButton.setOnClickListener(v ->
                closeMenu(v)
        );
    }

    // CLOSE MENU AND RETURN TO DASHBOARD
    public void closeMenu(View view) {

        Intent intent = new Intent(
                SendMoneyMenuActivity.this,
                DashboardActivity.class
        );

        intent.putExtra(
                "CUSTOMER_ID",
                customerId
        );

        intent.addFlags(
                Intent.FLAG_ACTIVITY_CLEAR_TOP |
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
        );

        startActivity(intent);
        finish();
    }

    // OPEN TRANSFER FORM
    private void openTransferForm(String selectedTransferType) {

        Intent intent = new Intent(
                SendMoneyMenuActivity.this,
                SendMoneyActivity.class
        );

        intent.putExtra(
                "CUSTOMER_ID",
                customerId
        );

        intent.putExtra(
                "TRANSFER_TYPE",
                selectedTransferType
        );

        startActivity(intent);
    }

    // ANDROID BACK BUTTON
    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

}
