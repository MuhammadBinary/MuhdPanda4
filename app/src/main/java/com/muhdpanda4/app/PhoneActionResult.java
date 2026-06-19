package com.muhdpanda4.app;

final class PhoneActionResult {
    final boolean success;
    final String message;

    PhoneActionResult(boolean success, String message) {
        this.success = success;
        this.message = message;
    }
}
