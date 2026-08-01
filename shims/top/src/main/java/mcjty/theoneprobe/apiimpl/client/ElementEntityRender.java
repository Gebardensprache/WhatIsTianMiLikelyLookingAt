package mcjty.theoneprobe.apiimpl.client;

import mcjty.theoneprobe.api.IEntityStyle;
import mcjty.theoneprobe.rendering.RenderHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.datafix.fixes.EntityId;
import net.minecraftforge.fml.common.registry.EntityEntry;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

/**
 * 1.12.2 backport: verbatim from The One Probe (mcjty/theoneprobe).
 */
public class ElementEntityRender {

    public static void renderPlayer(String entityName, Integer playerID, IEntityStyle style, int x, int y) {
        Entity entity = Minecraft.getMinecraft().world.getEntityByID(playerID);
        if (entity != null) {
            renderEntity(style, x, y, entity);
        }
    }

    public static void render(String entityName, NBTTagCompound entityNBT, IEntityStyle style, int x, int y) {
        if (entityName != null && !entityName.isEmpty()) {
            Entity entity = null;
            if (entityNBT != null) {
                entity = EntityList.createEntityFromNBT(entityNBT, Minecraft.getMinecraft().world);
            } else {
                String fixed = fixEntityId(entityName);
                EntityEntry value = ForgeRegistries.ENTITIES.getValue(new ResourceLocation(fixed));
                if (value != null) {
                    entity = value.newInstance(Minecraft.getMinecraft().world);
                }
            }
            if (entity != null) {
                renderEntity(style, x, y, entity);
            }
        }
    }

    private static final EntityId FIXER = new EntityId();

    public static String fixEntityId(String id) {
        NBTTagCompound nbt = new NBTTagCompound();
        nbt.setString("id", id);
        nbt = FIXER.fixTagCompound(nbt);
        return nbt.getString("id");
    }

    private static void renderEntity(IEntityStyle style, int x, int y, Entity entity) {
        float height = entity.height;
        height = (float) ((height - 1) * .7 + 1);
        float s = style.getScale() * ((style.getHeight() * 14.0f / 25) / height);

        RenderHelper.renderEntity(entity, x, y, s);
    }

}
