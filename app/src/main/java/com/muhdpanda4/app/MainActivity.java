package com.muhdpanda4.app;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    private static final String PREFS = "muhdpanda4";
    private static final String KEY_API = "openrouter_api_key";
    private static final String KEY_MODEL = "openrouter_model";

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private EditText apiKeyInput;
    private EditText modelInput;
    private EditText promptInput;
    private TextView chatLog;
    private Button chatButton;
    private Button controlButton;
    private TextView serviceStatus;
    private SharedPreferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        preferences = getSharedPreferences(PREFS, MODE_PRIVATE);
        setContentView(createLayout());
        apiKeyInput.setText(preferences.getString(KEY_API, ""));
        modelInput.setText(preferences.getString(KEY_MODEL, "openrouter/free"));
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateServiceStatus();
    }

    private View createLayout() {
        int padding = dp(16);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(padding, padding, padding, padding);
        root.setBackgroundColor(Color.rgb(248, 250, 252));

        TextView title = new TextView(this);
        title.setText("MuhdPanda4");
        title.setTextSize(28);
        title.setTextColor(Color.rgb(30, 41, 59));
        title.setGravity(Gravity.CENTER_HORIZONTAL);
        title.setPadding(0, 0, 0, dp(8));
        root.addView(title);

        TextView help = new TextView(this);
        help.setText("Paste your OpenRouter key, keep model as openrouter/free, enable Accessibility, then ask me to control your phone.");
        help.setTextColor(Color.rgb(71, 85, 105));
        help.setPadding(0, 0, 0, dp(12));
        root.addView(help);

        apiKeyInput = input("OpenRouter API key", true);
        root.addView(apiKeyInput);

        modelInput = input("Model", false);
        root.addView(modelInput);

        serviceStatus = new TextView(this);
        serviceStatus.setTextColor(Color.rgb(71, 85, 105));
        serviceStatus.setPadding(0, dp(8), 0, dp(8));
        root.addView(serviceStatus);

        Button accessibilityButton = new Button(this);
        accessibilityButton.setText("Enable phone control permission");
        accessibilityButton.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        root.addView(accessibilityButton);

        ScrollView scrollView = new ScrollView(this);
        chatLog = new TextView(this);
        chatLog.setText("Ready. Enable Accessibility, then try: open settings, open Chrome, tap Search, type hello, go back, scroll down.\n");
        chatLog.setTextSize(16);
        chatLog.setTextColor(Color.rgb(15, 23, 42));
        chatLog.setPadding(dp(12), dp(12), dp(12), dp(12));
        scrollView.addView(chatLog);
        root.addView(scrollView, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1));

        promptInput = input("Type your chat or phone-control command", false);
        promptInput.setMinLines(2);
        promptInput.setGravity(Gravity.TOP | Gravity.START);
        root.addView(promptInput);

        chatButton = new Button(this);
        chatButton.setText("Chat only");
        chatButton.setOnClickListener(v -> sendChatPrompt());
        root.addView(chatButton);

        controlButton = new Button(this);
        controlButton.setText("Control phone with OpenRouter/free");
        controlButton.setOnClickListener(v -> runPhoneControl());
        root.addView(controlButton);

        updateServiceStatus();
        return root;
    }

    private EditText input(String hint, boolean password) {
        EditText editText = new EditText(this);
        editText.setHint(hint);
        editText.setSingleLine(!hint.equals("Type your chat or phone-control command"));
        editText.setPadding(dp(12), dp(8), dp(12), dp(8));
        if (password) {
            editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        }
        return editText;
    }

    private void sendChatPrompt() {
        String apiKey = apiKeyInput.getText().toString().trim();
        String model = modelInput.getText().toString().trim();
        String prompt = promptInput.getText().toString().trim();
        if (!validate(apiKey, prompt)) {
            return;
        }
        saveSettings(apiKey, model);
        promptInput.setText("");
        setButtonsEnabled(false);
        append("You: " + prompt + "\nMuhdPanda4 chat: thinking...\n");
        executor.execute(() -> {
            try {
                String reply = new OpenRouterClient().sendMessage(apiKey, model, prompt);
                runOnUiThread(() -> append("MuhdPanda4 chat: " + reply + "\n\n"));
            } catch (Exception error) {
                runOnUiThread(() -> append("Error: " + error.getMessage() + "\n\n"));
            } finally {
                runOnUiThread(() -> setButtonsEnabled(true));
            }
        });
    }

    private void runPhoneControl() {
        String apiKey = apiKeyInput.getText().toString().trim();
        String model = modelInput.getText().toString().trim();
        String prompt = promptInput.getText().toString().trim();
        if (!validate(apiKey, prompt)) {
            return;
        }
        PandaAccessibilityService service = PandaAccessibilityService.getInstance();
        if (service == null) {
            append("App: Enable 'MuhdPanda4 Controller' in Android Accessibility settings first.\n\n");
            startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
            return;
        }
        saveSettings(apiKey, model);
        promptInput.setText("");
        setButtonsEnabled(false);
        append("You asked phone control: " + prompt + "\nMuhdPanda4 control: reading screen and planning...\n");
        executor.execute(() -> {
            try {
                String screen = service.describeCurrentScreen();
                String plan = new OpenRouterClient().createPhonePlan(apiKey, model, prompt, screen);
                PhoneActionResult result = service.executePlan(plan);
                runOnUiThread(() -> append("AI plan: " + plan + "\nResult:\n" + result.message + "\n\n"));
            } catch (Exception error) {
                runOnUiThread(() -> append("Control error: " + error.getMessage() + "\n\n"));
            } finally {
                runOnUiThread(() -> setButtonsEnabled(true));
            }
        });
    }

    private boolean validate(String apiKey, String prompt) {
        if (apiKey.isEmpty()) {
            append("App: Paste your OpenRouter API key first.\n");
            return false;
        }
        if (prompt.isEmpty()) {
            append("App: Type a command first.\n");
            return false;
        }
        return true;
    }

    private void saveSettings(String apiKey, String model) {
        preferences.edit().putString(KEY_API, apiKey).putString(KEY_MODEL, model).apply();
    }

    private void updateServiceStatus() {
        if (serviceStatus == null) {
            return;
        }
        if (PandaAccessibilityService.isRunning()) {
            serviceStatus.setText("Phone control: enabled");
            serviceStatus.setTextColor(Color.rgb(22, 101, 52));
        } else {
            serviceStatus.setText("Phone control: disabled. Tap the permission button and enable MuhdPanda4 Controller.");
            serviceStatus.setTextColor(Color.rgb(185, 28, 28));
        }
    }

    private void setButtonsEnabled(boolean enabled) {
        chatButton.setEnabled(enabled);
        controlButton.setEnabled(enabled);
        updateServiceStatus();
    }

    private void append(String text) {
        chatLog.append(text);
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    @Override
    protected void onDestroy() {
        executor.shutdownNow();
        super.onDestroy();
    }
}
