package eu.pb4.sus.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import net.minecraft.server.network.EntityTrackerEntry;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Consumer;

@Mixin(EntityTrackerEntry.class)
public class EntityTrackerEntryMixin {
    @Shadow @Mutable
    private int tickInterval;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void updateEveryTick(ServerWorld world, Entity entity, int tickInterval, boolean alwaysUpdateVelocity, Consumer receiver, CallbackInfo ci) {
        this.tickInterval = 1;
    }

    @Redirect(method = "sendPackets", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;createSpawnPacket()Lnet/minecraft/network/packet/Packet;"))
    private Packet<?> createStandardPacket(Entity instance) {
        return new EntitySpawnS2CPacket(instance);
    }
}
