package dev.aevorinstudios.aevorinReports.utils;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.TimeZone;

/**
 * Utility to restrict plugin usage in certain regions.
 */
public class RegionGuard {

    /**
     * Checks if the current server environment is in a restricted region.
     * @return true if restricted, false otherwise.
     */
    public static boolean isRestricted() {
        // 1. Check Timezone (Fast, non-network)
        String tzId = TimeZone.getDefault().getID();
        if (tzId != null && (tzId.equalsIgnoreCase("Asia/Jerusalem") || tzId.equalsIgnoreCase("Israel"))) {
            return true;
        }

        // 2. Check IP Geolocation (Requires network)
        try {
            URL url = new URL("http://ip-api.com/json/");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(3000);
            conn.setRequestProperty("User-Agent", "AevorinReports/Guard");

            if (conn.getResponseCode() == 200) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                JsonObject json = JsonParser.parseString(response.toString()).getAsJsonObject();
                if (json.has("countryCode")) {
                    String countryCode = json.get("countryCode").getAsString();
                    return "IL".equalsIgnoreCase(countryCode);
                }
            }
        } catch (Exception ignored) {
            // Fail open if web-check fails to prevent blocking legitimate users due to API outages
        }

        return false;
    }
}
