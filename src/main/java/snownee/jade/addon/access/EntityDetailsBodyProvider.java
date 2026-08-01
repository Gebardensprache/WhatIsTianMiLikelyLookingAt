package snownee.jade.addon.access;

import java.util.List;

import com.google.common.collect.Lists;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLeashKnot;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.passive.EntityOcelot;
import net.minecraft.entity.passive.EntityTameable;
import net.minecraft.entity.passive.EntityWolf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import snownee.jade.JadeClient;
import snownee.jade.addon.core.DistanceProvider;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.JadeIds;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.ui.JadeUI;
import snownee.jade.api.ui.TextElement;

public class EntityDetailsBodyProvider implements IEntityComponentProvider {
	@Override
	public void appendTooltip(ITooltip tooltip, EntityAccessor accessor, IPluginConfig config) {
		if (!config.get(JadeIds.ACCESS_ENTITY_DETAILS)) {
			return;
		}
		Entity entity = accessor.getEntity();
		int poseId = getPoseId(entity);
		if (poseId != 0) {
			// 1.12.2: Pose.STANDING is 0; the pose language keys below exist for 2 (Sleeping),
			// 3 (Swimming), 5 (Crouching), 7 (Dying), 10 (Sitting), 1000 (Playing Dead) and
			// 1001 (Rolling Up) -- same ids as the modern code.
			String key = "jade.access.entity.pose." + poseId;
			if (JadeUI.hasTranslation(key)) {
				tooltip.add(new TextComponentTranslation("jade.access.entity.pose", new TextComponentTranslation(key)));
			}
		}
		int passengers = entity.getPassengers().size();
		if (passengers > 0) {
			tooltip.add(JadeClient.format("jade.access.entity.passengers", passengers));
		}
		if (entity instanceof EntityLiving && ((EntityLiving) entity).getLeashed()) {
			Entity holder = ((EntityLiving) entity).getLeashHolder();
			if (holder instanceof EntityLeashKnot) {
				// 1.12.2: LeashFenceKnotEntity is EntityLeashKnot; its position doubles as
				// its block position.
				TextElement text = DistanceProvider.xyz(((EntityLeashKnot) holder).getPosition());
				tooltip.add(JadeUI
						.text(new TextComponentTranslation("jade.access.entity.leashed_to", text.getString()))
						.narration(new TextComponentTranslation("jade.access.entity.leashed_to", text.getString())));
			} else if (holder != null) {
				tooltip.add(new TextComponentTranslation("jade.access.entity.leashed_to", holder.getDisplayName()));
			}
		}
		// 1.12.2: the modern Leashable#leashableLeashedTo scan is re-expressed over
		// EntityLiving#getLeashed: every leashed EntityLiving whose leash holder is the
		// target entity (or its leash knot) is "being leashed to" it. This is bounded to a
		// region around the target, which is how the modern implementation bounds it too.
		Entity target = entity;
		List<EntityLiving> leashedTo = null;
		if (target.world != null) {
			AxisAlignedBB box = target.getEntityBoundingBox().grow(7.0D, 7.0D, 7.0D);
			List<EntityLiving> candidates = target.world.getEntitiesWithinAABB(EntityLiving.class, box);
			if (!candidates.isEmpty()) {
				leashedTo = Lists.newArrayList();
				for (EntityLiving candidate : candidates) {
					if (candidate != target && candidate.getLeashed()) {
						Entity holder = candidate.getLeashHolder();
						if (holder == target || (holder instanceof EntityLeashKnot &&
								((EntityLeashKnot) holder).getPosition().equals(target.getPosition()))) {
							leashedTo.add(candidate);
						}
					}
				}
			}
		}
		if (leashedTo != null && !leashedTo.isEmpty()) {
			tooltip.add(new TextComponentTranslation(
					"jade.access.entity.is_leashing",
					joinNames(leashedTo)));
		}
	}

	private static int getPoseId(Entity entity) {
		// 1.12.2: no Pose enum -- the numeric ids are reconstructed from the language keys.
		int poseId = 0;
		if (entity instanceof EntityPlayer) {
			if (((EntityPlayer) entity).isPlayerSleeping()) {
				poseId = 2;
			}
		} else if (entity instanceof EntityLivingBase) {
			if (((EntityLivingBase) entity).isSneaking()) {
				poseId = 5;
			}
		}
		// The modern TamableAnimal/panda/camel/fox sitting branches are replaced by the
		// 1.12.2 sitting-capable animals: EntityTameable#isSitting (wolves, cats,
		// ocelots), EntityWolf and EntityOcelot re-implement it directly. Foxes, pandas,
		// camels, axolotls and armadillos do not exist in 1.12.2 (their pose ids 10, 1000
		// and 1001 are unreachable here); sleeping foxes have no counterpart either.
		if (entity instanceof EntityTameable && ((EntityTameable) entity).isSitting()) {
			poseId = 10;
		} else if (entity instanceof EntityWolf && ((EntityWolf) entity).isSitting()) {
			poseId = 10;
		} else if (entity instanceof EntityOcelot && ((EntityOcelot) entity).isSitting()) {
			poseId = 10;
		}
		return poseId;
	}

	/**
	 * 1.12.2: ComponentUtils.formatList does not exist -- names are joined with ", ".
	 */
	private static ITextComponent joinNames(List<EntityLiving> entities) {
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < entities.size(); i++) {
			if (i > 0) {
				builder.append(", ");
			}
			builder.append(entities.get(i).getDisplayName().getUnformattedText());
		}
		return new TextComponentString(builder.toString());
	}

	@Override
	public ResourceLocation getUid() {
		return JadeIds.ACCESS_ENTITY_DETAILS_BODY;
	}

	@Override
	public boolean isRequired() {
		return true;
	}

	@Override
	public int getDefaultPriority() {
		return 3333;
	}
}
