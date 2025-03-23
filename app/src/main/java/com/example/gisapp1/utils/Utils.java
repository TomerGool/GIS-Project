package com.example.gisapp1.utils;

import android.util.Patterns;
import java.util.regex.Pattern;

public class Utils {
    public static boolean isValidEmail(String email) {
        if (email == null) return false;

        // Basic pattern matching
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            return false;
        }

        // Additional validation
        return isValidEmailDomain(email);
    }

    private static boolean isValidEmailDomain(String email) {
        // List of common email providers and domains
        String[] validDomains = {
                "gmail.com", "yahoo.com", "hotmail.com", "outlook.com",
                "icloud.com", "protonmail.com", "live.com", "yahoo.co.il",
                "gmail.co.il", "walla.co.il", "hotmail.co.il", "office365.com"
        };

        // Extract domain from email
        String domain = email.substring(email.lastIndexOf("@") + 1).toLowerCase();

        // Check against valid domains
        for (String validDomain : validDomains) {
            if (domain.equals(validDomain)) {
                return true;
            }
        }

        return false;
    }

    public static boolean isStrongPassword(String password) {
        // Minimum 8 characters, at least one uppercase, one lowercase, one number, one special character
        Pattern pattern = Pattern.compile(
                "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$"
        );
        return pattern.matcher(password).matches();
    }

    public static boolean isValidPhoneNumber(String phone) {
        // Basic phone number validation (adjust regex as needed for your region)
        Pattern pattern = Pattern.compile("^[+]?[0-9]{10,14}$");
        return pattern.matcher(phone).matches();
    }
}