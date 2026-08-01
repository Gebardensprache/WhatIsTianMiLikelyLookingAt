package snownee.jade.overlay;

import java.util.Objects;
import java.util.function.Predicate;

import org.jspecify.annotations.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import snownee.jade.api.config.IWailaConfig;
import snownee.jade.impl.WailaCommonRegistration;
import snownee.jade.util.CommonProxy;

/**
 * 1.12.2 translation notes:
 * <ul>
 *   <li>{@code Camera} is dropped; eye position and look vector are obtained from
 *       the render-view entity.</li>
 *   <li>{@code ClipContext} is replaced with {@link World#rayTraceBlocks(Vec3d, Vec3d, boolean, boolean, boolean)}.</li>
 *   <li>{@code BlockHitResult}/{@code EntityHitResult} are unified into {@link RayTraceResult}.</li>
 *   <li>{@code Vec3} -> {@link Vec3d}, {@code AABB} -> {@link AxisAlignedBB}.</li>
 *   <li>{@code EntityTypes.ITEM} -> {@code instanceof EntityItem}.</li>
 *   <li>{@code blockInteractionRange()}/{@code entityInteractionRange()} -> {@code EntityPlayer.REACH_DISTANCE}
 *       attribute lookup.</li>
 *   <li>Frozen-projectile check (<code>tickRateManager().isEntityFrozen(target)</code>) dropped
 *       (1.12.2 has no {@code TickRateManager} equivalent).</li>
 * </ul>
 */
public class RayTracing {

	public static final RayTracing INSTANCE = new RayTracing();
	private final Minecraft mc = Minecraft.getMinecraft();
	public Predicate<Entity> entityFilter = entity -> true;
	@Nullable
	private RayTraceResult target;
	private Vec3d hitLocation = Vec3d.ZERO;

	public RayTracing() {
	}

	// from ProjectileUtil (adapted to 1.12.2's AxisAlignedBB API)
	@Nullable
	public static RayTraceResult getEntityHitResult(
			World worldIn,
			Vec3d startVec,
			Vec3d endVec,
			AxisAlignedBB boundingBox,
			Predicate<Entity> filter) {
		double d0 = Double.MAX_VALUE;
		Entity entity = null;

		com.google.common.base.Predicate<Entity> guavaFilter = new com.google.common.base.Predicate<Entity>() {
			@Override
			public boolean apply(Entity input) {
				return filter.test(input);
			}
		};
		for (Entity entity1 : worldIn.getEntitiesWithinAABB(Entity.class, boundingBox, guavaFilter)) {
			AxisAlignedBB axisalignedbb = entity1.getEntityBoundingBox();
			double size = Math.min(axisalignedbb.maxX - axisalignedbb.minX,
					Math.min(axisalignedbb.maxY - axisalignedbb.minY, axisalignedbb.maxZ - axisalignedbb.minZ));
			if (size < 0.3) {
				axisalignedbb = axisalignedbb.grow(0.3);
			}
			if (axisalignedbb.contains(startVec)) {
				entity = entity1;
				break;
			}
			RayTraceResult intercept = axisalignedbb.calculateIntercept(startVec, endVec);
			if (intercept != null) {
				double d1 = startVec.squareDistanceTo(intercept.hitVec);
				if (d1 < d0) {
					entity = entity1;
					d0 = d1;
				}
			}
		}

		return entity == null ? null : new RayTraceResult(entity, entity.getPositionVector());
	}

	public void fire() {
		Entity viewEntity = mc.getRenderViewEntity();
		EntityPlayer viewPlayer = viewEntity instanceof EntityPlayer ? (EntityPlayer) viewEntity : mc.player;
		if (viewEntity == null || viewPlayer == null) {
			return;
		}

		if (mc.objectMouseOver != null && mc.objectMouseOver.typeOfHit == RayTraceResult.Type.ENTITY) {
			Entity targetEntity = mc.objectMouseOver.entityHit;
			if (canBeTarget(targetEntity, viewEntity)) {
				target = mc.objectMouseOver;
				return;
			}
		}

		float extendedReach = IWailaConfig.get().general().getExtendedReach();
		double reach = viewPlayer.getEntityAttribute(EntityPlayer.REACH_DISTANCE).getAttributeValue() + extendedReach;
		double blockReach = reach;
		double entityReach = reach;
		rayTrace(viewEntity, blockReach, entityReach);
		if (target != null) {
			hitLocation = target.hitVec;
		}
	}

	@Nullable
	public RayTraceResult getTarget() {
		return target;
	}

	public Vec3d getHitLocation() {
		return hitLocation;
	}

