package com.dmzdamagedclothes;

import com.dmzdamagedclothes.client.DamagedTextures;
import com.dragonminez.common.init.armor.DbzArmorItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(DmzDamagedClothes.MOD_ID)
public final class DmzDamagedClothes {

    public static final String MOD_ID = "dmz_damaged_clothes";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MOD_ID);
    private static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MOD_ID);

    /**
     * Armadura de teste: usa as texturas do Goku gi do DMZ (modId "dragonminez", id "goku_gi"), entao passa pelo mesmo
     * caminho do render e das 11 fases, mas com so 200 de durabilidade.
     */
    public static final RegistryObject<Item> TEST_CHESTPLATE = testPiece("test_gi_chestplate", ArmorItem.Type.CHESTPLATE);
    public static final RegistryObject<Item> TEST_LEGGINGS = testPiece("test_gi_leggings", ArmorItem.Type.LEGGINGS);
    public static final RegistryObject<Item> TEST_BOOTS = testPiece("test_gi_boots", ArmorItem.Type.BOOTS);

    public static final RegistryObject<CreativeModeTab> TAB = TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup." + MOD_ID))
            .icon(() -> new ItemStack(TEST_CHESTPLATE.get()))
            .displayItems((params, output) -> {
                output.accept(TEST_CHESTPLATE.get());
                output.accept(TEST_LEGGINGS.get());
                output.accept(TEST_BOOTS.get());
            })
            .build());

    private static RegistryObject<Item> testPiece(String name, ArmorItem.Type type) {
        return ITEMS.register(name, () -> new DbzArmorItem(TestArmorMaterial.INSTANCE, type,
                new Item.Properties().stacksTo(1), "dragonminez", "goku_gi"));
    }

    public DmzDamagedClothes() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        ITEMS.register(bus);
        TABS.register(bus);
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, DamagedConfig.SPEC);
    }

    @Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class ClientEvents {
        @SubscribeEvent
        public static void reload(RegisterClientReloadListenersEvent event) {
            event.registerReloadListener((ResourceManagerReloadListener) manager -> DamagedTextures.clear());
        }
    }
}
