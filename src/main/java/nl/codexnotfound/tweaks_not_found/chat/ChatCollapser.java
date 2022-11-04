package nl.codexnotfound.tweaks_not_found.chat;

import net.minecraft.client.gui.hud.ChatHudLine;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

import java.util.Collection;

public interface ChatCollapser<T extends Text> {

    boolean isMessageApplicable(T newMessage, Collection<ChatHudLine<Text>> historicMessages);
    MutableText getNewMessage(T newMessage, Collection<ChatHudLine<Text>> historicMessages);
}
