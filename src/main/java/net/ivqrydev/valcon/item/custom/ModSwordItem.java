package net.ivqrydev.valcon.item.custom;

import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.Item;

public class ModSwordItem extends SwordItem {
    private final Tier tier;

    public ModSwordItem(Tier tier, Item.Properties properties) {
        super(tier, properties);
        this.tier = tier;
    }

    @Override
    public int getEnchantmentValue() {
        return tier.getEnchantmentValue();
    }
}

