package com.example.mobilebankingapplication;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

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

public class DashboardActivity extends AppCompatActivity {

    LinearLayout servicesContainer;
    TextView bankBalanceText, walletBalanceText;

    private ExecutorService executorService;
    private Handler mainHandler;

    private int customerId;

    String[] serviceNames = {
            "Add Money",
            "Bank Deposit",
            "Send Money",
            "Bill Pay",
            "Recharge",
            "DPS & FDR",
            "BEFTN",
            "Cards",
            "NPSB",
            "Remittance",
            "Loan",
            "e-Bank",
            "RTGS",
            "SOD",
            "Swadhin Cash"
    };

    String[] serviceIcons = {
            "💰", "🏦", "💸",
            "🧾", "📱", "🏛️",
            "🏦", "💳", "🏛️",
            "💵", "💰", "📲",
            "🏦", "💳", "💸"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        servicesContainer = findViewById(R.id.servicesContainer);

        bankBalanceText = findViewById(R.id.bankBalanceText);
        walletBalanceText = findViewById(R.id.walletBalanceText);

        executorService = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());

        // Get Customer ID from Login Activity
        customerId = getIntent().getIntExtra("CUSTOMER_ID", -1);

        // Initial balance display
        bankBalanceText.setText("৳ Loading...");
        walletBalanceText.setText("৳ Loading...");

        createServiceCards();

        // Load balances from Oracle Database
        loadDashboardBalance();
    }

    // DASHBOARD BALANCE API
    private void loadDashboardBalance() {

        if (customerId <= 0) {
            bankBalanceText.setText("৳ 0.00");
            walletBalanceText.setText("৳ 0.00");

            Toast.makeText(
                    this,
                    "Customer ID not found. Please login again.",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        executorService.execute(() -> {

            HttpURLConnection connection = null;

            try {

                URL url = new URL(
                        "http://10.0.2.2:5000/api/dashboard/" + customerId
                );

                connection = (HttpURLConnection) url.openConnection();

                connection.setRequestMethod("GET");
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(10000);

                int responseCode = connection.getResponseCode();

                InputStream inputStream;

                if (responseCode >= 200 && responseCode < 300) {
                    inputStream = connection.getInputStream();
                } else {
                    inputStream = connection.getErrorStream();
                }

                if (inputStream == null) {
                    throw new Exception("Empty server response");
                }

                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(inputStream)
                );

                StringBuilder response = new StringBuilder();

                String line;

                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }

                reader.close();

                if (responseCode != HttpURLConnection.HTTP_OK) {
                    throw new Exception("Server error: " + responseCode);
                }

                JSONObject jsonResponse = new JSONObject(
                        response.toString()
                );

                if (!jsonResponse.optBoolean("success", false)) {
                    throw new Exception(
                            jsonResponse.optString(
                                    "message",
                                    "Unable to load balance"
                            )
                    );
                }

                double bankBalance = jsonResponse.optDouble(
                        "bankBalance",
                        0.0
                );

                double walletBalance = jsonResponse.optDouble(
                        "walletBalance",
                        0.0
                );

                mainHandler.post(() -> {

                    bankBalanceText.setText(
                            formatBalance(bankBalance)
                    );

                    walletBalanceText.setText(
                            formatBalance(walletBalance)
                    );

                });

            } catch (Exception error) {

                android.util.Log.e(
                        "DashboardAPI",
                        "Balance loading failed",
                        error
                );

                mainHandler.post(() -> {

                    bankBalanceText.setText("৳ Unavailable");
                    walletBalanceText.setText("৳ Unavailable");

                    Toast.makeText(
                            DashboardActivity.this,
                            "Unable to load account balance",
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

    // Format balance
    private String formatBalance(double balance) {

        NumberFormat formatter = NumberFormat.getNumberInstance(
                Locale.US
        );

        formatter.setMinimumFractionDigits(2);
        formatter.setMaximumFractionDigits(2);

        return "৳ " + formatter.format(balance);
    }

    // CREATE SERVICE CARDS
    private void createServiceCards() {

        LayoutInflater inflater = LayoutInflater.from(this);

        for (int i = 0; i < serviceNames.length; i += 3) {

            LinearLayout row = new LinearLayout(this);

            row.setOrientation(LinearLayout.HORIZONTAL);

            LinearLayout.LayoutParams rowParams =
                    new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            dpToPx(115)
                    );

            rowParams.bottomMargin = dpToPx(5);

            servicesContainer.addView(row, rowParams);

            for (int j = i; j < i + 3 && j < serviceNames.length; j++) {

                View card = inflater.inflate(
                        R.layout.item_dashboard_service,
                        row,
                        false
                );

                TextView icon = card.findViewById(
                        R.id.serviceIcon
                );

                TextView name = card.findViewById(
                        R.id.serviceName
                );

                icon.setText(serviceIcons[j]);
                name.setText(serviceNames[j]);

                row.addView(card);
            }
        }
    }

    // Convert DP to Pixels
    private int dpToPx(int dp) {

        return (int) (
                dp * getResources()
                        .getDisplayMetrics().density + 0.5f
        );
    }

    @Override
    protected void onDestroy() {

        if (executorService != null) {
            executorService.shutdownNow();
        }

        super.onDestroy();
    }
}
