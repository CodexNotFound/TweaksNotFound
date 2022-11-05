package nl.codexnotfound.tweaks_not_found.config;

import io.wispforest.owo.config.annotation.*;

import java.util.ArrayList;
import java.util.List;

@Modmenu(modId = "tweaks_not_found")
@Config(name="tweaks-not-found-config", wrapperName = "TweaksConfig")
public class TnfConfigModel {
    @SectionHeader("timestamp")
    public boolean showTimestamp = true;
    public ClockFormat timeFormat = ClockFormat.TwentyFour;

    @SectionHeader("chatCollapsing")
    public boolean enableChatCollapsing = true;
    @RangeConstraint(min = 1, max = 20)
    public int collapseDistance = 5;
    public String nonCollapsingMessages = "";

    @SectionHeader("rejoin")
    public boolean showRejoin = false;

    @SectionHeader("mentions")
    public boolean enableMentions = false;

    public List<String> mentionKeywords = new ArrayList<>();

}
