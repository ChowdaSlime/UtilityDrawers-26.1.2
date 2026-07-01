package net.drawers.utilitydrawers.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import io.netty.buffer.ByteBuf;

import java.util.Optional;
import java.util.UUID;

public record WirelessNetworkKey(
        int color1,
        int color2,
        int color3,
        boolean isPublic,
        Optional<UUID> owner,
        int slotCount
) {
    public static final Codec<WirelessNetworkKey> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.INT.fieldOf("color1").forGetter(WirelessNetworkKey::color1),
            Codec.INT.fieldOf("color2").forGetter(WirelessNetworkKey::color2),
            Codec.INT.fieldOf("color3").forGetter(WirelessNetworkKey::color3),
            Codec.BOOL.fieldOf("isPublic").forGetter(WirelessNetworkKey::isPublic),
            Codec.STRING.optionalFieldOf("owner")
                    .<Optional<UUID>>xmap(
                            opt -> opt.map(UUID::fromString),
                            opt -> opt.map(UUID::toString)
                    ).forGetter(WirelessNetworkKey::owner),
            Codec.INT.fieldOf("slotCount").forGetter(WirelessNetworkKey::slotCount)
    ).apply(inst, WirelessNetworkKey::new));

    public static final StreamCodec<ByteBuf, WirelessNetworkKey> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.INT, WirelessNetworkKey::color1,
                    ByteBufCodecs.INT, WirelessNetworkKey::color2,
                    ByteBufCodecs.INT, WirelessNetworkKey::color3,
                    ByteBufCodecs.BOOL, WirelessNetworkKey::isPublic,
                    ByteBufCodecs.STRING_UTF8.map(
                            s -> s.isEmpty() ? Optional.empty() : Optional.of(UUID.fromString(s)),
                            opt -> opt.map(UUID::toString).orElse("")
                    ), WirelessNetworkKey::owner,
                    ByteBufCodecs.INT, WirelessNetworkKey::slotCount,
                    WirelessNetworkKey::new
            );

    public boolean matches(WirelessNetworkKey other) {
        if (this.slotCount != other.slotCount) return false;

        if (this.color1 != other.color1 || this.color2 != other.color2 || this.color3 != other.color3)
            return false;

        if (this.isPublic && other.isPublic) return true;

        if (!this.isPublic && !other.isPublic)
            return this.owner.isPresent() && this.owner.equals(other.owner);

        return false;
    }

    public String toKey() {
        String base = color1 + "," + color2 + "," + color3 + ",slots:" + slotCount;
        if (isPublic) return base + ",public";
        return base + ",personal," + owner.map(UUID::toString).orElse("unknown");
    }
}