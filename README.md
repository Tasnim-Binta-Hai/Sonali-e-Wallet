# Sonali e-Wallet (Android)

Sonali e-Wallet is a mobile banking app for Sonali Bank PLC. This repository is the Android app only. Account data lives in an Oracle database and is reached through a separate Node.js API.

A customer can register, log in with a mobile number and a 6-digit PIN, and see two balances on the home screen: bank balance and wallet balance. Other banking services are shown as buttons, but they do not open a screen yet.

## What you need before you run it

- Android Studio
- An Android emulator (or a phone)
- The Sonali e-Wallet backend running on your computer at port `5000`

The app calls `http://10.0.2.2:5000`. On the Android emulator, `10.0.2.2` means "the computer that is running the emulator." If the backend is not running, login, registration, and the balance cards will fail with a connection error.

The app is allowed to use normal HTTP (not HTTPS). That setting is `android:usesCleartextTraffic="true"` in `AndroidManifest.xml`. It is there so the emulator can talk to a local server.

## How to open and run

1. Open this folder in Android Studio.
2. Start the backend (see the backend README).
3. Start an emulator.
4. Press Run. The first screen is the login screen.

## Screens

The app has three screens. Each screen is one Java class and one layout XML file.

| Screen | Java class | Layout | What it does |
| --- | --- | --- | --- |
| Login | `MainActivity` | `activity_main.xml` | Mobile number, PIN, Login, link to Register |
| Registration | `RegistrationActivity` | `activity_registration.xml` | Create an account |
| Dashboard | `DashboardActivity` | `activity_dashboard.xml` | Balances and service tiles |

`MainActivity` is the launcher. After a successful login the app opens the dashboard and closes the login screen, so the Back button does not return to login.

## Login

The customer types:

- **Mobile number.** Must look like a Bangladesh number: `01`, then a digit from `3` to `9`, then 8 more digits. Example shape: `017XXXXXXXX`.
- **PIN.** Exactly 6 digits.

The eye icon shows or hides the PIN. The cursor stays where it was when you toggle it.

If either field is empty, the app shows "Please enter mobile number and PIN" and does not call the server. If the format is wrong, it shows "Enter a valid mobile number and 6-digit PIN."

When the format is valid, the app sends:

```http
POST http://10.0.2.2:5000/api/login
Content-Type: application/json

{
  "mobile": "017XXXXXXXX",
  "pin": "123456"
}
```

The button text changes to "Please wait..." and the button is disabled until the server answers.

If the server returns `"success": true`, the app reads `userId` and `customerId` and opens the dashboard with those values. If login fails, it shows the server's `message`. If the phone cannot reach the server, it shows "Connection Error: ..." .

The network call runs on a background thread so the screen does not freeze. `HttpURLConnection` is used directly. There is no extra HTTP library.

## Registration

From the login screen, tap **Register**.

Fields:

| Field | Rule |
| --- | --- |
| Full name | Cannot be empty |
| Mobile number | Same Bangladesh pattern as login |
| NID | 10, 13, or 17 digits |
| Date of birth | Chosen from a date picker. Future dates are blocked. Saved as `dd/MM/yyyy` |
| PIN | Exactly 6 digits |
| Confirm PIN | Must match the first PIN |

The app checks these rules before it calls the server. A problem is shown on the field itself (`setError`), not only as a toast.

The request is:

```http
POST http://10.0.2.2:5000/api/register
Content-Type: application/json

{
  "fullName": "Customer Name",
  "mobile": "017XXXXXXXX",
  "nid": "1234567890",
  "dob": "01/01/2000",
  "pin": "123456"
}
```

While it runs, the button says "Registering...". On success the app shows the server message and goes back to login. On failure it shows the message and stays on the form. "Already have an account? Login" also goes back to login.

Registration creates the customer and the PIN. It does not create a bank account or a wallet. A new customer will see `৳ 0.00` until those records exist in the database.

## Dashboard

The gold header shows the Sonali e-Wallet name and the text "Welcome to Sonali e-Wallet". The customer's name from the login response is not shown.

Two cards load from:

```http
GET http://10.0.2.2:5000/api/dashboard/{customerId}
```

Amounts are shown with a taka sign and two decimal places, for example `৳ 1,250.00`. While loading, both cards say `৳ Loading...`. If `customerId` was not passed in, both cards show `৳ 0.00` and a toast asks the user to log in again. If the request fails, both cards show `৳ Unavailable`.

The service grid is built in code, three tiles per row, from `item_dashboard_service.xml`. The names are:

1. Add Money
2. Bank Deposit
3. Send Money
4. Bill Pay
5. Recharge
6. DPS & FDR
7. BEFTN
8. Cards
9. NPSB
10. Remittance
11. Loan
12. e-Bank
13. RTGS
14. SOD
15. Swadhin Cash

These tiles are display only. Tapping one does nothing yet.

The same is true for these controls. They are drawn, and they have no click action:

- Login screen: Branch Locations, ATM Locations, Help & Support
- Dashboard header: notification icon, profile icon
- Bottom bar: Home, History, the center QR-style button, Analytics, Profile

## Project layout

```text
app/src/main/java/com/example/mobilebankingapplication/
    MainActivity.java           Login
    RegistrationActivity.java   Create account
    DashboardActivity.java      Balances and service tiles

app/src/main/res/layout/
    activity_main.xml
    activity_registration.xml
    activity_dashboard.xml
    item_dashboard_service.xml

app/src/main/res/drawable/
    sonali_bank_logo.xml
    bg_screen.xml               Screen background
    bg_input.xml                Input boxes
    bg_gold_button.xml          Login and Register buttons
    bg_tile.xml                 Service and info tiles

app/src/main/AndroidManifest.xml
```

The package name is `com.example.mobilebankingapplication`. The name under the app icon comes from `res/values/strings.xml` (`Mobile Banking Application`). The words on the screens say "Sonali e-Wallet".

The app needs the `INTERNET` permission. That is declared in the manifest.

## A short picture of the code

Each screen is an `AppCompatActivity`. `onCreate` loads the layout with `setContentView`, finds views with `findViewById`, and sets click listeners.

Server calls are not made on the UI thread. Login and registration use a single-thread `ExecutorService`. The dashboard does the same, then posts the result back to the main thread with a `Handler`. When the screen is destroyed, that executor is shut down.

JSON is built and read with `org.json.JSONObject`. The app looks at the boolean `success` and the string `message`.

There is no saved login. Closing the app means the customer must log in again. The dashboard only knows who you are because login put `USER_ID` and `CUSTOMER_ID` on the intent.

## What this app does not do yet

- Send money, add money, bill pay, recharge, cards, loans, or any other service tile
- Branch, ATM, or help screens
- History, QR scan, analytics, or profile
- Stay logged in after the app is closed
- Prove to the server that the dashboard request belongs to the person who logged in (it only sends the customer id)
