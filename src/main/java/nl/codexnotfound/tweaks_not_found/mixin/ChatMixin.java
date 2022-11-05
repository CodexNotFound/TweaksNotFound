package nl.codexnotfound.tweaks_not_found.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.ChatHudLine;
import net.minecraft.client.util.ChatMessages;
import net.minecraft.text.*;
import net.minecraft.util.math.MathHelper;
import nl.codexnotfound.tweaks_not_found.TweaksNotFound;
import nl.codexnotfound.tweaks_not_found.chat.ChatFormats;
import nl.codexnotfound.tweaks_not_found.chat.ChatStringCleaner;
import nl.codexnotfound.tweaks_not_found.chat.MentionChatCollapser;
import nl.codexnotfound.tweaks_not_found.chat.RelogChatCollapser;
import nl.codexnotfound.tweaks_not_found.config.ClockFormat;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;


@Mixin(ChatHud.class)
public class ChatMixin {
    @Shadow @Final private static Logger LOGGER;
    @Shadow @Final private List<ChatHudLine<Text>> messages;
    @Shadow @Final private List<ChatHudLine<OrderedText>> visibleMessages;
    @Shadow @Final private MinecraftClient client;

    @ModifyArgs(method = "addMessage(Lnet/minecraft/text/Text;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/hud/ChatHud;addMessage(Lnet/minecraft/text/Text;I)V"))
    private void modifyArgsAddMessage(org.spongepowered.asm.mixin.injection.invoke.arg.Args args) {
        MutableText msg = args.get(0);
        var historicMessages = messages.subList(0, getSearchDistance());
        var relogChatCollapser = new RelogChatCollapser();
        if(relogChatCollapser.isMessageApplicable(msg, historicMessages)){
            var style = msg.getStyle();
            msg = relogChatCollapser.getNewMessage(msg, historicMessages);
            msg.setStyle(style);
        }

        if (TweaksNotFound.CONFIG.showTimestamp()) {
            var style = msg.getStyle();
            var timeText = buildTimePrefix();
            msg = timeText.append(msg.setStyle(style));
        }

        var mentionChatCollapser = new MentionChatCollapser();
        if(mentionChatCollapser.isMessageApplicable(msg, historicMessages)){
            msg = mentionChatCollapser.getNewMessage(msg, historicMessages);
        }


        args.set(0, msg);
    }

    @Inject(method = "addMessage(Lnet/minecraft/text/Text;IIZ)V", at = @At("HEAD"), cancellable = true)
    private void onAddMessage(Text msg, int messageId, int timestamp, boolean refresh, CallbackInfo ci) {
        if (!TweaksNotFound.CONFIG.enableChatCollapsing()) {
            return;
        }
        // No matter what happens inside, if it fails it shouldn't break the chat.
        // If exceptions happen the normal chat code should be called upon.
        try {
            var message = (MutableText) msg;
            var searchDistance = getSearchDistance();

            var newMessageText = message.getString();
            var newMessageTextCleanedUp = ChatStringCleaner.clean(newMessageText).trim();

            if (!shouldCollapseMessage(newMessageTextCleanedUp)) {
                return;
            }

            var sameMessageIndex = findSameMessageIndex(searchDistance, newMessageTextCleanedUp);
            if (sameMessageIndex == null) {
                return;
            }

            var oldMessage = messages.get(sameMessageIndex);
            MutableText countText = getNewCountText(oldMessage);

            var newChatText = message.append(countText);
            messages.remove((int) sameMessageIndex);
            messages.add(0, new ChatHudLine<>(timestamp, newChatText, messageId));
            var success = updateVisibleMessages(messageId, timestamp, oldMessage, newChatText);
            if (success) {
                ci.cancel();
            }
        } catch (Exception ex) {
            LOGGER.error(ex.getMessage(), ex);
        }
    }

    private int getSearchDistance() {
        return Math.min(TweaksNotFound.CONFIG.collapseDistance(), messages.size());
    }

    private boolean updateVisibleMessages(int messageId, int timestamp, ChatHudLine<Text> oldMessage, MutableText newChatText) {
        int width = MathHelper.floor(getWidth() / getChatScale());
        List<OrderedText> lines = ChatMessages.breakRenderedChatMessageLines(newChatText, width, this.client.textRenderer);

        int sameVisibleMessageIndex = -1;
        for (int i = 0; i < this.visibleMessages.size(); i++) {
            var visibleMessage = this.visibleMessages.get(i);
            if (visibleMessage.getCreationTick() == oldMessage.getCreationTick()) {
                sameVisibleMessageIndex = i;
                break;
            }
        }

        if (sameVisibleMessageIndex == -1) {
            return false;
        }

        for (int i = 0; i < lines.size(); i++) {
            OrderedText orderedText = lines.get(i);
            var absoluteLineIndex = sameVisibleMessageIndex + i;
            if (absoluteLineIndex >= 0 && absoluteLineIndex < this.visibleMessages.size()) {
                this.visibleMessages.remove(absoluteLineIndex);
                this.visibleMessages.add(0, new ChatHudLine<>(timestamp, orderedText, messageId));
            }
        }
        return true;
    }

    private MutableText getNewCountText(ChatHudLine<Text> oldMessage) {
        var COLLAPSE_CHAT_FORMAT = " {×%d}";

        var match = ChatFormats.COLLAPSE_CHAT_FORMAT_REGEX.matcher(oldMessage.getText().getString());
        var count = 2;
        if (match.find()) {
            count = Integer.parseInt(match.group(1)) + 1;
        }

        return MutableText
                .of(new LiteralTextContent(String.format(COLLAPSE_CHAT_FORMAT, count)))
                .setStyle(Style.EMPTY.withColor(TextColor.parse("yellow")));
    }

    @Nullable
    private Integer findSameMessageIndex(int searchDistance, String newMessageTextCleanedUp) {
        var sameMessageIndex = -1;
        for (int i = 0; i < searchDistance; i++) {
            var tempMessage = ChatStringCleaner.clean(messages.get(i).getText().getString());

            if (tempMessage.equals(newMessageTextCleanedUp)) {
                sameMessageIndex = i;
                break;
            }
        }
        if (sameMessageIndex == -1) {
            return null;
        }
        return sameMessageIndex;
    }

    private static boolean shouldCollapseMessage(String newMessageTextCleanedUp) {
        // https://github.com/wisp-forest/owo-lib/issues/59
        // Backslashes are not saved properly, allow backslashes using a replacement string "&bs&"
        var nonCollapsingMessagesString = TweaksNotFound.CONFIG.nonCollapsingMessages().replace("&bs&", "\\");
        var nonCollapsingMessages = Arrays.stream(nonCollapsingMessagesString.split(";"))
                .filter(x -> x.trim().length() > 1);
        return nonCollapsingMessages.noneMatch(newMessageTextCleanedUp::contains);
    }

    private MutableText buildTimePrefix() {
        var format = TweaksNotFound.CONFIG.timeFormat() == ClockFormat.TwentyFour ? "HH:mm" : "hh:mm a";

        var time = LocalTime.now().format(DateTimeFormatter.ofPattern(format));
        var timeText = String.format("[%s] ", time);

        return MutableText
                .of(new LiteralTextContent(timeText));
//                .setStyle(Style.EMPTY.withColor(TextColor.parse("aqua")));
    }

    private double getWidth() {
        return MathHelper.floor(client.options.getChatWidth().getValue() * 280.0 + 40.0);
    }

    public double getChatScale() {
        return this.client.options.getChatScale().getValue();
    }
}
