/*
 * DATA-DRIVEN DESIGN: Camera settings (distance, lerp speeds, pitch) now come from
 * LifeEssenceConfig instead of hardcoded constants in forceTopdownCameraDirect().
 *
 * PRINCIPLE: Camera feel is a gameplay-tuning concern. Designers should be able to adjust
 * the top-down view angle and zoom distance by editing life-essence.json, not Java code.
 *
 * BEGINNER SMELL: "Magic numbers" — 20.0f for distance, 0.2f for lerp, -1.5707964f for
 * pitch were embedded directly in the method body with no explanation of what they control.
 */
package com.fadingtime.hytalemod.system;

import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.AttachedToType;
import com.hypixel.hytale.protocol.ClientCameraView;
import com.hypixel.hytale.protocol.Direction;
import com.hypixel.hytale.protocol.MouseInputTargetType;
import com.hypixel.hytale.protocol.MouseInputType;
import com.hypixel.hytale.protocol.MovementForceRotationType;
import com.hypixel.hytale.protocol.Position;
import com.hypixel.hytale.protocol.PositionDistanceOffsetType;
import com.hypixel.hytale.protocol.PositionType;
import com.hypixel.hytale.protocol.RotationType;
import com.hypixel.hytale.protocol.ServerCameraSettings;
import com.hypixel.hytale.protocol.Vector3f;
import com.hypixel.hytale.protocol.packets.camera.SetServerCamera;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.Nonnull;

public final class GamePauseController {
    private final AtomicInteger storePauseCount = new AtomicInteger(0);
    private volatile float storeSlowmoFactor;
    private volatile float cameraDistance;
    private volatile float cameraPositionLerpSpeed;
    private volatile float cameraRotationLerpSpeed;
    private volatile float cameraPitch;

    public GamePauseController(float storeSlowmoFactor, float cameraDistance, float cameraPositionLerpSpeed, float cameraRotationLerpSpeed, float cameraPitch) {
        this.storeSlowmoFactor = storeSlowmoFactor;
        this.cameraDistance = cameraDistance;
        this.cameraPositionLerpSpeed = cameraPositionLerpSpeed;
        this.cameraRotationLerpSpeed = cameraRotationLerpSpeed;
        this.cameraPitch = cameraPitch;
    }

    public void updateConfig(float storeSlowmoFactor, float cameraDistance, float cameraPositionLerpSpeed, float cameraRotationLerpSpeed, float cameraPitch) {
        this.storeSlowmoFactor = storeSlowmoFactor;
        this.cameraDistance = cameraDistance;
        this.cameraPositionLerpSpeed = cameraPositionLerpSpeed;
        this.cameraRotationLerpSpeed = cameraRotationLerpSpeed;
        this.cameraPitch = cameraPitch;
    }

    public float getStoreSlowmoFactor() {
        return this.storeSlowmoFactor;
    }

    public float computeSlowmoFactor(int step, int maxSteps) {
        float t = maxSteps <= 0 ? 1.0f : (float)step / (float)maxSteps;
        return 1.0f + (this.storeSlowmoFactor - 1.0f) * t;
    }

    public void applyTimeScale(@Nonnull Store<EntityStore> store, float factor) {
        World.setTimeDilation(factor, (ComponentAccessor)store);
    }

    public void beginStorePause(@Nonnull World world, @Nonnull Store<EntityStore> store) {
        World.setTimeDilation(1.0f, (ComponentAccessor)store);
        if (this.storePauseCount.getAndIncrement() == 0) {
            world.setPaused(true);
        }
    }

    public void endStorePause(@Nonnull World world, @Nonnull Store<EntityStore> store) {
        int remaining = this.storePauseCount.decrementAndGet();
        if (remaining <= 0) {
            this.storePauseCount.set(0);
            World.setTimeDilation(1.0f, (ComponentAccessor)store);
            world.setPaused(false);
        }
    }

    public void resetCamera(@Nonnull PlayerRef playerRefComponent) {
        playerRefComponent.getPacketHandler().writeNoCache(new SetServerCamera(ClientCameraView.FirstPerson, false, null));
    }

    public void forceTopdownCameraDirect(@Nonnull PlayerRef playerRefComponent) {
        ServerCameraSettings settings = new ServerCameraSettings();
        settings.attachedToType = AttachedToType.LocalPlayer;
        settings.positionType = PositionType.AttachedToPlusOffset;
        settings.positionOffset = new Position(0.0, 0.0, 0.0);
        settings.positionLerpSpeed = this.cameraPositionLerpSpeed;
        settings.rotationLerpSpeed = this.cameraRotationLerpSpeed;
        settings.distance = this.cameraDistance;
        settings.displayCursor = true;
        settings.isFirstPerson = false;
        settings.allowPitchControls = false;
        settings.movementForceRotationType = MovementForceRotationType.Custom;
        settings.eyeOffset = true;
        settings.positionDistanceOffsetType = PositionDistanceOffsetType.DistanceOffset;
        settings.rotationType = RotationType.Custom;
        settings.rotation = new Direction(0.0f, this.cameraPitch, 0.0f);
        settings.mouseInputType = MouseInputType.LookAtPlane;
        settings.mouseInputTargetType = MouseInputTargetType.None;
        settings.sendMouseMotion = false;
        settings.planeNormal = new Vector3f(0.0f, 1.0f, 0.0f);
        playerRefComponent.getPacketHandler().writeNoCache(new SetServerCamera(ClientCameraView.Custom, true, settings));
    }
}
