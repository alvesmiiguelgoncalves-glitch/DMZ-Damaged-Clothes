package com.dmzdamagedclothes.mixin;

import com.dmzdamagedclothes.client.DamagedTextures;
import com.dragonminez.client.util.ArmorTextureResolver;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Todo caminho de render do DMZ que escolhe textura de armadura passa por {@code ArmorTextureResolver.resolve},
 * que recebe o item (e portanto a durabilidade). Se existe um conjunto de estagios para a roupa, trocamos o
 * resultado pela textura do estagio atual (ou pela mistura pixel a pixel entre dois estagios).
 */
@Mixin(value = ArmorTextureResolver.class, remap = false)
public abstract class ArmorTextureResolverMixin {

    @Inject(method = "resolve", at = @At("RETURN"), cancellable = true)
    private static void dmzdamaged$stage(String modId, String itemId, EquipmentSlot slot, ItemStack stack,
                                         CallbackInfoReturnable<ResourceLocation> cir) {
        ResourceLocation original = cir.getReturnValue();
        ResourceLocation staged = DamagedTextures.apply(modId, itemId, slot, stack, original);
        if (staged != original) {
            cir.setReturnValue(staged);
        }
    }
}
