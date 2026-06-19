package com.muhdpanda4.app;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
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
    private Button sendButton;
    private SharedPreferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        preferences = getSharedPreferences(PREFS, MODE_PRIVATE);
        setContentView(createLayout());
        apiKeyInput.setText(preferences.getString(KEY_API, ""));
        modelInput.setText(preferences.getString(KEY_MODEL, "openrouter/free"));
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
        help.setText("Paste your OpenRouter key, keep model as openrouter/free, then chat. Your key is saved only on this phone.");
        help.setTextColor(Color.rgb(71, 85, 105));
        help.setPadding(0, 0, 0, dp(12));
        root.addView(help);

        apiKeyInput = input("OpenRouter API key", true);
        root.addView(apiKeyInput);

        modelInput = input("Model", false);
        root.addView(modelInput);

        ScrollView scrollView = new ScrollView(this);
        chatLog = new TextView(this);
        chatLog.setText("Ready. Ask me anything.\n");
        chatLog.setTextSize(16);
        chatLog.setTextColor(Color.rgb(15, 23, 42));
        chatLog.setPadding(dp(12), dp(12), dp(12), dp(12));
        scrollView.addView(chatLog);
        root.addView(scrollView, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1));

        promptInput = input("Type your message", false);
        promptInput.setMinLines(2);
        promptInput.setGravity(Gravity.TOP | Gravity.START);
        root.addView(promptInput);

        sendButton = new Button(this);
        sendButton.setText("Send with OpenRouter/free");
        sendButton.setOnClickListener(v -> sendPrompt());
        root.addView(sendButton);
        return root;
    }

    private EditText input(String hint, boolean password) {
        EditText editText = new EditText(this);
        editText.setHint(hint);
        editText.setSingleLine(!hint.equals("Type your message"));
        editText.setPadding(dp(12), dp(8), dp(12), dp(8));
        if (password) {
            editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        }
        return editText;
    }

    private void sendPrompt() {
        String apiKey = apiKeyInput.getText().toString().trim();
        String model = modelInput.getText().toString().trim();
        String prompt = promptInput.getText().toString().trim();

        if (apiKey.isEmpty()) {
            append("App: Paste your OpenRouter API key first.\n");
            return;
        }
        if (prompt.isEmpty()) {
            append("App: Type a message first.\n");
            return;
        }

        preferences.edit().putString(KEY_API, apiKey).putString(KEY_MODEL, model).apply();
        promptInput.setText("");
        sendButton.setEnabled(false);
        append("You: " + prompt + "\nMuhdPanda4: thinking...\n");

        executor.execute(() -> {
            try {
                String reply = new OpenRouterClient().sendMessage(apiKey, model, prompt);
                runOnUiThread(() -> append("MuhdPanda4: " + reply + "\n\n"));
            } catch (Exception error) {
                runOnUiThread(() -> append("Error: " + error.getMessage() + "\n\n"));
            } finally {
                runOnUiThread(() -> sendButton.setEnabled(true));
            }
        });
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
