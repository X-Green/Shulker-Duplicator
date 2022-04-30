package dev.eeasee.shulker_duplicator.mixin;

import dev.eeasee.shulker_duplicator.mixin_interface.IShulkerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.goal.GoalSelector;
import net.minecraft.entity.ai.goal.RevengeGoal;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.mob.ShulkerEntity;
import net.minecraft.entity.passive.GolemEntity;
import net.minecraft.util.DyeColor;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(ShulkerEntity.class)
public class MixinShulkerEntity extends GolemEntity implements IShulkerEntity {

    private boolean isTryingTeleportOnDamage = false;

    protected MixinShulkerEntity(EntityType<? extends GolemEntity> entityType, World world) {
        super(entityType, world);
    }

    @Inject(method = "damage", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/mob/ShulkerEntity;tryTeleport()Z"))
    private void onTryTeleport(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        this.isTryingTeleportOnDamage = true;
    }

    @Inject(method = "damage", at = @At(value = "RETURN", ordinal = 1))
    private void trySpawnNewShulker(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (isTryingTeleportOnDamage) {
            isTryingTeleportOnDamage = false;
        } else {
            Entity entity;
            if (source.isProjectile() && (entity = source.getSource()) != null && entity.getType() == EntityType.SHULKER_BULLET) {
                this.spawnNewShulker();
            }
        }
    }


    /**
     * @author eeasee
     * To implement stay judgement of 1.17+
     */
    @Redirect(method = "canStay", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/mob/ShulkerLidCollisions;getLidCollisionBox(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/util/math/Direction;)Lnet/minecraft/util/math/Box;"))
    private Box getFullLidCollisionBox(BlockPos pos, Direction direction) {
        return VoxelShapes.fullCube().getBoundingBox()
                .stretch(1 * (float) direction.getOffsetX(), 1 * (float) direction.getOffsetY(), 1 * (float) direction.getOffsetZ())
                .shrink(direction.getOffsetX(), direction.getOffsetY(), direction.getOffsetZ())
                .offset(pos.offset(direction))
                .contract(1e-6);
    }


    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Ljava/util/List;isEmpty()Z"))
    private boolean abortNearbyShulkerSearch(List instance) {
        return true;
    }

    @Redirect(method = "initGoals", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/ai/goal/GoalSelector;add(ILnet/minecraft/entity/ai/goal/Goal;)V", ordinal = 4))
    private void addExclusiveRevengeGoal(GoalSelector instance, int priority, Goal goal) {
        instance.add(priority, new RevengeGoal(this, ShulkerEntity.class).setGroupRevenge(new Class[0]));
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
