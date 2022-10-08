package nl.codexnotfound.tweaks_not_found.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.ChatHudLine;
import net.minecraft.client.util.ChatMessages;
import net.minecraft.text.*;
import net.minecraft.util.math.MathHelper;
import nl.codexnotfound.tweaks_not_found.TweaksNotFound;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;


@Mixin(ChatHud.class)
public class ChatMixin {
    @Shadow @Final private static Logger LOGGER;
    @Shadow @Final private List<ChatHudLine<Text>> messages;
    @Shadow @Final private List<ChatHudLine<OrderedText>> visibleMessages;
    @Shadow @Final private MinecraftClient client;

    private final String COLLAPSE_CHAT_FORMAT = " {×%d}";
    private final Pattern COLLAPSE_CHAT_FORMAT_REGEX = Pattern.compile(" \\{×(\\d+)}");
    private final String TIMESTAMP_FORMAT = "[%02d:%02d] ";
    private final Pattern TIMESTAMP_FORMAT_REGEX = Pattern.compile("\\[\\d{2}:\\d{2}] ");

    @ModifyArgs(method = "addMessage(Lnet/minecraft/text/Text;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/hud/ChatHud;addMessage(Lnet/minecraft/text/Text;I)V"))
    private void modifyArgsAddMessage(org.spongepowered.asm.mixin.injection.invoke.arg.Args args) {
        if(TweaksNotFound.CONFIG.showTimestamp()) {
            MutableText msg = args.get(0);
            var style = msg.getStyle();
            var timeText = buildTimePrefix();
            msg = timeText.append(msg.setStyle(style));
            args.set(0, msg);
        }
    }

    @Inject(method = "addMessage(Lnet/minecraft/text/Text;IIZ)V", at = @At("HEAD"), cancellable = true)
    private void onAddMessage(Text msg, int messageId, int timestamp, boolean refresh, CallbackInfo ci) {
        if(!TweaksNotFound.CONFIG.enableChatCollapsing()){
            return;
        }

        var message = (MutableText) msg;
        var searchDistance = Math.min(TweaksNotFound.CONFIG.collapseDistance(), messages.size());
        var newMessageText = message.getString();
        var newMessageTextCleanedUp = newMessageText.replaceFirst(TIMESTAMP_FORMAT_REGEX.pattern(), "").trim();

        // https://github.com/wisp-forest/owo-lib/issues/59
        // Backslashes are not saved properly, allow backslashes using a replacement string "&bs&"
        var nonCollapsingMessagesString = TweaksNotFound.CONFIG.nonCollapsingMessages().replaceAll("&bs&", "\\");
        var nonCollapsingMessages = Arrays.stream(nonCollapsingMessagesString.split(";"));
        if (nonCollapsingMessages.anyMatch(newMessageTextCleanedUp::contains)) {
            return;
        }

        var sameMessageIndex = -1;
        // See if the message has been send before
        for (int i = 0; i < searchDistance; i++) {
            var tempMessage = messages.get(i).getText().getString()
                    .replaceFirst(TIMESTAMP_FORMAT_REGEX.pattern(), "")
                    .replaceAll(COLLAPSE_CHAT_FORMAT_REGEX.pattern(), "");

            if (tempMessage.equals(newMessageTextCleanedUp)) {
                sameMessageIndex = i;
                break;
            }
        }
        if (sameMessageIndex == -1) {
            return;
        }

        // Decide the times the message has been send.
        var oldMessage = messages.get(sameMessageIndex);
        var match = COLLAPSE_CHAT_FORMAT_REGEX.matcher(oldMessage.getText().getString());
        var count = 2;
        if (match.find()) {
            count = Integer.parseInt(match.group(1)) + 1;
        }

        var countText = MutableText
                .of(new LiteralTextContent(String.format(COLLAPSE_CHAT_FORMAT, count)))
                .setStyle(Style.EMPTY.withColor(TextColor.parse("yellow")));

        var newChatText = message.append(countText);

        messages.remove(sameMessageIndex);
        messages.add(0, new ChatHudLine<>(timestamp, newChatText, oldMessage.getId()));

        int width = MathHelper.floor(getWidth() / getChatScale());
        List<OrderedText> lines = ChatMessages.breakRenderedChatMessageLines(newChatText, width, this.client.textRenderer);

        int sameVisibleMessageIndex = sameMessageIndex; // Use as fallback
        for (int i = 0; i < this.visibleMessages.size(); i++) {
            var visibleMessage = this.visibleMessages.get(i);
            if (visibleMessage.getCreationTick() == oldMessage.getCreationTick()) {
                sameVisibleMessageIndex = i;
                break;
            }
        }

        for (int i = 0; i < lines.size(); i++) {
            OrderedText orderedText = lines.get(i);
            var absoluteLineIndex = sameVisibleMessageIndex + i;
            this.visibleMessages.remove(absoluteLineIndex);
            this.visibleMessages.add(0, new ChatHudLine(timestamp, orderedText, messageId));
        }
        ci.cancel();
    }

    private MutableText buildTimePrefix() {
        var time = LocalTime.now();
        var hours = time.getHour();
        var minutes = time.getMinute();
        var timeText = String.format(TIMESTAMP_FORMAT, hours, minutes);
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
