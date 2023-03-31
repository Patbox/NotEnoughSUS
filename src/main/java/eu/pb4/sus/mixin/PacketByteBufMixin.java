package eu.pb4.sus.mixin;

import com.mojang.authlib.properties.Property;
import eu.pb4.sus.SusColorus;
import net.minecraft.network.PacketByteBuf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(PacketByteBuf.class)
public class PacketByteBufMixin {
    /*@ModifyVariable(method = "writeProperty", at = @At("HEAD"))
    private Property swapTexture(Property property) {
        if (property.getName().equals("textures")) {
            return new Property("textures", SusColorus.RED.texture);
        } else {
            return property;
        }
    }*/
}
