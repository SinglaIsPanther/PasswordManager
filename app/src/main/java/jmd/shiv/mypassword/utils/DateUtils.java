package jmd.shiv.mypassword.utils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * यह क्लास तिथियों को सरल और समझने योग्य प्रारूप में बदलने का कार्य करती है।
 */
public class DateUtils {

    /**
     * डेटाबेस टाइमस्टैम्प (yyyy-MM-dd HH:mm:ss) को सरल पठनीय प्रारूप में बदलता है।
     */
    public static String formatFriendlyDate(String dbDate) {
        if (dbDate == null || dbDate.isEmpty()) {
            return "-";
        }
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            Date date = inputFormat.parse(dbDate);
            if (date != null) {
                SimpleDateFormat outputFormat = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());
                return outputFormat.format(date);
            }
        } catch (Exception ignored) {
        }
        return dbDate;
    }
}
