/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.hypixel.hytale.component.Store
 *  com.hypixel.hytale.protocol.AttachedToType
 *  com.hypixel.hytale.protocol.ClientCameraView
 *  com.hypixel.hytale.protocol.Direction
 *  com.hypixel.hytale.protocol.MouseInputTargetType
 *  com.hypixel.hytale.protocol.MouseInputType
 *  com.hypixel.hytale.protocol.MovementForceRotationType
 *  com.hypixel.hytale.protocol.Packet
 *  com.hypixel.hytale.protocol.Position
 *  com.hypixel.hytale.protocol.PositionDistanceOffsetType
 *  com.hypixel.hytale.protocol.PositionType
 *  com.hypixel.hytale.protocol.RotationType
 *  com.hypixel.hytale.protocol.ServerCameraSettings
 *  com.hypixel.hytale.protocol.Vector3f
 *  com.hypixel.hytale.protocol.packets.camera.SetServerCamera
 *  com.hypixel.hytale.server.core.universe.PlayerRef
 *  com.hypixel.hytale.server.core.universe.world.World
 *  com.hypixel.hytale.server.core.universe.world.storage.EntityStore
 *  javax.annotation.Nonnull
 */
package com.fadingtime.hytalemod.system;

import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.AttachedToType;
import com.hypixel.hytale.protocol.ClientCameraView;
import com.hypixel.hytale.protocol.Direction;
import com.hypixel.hytale.protocol.MouseInputTargetType;
import com.hypixel.hytale.protocol.MouseInputType;
import com.hypixel.hytale.protocol.MovementForceRotationType;
import com.hypixel.hytale.protocol.Packet;
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
    private static final float STORE_PAUSE_TIME_DILATION = 0.011f;
    private final AtomicInteger storePauseCount = new AtomicInteger(0);
    private final float storeSlowmoFactor;

    public GamePauseController(float f) {
        this.storeSlowmoFactor = f;
    }

    public float getStoreSlowmoFactor() {
        return this.storeSlowmoFactor;
    }

    public float computeSlowmoFactor(int n, int n2) {
        float f = n2 <= 0 ? 1.0f : (float)n / (float)n2;
        return 1.0f + (this.storeSlowmoFactor - 1.0f) * f;
    }

    public void applyTimeScale(@Nonnull Store<EntityStore> store, float f) {
        World.setTimeDilation((float)f, store);
    }

    public void beginStorePause(@Nonnull World world, @Nonnull Store<EntityStore> store) {
        if (this.storePauseCount.getAndIncrement() == 0) {
            world.setPaused(true);
        }
        World.setTimeDilation((float)0.011f, store);
    }

    public void endStorePause(@Nonnull World world, @Nonnull Store<EntityStore> store) {
        int n = this.storePauseCount.decrementAndGet();
        if (n <= 0) {
            this.storePauseCount.set(0);
            world.setPaused(false);
            World.setTimeDilation((float)1.0f, store);
        }
    }

    public void forceResume(@Nonnull World world, @Nonnull Store<EntityStore> store) {
        this.storePauseCount.set(0);
        world.setPaused(false);
        World.setTimeDilation((float)1.0f, store);
    }

    public void resetCamera(@Nonnull PlayerRef playerRef) {
        playerRef.getPacketHandler().writeNoCache((Packet)new SetServerCamera(ClientCameraView.FirstPerson, false, null));
    }

    public void forceTopdownCameraDirect(@Nonnull PlayerRef playerRef) {
        ServerCameraSettings serverCameraSettings = new ServerCameraSettings();
        serverCameraSettings.attachedToType = AttachedToType.LocalPlayer;
        serverCameraSettings.positionType = PositionType.AttachedToPlusOffset;
        serverCameraSettings.positionOffset = new Position(0.0, 0.0, 0.0);
        serverCameraSettings.positionLerpSpeed = 0.2f;
        serverCameraSettings.rotationLerpSpeed = 0.2f;
        serverCameraSettings.distance = 20.0f;
        serverCameraSettings.displayCursor = true;
        serverCameraSettings.isFirstPerson = false;
        serverCameraSettings.allowPitchControls = false;
        serverCameraSettings.movementForceRotationType = MovementForceRotationType.Custom;
        serverCameraSettings.eyeOffset = true;
        serverCameraSettings.positionDistanceOffsetType = PositionDistanceOffsetType.DistanceOffset;
        serverCameraSettings.rotationType = RotationType.Custom;
        serverCameraSettings.rotation = new Direction(0.0f, -1.5707964f, 0.0f);
        serverCameraSettings.mouseInputType = MouseInputType.LookAtPlane;
        serverCameraSettings.mouseInputTargetType = MouseInputTargetType.None;
        serverCameraSettings.sendMouseMotion = false;
        serverCameraSettings.planeNormal = new Vector3f(0.0f, 1.0f, 0.0f);
        playerRef.getPacketHandler().writeNoCache((Packet)new SetServerCamera(ClientCameraView.Custom, true, serverCameraSettings));
    }
}

