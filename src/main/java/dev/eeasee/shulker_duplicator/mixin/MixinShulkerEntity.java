package dev.eeasee.shulker_duplicator.mixin;

import dev.eeasee.shulker_duplicator.mixin_interface.IShulkerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.mob.ShulkerEntity;
import net.minecraft.entity.passive.GolemEntity;
import net.minecraft.util.DyeColor;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ShulkerEntity.class)
public class MixinShulkerEntity extends GolemEntity implements IShulkerEntity {

    protected MixinShulkerEntity(EntityType<? extends GolemEntity> entityType, World world) {
        super(entityType, world);
    }

    @Inject(method = "damage", at = @At(value = "RETURN", ordinal = 1))
    private void trySpawnNewShulker(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {


        System.out.println("Shulker shot!");

        if ((double) this.getHealth() < (double) this.getMaxHealth() * 0.5 && this.random.nextInt(4) == 0) {
        } else if (source.isProjectile() && (source.getSource()) != null && source.getSource().getType() == EntityType.SHULKER_BULLET) {
            this.spawnNewShulker();
        }
    }

    private void spawnNewShulker() {
        Vec3d vec3d = this.getPos();
        Box box = this.getBoundingBox();
        if (this.isClosed() || !this.tryTeleport()) {
            return;
        }
        int i = this.world.getEntitiesByType(EntityType.SHULKER, box.expand(8.0), Entity::isAlive).size();
        float f = (float) (i - 1) / 5.0f;
        if (this.world.random.nextFloat() < f) {
            return;
        }
        ShulkerEntity shulkerEntity = EntityType.SHULKER.create(this.world);
        DyeColor dyeColor = this.getColor();
        if (dyeColor != null) {
            ((IShulkerEntity) shulkerEntity).setColor(dyeColor);
        }
        shulkerEntity.refreshPositionAfterTeleport(vec3d);
        this.world.spawnEntity(shulkerEntity);
    }


    @Shadow
    private DyeColor getColor() {
        return null;
    }

    @Shadow
    private boolean tryTeleport() {
        return false;
    }

    @Shadow
    private boolean isClosed() {
        return false;
    }

    @Shadow
    @Final
    protected static TrackedData<Byte> COLOR;

    @Override
    public void setColor(DyeColor color) {
        this.dataTracker.set(COLOR, (byte) color.getId());
    }
}
