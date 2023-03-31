package eu.pb4.sus;

import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.VirtualEntityUtils;
import eu.pb4.polymer.virtualentity.api.attachment.EntityAttachment;
import eu.pb4.polymer.virtualentity.api.elements.*;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.entity.*;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.entity.mob.EndermanEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4x3fStack;
import org.joml.Vector3f;

import java.util.function.Consumer;

public class SuspiciousModel extends ElementHolder {
    private final ItemDisplayElement leftLeg = new ItemDisplayElement();
    private final ItemDisplayElement rightLeg = new ItemDisplayElement();
    private final ItemDisplayElement leftHand = new ItemDisplayElement();
    private final ItemDisplayElement rightHand = new ItemDisplayElement();

    private final ItemDisplayElement torso = new ItemDisplayElement();
    private final InteractionElement interaction;
    public final GenericEntityElement rideAnchor = new MarkerElement();
    private final Entity entity;
    private Vec3d pos;
    private Vector3f scale = new Vector3f();

    private Matrix4x3fStack stack = new Matrix4x3fStack(8);
    private float previousSpeed = Float.MIN_NORMAL;
    private float previousLimbPos = Float.MIN_NORMAL;
    private float previousYaw = Float.MIN_NORMAL;
    private float deathAngle;

    public SuspiciousModel(Entity sourceEntity) {
        this.entity = sourceEntity;
        this.pos = sourceEntity.getPos();

        this.rideAnchor.setOffset(new Vec3d(0, 1.3f, 0));

        leftLeg.setInterpolationDuration(2);
        rightLeg.setInterpolationDuration(2);
        torso.setInterpolationDuration(2);
        leftHand.setInterpolationDuration(2);
        rightHand.setInterpolationDuration(2);
        leftHand.setModelTransformation(ModelTransformationMode.THIRD_PERSON_RIGHT_HAND);
        rightHand.setModelTransformation(ModelTransformationMode.THIRD_PERSON_RIGHT_HAND);
        if (sourceEntity.canHit() || sourceEntity instanceof EnderDragonEntity) {
            this.interaction = InteractionElement.redirect(sourceEntity);
            this.addElement(interaction);
        } else {
            this.interaction = null;
        }
        this.setSize(sourceEntity.getDimensions(sourceEntity.getPose()));
        try {
            this.updateAnimation();
        } catch (Throwable e) {
            e.printStackTrace();
        }

        this.addElement(leftLeg);
        this.addElement(rightLeg);
        this.addElement(torso);
        this.addElement(rideAnchor);
        this.addElement(leftHand);
        this.addElement(rightHand);

        this.updateTexture();

        new EntityAttachment(this, sourceEntity, false);
    }

    public void updateTexture() {
        var x = SusColorus.selectFor(entity.getUuid());
        this.leftLeg.setItem(x.legs.getDefaultStack());
        this.rightLeg.setItem(x.legs.getDefaultStack());
        this.torso.setItem(x.textureHead);
    }

    public void setSize(EntityDimensions dimensions) {
        try {
            if (this.interaction != null) {
                this.interaction.setSize(dimensions.width, dimensions.height);
            }
            this.rideAnchor.setOffset(new Vec3d(0, this.entity.getMountedHeightOffset(), 0));
            scale.set(Math.sqrt(dimensions.width * dimensions.height / 2));
            this.torso.setDisplayHeight(dimensions.height);
            this.torso.setDisplayWidth(dimensions.width);
            this.leftLeg.setDisplayHeight(dimensions.height);
            this.leftLeg.setDisplayWidth(dimensions.width);
            this.rightLeg.setDisplayHeight(dimensions.height);
            this.rightLeg.setDisplayWidth(dimensions.width);
            this.leftHand.setDisplayHeight(dimensions.height + 0.2f);
            this.leftHand.setDisplayWidth(dimensions.width + 0.2f);
            this.rightHand.setDisplayHeight(dimensions.height + 0.2f);
            this.rightHand.setDisplayWidth(dimensions.width + 0.2f);
            this.deathAngle = -1;
        } catch (Throwable e) {

        }
    }

