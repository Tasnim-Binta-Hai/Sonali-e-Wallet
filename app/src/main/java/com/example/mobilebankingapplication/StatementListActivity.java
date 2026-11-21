package com.example.mobilebankingapplication;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class StatementListActivity extends Activity {

    public static final String API_BASE =
            "http://10.0.2.2:5000";

    private LinearLayout transactionContainer;
    private TextView loadingText;
    private ExecutorService executor;

    private int customerId;
    private String statementType = "wallet";

    private final int BLUE = Color.rgb(23, 99, 165);
    private final int DARK = Color.rgb(23, 59, 94);
    private final int GREY = Color.rgb(110, 120, 130);
    private final int GOLD = Color.rgb(233, 184, 47);
    private final int BACKGROUND = Color.rgb(247, 248, 250);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        statementType = getIntent().getStringExtra(
                "STATEMENT_TYPE"
        );

        if (statementType == null) {
            statementType = "wallet";
        }

        customerId = getIntent().getIntExtra(
                "CUSTOMER_ID", -1
        );

        executor = Executors.newSingleThreadExecutor();

        // FIX SYSTEM BAR OVERLAPPING
        Window window = getWindow();

        WindowCompat.setDecorFitsSystemWindows(
                window, false
        );

        window.setStatusBarColor(GOLD);
        window.setNavigationBarColor(BACKGROUND);

        WindowInsetsControllerCompat controller =
                WindowCompat.getInsetsController(
                        window,
                        window.getDecorView()
                );

        controller.setAppearanceLightStatusBars(false);
        controller.setAppearanceLightNavigationBars(true);

        createScreen();

        if (customerId <= 0) {
            loadingText.setText("Customer ID not found");
            return;
        }

        loadStatement();
    }

    private void createScreen() {

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BACKGROUND);

        root.setClipToPadding(false);

        // FIX TOP AND BOTTOM SYSTEM BAR SPACE
        ViewCompat.setOnApplyWindowInsetsListener(
                root,
                (view, windowInsets) -> {

                    Insets systemBars = windowInsets.getInsets(
                            WindowInsetsCompat.Type.systemBars()
                    );

                    view.setPadding(
                            0,
                            systemBars.top,
                            0,
                            systemBars.bottom
                    );

                    return windowInsets;
                }
        );

        // HEADER
        LinearLayout header = new LinearLayout(this);

        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setPadding(
                dp(8), dp(6), dp(18), dp(6)
        );

        header.setBackgroundColor(GOLD);

        // BACK BUTTON
        TextView back = new TextView(this);

        back.setText("‹");
        back.setTextSize(38);
        back.setTextColor(Color.WHITE);
        back.setGravity(Gravity.CENTER);

        back.setClickable(true);
        back.setFocusable(true);

        LinearLayout.LayoutParams backParams =
                new LinearLayout.LayoutParams(
                        dp(52),
                        dp(52)
                );

        back.setLayoutParams(backParams);

        // BACK ACTION
        back.setOnClickListener(v -> {
            finish();
        });

        // SCREEN TITLE
        TextView title = new TextView(this);

        title.setText(
                statementType.equalsIgnoreCase("wallet")
                        ? "Wallet Statement"
                        : "Bank Statement"
        );

        title.setTextSize(20);
        title.setTextColor(Color.WHITE);
        title.setTypeface(null, Typeface.BOLD);

        header.addView(back);
        header.addView(title);

        root.addView(
                header,
                new LinearLayout.LayoutParams(
                        -1,
                        -2
                )
        );

        // INTRO TITLE
        TextView intro = new TextView(this);

        intro.setText(
                statementType.equalsIgnoreCase("wallet")
                        ? "Your Wallet Transactions"
                        : "Your Bank Account Transactions"
        );

        intro.setTextSize(16);
        intro.setTextColor(DARK);
        intro.setTypeface(null, Typeface.BOLD);

        intro.setPadding(
                dp(18),
                dp(20),
                dp(18),
                dp(12)
        );

        root.addView(intro);

        // LOADING TEXT
        loadingText = new TextView(this);

        loadingText.setText("Loading transactions...");
        loadingText.setTextColor(GREY);
        loadingText.setGravity(Gravity.CENTER);

        loadingText.setPadding(
                dp(12),
                dp(20),
                dp(12),
                dp(20)
        );

        root.addView(loadingText);

        // TRANSACTION SCROLL VIEW
        ScrollView scrollView = new ScrollView(this);

        scrollView.setFillViewport(false);

        transactionContainer = new LinearLayout(this);

        transactionContainer.setOrientation(
                LinearLayout.VERTICAL
        );

        transactionContainer.setPadding(
                dp(12),
                dp(4),
                dp(12),
                dp(16)
        );

        scrollView.addView(transactionContainer);

        root.addView(
                scrollView,
                new LinearLayout.LayoutParams(
                        -1,
                        0,
                        1
                )
        );

        setContentView(root);

        ViewCompat.requestApplyInsets(root);
    }

    // LOAD STATEMENT FROM SERVER
    private void loadStatement() {

        executor.execute(() -> {

            HttpURLConnection connection = null;

            try {

                String urlText = API_BASE
                        + "/api/statements/"
                        + statementType
                        + "/"
                        + customerId;

                connection = (HttpURLConnection)
                        new URL(urlText).openConnection();

                connection.setRequestMethod("GET");
                connection.setConnectTimeout(15000);
                connection.setReadTimeout(15000);

                int responseCode = connection.getResponseCode();

                InputStream input = responseCode >= 200
                        && responseCode < 300
                        ? connection.getInputStream()
                        : connection.getErrorStream();

                if (input == null) {
                    throw new Exception(
                            "Empty server response"
                    );
                }

                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(input)
                );

                StringBuilder response = new StringBuilder();
                String line;

                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }

                reader.close();

                if (responseCode != 200) {
                    throw new Exception(
                            "Server error: " + responseCode
                    );
                }

                JSONObject json = new JSONObject(
                        response.toString()
                );

                if (!json.optBoolean("success")) {
                    throw new Exception(
                            json.optString(
                                    "message",
                                    "Unable to load statement"
                            )
                    );
                }

                JSONArray transactions =
                        json.getJSONArray("transactions");

                runOnUiThread(() ->
                        displayTransactions(transactions)
                );

            } catch (Exception error) {

                android.util.Log.e(
                        "StatementAPI",
                        "Statement loading failed",
                        error
                );

                runOnUiThread(() -> {

                    loadingText.setText(
                            "Unable to load transactions"
                    );

                    Toast.makeText(
                            StatementListActivity.this,
                            error.getMessage(),
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

    // DISPLAY TRANSACTION LIST
    private void displayTransactions(
            JSONArray transactions
    ) {

        transactionContainer.removeAllViews();

        loadingText.setVisibility(View.GONE);

        if (transactions.length() == 0) {

            TextView empty = new TextView(this);

            empty.setText("No transactions found");
            empty.setGravity(Gravity.CENTER);
            empty.setTextColor(GREY);

            empty.setPadding(
                    0, dp(40), 0, dp(40)
            );

            transactionContainer.addView(empty);

            return;
        }

        for (int i = 0; i < transactions.length(); i++) {

            JSONObject item =
                    transactions.optJSONObject(i);

            if (item == null) {
                continue;
            }

            addTransactionCard(item);
        }
    }

    // TRANSACTION CARD
    private void addTransactionCard(JSONObject item) {

        LinearLayout card = new LinearLayout(this);

        card.setOrientation(LinearLayout.VERTICAL);

        card.setPadding(
                dp(16),
                dp(14),
                dp(16),
                dp(14)
        );

        card.setBackgroundColor(Color.WHITE);
        card.setElevation(dp(2));

        LinearLayout.LayoutParams cardParams =
                new LinearLayout.LayoutParams(
                        -1,
                        -2
                );

        cardParams.bottomMargin = dp(10);

        // TRANSACTION TYPE
        TextView type = new TextView(this);

        type.setText(
                item.optString(
                        "transactionType",
                        "Transaction"
                )
        );

        type.setTextSize(15);
        type.setTextColor(DARK);
        type.setTypeface(null, Typeface.BOLD);

        // DATE
        TextView date = new TextView(this);

        date.setText(
                item.optString("date", "")
        );

        date.setTextSize(12);
        date.setTextColor(GREY);

        date.setPadding(
                0, dp(5), 0, dp(8)
        );

        // ENTRY TYPE
        String entryType = item.optString(
                "entryType",
                ""
        );

        // AMOUNT
        TextView amount = new TextView(this);

        String sign = entryType.equalsIgnoreCase("CREDIT")
                ? "+"
                : "-";

        amount.setText(
                sign + " ৳ " + formatAmount(
                        item.optDouble("amount", 0)
                )
        );

        amount.setTextSize(19);
        amount.setTypeface(null, Typeface.BOLD);

        amount.setTextColor(
                entryType.equalsIgnoreCase("CREDIT")
                        ? Color.rgb(20, 145, 85)
                        : Color.rgb(205, 65, 65)
        );

        // STATUS
        TextView status = new TextView(this);

        status.setText(
                item.optString("status", "UNKNOWN")
        );

        status.setTextSize(12);
        status.setTextColor(BLUE);

        status.setPadding(
                0, dp(6), 0, 0
        );

        // REFERENCE
        TextView reference = new TextView(this);

        reference.setText(
                "Ref: " + item.optString("reference", "")
        );

        reference.setTextSize(11);
        reference.setTextColor(GREY);

        reference.setPadding(
                0, dp(5), 0, 0
        );

        card.addView(type);
        card.addView(date);
        card.addView(amount);
        card.addView(status);
        card.addView(reference);

        // OPEN TRANSACTION DETAILS
        card.setClickable(true);
        card.setFocusable(true);

        card.setOnClickListener(v -> {

            Intent intent = new Intent(
                    StatementListActivity.this,
                    TransactionDetailsActivity.class
            );

            intent.putExtra(
                    "CUSTOMER_ID",
                    customerId
            );

            intent.putExtra(
                    "STATEMENT_TYPE",
                    statementType
            );

            intent.putExtra(
                    "TRANSACTION_ID",
                    item.optInt("transactionId")
            );

            startActivity(intent);
        });

        transactionContainer.addView(
                card,
                cardParams
        );
    }

    // FORMAT AMOUNT
    private String formatAmount(double amount) {

        NumberFormat formatter =
                NumberFormat.getNumberInstance(
                        Locale.US
                );

        formatter.setMinimumFractionDigits(2);
        formatter.setMaximumFractionDigits(2);

        return formatter.format(amount);
    }

    // DP CONVERSION
    private int dp(int value) {

        return (int) (
                value * getResources()
                        .getDisplayMetrics()
                        .density + 0.5f
        );
    }

    @Override
    protected void onDestroy() {

        if (executor != null) {
            executor.shutdownNow();
        }

        super.onDestroy();
    }
}
