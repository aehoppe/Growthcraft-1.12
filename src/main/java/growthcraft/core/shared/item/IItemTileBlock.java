package growthcraft.core.shared.item;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

public interface IItemTileBlock {
    NBTTagCompound getTileTagCompound(ItemStack stack);

    void setTileTagCompound(ItemStack stack, NBTTagCompound tag);
}
