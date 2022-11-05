package nl.codexnotfound.tweaks_not_found.chat;

import java.util.regex.Pattern;

public abstract class ChatFormats {
    public static final Pattern COLLAPSE_CHAT_FORMAT_REGEX = Pattern.compile(" \\{×(\\d+)}");
    public static final Pattern TIMESTAMP_FORMAT_REGEX = Pattern.compile("\\[\\d{2}:\\d{2}( [AP]M)?] ", Pattern.CASE_INSENSITIVE);
}
