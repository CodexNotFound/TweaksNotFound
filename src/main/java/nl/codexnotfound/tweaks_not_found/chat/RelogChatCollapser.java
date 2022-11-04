package nl.codexnotfound.tweaks_not_found.chat;

import net.minecraft.client.gui.hud.ChatHudLine;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableTextContent;
import net.minecraft.util.Language;
import nl.codexnotfound.tweaks_not_found.TweaksNotFound;

import java.util.Collection;

public class RelogChatCollapser implements ChatCollapser<MutableText> {

    public static String leftGameTranslationKey = "multiplayer.player.left";
    public static String joinedGameTranslationKey = "multiplayer.player.joined";
    public static String rejoinedGameTranslationKey = "tweaks-not-found.multiplayer.player.rejoin";

    @Override
    public boolean isMessageApplicable(MutableText newMessage, Collection<ChatHudLine<Text>> historicMessages) {
        if(!TweaksNotFound.CONFIG.showRejoin())
            return false;

        MutableText playerArg;
        var content = (TranslatableTextContent) newMessage.getContent();
        if (!content.getKey().equals(joinedGameTranslationKey)) {
            return false;
        } else {
            playerArg = (MutableText)content.getArgs()[0];
        }

        var language = Language.getInstance();
        var expectedString = language.get(leftGameTranslationKey);
        expectedString = expectedString.replace("%s", playerArg.getString());
        for (var historicMessage : historicMessages) {
            var text = historicMessage.getText().getString();
            text = ChatStringCleaner.clean(text);
            if(text.equals(expectedString)){
                return true;
            }
        }

        return false;
    }

    @Override
    public MutableText getNewMessage(MutableText newMessage, Collection<ChatHudLine<Text>> historicMessages) {
        var translatableText = (TranslatableTextContent)newMessage.getContent();
        var playerArg = (MutableText)translatableText.getArgs()[0];
        return Text.translatable(rejoinedGameTranslationKey, playerArg);
    }
}
