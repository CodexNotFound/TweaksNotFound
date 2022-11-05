package nl.codexnotfound.tweaks_not_found.chat;

import java.util.regex.Pattern;

public abstract class ChatStringCleaner {
    public static String clean(String text){
        return text
                .replaceFirst(ChatFormats.TIMESTAMP_FORMAT_REGEX.pattern(), "")
                .replaceAll(ChatFormats.COLLAPSE_CHAT_FORMAT_REGEX.pattern(), "")
                .replace(MentionChatCollapser.MentionPrefix, "");
    }

    private static final Pattern playerPrefix = Pattern.compile("<[a-zA-Z0-9_]{2,16}>");
    public static String removePlayerPrefix(String text){
        return text
                .replaceFirst(playerPrefix.pattern(), "");
    }
}
