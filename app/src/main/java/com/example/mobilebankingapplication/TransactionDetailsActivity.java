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

public class TransactionDetailsActivity extends Activity {

    private LinearLayout detailsContainer;
    private TextView loadingText;

    private ExecutorService executor;

    private int customerId;
    private int transactionId;
    private String statementType;

    private final int DARK = Color.rgb(23, 59, 94);
    private final int GREY = Color.rgb(110, 120, 130);
    private final int BLUE = Color.rgb(23, 99, 165);
    private final int GOLD = Color.rgb(233, 184, 47);
    private final int BACKGROUND = Color.rgb(247, 248, 250);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        customerId = getIntent().getIntExtra(
                "CUSTOMER_ID", -1
        );

        transactionId = getIntent().getIntExtra(
                "TRANSACTION_ID", -1
        );

        statementType = getIntent().getStringExtra(
                "STATEMENT_TYPE"
        );

        executor = Executors.newSingleThreadExecutor();

        // SYSTEM BAR FIX
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

        if (customerId <= 0 ||
                transactionId <= 0 ||
                statementType == null) {

            loadingText.setText(
                    "Invalid transaction information"
            );

            return;
        }

        loadDetails();
    }

    private void createScreen() {

        LinearLayout root = new LinearLayout(this);

        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BACKGROUND);

        // SYSTEM BAR PADDING
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

        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);

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

        // TITLE
        TextView title = new TextView(this);

        title.setText("Transaction Details");
        title.setTextSize(20);
        title.setTextColor(Color.WHITE);
        title.setTypeface(null, Typeface.BOLD);

        header.addView(back);
        header.addView(title);

        root.addView(
                header,
                new LinearLayout.LayoutParams(
                        -1, -2
                )
        );

        // LOADING TEXT
        loadingText = new TextView(this);

        loadingText.setText(
                "Loading transaction details..."
        );

        loadingText.setGravity(Gravity.CENTER);
        loadingText.setTextColor(GREY);

        loadingText.setPadding(
                dp(12), dp(20), dp(12), dp(20)
        );

        root.addView(loadingText);

        // DETAILS SCROLL VIEW
        ScrollView scroll = new ScrollView(this);

        detailsContainer = new LinearLayout(this);

        detailsContainer.setOrientation(
                LinearLayout.VERTICAL
        );

        detailsContainer.setPadding(
                dp(16), dp(12), dp(16), dp(20)
        );

        scroll.addView(detailsContainer);

        root.addView(
                scroll,
                new LinearLayout.LayoutParams(
                        -1, 0, 1
                )
        );

        setContentView(root);

        ViewCompat.requestApplyInsets(root);
    }

    // LOAD TRANSACTION DETAILS
    private void loadDetails() {

        executor.execute(() -> {

            HttpURLConnection connection = null;

            try {

                String urlText =
                        StatementListActivity.API_BASE
                                + "/api/statements/"
                                + statementType
                                + "/"
                                + customerId
                                + "/"
                                + transactionId;

                connection = (HttpURLConnection)
                        new URL(urlText).openConnection();

                connection.setRequestMethod("GET");
                connection.setConnectTimeout(15000);
                connection.setReadTimeout(15000);

                int responseCode =
                        connection.getResponseCode();

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
                                    "Unable to load details"
                            )
                    );
                }

                JSONObject transaction =
                        json.getJSONObject("transaction");

                runOnUiThread(() ->
                        showDetails(transaction)
                );

            } catch (Exception error) {

                android.util.Log.e(
                        "TransactionDetails",
                        "Details loading failed",
                        error
                );

                runOnUiThread(() -> {

                    loadingText.setText(
                            "Unable to load transaction details"
                    );

                    Toast.makeText(
                            TransactionDetailsActivity.this,
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

    // DISPLAY DETAILS
    private void showDetails(JSONObject transaction) {

        loadingText.setVisibility(View.GONE);

        addTitle("Transaction Information");

        addRow(
                "Transaction ID",
                String.valueOf(
                        transaction.optInt("transactionId")
                )
        );

        addRow(
                "Reference Number",
                transaction.optString("reference", "")
        );

        addRow(
                "Transaction Type",
                transaction.optString("transactionType", "")
        );

        addRow(
                "Date & Time",
                transaction.optString("date", "")
        );

        addRow(
                "Description",
                transaction.optString("description", "")
        );

        addRow(
                "Status",
                transaction.optString("status", "UNKNOWN")
        );

        JSONArray entries =
                transaction.optJSONArray("entries");

        if (entries != null) {

            addTitle("Account Entries");

            for (int i = 0; i < entries.length(); i++) {

                JSONObject entry =
                        entries.optJSONObject(i);

                if (entry == null) {
                    continue;
                }

                String type = entry.optString(
                        "entryType", ""
                );

                String amount = "৳ " + formatAmount(
                        entry.optDouble("amount", 0)
                );

                addRow(type, amount);

                String account = entry.optString(
                        "accountNumber", ""
                );

                if (!account.isEmpty()) {
                    addRow(
                            "Account / Wallet",
                            account
                    );
                }
            }
        }
    }

    // SECTION TITLE
    private void addTitle(String title) {

        TextView text = new TextView(this);

        text.setText(title);
        text.setTextSize(17);
        text.setTextColor(DARK);
        text.setTypeface(null, Typeface.BOLD);

        text.setPadding(
                0, dp(16), 0, dp(12)
        );

        detailsContainer.addView(text);
    }

    // DETAIL ROW
    private void addRow(
            String label,
            String value
    ) {

        LinearLayout card = new LinearLayout(this);

        card.setOrientation(LinearLayout.VERTICAL);

        card.setPadding(
                dp(15), dp(12), dp(15), dp(12)
        );

        card.setBackgroundColor(Color.WHITE);
        card.setElevation(dp(1));

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        -1, -2
                );

        params.bottomMargin = dp(8);

        TextView labelText = new TextView(this);

        labelText.setText(label);
        labelText.setTextSize(12);
        labelText.setTextColor(GREY);

        TextView valueText = new TextView(this);

        valueText.setText(
                value.isEmpty()
                        ? "Not available"
                        : value
        );

        valueText.setTextSize(15);
        valueText.setTextColor(BLUE);
        valueText.setTypeface(null, Typeface.BOLD);

        valueText.setPadding(
                0, dp(5), 0, 0
        );

        card.addView(labelText);
        card.addView(valueText);

        detailsContainer.addView(card, params);
    }

    // AMOUNT FORMAT
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
