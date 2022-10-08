package nl.codexnotfound.tweaks_not_found.config;

import io.wispforest.owo.config.annotation.Config;
import io.wispforest.owo.config.annotation.Modmenu;
import io.wispforest.owo.config.annotation.RangeConstraint;
import io.wispforest.owo.config.annotation.SectionHeader;

@Modmenu(modId = "tweaks_not_found")
@Config(name="tweaks-not-found-config", wrapperName = "TweaksConfig")
public class TnfConfigModel {
    public boolean showTimestamp = true;

    @SectionHeader("chatCollapsing")
    public boolean enableChatCollapsing = true;
    @RangeConstraint(min = 1, max = 20)
    public int collapseDistance = 5;
    public String nonCollapsingMessages = "\\o;o/";
}
