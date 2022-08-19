package nl.codexnotfound.tweaks_not_found.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.ChatHudLine;
import net.minecraft.client.util.ChatMessages;
import net.minecraft.text.*;
import net.minecraft.util.math.MathHelper;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.regex.Pattern;


@Mixin(ChatHud.class)
public class ChatMixin {
    @Shadow @Final private static Logger LOGGER;
    @Shadow @Final private List<ChatHudLine<Text>> messages;
    @Shadow @Final private List<ChatHudLine<OrderedText>> visibleMessages;
    @Shadow @Final private MinecraftClient client;
    private final int COLLAPSE_CHAT_SEARCH_DISTANCE = 5;
    private final String COLLAPSE_CHAT_FORMAT = " {×%d}";
    private final Pattern COLLAPSE_CHAT_FORMAT_REGEX = Pattern.compile(" \\{×(\\d+)}");
    private final Pattern TIMESTAMP_FORMAT_REGEX = Pattern.compile("\\d{2}:\\d{2} ");

    @Inject(method = "addMessage(Lnet/minecraft/text/Text;IIZ)V", at = @At("HEAD"), cancellable = true)
    private void onAddMessage(Text msg, int messageId, int timestamp, boolean refresh, CallbackInfo ci){
        var message = (MutableText)msg;
        var searchDistance = Math.min(COLLAPSE_CHAT_SEARCH_DISTANCE, messages.size());
        var newMessageText = message.getString();
        var sameMessageIndex = -1;
        for(int i=0; i<searchDistance; i++){
            var tempMessage = messages.get(i).getText().getString()
                    .replaceFirst(TIMESTAMP_FORMAT_REGEX.pattern(), "")
                    .replaceAll(COLLAPSE_CHAT_FORMAT_REGEX.pattern(), "");

            if(tempMessage.equals(newMessageText)) {
                sameMessageIndex = i;
                break;
            }
        }
        if(sameMessageIndex == -1){
            return;
        }

        var oldMessage = messages.get(sameMessageIndex);
        var match = COLLAPSE_CHAT_FORMAT_REGEX.matcher(oldMessage.getText().getString());
        var count = 2;
        if(match.find()) {
            count = Integer.parseInt(match.group(1)) + 1;
        }

        var countText = MutableText
                .of(new LiteralTextContent(String.format(COLLAPSE_CHAT_FORMAT, count)))
                .setStyle(Style.EMPTY.withColor(TextColor.parse("yellow")));
        var newChatText = message.append(countText);

        messages.remove(sameMessageIndex);
        messages.add(0, new ChatHudLine<>(oldMessage.getCreationTick(), newChatText, oldMessage.getId()));

        int width = MathHelper.floor(getWidth() / getChatScale());
        List<OrderedText> lines = ChatMessages.breakRenderedChatMessageLines(newChatText, width, this.client.textRenderer);

        for (int i = 0; i < lines.size(); i++) {
            OrderedText orderedText = lines.get(i);
            var absoluteLineIndex = sameMessageIndex+lines.size()-i-1;
            this.visibleMessages.remove(absoluteLineIndex);
            this.visibleMessages.add(0, new ChatHudLine(timestamp, orderedText, messageId));
        }
        ci.cancel();
    }

    private double getWidth(){
        return MathHelper.floor(client.options.getChatWidth().getValue() * 280.0 + 40.0);
    }

    public double getChatScale() {
        return this.client.options.getChatScale().getValue();
    }
}
