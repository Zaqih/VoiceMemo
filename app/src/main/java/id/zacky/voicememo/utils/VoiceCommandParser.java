package id.zacky.voicememo.utils;

import java.util.Calendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VoiceCommandParser {

    /**
     * Checks if the dictated text contains an alarm command.
     * Examples: "pasang alarm jam 1 siang", "ingatkan saya jam 14"
     */
    public static boolean isAlarmCommand(String text) {
        String lowerText = text.toLowerCase();
        return lowerText.contains("alarm") || lowerText.contains("ingat");
    }

    /**
     * Parses the time from the dictated text.
     * Supports patterns like "jam [X]" or "pukul [X]".
     * Handles "siang", "sore", "malam" for 12-hour adjustments.
     */
    public static Calendar parseTime(String text) {
        String lowerText = text.toLowerCase();
        
        // Regex to find "jam X" or "pukul X", optional minutes "lewat Y" or ".Y", optional "siang/malam/sore/pagi"
        // Example matches: "jam 1", "jam 13", "pukul 2 siang", "jam 2 lewat 15", "jam 14.30"
        Pattern pattern = Pattern.compile("(?:jam|pukul)\\s+(\\d{1,2})(?:\\s*(?:lewat|titik|\\.|:)\\s*(\\d{1,2}))?\\s*(pagi|siang|sore|malam)?");
        Matcher matcher = pattern.matcher(lowerText);

        if (matcher.find()) {
            int hour = Integer.parseInt(matcher.group(1));
            int minute = 0;
            
            if (matcher.group(2) != null) {
                minute = Integer.parseInt(matcher.group(2));
            }
            
            String period = matcher.group(3);
            
            // Adjust for 12-hour formats spoken with period
            if (period != null) {
                if ((period.equals("siang") || period.equals("sore") || period.equals("malam")) && hour < 12) {
                    hour += 12;
                } else if (period.equals("pagi") && hour == 12) {
                    hour = 0;
                }
            }

            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.HOUR_OF_DAY, hour);
            calendar.set(Calendar.MINUTE, minute);
            calendar.set(Calendar.SECOND, 0);

            // If time is in the past for today, schedule for tomorrow
            if (calendar.before(Calendar.getInstance())) {
                calendar.add(Calendar.DATE, 1);
            }
            
            return calendar;
        }
        
        return null; // Return null if no valid time pattern found
    }
    
    /**
     * Cleans the note text by removing the command portion.
     * E.g. "pasang alarm jam 2 siang untuk rapat tim" -> "rapat tim"
     */
    public static String extractNoteContent(String text) {
        String lowerText = text.toLowerCase();
        // Just split by "untuk" if exists, else return original
        if (lowerText.contains("untuk")) {
            return text.substring(lowerText.indexOf("untuk") + 6).trim();
        }
        return text;
    }
}
