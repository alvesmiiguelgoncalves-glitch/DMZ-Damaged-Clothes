package com.dmzdamagedclothes;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;

/** Material de teste: 200 de durabilidade em todas as pecas, protecao baixa. Conserta com barra de ferro na bigorna. */
public final class TestArmorMaterial implements ArmorMaterial {

    public static final TestArmorMaterial INSTANCE = new TestArmorMaterial();
    public static final int DURABILITY = 200;

    private TestArmorMaterial() {
    }

    @Override
    public int getDurabilityForType(ArmorItem.Type type) {
        return DURABILITY;
    }

    @Override
    public int getDefenseForType(ArmorItem.Type type) {
        return 1;
    }

    @Override
    public int getEnchantmentValue() {
        return 0;
    }

    @Override
    public SoundEvent getEquipSound() {
        return SoundEvents.ARMOR_EQUIP_LEATHER;
    }

    @Override
    public Ingredient getRepairIngredient() {
        return Ingredient.of(Items.IRON_INGOT);
    }

    @Override
    public String getName() {
        return DmzDamagedClothes.MOD_ID + ":test_gi";
    }

    @Override
    public float getToughness() {
        return 0.0F;
    }

    @Override
    public float getKnockbackResistance() {
        return 0.0F;
    }
}
