package cam72cam.immersiverailroading.tile;

import net.minecraft.entity.item.EntityItem;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.ArrayList;
import java.util.List;

import cam72cam.immersiverailroading.Config;
import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.entity.EntityCoupleableRollingStock;
import cam72cam.immersiverailroading.library.Gauge;
import cam72cam.immersiverailroading.library.SwitchState;
import cam72cam.immersiverailroading.library.TrackDirection;
import cam72cam.immersiverailroading.library.TrackItems;
import cam72cam.immersiverailroading.track.BuilderTurn;
import cam72cam.immersiverailroading.track.TrackBase;
import cam72cam.immersiverailroading.track.TrackRail;
import cam72cam.immersiverailroading.util.BlockUtil;
import cam72cam.immersiverailroading.util.PlacementInfo;
import cam72cam.immersiverailroading.util.RailInfo;
import cam72cam.immersiverailroading.util.VecUtil;

public class TileRail extends TileRailBase {
	public static TileRail get(IBlockAccess world, BlockPos pos) {
		TileEntity te = world.getTileEntity(pos);
		return te instanceof TileRail ? (TileRail) te : null;
	}
	
	private TrackItems type;
	private ItemStack railBed;
	
	private Gauge gauge;
	
	private Vec3d center;
	
	private SwitchState switchState = SwitchState.NONE;
	private double tablePos = 0;
	
	private int length;
	private int turnQuarters;
	
	private PlacementInfo placementInfo;
	private PlacementInfo customInfo;
	
	private List<ItemStack> drops;
	

	@Override
	@SideOnly(Side.CLIENT)
	public net.minecraft.util.math.AxisAlignedBB getRenderBoundingBox() {
		int length = this.length;
		if (type == TrackItems.CUSTOM && this.customInfo != null) {
			length = (int) this.customInfo.placementPosition.distanceTo(this.placementInfo.placementPosition);
		}
		return new AxisAlignedBB(-length, -length, -length, length, length, length).offset(pos);
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public double getMaxRenderDistanceSquared() {
		return Math.pow(8*32, 2);
	}

	public EnumFacing getFacing() {
		if (placementInfo.facing == EnumFacing.DOWN) {
			return EnumFacing.NORTH;
		}
		return placementInfo.facing;
	}
	
	public TrackItems getType() {
		return this.type;
	}
	public void setType(TrackItems value) {
		this.type = value;
	}
	public ItemStack getRailBed() {
		return this.railBed;
	}
	public void setRailBed(ItemStack railBed) {
		this.railBed = railBed;
	}
	
	public void setGauge(Gauge gauge) {
		this.gauge = gauge;
	}
	public Gauge getGauge() {
		return this.gauge;
	}
	
	public SwitchState getSwitchState() {
		return switchState;
	}
	public void setSwitchState(SwitchState state) {
		if (state != switchState) {
			this.switchState = state;
			this.markDirty();
		}
	}

	public Vec3d getCenter() {
		if (center == null) {
			Vec3d off = BuilderTurn.followCurve(this.getRailRenderInfo(), 1, null);
			off = VecUtil.rotateYaw(off, placementInfo.facing.getHorizontalAngle() + 90);
			center = new Vec3d(-off.x, 0, -off.z).add(this.getPlacementPosition());
		}
		return center;
	}
	public double getRadius() {
		return length;
	}

	public TrackDirection getDirection() {
		return placementInfo.direction;
	}
	
	
	public Vec3d getPlacementPosition() {
		return placementInfo.placementPosition;
	}
	
	public PlacementInfo getCustomInfo() {
		return customInfo;
	}
	public void setCustomInfo(PlacementInfo customInfo) {
		this.customInfo = customInfo;
	}
	public PlacementInfo getPlacementInfo() {
		return placementInfo;
	}
	public void setPlacementInfo(PlacementInfo placementInfo) {
		this.placementInfo = placementInfo;
	}
	
	
	/*
	 * Either blocks or quarters
	 */
	public int getLength() {
		return this.length;
	}
	public double getSlope() {
		return 1 / this.length;
	}
	public void setLength(int length) {
		this.length = length;
	}

	public int getRotationQuarter() {
		return this.placementInfo.rotationQuarter;
	}

	public int getTurnQuarters() {
		return this.turnQuarters;
	}
	public void setTurnQuarters(int quarters) {
		this.turnQuarters = quarters;
	}
	
	public double getTablePos() {
		return tablePos;
	}
	public void nextTablePos(boolean back) {
		tablePos = (tablePos + 1.0/length * (back ? 1 : -1)) % 8;
		this.markDirty();
		
		List<EntityCoupleableRollingStock> ents = world.getEntitiesWithinAABB(EntityCoupleableRollingStock.class, new AxisAlignedBB(-length, 0, -length, length, 5, length).offset(this.getPos()));
		for(EntityCoupleableRollingStock stock : ents) {
			stock.triggerResimulate();
		}
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);
		
		int version = 0;
		if (nbt.hasKey("version")) {
			version = nbt.getInteger("version");
		}
		
		
		type = TrackItems.valueOf(nbt.getString("type"));
		
		switchState = SwitchState.values()[nbt.getInteger("switchState")];
		
		length = nbt.getInteger("length");
		turnQuarters = nbt.getInteger("turnQuarters");
		
		this.drops = new ArrayList<ItemStack>();
		if (nbt.hasKey("drops")) {
			NBTTagCompound dropNBT = nbt.getCompoundTag("drops");
			int count = dropNBT.getInteger("count");
			for (int i = 0; i < count; i++) {
				drops.add(new ItemStack(dropNBT.getCompoundTag("drop_" + i)));
			}
		}
		switch(version) {
		case 0:
			// Add missing railbed setting
			nbt.setTag("railBed", new ItemStack(Blocks.GRAVEL).serializeNBT());
		case 1:
			// Convert positions to relative
			if (getNBTVec3d(nbt, "center") != null) {
				setNBTVec3d(nbt, "center", getNBTVec3d(nbt, "center").subtract(pos.getX(), pos.getY(), pos.getZ()));
			}
			setNBTVec3d(nbt, "placementPosition", getNBTVec3d(nbt, "placementPosition").subtract(pos.getX(), pos.getY(), pos.getZ()));
		case 2:
			nbt.setDouble("gauge", Gauge.STANDARD);
		}
		
		railBed = new ItemStack(nbt.getCompoundTag("railBed"));
		gauge = Gauge.from(nbt.getDouble("gauge"));
		tablePos = nbt.getDouble("tablePos");
		
		if (nbt.hasKey("placementInfo")) {
			placementInfo = new PlacementInfo(nbt.getCompoundTag("placementInfo"), pos);
		} else {
			//Legacy
			placementInfo = new PlacementInfo(nbt, pos);
		}
		
		if (nbt.hasKey("customInfo")) {
			customInfo = new PlacementInfo(nbt.getCompoundTag("customInfo"), pos);
		}
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
		if (placementInfo == null) {
			// Something is wrong...
			ImmersiveRailroading.error("INVALID TILE SAVE");
			return super.writeToNBT(nbt);
		}
		nbt.setString("type", type.name());
		
		
		nbt.setInteger("switchState", switchState.ordinal());
		
		nbt.setInteger("length", length);
		nbt.setInteger("turnQuarters", turnQuarters);
		
		if (drops != null && drops.size() != 0) {
			NBTTagCompound dropNBT = new NBTTagCompound();
			dropNBT.setInteger("count", drops.size());
			for (int i = 0; i < drops.size(); i++) {
				dropNBT.setTag("drop_" + i, drops.get(i).serializeNBT());
			}
			nbt.setTag("drops", dropNBT);
		}
		
		nbt.setTag("railBed", railBed.serializeNBT());
		nbt.setTag("placementInfo", placementInfo.toNBT(pos));
		if (customInfo != null) {
			nbt.setTag("customInfo", customInfo.toNBT(pos));
		}
		nbt.setDouble("gauge", gauge.value());
		
		nbt.setDouble("tablePos", tablePos);
		
		
		return super.writeToNBT(nbt);
	}

