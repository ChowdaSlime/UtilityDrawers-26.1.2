package net.drawers.utilitydrawers.attachment;

import net.drawers.utilitydrawers.UtilityDrawers;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public class ModAttachments {

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, UtilityDrawers.MODID);

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<PlayerPreferences>> PLAYER_PREFERENCES =
            ATTACHMENTS.register("player_preferences",
                    () -> AttachmentType.builder(PlayerPreferences::new)
                            .serialize(PlayerPreferences.CODEC)
                            .build());

    public static void register(IEventBus eventBus) {
        ATTACHMENTS.register(eventBus);
    }
}