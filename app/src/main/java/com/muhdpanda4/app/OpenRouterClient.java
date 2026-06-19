package com.muhdpanda4.app;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

final class OpenRouterClient {
    private static final String ENDPOINT = "https://openrouter.ai/api/v1/chat/completions";

    String sendMessage(String apiKey, String model, String userMessage) throws Exception {
        JSONArray messages = new JSONArray();
        messages.put(new JSONObject()
                .put("role", "system")
                .put("content", "You are MuhdPanda4, a helpful Android assistant. Keep answers clear and practical."));
        messages.put(new JSONObject()
                .put("role", "user")
                .put("content", userMessage));
        return sendMessages(apiKey, model, messages);
    }

    String createPhonePlan(String apiKey, String model, String userCommand, String screenContext) throws Exception {
        JSONArray messages = new JSONArray();
        messages.put(new JSONObject()
                .put("role", "system")
                .put("content", "You control an Android phone through an AccessibilityService. Return only JSON. No markdown. Allowed action types: open_app with app, tap_text with text, type_text with text, back, home, recents, scroll_down, scroll_up, tap_xy with x and y, wait with ms. Prefer tap_text over tap_xy. Use at most 5 actions. Format: {\"summary\":\"short explanation\",\"actions\":[{\"type\":\"open_app\",\"app\":\"Settings\"}]}"));
        messages.put(new JSONObject()
                .put("role", "user")
                .put("content", "User command: " + userCommand + "\n\nCurrent Android screen context:\n" + screenContext));
        return sendMessages(apiKey, model, messages);
    }

    private String sendMessages(String apiKey, String model, JSONArray messages) throws Exception {
        JSONObject body = new JSONObject();
        body.put("model", model == null || model.trim().isEmpty() ? "openrouter/free" : model.trim());
        body.put("messages", messages);

        HttpURLConnection connection = (HttpURLConnection) new URL(ENDPOINT).openConnection();
        connection.setRequestMethod("POST");
        connection.setConnectTimeout(30000);
        connection.setReadTimeout(60000);
        connection.setDoOutput(true);
        connection.setRequestProperty("Authorization", "Bearer " + apiKey.trim());
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("HTTP-Referer", "https://github.com/MuhdPanda4/MuhdPanda4");
        connection.setRequestProperty("X-Title", "MuhdPanda4");

        byte[] payload = body.toString().getBytes(StandardCharsets.UTF_8);
        try (OutputStream output = connection.getOutputStream()) {
            output.write(payload);
        }

        int code = connection.getResponseCode();
        InputStream stream = code >= 200 && code < 300 ? connection.getInputStream() : connection.getErrorStream();
        String response = readAll(stream);
        if (code < 200 || code >= 300) {
            throw new IllegalStateException("OpenRouter error " + code + ": " + response);
        }

        JSONObject json = new JSONObject(response);
        JSONArray choices = json.getJSONArray("choices");
        if (choices.length() == 0) {
            return "OpenRouter returned no answer.";
        }
        return choices.getJSONObject(0).getJSONObject("message").optString("content", "No text response.").trim();
    }

    private static String readAll(InputStream stream) throws Exception {
        if (stream == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
        }
        return builder.toString();
    }
}