	private RailInfo info;
	public RailInfo getRailRenderInfo() {
		if (!hasTileData && world.isRemote) {
			return null;
		}
		if (info == null) {
			info = new RailInfo(getWorld(), getPlacementInfo(), getCustomInfo(), getType(),getLength(), getTurnQuarters(), getGauge(), getRailBed(), ItemStack.EMPTY, null, 0, false);
		}
		// Changes moment to moment
		if (info.switchState != switchState || info.tablePos != tablePos) {
			info.renderIdCache = null;
		}
		info.switchState = switchState;
		info.tablePos = tablePos;
		
		return info;
	}


	public void setDrops(List<ItemStack> drops) {
		this.drops = drops;
	}
	public void spawnDrops() {
		if (!world.isRemote) {
			if (drops != null && drops.size() != 0) {
				for(ItemStack drop : drops) {
					world.spawnEntity(new EntityItem(world, pos.getX(), pos.getY(), pos.getZ(), drop));
				}
				drops = new ArrayList<ItemStack>();
			}
		}
	}

	private List<TrackBase> trackCheckCache;
	public double percentFloating() {
		if (trackCheckCache == null) {
			RailInfo buildInfo = this.getRailRenderInfo().clone();
			if (info == null) {
				return 0;
			}
			
			trackCheckCache = buildInfo.getBuilder().getTracksForRender();
		}
		
		double floating = 0;
		
		BlockPos offset = null;
		for (TrackBase track : trackCheckCache) {
			if (track instanceof TrackRail) {
				offset = this.getPos().subtract(new BlockPos(track.getPos().getX(), 0, track.getPos().getZ()));
				break;
			}
		}
		
		for (TrackBase track : trackCheckCache) {
			BlockPos tpos = track.getPos().down().add(offset);
			if (!world.isBlockLoaded(tpos)) {
				return 0;
			}
			boolean isOnRealBlock = world.isSideSolid(tpos, EnumFacing.UP, false) ||
					!Config.ConfigDamage.requireSolidBlocks && !world.isAirBlock(tpos) ||
					BlockUtil.isIRRail(world, tpos);
			if (!isOnRealBlock) {
				floating += 1.0 / trackCheckCache.size();
			}
		}
		return floating;
	}
}
