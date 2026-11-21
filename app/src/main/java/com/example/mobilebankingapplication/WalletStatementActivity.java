package com.example.mobilebankingapplication;

import android.content.Intent;
import android.os.Bundle;

public class WalletStatementActivity extends StatementListActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        Intent intent = getIntent();

        intent.putExtra("STATEMENT_TYPE", "wallet");

        super.onCreate(savedInstanceState);
    }
}