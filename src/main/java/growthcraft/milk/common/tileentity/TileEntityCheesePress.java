package growthcraft.milk.common.tileentity;

import growthcraft.core.shared.inventory.GrowthcraftInternalInventory;
import growthcraft.core.shared.item.ItemTest;
import growthcraft.core.shared.item.ItemUtils;
import growthcraft.core.shared.tileentity.GrowthcraftTileDeviceBase;
import growthcraft.core.shared.tileentity.device.DeviceBase;
import growthcraft.core.shared.tileentity.device.DeviceInventorySlot;
import growthcraft.core.shared.tileentity.event.TileEventHandler;
import growthcraft.core.shared.tileentity.feature.IItemOperable;
import growthcraft.core.shared.tileentity.feature.ITileProgressiveDevice;
import growthcraft.milk.common.item.ItemBlockHangingCurds;
import growthcraft.milk.common.tileentity.device.CheesePress;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.io.IOException;

public class TileEntityCheesePress extends GrowthcraftTileDeviceBase implements ITickable, IItemOperable, ITileProgressiveDevice {
    private static int[][] accessibleSlots = {
            {0},
            {0},
            {0},
            {0},
            {0},
            {0}
    };

    //	@SideOnly(Side.CLIENT)
//	public boolean isAnim;
    @SideOnly(Side.CLIENT)
    public float animProgress;
    @SideOnly(Side.CLIENT)
    public int animDir;
    private int screwState;
    private DeviceInventorySlot invSlot = new DeviceInventorySlot(this, 0);
    private final CheesePress cheesePress = new CheesePress(this);
    @Override
    public DeviceBase[] getDevices(){return new DeviceBase[]{cheesePress};}

    @Override
    public float getDeviceProgress() {
        return cheesePress.getProgress();
    }

    @Override
    public int getDeviceProgressScaled(int scale) {
        return cheesePress.getProgressScaled(scale);
    }

    public boolean isPressed() {
        return screwState == 1;
    }

    public boolean isUnpressed() {
        return screwState == 0;
    }

    public boolean isAnimating() {
        // return this.isAnim;
        return animProgress > 0 && animProgress < 1;
    }

    @Override
    public void onInventoryChanged(IInventory inv, int index) {
        super.onInventoryChanged(inv, index);
        cheesePress.markForRecipeRecheck();
    }

    @Override
    public GrowthcraftInternalInventory createInventory() {
        return new GrowthcraftInternalInventory(this, 1, 1);
    }

    @Override
    public String getDefaultInventoryName() {
        return "container.grcmilk.CheesePress";
    }

    @Override
    public boolean isItemValidForSlot(int index, ItemStack itemstack) {
        return true;
    }

    @Override
    public int[] getSlotsForFace(EnumFacing side) {
        return accessibleSlots[side.ordinal()];
    }

    @Override
    public boolean canInsertItem(int index, ItemStack stack, EnumFacing side) {
        return isUnpressed() && index == 0;
    }

    @Override
    public boolean canExtractItem(int index, ItemStack stack, EnumFacing side) {
        return isUnpressed() && index == 0;
    }

    @SideOnly(Side.CLIENT)
    private void updateEntityClient() {
        final float step = 1.0f / 20.0f;
        int prevAnimDir = this.animDir;
        if (isUnpressed()) {
            this.animDir = 1;
        } else {
            this.animDir = -1;
        }

//		if( prevAnimDir != this.animDir && animProgress > 0 && animProgress < 1 )
//			animProgress = 1 - animProgress;

        if (animDir > 0 && animProgress < 1.0f || animDir < 0 && animProgress > 0) {
            this.animProgress = MathHelper.clamp(this.animProgress + step * animDir, 0.0f, 1.0f);
        }
		
/*		if( animProgress <= 0 || animProgress >= 1 )
			isAnim = false;
		else
			isAnim = true;*/
    }



    private void updateEntityServer() {
        cheesePress.update();
    }

    @Override
    public void update() {
        if (world.isRemote) {
            updateEntityClient();
        } else {
            updateEntityServer();
        }
    }

    /**
     * Toggles the cheese press state
     *
     * @param state - true: the press should be active; false: it should inactive
     * @return did the state change?
     */
    public boolean toggle(boolean state) {
        final int oldScrewState = screwState;
        this.screwState = state ? 1 : 0;
        if (oldScrewState != screwState) {
//			isAnim = true;
            markDirtyAndUpdate(true);
        }
        return oldScrewState != screwState;
    }

    /**
     * Flip the current press state
     *
     * @return did the state change? (most likely it will)
     */
    public boolean toggle() {
        return toggle(screwState == 0);
    }

    @Override
    public boolean tryPlaceItem(IItemOperable.Action action, EntityPlayer player, ItemStack stack) {
        if (IItemOperable.Action.RIGHT != action) return false;
        if (ItemTest.isValid(stack)) {
            // Items cannot be added if the user slot already has an item AND
            // the press is active
            if (invSlot.isEmpty() && isUnpressed()) {
                if (/*GrowthcraftMilkBlocks.hangingCurds.getItem() == */ stack.getItem() instanceof ItemBlockHangingCurds) {
                    final ItemBlockHangingCurds<?> item = (ItemBlockHangingCurds<?>) stack.getItem();
                    if (item.isDried(stack)) {
                        final ItemStack result = ItemUtils.decrPlayerCurrentInventorySlot(player, 1);
                        invSlot.set(result);
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public boolean tryTakeItem(IItemOperable.Action action, EntityPlayer player, ItemStack onHand) {
        if (IItemOperable.Action.RIGHT != action) return false;
        // Items cannot be removed if the cheese press is active
//		if( !ItemUtils.isEmpty( onHand ) )
//			return false;
        if (!player.isSneaking())
            return false;
        if (isUnpressed()) {
            final ItemStack result = invSlot.yank();
            if (!ItemUtils.isEmpty(result)) {
                ItemUtils.spawnItemStackAtTile(result, this, world.rand);
                return true;
            }
        }
        return false;
    }

    @TileEventHandler(event = TileEventHandler.EventType.NBT_READ)
    public void readFromNBT_CheesePress(NBTTagCompound nbt) {
        this.screwState = nbt.getInteger("screw_state");
    }

    @TileEventHandler(event = TileEventHandler.EventType.NBT_WRITE)
    public void writeToNBT_CheesePress(NBTTagCompound nbt) {
        nbt.setInteger("screw_state", screwState);
    }

    @TileEventHandler(event = TileEventHandler.EventType.NETWORK_READ)
    public boolean readFromStream_CheesePress(ByteBuf stream) throws IOException {
        this.screwState = stream.readInt();
        return false;
    }

    @TileEventHandler(event = TileEventHandler.EventType.NETWORK_WRITE)
    public boolean writeToStream_CheesePress(ByteBuf stream) throws IOException {
        stream.writeInt(screwState);
        return false;
    }
}