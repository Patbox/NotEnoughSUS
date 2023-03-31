package eu.pb4.sus.mixin;

import com.mojang.datafixers.util.Pair;
import eu.pb4.polymer.core.api.entity.PolymerEntity;
import eu.pb4.polymer.core.api.utils.PolymerKeepModel;
import eu.pb4.polymer.virtualentity.api.VirtualEntityUtils;
import eu.pb4.polymer.virtualentity.api.tracker.EntityTrackedData;
import eu.pb4.sus.SuspiciousModel;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.entity.*;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.EntityEquipmentUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityPassengersSetS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.EntityTrackingListener;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

@Mixin(Entity.class)
public abstract class EntityMixin implements PolymerEntity, PolymerKeepModel {
    @Shadow @Final private static TrackedData<Boolean> NO_GRAVITY;

    @Shadow public abstract EntityDimensions getDimensions(EntityPose pose);

    @Shadow public abstract EntityPose getPose();

    private SuspiciousModel model;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void initializeSus(EntityType type, World world, CallbackInfo ci) {
        if (world instanceof ServerWorld) {
            this.model = new SuspiciousModel((Entity) (Object) this);
        } else {
            this.model = null;
        }
    }

    @Redirect(method = "isLogicalSideForUpdatingMovement", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerEntity;isMainPlayer()Z"))
    private boolean makeEntitiesControllable(PlayerEntity instance) {
        return true;
    }

    @Inject(method = "setPose", at = @At("HEAD"))
    private void updateSize(EntityPose pose, CallbackInfo ci) {
        if (model != null) {
            this.model.setSize(this.getDimensions(pose));
        }
    }

    @Inject(method = "calculateDimensions", at = @At("HEAD"))
    private void updateSize2(CallbackInfo ci) {
        if (model != null) {
            this.model.setSize(this.getDimensions(this.getPose()));
        }
    }

    @Inject(method = "readNbt", at = @At("TAIL"))
    private void updateSize2(NbtCompound nbt, CallbackInfo ci) {
        if (model != null) {
            this.model.setSize(this.getDimensions(this.getPose()));
            this.model.updateTexture();
        }
    }

    @Inject(method = "setUuid", at = @At("TAIL"))
    private void updateTexture(UUID uuid, CallbackInfo ci) {
        if (model != null) {
            this.model.updateTexture();
        }
    }

    @Override
    public void onEntityTrackerTick(Set<EntityTrackingListener> listeners) {
        if (model != null) {
            this.model.tick();
        }
    }


    @Override
    public EntityType<?> getPolymerEntityType(ServerPlayerEntity player) {
        return EntityType.ARMOR_STAND;
    }

    @Override
    public List<Pair<EquipmentSlot, ItemStack>> getPolymerVisibleEquipment(List<Pair<EquipmentSlot, ItemStack>> items, ServerPlayerEntity player) {
        return List.of();
    }

    @Override
    public void onEntityPacketSent(Consumer<Packet<?>> consumer, Packet<?> packet) {
        if (packet instanceof EntityEquipmentUpdateS2CPacket) {
            return;
        }

        if (packet instanceof EntityPassengersSetS2CPacket passengersSetS2CPacket && model != null) {
            consumer.accept(VirtualEntityUtils.createRidePacket(this.model.rideAnchor.getEntityId(), IntList.of(passengersSetS2CPacket.getPassengerIds())));
            return;
        }

        consumer.accept(packet);
    }

    @Override
    public void modifyRawTrackedData(List<DataTracker.SerializedEntry<?>> data, ServerPlayerEntity player, boolean initial) {
        if ((Object) this == player) {
            return;
        }

        data.clear();
        data.add(DataTracker.SerializedEntry.of(EntityTrackedData.FLAGS, (byte) (1 << EntityTrackedData.INVISIBLE_FLAG_INDEX)));
        data.add(DataTracker.SerializedEntry.of(NO_GRAVITY, true));
        data.add(DataTracker.SerializedEntry.of(ArmorStandEntity.ARMOR_STAND_FLAGS, (byte) (ArmorStandEntity.SMALL_FLAG | ArmorStandEntity.MARKER_FLAG)));
    }
}
