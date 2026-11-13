package com.shadowai.app;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;

public final class MemoryStore {
    private static final String PREFS = "shadowai_memory";
    private static final String KEY = "entries";
    private static final String PROFILE = "profile";

    private final SharedPreferences prefs;
    private final int maxBytes;

    public MemoryStore(Context context, boolean owner) {
        prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        maxBytes = owner ? 1024 * 1024 : 30 * 1024;
    }

    public synchronized boolean add(String text) {
        if (text == null || text.trim().isEmpty()) return false;

        try {
            JSONArray entries = getEntries();
            String value = text.trim();

            for (int i = 0; i < entries.length(); i++) {
                if (value.equalsIgnoreCase(entries.getString(i))) return false;
            }

            entries.put(value);
            return saveEntries(entries);
        } catch (Exception e) {
            return false;
        }
    }

    public synchronized boolean remove(int index) {
        try {
            JSONArray old = getEntries();
            if (index < 0 || index >= old.length()) return false;

            JSONArray updated = new JSONArray();
            for (int i = 0; i < old.length(); i++) {
                if (i != index) updated.put(old.getString(i));
            }
            return saveEntries(updated);
        } catch (Exception e) {
            return false;
        }
    }

    public synchronized String getAll() {
        StringBuilder result = new StringBuilder();
        JSONArray entries = getEntries();

        for (int i = 0; i < entries.length(); i++) {
            result.append(i + 1).append(". ").append(entries.optString(i)).append("\n");
        }

        return result.toString().trim();
    }

    public synchronized int size() {
        return getEntries().length();
    }

    public synchronized void clear() {
        prefs.edit().remove(KEY).remove(PROFILE).apply();
    }

    public synchronized boolean setProfile(String profile) {
        if (profile == null) return false;
        if (profile.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > maxBytes)
            return false;

        prefs.edit().putString(PROFILE, profile.trim()).apply();
        return true;
    }

    public synchronized String getProfile() {
        return prefs.getString(PROFILE, "");
    }

    public synchronized String getContext() {
        String profile = getProfile();
        String memories = getAll();

        if (profile.isEmpty() && memories.isEmpty()) return "";

        return "Gespeichertes Wissen über den Nutzer:\n"
                + (profile.isEmpty() ? "" : "Profil:\n" + profile + "\n")
                + (memories.isEmpty() ? "" : "Erinnerungen:\n" + memories);
    }

    private JSONArray getEntries() {
        try {
            return new JSONArray(prefs.getString(KEY, "[]"));
        } catch (Exception e) {
            return new JSONArray();
        }
    }

    private boolean saveEntries(JSONArray entries) {
        String data = entries.toString();
        int profileBytes = getProfile().getBytes(
                java.nio.charset.StandardCharsets.UTF_8).length;

        if (data.getBytes(java.nio.charset.StandardCharsets.UTF_8).length
                + profileBytes > maxBytes) return false;

        prefs.edit().putString(KEY, data).apply();
        return true;
    }
}
