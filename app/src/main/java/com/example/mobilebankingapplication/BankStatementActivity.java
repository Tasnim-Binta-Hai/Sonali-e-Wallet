package com.example.mobilebankingapplication;

import android.content.Intent;
import android.os.Bundle;

public class BankStatementActivity extends StatementListActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        Intent intent = getIntent();

        intent.putExtra("STATEMENT_TYPE", "bank");

        super.onCreate(savedInstanceState);
    }
}