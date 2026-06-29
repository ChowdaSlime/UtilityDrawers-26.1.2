package net.drawers.utilitydrawers.attachment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public class PlayerPreferences {

    public static final MapCodec<PlayerPreferences> CODEC =
            RecordCodecBuilder.mapCodec(instance -> instance.group(
                    Codec.BOOL.optionalFieldOf("sort_by_count", false)
                            .forGetter(PlayerPreferences::isSortByCount),
                    Codec.BOOL.optionalFieldOf("sort_ascending", true)
                            .forGetter(PlayerPreferences::isSortAscending)
            ).apply(instance, (sortByCount, sortAscending) -> {
                PlayerPreferences prefs = new PlayerPreferences();
                prefs.setSortByCount(sortByCount);
                prefs.setSortAscending(sortAscending);
                return prefs;
            }));

    private boolean sortByCount = false;
    private boolean sortAscending = true;

    public boolean isSortAscending() { return sortAscending; }
    public void setSortAscending(boolean sortAscending) { this.sortAscending = sortAscending; }

    public PlayerPreferences() {}

    public boolean isSortByCount() {
        return sortByCount;
    }

    public void setSortByCount(boolean sortByCount) {
        this.sortByCount = sortByCount;
    }
}