package nl.codexnotfound.tweaks_not_found.chat;

public abstract class ChatStringCleaner {
    public static String clean(String text){
        return text
                .replaceFirst(ChatFormats.TIMESTAMP_FORMAT_REGEX.pattern(), "")
                .replaceAll(ChatFormats.COLLAPSE_CHAT_FORMAT_REGEX.pattern(), "");
    }
}