    @Override
    protected void startWatchingExtraPackets(ServerPlayNetworkHandler player, Consumer<Packet<ClientPlayPacketListener>> packetConsumer) {
        super.startWatchingExtraPackets(player, packetConsumer);

        packetConsumer.accept(VirtualEntityUtils.createRidePacket(this.entity.getId(), IntList.of(
                this.leftLeg.getEntityId(),
                this.leftHand.getEntityId(),
                this.rightLeg.getEntityId(),
                this.rightHand.getEntityId(),
                this.torso.getEntityId(),
                this.interaction != null ? this.interaction.getEntityId() : this.leftLeg.getEntityId()
        )));
    }

    @Override
    protected void notifyElementsOfPositionUpdate(Vec3d newPos, Vec3d delta) {
        this.rideAnchor.notifyMove(this.currentPos, newPos, delta);
    }

    @Override
    public Vec3d getPos() {
        return this.getAttachment().getPos();
    }

    @Override
    public void onTick() {

        if (this.entity.age % 2 == 1) {
            return;
        }
        try {

            this.setSize(this.entity.getDimensions(this.entity.getPose()));

            this.updateAnimation();
        } catch (Throwable e) {

        }
    }

    private void updateAnimation() {
        float speed;
        float limbPos;
        float deathAngle;

        if (this.entity instanceof PlayerEntity player && player.limbAnimator != null) {
            var movDelta = this.pos.subtract(player.getPos());
            speed = (float) movDelta.horizontalLength();
            limbPos = this.previousLimbPos + speed * MathHelper.PI;
            deathAngle = player.deathTime / 20.0F * 1.6F;
        } else if (this.entity instanceof LivingEntity l && l.limbAnimator != null) {
            speed = l.limbAnimator.getSpeed();
            limbPos = l.limbAnimator.getPos();
            deathAngle = l.deathTime / 20.0F * 1.6F;
        } else {
            speed = 0;
            limbPos = 0;
            deathAngle = 0;
        }
        this.pos = this.entity.getPos();
        deathAngle = MathHelper.sqrt(deathAngle);
        if (deathAngle > 1.0F) {
            deathAngle = 1.0F;
        }
        try {
            if (this.entity instanceof EndermanEntity endermanEntity) {
                if (endermanEntity.getCarriedBlock().isAir()) {
                    rightHand.setItem(ItemStack.EMPTY);
                } else {
                    rightHand.setItem(endermanEntity.getCarriedBlock().getBlock().asItem().getDefaultStack());
                }
            } else if (this.entity instanceof LivingEntity livingEntity) {
                    var a = livingEntity.getMainHandStack();
                    rightHand.setItem(a.isEmpty() ? ItemStack.EMPTY : a);
                    a = livingEntity.getOffHandStack();
                    leftHand.setItem(a.isEmpty() ? ItemStack.EMPTY : a);

            } else if (this.entity instanceof ItemEntity entity) {
                rightHand.setItem(entity.getStack());

            } else if (this.entity instanceof ItemFrameEntity entity) {
                rightHand.setItem(entity.getHeldItemStack());
            }
        } catch (Throwable e) {

        }

        if (this.deathAngle == deathAngle && speed == this.previousSpeed && limbPos == this.previousLimbPos
                && this.entity.getYaw() == this.previousYaw
        ) {
            return;
        }

        var height = this.entity.getHeight() / this.scale.y;
        this.deathAngle = deathAngle;
        this.previousSpeed = speed;
        this.previousLimbPos = limbPos;
        this.previousYaw = this.entity.getYaw();

        stack.clear();

        if (this.entity instanceof ItemEntity ) {
            stack.translate(0, MathHelper.sin(MathHelper.wrapDegrees(this.entity.age * 5) * MathHelper.RADIANS_PER_DEGREE) * 0.05f + 0.1f, 0);
            stack.rotateY(MathHelper.wrapDegrees(-this.entity.age * 5) * MathHelper.RADIANS_PER_DEGREE);
        } else if (this.entity instanceof EndCrystalEntity) {
            stack.translate(0, MathHelper.sin(MathHelper.wrapDegrees(this.entity.age * 5) * MathHelper.RADIANS_PER_DEGREE) * 0.25f + 0.25f, 0);
            stack.rotateY(MathHelper.wrapDegrees(-this.entity.age * 3) * MathHelper.RADIANS_PER_DEGREE);
        } else if (this.entity instanceof EnderDragonEntity) {
            stack.rotateY(MathHelper.PI);
        }

        stack.scale(scale);

        stack.rotateY((float) Math.toRadians(180.0F * 3 - this.previousYaw));
        if (deathAngle > 0) {
            stack.rotate(RotationAxis.POSITIVE_Z.rotation(deathAngle * MathHelper.HALF_PI));
        }

        stack.pushMatrix();
        stack.rotateY(MathHelper.PI);
        stack.translate(0, height - 0.1f, 0);
        stack.scale(2f);
        try {
            stack.scale(this.entity.getDimensions(EntityPose.STANDING).height / this.torso.getDisplayHeight());
        } catch (Throwable e) {

        }
        torso.setTransformation(stack);

        stack.popMatrix();

        var legHeight = height - 1f;
        var legWidth = this.entity instanceof EndermanEntity ? 0.25f : 0.4f;

        stack.pushMatrix();
        stack
                .translate(-0.55f, legHeight + 0.2f, -0.6f);
        ;

        if (leftHand.getItem().isOf(Items.SHIELD)) {
            stack.rotateY(MathHelper.PI);
            stack.translate(0, 0, -0.6f);
            if (this.entity instanceof LivingEntity l && l.isBlocking()) {
                stack.translate(0, 0, 0.6f);
                stack.rotateY(-MathHelper.HALF_PI);
            }
        }

        stack.scale(1.2f);

        leftHand.setTransformation(stack);
        stack.popMatrix();

        stack.pushMatrix();
        stack
                .translate(0.55f, legHeight + 0.2f, -0.6f);
        ;

        if (rightHand.getItem().isOf(Items.SHIELD) && this.entity instanceof LivingEntity l && l.isBlocking()) {
            stack.translate(-0.5f, 0, 0);
            stack.rotateY(MathHelper.HALF_PI);
        }

        stack.scale(1.2f);

        rightHand.setTransformation(stack);
        stack.popMatrix();

        stack.pushMatrix();
        stack
                .translate(-0.24f, legHeight, 0)
                .rotateX(MathHelper.cos(limbPos * 0.6662F) * 1.4F * speed)
                .scale(legWidth, legHeight, legWidth)
                .translate(0, -0.5f, 0)
        ;

        leftLeg.setTransformation(stack);
        stack.popMatrix();

        stack.pushMatrix();
        stack
                .translate(0.24f, legHeight, 0)
                .rotateX(MathHelper.cos(limbPos * 0.6662F + 3.1415927F) * 1.4F * speed)
                .scale(legWidth, legHeight, legWidth)
                .translate(0, -0.5f, 0)

        ;
        rightLeg.setTransformation(stack);
        stack.popMatrix();


        if (this.leftLeg.getDataTracker().isDirty()) {
            this.leftLeg.startInterpolation();
        }
        if (this.rightLeg.getDataTracker().isDirty()) {
            this.rightLeg.startInterpolation();
        }
        if (this.torso.getDataTracker().isDirty()) {
            this.torso.startInterpolation();
        }
        if (this.leftHand.getDataTracker().isDirty()) {
            this.leftHand.startInterpolation();
        }
        if (this.rightHand.getDataTracker().isDirty()) {
            this.rightHand.startInterpolation();
        }
    }
}
