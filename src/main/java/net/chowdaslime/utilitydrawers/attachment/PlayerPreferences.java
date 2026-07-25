package net.chowdaslime.utilitydrawers.attachment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public class PlayerPreferences {

    public static final MapCodec<PlayerPreferences> CODEC =
            RecordCodecBuilder.mapCodec(instance -> instance.group(
                    Codec.BOOL.optionalFieldOf("sort_by_count", false)
                            .forGetter(PlayerPreferences::isSortByCount),
                    Codec.BOOL.optionalFieldOf("sort_ascending", true)
                            .forGetter(PlayerPreferences::isSortAscending),
                    Codec.INT.optionalFieldOf("viewer_rows", 3)
                            .forGetter(PlayerPreferences::getViewerRows),
                    Codec.BOOL.optionalFieldOf("sync_jei", false)
                            .forGetter(PlayerPreferences::isSyncJei)
            ).apply(instance, (sortByCount, sortAscending, viewerRows, syncJei) -> {
                PlayerPreferences prefs = new PlayerPreferences();
                prefs.setSortByCount(sortByCount);
                prefs.setSortAscending(sortAscending);
                prefs.setViewerRows(viewerRows);
                prefs.setSyncJei(syncJei);
                return prefs;
            }));

    private boolean sortByCount = false;
    private boolean sortAscending = true;
    private int viewerRows = 3;
    private boolean syncJei = false;

    public PlayerPreferences() {}

    public boolean isSortAscending() { return sortAscending; }
    public void setSortAscending(boolean sortAscending) { this.sortAscending = sortAscending; }

    public boolean isSortByCount() { return sortByCount; }
    public void setSortByCount(boolean sortByCount) { this.sortByCount = sortByCount; }

    public int getViewerRows() { return viewerRows; }
    public void setViewerRows(int viewerRows) { this.viewerRows = viewerRows; }

    public boolean isSyncJei() { return syncJei; }
    public void setSyncJei(boolean syncJei) { this.syncJei = syncJei; }
}