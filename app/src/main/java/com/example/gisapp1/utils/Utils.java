package com.example.gisapp1.utils;

import android.util.Patterns;

public class Utils {
    public static boolean isValidEmail(String email) {
        return email != null && Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }
}
