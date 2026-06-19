package com.muhdpanda4.app;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Path;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Locale;

public class PandaAccessibilityService extends AccessibilityService {
    private static PandaAccessibilityService instance;

    static PandaAccessibilityService getInstance() {
        return instance;
    }

    static boolean isRunning() {
        return instance != null;
    }

    @Override
    protected void onServiceConnected() {
        instance = this;
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
    }

    @Override
    public void onInterrupt() {
    }

    @Override
    public boolean onUnbind(Intent intent) {
        if (instance == this) {
            instance = null;
        }
        return super.onUnbind(intent);
    }

    String describeCurrentScreen() {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) {
            return "No active window is available. Ask the user to open a target app or enable Accessibility.";
        }
        StringBuilder builder = new StringBuilder();
        builder.append("Current package: ").append(root.getPackageName()).append('\n');
        builder.append("Visible screen elements:\n");
        ArrayDeque<AccessibilityNodeInfo> queue = new ArrayDeque<>();
        queue.add(root);
        int count = 0;
        while (!queue.isEmpty() && count < 80) {
            AccessibilityNodeInfo node = queue.removeFirst();
            CharSequence text = firstNonEmpty(node.getText(), node.getContentDescription());
            if (text != null) {
                Rect bounds = new Rect();
                node.getBoundsInScreen(bounds);
                builder.append("- ").append(text)
                        .append(" | clickable=").append(node.isClickable())
                        .append(" | editable=").append(node.isEditable())
                        .append(" | bounds=").append(bounds.flattenToString())
                        .append('\n');
                count++;
            }
            for (int index = 0; index < node.getChildCount(); index++) {
                AccessibilityNodeInfo child = node.getChild(index);
                if (child != null) {
                    queue.add(child);
                }
            }
        }
        return builder.toString();
    }

    PhoneActionResult executePlan(String jsonPlan) {
        try {
            JSONObject plan = extractJson(jsonPlan);
            JSONArray actions = plan.optJSONArray("actions");
            if (actions == null || actions.length() == 0) {
                return new PhoneActionResult(false, "The AI did not return any actions. Raw response: " + jsonPlan);
            }
            StringBuilder log = new StringBuilder();
            boolean allSucceeded = true;
            for (int index = 0; index < actions.length(); index++) {
                JSONObject action = actions.getJSONObject(index);
                PhoneActionResult result = executeAction(action);
                allSucceeded = allSucceeded && result.success;
                log.append(index + 1).append(". ").append(result.message).append('\n');
                sleepQuietly(450);
            }
            return new PhoneActionResult(allSucceeded, log.toString().trim());
        } catch (Exception error) {
            return new PhoneActionResult(false, "Could not run AI plan: " + error.getMessage() + "\nRaw response: " + jsonPlan);
        }
    }

    private PhoneActionResult executeAction(JSONObject action) throws Exception {
        String type = action.optString("type", "").toLowerCase(Locale.US);
        if ("open_app".equals(type)) {
            return openApp(action.optString("app", ""));
        }
        if ("tap_text".equals(type)) {
            return tapText(action.optString("text", ""));
        }
        if ("type_text".equals(type)) {
            return typeText(action.optString("text", ""));
        }
        if ("back".equals(type)) {
            return global(GLOBAL_ACTION_BACK, "Back");
        }
        if ("home".equals(type)) {
            return global(GLOBAL_ACTION_HOME, "Home");
        }
        if ("recents".equals(type)) {
            return global(GLOBAL_ACTION_RECENTS, "Recents");
        }
        if ("scroll_down".equals(type)) {
            return scroll(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD, "Scroll down");
        }
        if ("scroll_up".equals(type)) {
            return scroll(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD, "Scroll up");
        }
        if ("tap_xy".equals(type)) {
            return tapXY(action.optInt("x", -1), action.optInt("y", -1));
        }
        if ("wait".equals(type)) {
            sleepQuietly(Math.max(250, action.optInt("ms", 1000)));
            return new PhoneActionResult(true, "Waited");
        }
        return new PhoneActionResult(false, "Unknown action type: " + type);
    }

    private PhoneActionResult openApp(String appName) {
        if (appName.trim().isEmpty()) {
            return new PhoneActionResult(false, "open_app needs an app name");
        }
        PackageManager manager = getPackageManager();
        List<ApplicationInfo> apps = manager.getInstalledApplications(0);
        String wanted = appName.toLowerCase(Locale.US).trim();
        for (ApplicationInfo app : apps) {
            String label = manager.getApplicationLabel(app).toString();
            if (label.toLowerCase(Locale.US).contains(wanted)) {
                Intent launch = manager.getLaunchIntentForPackage(app.packageName);
                if (launch == null) {
                    return new PhoneActionResult(false, "Found " + label + " but it cannot be launched");
                }
                launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(launch);
                return new PhoneActionResult(true, "Opened app: " + label);
            }
        }
        return new PhoneActionResult(false, "Could not find app named: " + appName);
    }

    private PhoneActionResult tapText(String text) {
        AccessibilityNodeInfo node = findNodeByText(text);
        if (node == null) {
            return new PhoneActionResult(false, "Could not find text: " + text);
        }
        AccessibilityNodeInfo clickable = findClickableParent(node);
        if (clickable != null && clickable.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
            return new PhoneActionResult(true, "Tapped: " + text);
        }
        Rect bounds = new Rect();
        node.getBoundsInScreen(bounds);
        return tapXY(bounds.centerX(), bounds.centerY());
    }

    private PhoneActionResult typeText(String text) {
        AccessibilityNodeInfo editable = findEditableNode();
        if (editable == null) {
            return new PhoneActionResult(false, "No editable text field is focused or visible");
        }
        Bundle arguments = new Bundle();
        arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text);
        boolean ok = editable.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments);
        return new PhoneActionResult(ok, ok ? "Typed text" : "Failed to type text");
    }

    private PhoneActionResult global(int action, String label) {
        boolean ok = performGlobalAction(action);
        return new PhoneActionResult(ok, ok ? "Performed " + label : "Failed " + label);
    }

    private PhoneActionResult scroll(int action, String label) {
        AccessibilityNodeInfo scrollable = findScrollableNode();
        if (scrollable == null) {
            return new PhoneActionResult(false, "No scrollable area found");
        }
        boolean ok = scrollable.performAction(action);
        return new PhoneActionResult(ok, ok ? label : "Failed: " + label);
    }

    private PhoneActionResult tapXY(int x, int y) {
        if (x < 0 || y < 0) {
            return new PhoneActionResult(false, "tap_xy needs x and y");
        }
        Path path = new Path();
        path.moveTo(x, y);
        GestureDescription.StrokeDescription stroke = new GestureDescription.StrokeDescription(path, 0, 80);
        GestureDescription gesture = new GestureDescription.Builder().addStroke(stroke).build();
        boolean ok = dispatchGesture(gesture, null, null);
        return new PhoneActionResult(ok, ok ? "Tapped coordinates " + x + "," + y : "Failed coordinate tap");
    }

    private AccessibilityNodeInfo findNodeByText(String text) {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null || text == null || text.trim().isEmpty()) {
            return null;
        }
        String wanted = text.toLowerCase(Locale.US).trim();
        ArrayDeque<AccessibilityNodeInfo> queue = new ArrayDeque<>();
        queue.add(root);
        while (!queue.isEmpty()) {
            AccessibilityNodeInfo node = queue.removeFirst();
            CharSequence label = firstNonEmpty(node.getText(), node.getContentDescription());
            if (label != null && label.toString().toLowerCase(Locale.US).contains(wanted)) {
                return node;
            }
            for (int index = 0; index < node.getChildCount(); index++) {
                AccessibilityNodeInfo child = node.getChild(index);
                if (child != null) {
                    queue.add(child);
                }
            }
        }
        return null;
    }

    private AccessibilityNodeInfo findEditableNode() {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) {
            return null;
        }
        ArrayDeque<AccessibilityNodeInfo> queue = new ArrayDeque<>();
        queue.add(root);
        AccessibilityNodeInfo firstEditable = null;
        while (!queue.isEmpty()) {
            AccessibilityNodeInfo node = queue.removeFirst();
            if (node.isEditable()) {
                if (node.isFocused()) {
                    return node;
                }
                if (firstEditable == null) {
                    firstEditable = node;
                }
            }
            for (int index = 0; index < node.getChildCount(); index++) {
                AccessibilityNodeInfo child = node.getChild(index);
                if (child != null) {
                    queue.add(child);
                }
            }
        }
        return firstEditable;
    }

    private AccessibilityNodeInfo findScrollableNode() {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) {
            return null;
        }
        ArrayDeque<AccessibilityNodeInfo> queue = new ArrayDeque<>();
        queue.add(root);
        while (!queue.isEmpty()) {
            AccessibilityNodeInfo node = queue.removeFirst();
            if (node.isScrollable()) {
                return node;
            }
            for (int index = 0; index < node.getChildCount(); index++) {
                AccessibilityNodeInfo child = node.getChild(index);
                if (child != null) {
                    queue.add(child);
                }
            }
        }
        return null;
    }

    private AccessibilityNodeInfo findClickableParent(AccessibilityNodeInfo node) {
        AccessibilityNodeInfo current = node;
        while (current != null) {
            if (current.isClickable()) {
                return current;
            }
            current = current.getParent();
        }
        return null;
    }

    private static CharSequence firstNonEmpty(CharSequence first, CharSequence second) {
        if (first != null && first.length() > 0) {
            return first;
        }
        if (second != null && second.length() > 0) {
            return second;
        }
        return null;
    }

    private static JSONObject extractJson(String text) throws Exception {
        String trimmed = text.trim();
        int start = trimmed.indexOf('{');
        int end = trimmed.lastIndexOf('}');
        if (start < 0 || end <= start) {
            throw new IllegalArgumentException("No JSON object found");
        }
        return new JSONObject(trimmed.substring(start, end + 1));
    }

    private static void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
        }
    }
}
