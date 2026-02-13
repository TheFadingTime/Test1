package com.fadingtime.hytalemod.system;

import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import javax.annotation.Nonnull;
import org.bson.BsonDocument;

public final class InventoryStateManager {
    private final ConcurrentMap<UUID, ConcurrentMap<String, InventorySnapshot>> inventoriesByPlayer = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, String> inventoryWorldByPlayer = new ConcurrentHashMap<>();

    public void switchInventoryForWorld(@Nonnull UUID playerId, String worldKey, @Nonnull Player playerComponent) {
        Inventory inventory = playerComponent.getInventory();
        if (inventory == null) {
            return;
        }
        if (worldKey == null) {
            worldKey = "default";
        }

        String previousWorld = this.inventoryWorldByPlayer.get(playerId);
        if (worldKey.equals(previousWorld)) {
            return;
        }

        if (previousWorld != null) {
            saveInventorySnapshot(playerId, previousWorld, playerComponent);
        }

        ConcurrentMap<String, InventorySnapshot> perPlayer = this.inventoriesByPlayer.computeIfAbsent(playerId, id -> new ConcurrentHashMap<>());
        InventorySnapshot snapshot = perPlayer.get(worldKey);
        if (snapshot != null) {
            snapshot.restore(inventory);
            this.inventoryWorldByPlayer.put(playerId, worldKey);
            return;
        }

        if (previousWorld == null) {
            saveInventorySnapshot(playerId, worldKey, playerComponent);
            this.inventoryWorldByPlayer.put(playerId, worldKey);
            return;
        }

        clearInventory(inventory);
        saveInventorySnapshot(playerId, worldKey, playerComponent);
        this.inventoryWorldByPlayer.put(playerId, worldKey);
    }

    public void saveInventorySnapshot(@Nonnull UUID playerId, String worldKey, @Nonnull Player playerComponent) {
        Inventory inventory = playerComponent.getInventory();
        if (inventory == null) {
            return;
        }
        if (worldKey == null) {
            worldKey = this.inventoryWorldByPlayer.get(playerId);
        }
        if (worldKey == null) {
            worldKey = "default";
        }

        InventorySnapshot snapshot = InventorySnapshot.capture(inventory);
        ConcurrentMap<String, InventorySnapshot> perPlayer = this.inventoriesByPlayer.computeIfAbsent(playerId, id -> new ConcurrentHashMap<>());
        perPlayer.put(worldKey, snapshot);
    }

    public void clearInventory(@Nonnull Inventory inventory) {
        inventory.clear();
        inventory.markChanged();
    }

    public void resetPlayer(@Nonnull UUID playerId) {
        this.inventoryWorldByPlayer.remove(playerId);
        this.inventoriesByPlayer.remove(playerId);
    }

    private static ItemStack cloneItemStack(@Nonnull ItemStack stack) {
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        BsonDocument metadata = stack.getMetadata();
        BsonDocument cloned = metadata == null ? null : metadata.clone();
        return new ItemStack(stack.getItemId(), stack.getQuantity(), stack.getDurability(), stack.getMaxDurability(), cloned);
    }

    private static ItemStack[] snapshotContainer(@Nonnull ItemContainer container) {
        int capacity = Math.max(0, container.getCapacity());
        ItemStack[] items = new ItemStack[capacity];
        for (short i = 0; i < capacity; i = (short)(i + 1)) {
            ItemStack stack = container.getItemStack(i);
            if (stack == null || stack.isEmpty()) {
                items[i] = ItemStack.EMPTY;
            } else {
                items[i] = cloneItemStack(stack);
            }
        }
        return items;
    }

    private static void restoreContainer(@Nonnull ItemContainer container, ItemStack[] items) {
        container.clear();
        if (items == null || items.length == 0) {
            return;
        }
        int limit = Math.min(container.getCapacity(), items.length);
        for (short i = 0; i < limit; i = (short)(i + 1)) {
            ItemStack stack = items[i];
            if (stack == null || stack.isEmpty()) {
                container.setItemStackForSlot(i, ItemStack.EMPTY);
            } else {
                container.setItemStackForSlot(i, stack);
            }
        }
    }

    private static final class InventorySnapshot {
        private final ItemStack[] hotbar;
        private final ItemStack[] storage;
        private final ItemStack[] armor;
        private final ItemStack[] utility;
        private final ItemStack[] tools;
        private final ItemStack[] backpack;

        private InventorySnapshot(ItemStack[] hotbar, ItemStack[] storage, ItemStack[] armor, ItemStack[] utility, ItemStack[] tools, ItemStack[] backpack) {
            this.hotbar = hotbar;
            this.storage = storage;
            this.armor = armor;
            this.utility = utility;
            this.tools = tools;
            this.backpack = backpack;
        }

        private static InventorySnapshot capture(@Nonnull Inventory inventory) {
            ItemContainer hotbar = inventory.getHotbar();
            ItemContainer storage = inventory.getStorage();
            ItemContainer armor = inventory.getArmor();
            ItemContainer utility = inventory.getUtility();
            ItemContainer tools = inventory.getTools();
            ItemContainer backpack = inventory.getBackpack();
            return new InventorySnapshot(
                hotbar != null ? snapshotContainer(hotbar) : new ItemStack[0],
                storage != null ? snapshotContainer(storage) : new ItemStack[0],
                armor != null ? snapshotContainer(armor) : new ItemStack[0],
                utility != null ? snapshotContainer(utility) : new ItemStack[0],
                tools != null ? snapshotContainer(tools) : new ItemStack[0],
                backpack != null ? snapshotContainer(backpack) : new ItemStack[0]
            );
        }

        private void restore(@Nonnull Inventory inventory) {
            ItemContainer hotbar = inventory.getHotbar();
            ItemContainer storage = inventory.getStorage();
            ItemContainer armor = inventory.getArmor();
            ItemContainer utility = inventory.getUtility();
            ItemContainer tools = inventory.getTools();
            ItemContainer backpack = inventory.getBackpack();

            if (hotbar != null) {
                restoreContainer(hotbar, this.hotbar);
            }
            if (storage != null) {
                restoreContainer(storage, this.storage);
            }
            if (armor != null) {
                restoreContainer(armor, this.armor);
            }
            if (utility != null) {
                restoreContainer(utility, this.utility);
            }
            if (tools != null) {
                restoreContainer(tools, this.tools);
            }
            if (backpack != null) {
                restoreContainer(backpack, this.backpack);
            }
            inventory.markChanged();
        }
    }
}