	public void rayTrace(Entity entity, double blockReach, double entityReach) {
		float partialTick = mc.timer.renderPartialTicks;
		Vec3d eyePosition = entity.getPositionEyes(partialTick);
		boolean startFromEye = IWailaConfig.get().general().getPerspectiveMode() == IWailaConfig.PerspectiveMode.EYE;
		Vec3d traceStart = startFromEye ? eyePosition : entity.getPositionEyes(partialTick);
		double distance = startFromEye ? 0 : eyePosition.squareDistanceTo(traceStart);
		if (distance > 1e-5) {
			distance = Math.sqrt(distance);
			blockReach += distance;
			entityReach += distance;
		}

		Vec3d traceEnd;
		Vec3d lookVector;
		if (mc.objectMouseOver == null) {
			lookVector = entity.getLook(partialTick);
			traceEnd = traceStart.add(lookVector.scale(entityReach));
		} else {
			traceEnd = mc.objectMouseOver.hitVec.subtract(traceStart);
			lookVector = entity.getLook(partialTick);
			double traceEndLenSq = traceEnd.x * traceEnd.x + traceEnd.y * traceEnd.y + traceEnd.z * traceEnd.z;
			// when it comes to a block hit, we only need to find entities that closer than the block
			if (mc.objectMouseOver.typeOfHit == RayTraceResult.Type.BLOCK && traceEndLenSq < entityReach * entityReach) {
				double traceLen = Math.sqrt(traceEndLenSq);
				traceEnd = traceStart.add(lookVector.scale(traceLen + 1e-5));
			} else {
				traceEnd = traceStart.add(lookVector.scale(entityReach));
			}
		}

		World world = entity.world;
		AxisAlignedBB bound = new AxisAlignedBB(traceStart, traceEnd);
		Predicate<Entity> predicate = e -> canBeTarget(e, entity);
		RayTraceResult entityResult = getEntityHitResult(world, traceStart, traceEnd, bound, predicate);

		if (blockReach != entityReach) {
			traceEnd = traceStart.add(lookVector.scale(blockReach * 1.001));
		}

		boolean stopOnLiquid = false;
		IWailaConfig.FluidMode fluidMode = IWailaConfig.get().general().getDisplayFluids();
		if (fluidMode == IWailaConfig.FluidMode.ANY) {
			stopOnLiquid = true;
		}
		boolean ignoreBlockWithoutBoundingBox = !IWailaConfig.get().general().getPerspectiveMode().name().equals("CAMERA");

		// 1.12.2: World.rayTraceBlocks instead of ClipContext
		RayTraceResult blockResult = world.rayTraceBlocks(traceStart, traceEnd, stopOnLiquid, ignoreBlockWithoutBoundingBox, false);
		hitLocation = blockResult != null ? blockResult.hitVec : traceEnd;
		if (entityResult != null) {
			if (blockResult != null && blockResult.typeOfHit == RayTraceResult.Type.BLOCK) {
				double entityDist = entityResult.hitVec.squareDistanceTo(traceStart);
				double blockDist = blockResult.hitVec.squareDistanceTo(traceStart);
				if (entityDist < blockDist) {
					target = entityResult;
					return;
				}
			} else {
				target = entityResult;
				return;
			}
		}
		if (blockResult == null || blockResult.typeOfHit == RayTraceResult.Type.MISS) {
			if (mc.objectMouseOver instanceof RayTraceResult && mc.objectMouseOver.typeOfHit == RayTraceResult.Type.BLOCK) {
				// weird, we didn't hit a block in our way. try the vanilla result
				blockResult = mc.objectMouseOver;
			}
		}
		if (blockResult == null || blockResult.typeOfHit != RayTraceResult.Type.BLOCK) {
			blockResult = null;
		}
		if (blockResult == null && fluidMode == IWailaConfig.FluidMode.FALLBACK) {
			blockResult = world.rayTraceBlocks(traceStart, traceEnd, true, ignoreBlockWithoutBoundingBox, false);
			hitLocation = blockResult != null ? blockResult.hitVec : traceEnd;
		}

		target = blockResult;
	}

private boolean canBeTarget(Entity target, Entity viewEntity) {
			if (target.isDead) {
			return false;
		}
		// 1.12.2: modern getEntityHitResult passes the view entity as the excluded
		// "projectile" to Level#getEntities(except, ...), so the player is never tested
		// against its own ray. getEntitiesWithinAABB has no such exclusion, and the ray
		// starts inside the player's own bounding box, so without this the probe targets
		// the player itself whenever nothing else is aimed at.
		if (target == viewEntity) {
			return false;
		}
		if (target instanceof EntityPlayer && ((EntityPlayer) target).isSpectator()) {
			return false;
		}
		if (target == viewEntity.getRidingEntity()) {
			return false;
		}
		if (target.ticksExisted <= 10) {
			return false;
		}
		if (CommonProxy.isMultipartEntity(target) && !target.canBeCollidedWith()) {
			return false;
		}
		if (viewEntity instanceof EntityPlayer) {
			EntityPlayer player = (EntityPlayer) viewEntity;
			if (target.isInvisibleToPlayer(player)) {
				return false;
			}
			if (mc.playerController != null && mc.playerController.getIsHittingBlock() && target instanceof EntityItem) {
				return false;
			}
		} else {
			if (target.isInvisible()) {
				return false;
			}
		}
		return !WailaCommonRegistration.instance().entityTypeOperations().shouldHide(target) && entityFilter.test(target);
	}

}
