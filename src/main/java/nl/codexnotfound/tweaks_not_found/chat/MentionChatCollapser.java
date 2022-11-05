package nl.codexnotfound.tweaks_not_found.chat;

import net.minecraft.client.gui.hud.ChatHudLine;
import net.minecraft.text.*;
import nl.codexnotfound.tweaks_not_found.TweaksNotFound;

import java.util.Collection;

public class MentionChatCollapser implements ChatCollapser<MutableText> {
    static public String MentionPrefix = "✮ ";
    @Override
    public boolean isMessageApplicable(MutableText incomingMessage, Collection<ChatHudLine<Text>> historicMessages) {
        if(!TweaksNotFound.CONFIG.enableMentions())
            return false;

        var keywords = TweaksNotFound.CONFIG.mentionKeywords();
        if(keywords.size() == 0)
            return false;
        var messageString = incomingMessage.getSiblings().get(incomingMessage.getSiblings().size()-1).getString().toLowerCase();
        messageString = ChatStringCleaner.removePlayerPrefix(messageString);
        for (var keyword : keywords) {
            if(messageString.contains(keyword.toLowerCase()))
                return true;
        }

        return false;
    }

    @Override
    public MutableText getNewMessage(MutableText incomingMessage, Collection<ChatHudLine<Text>> historicMessages) {
        var style = incomingMessage.getStyle();
        var updatedText = MutableText.of(new LiteralTextContent(MentionPrefix));
        return updatedText.append(incomingMessage.setStyle(style));
    }
}
