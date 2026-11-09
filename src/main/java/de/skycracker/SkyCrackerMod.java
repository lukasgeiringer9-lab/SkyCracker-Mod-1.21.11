package de.skycracker;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;

public class SkyCrackerMod implements ModInitializer {
    public static final String MOD_ID = "skycracker";

    public static final EntityType<SkyCrackerEntity> SKYCRACKER = registerEntityType();
    public static final Item SKYCRACKER_SPAWN_EGG = registerSpawnEgg();

    private static EntityType<SkyCrackerEntity> registerEntityType() {
        ResourceKey<EntityType<?>> key = ResourceKey.create(
                Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(MOD_ID, "skycracker"));
        EntityType<SkyCrackerEntity> type = EntityType.Builder
                .<SkyCrackerEntity>of(SkyCrackerEntity::new, MobCategory.CREATURE)
                .sized(0.6f, 1.95f)
                .build(key);
        return Registry.register(BuiltInRegistries.ENTITY_TYPE, key, type);
    }

    private static Item registerSpawnEgg() {
        ResourceKey<Item> key = ResourceKey.create(
                Registries.ITEM, Identifier.fromNamespaceAndPath(MOD_ID, "skycracker_spawn_egg"));
        Item item = new SpawnEggItem(new Item.Properties().setId(key).spawnEgg(SKYCRACKER));
        return Registry.register(BuiltInRegistries.ITEM, key, item);
    }

    @Override
    public void onInitialize() {
        FabricDefaultAttributeRegistry.register(SKYCRACKER, SkyCrackerEntity.createSkyCrackerAttributes());

        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.SPAWN_EGGS)
                .register(entries -> entries.accept(SKYCRACKER_SPAWN_EGG));

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                SkyCrackerCommands.register(dispatcher));
    }
}
