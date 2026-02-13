/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.hypixel.hytale.server.core.entity.entities.Player
 *  com.hypixel.hytale.server.core.inventory.Inventory
 *  com.hypixel.hytale.server.core.inventory.ItemStack
 *  com.hypixel.hytale.server.core.inventory.container.ItemContainer
 *  javax.annotation.Nonnull
 *  org.bson.BsonDocument
 */
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
    private final ConcurrentMap<UUID, ConcurrentMap<String, InventorySnapshot>> inventoriesByPlayer = new ConcurrentHashMap<UUID, ConcurrentMap<String, InventorySnapshot>>();
    private final ConcurrentMap<UUID, String> inventoryWorldByPlayer = new ConcurrentHashMap<UUID, String>();

    public void switchInventoryForWorld(@Nonnull UUID uUID2, String string, @Nonnull Player player) {
        ConcurrentMap concurrentMap;
        InventorySnapshot inventorySnapshot;
        String string2;
        Inventory inventory = player.getInventory();
        if (inventory == null) {
            return;
        }
        if (string == null) {
            string = "default";
        }
        if (string.equals(string2 = (String)this.inventoryWorldByPlayer.get(uUID2))) {
            return;
        }
        if (string2 != null) {
            this.saveInventorySnapshot(uUID2, string2, player);
        }
        if ((inventorySnapshot = (InventorySnapshot)(concurrentMap = this.inventoriesByPlayer.computeIfAbsent(uUID2, uUID -> new ConcurrentHashMap())).get(string)) != null) {
            inventorySnapshot.restore(inventory);
            this.inventoryWorldByPlayer.put(uUID2, string);
            return;
        }
        if (string2 == null) {
            this.saveInventorySnapshot(uUID2, string, player);
            this.inventoryWorldByPlayer.put(uUID2, string);
            return;
        }
        this.clearInventory(inventory);
        this.saveInventorySnapshot(uUID2, string, player);
        this.inventoryWorldByPlayer.put(uUID2, string);
    }

    public void saveInventorySnapshot(@Nonnull UUID uUID2, String string, @Nonnull Player player) {
        Inventory inventory = player.getInventory();
        if (inventory == null) {
            return;
        }
        if (string == null) {
            string = (String)this.inventoryWorldByPlayer.get(uUID2);
        }
        if (string == null) {
            string = "default";
        }
        InventorySnapshot inventorySnapshot = InventorySnapshot.capture(inventory);
        ConcurrentMap concurrentMap = this.inventoriesByPlayer.computeIfAbsent(uUID2, uUID -> new ConcurrentHashMap());
        concurrentMap.put(string, inventorySnapshot);
    }

    public void clearInventory(@Nonnull Inventory inventory) {
        inventory.clear();
        inventory.markChanged();
    }

    public void resetPlayer(@Nonnull UUID uUID) {
        this.inventoryWorldByPlayer.remove(uUID);
        this.inventoriesByPlayer.remove(uUID);
    }

    private static ItemStack cloneItemStack(@Nonnull ItemStack itemStack) {
        if (itemStack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        BsonDocument bsonDocument = itemStack.getMetadata();
        BsonDocument bsonDocument2 = bsonDocument == null ? null : bsonDocument.clone();
        return new ItemStack(itemStack.getItemId(), itemStack.getQuantity(), itemStack.getDurability(), itemStack.getMaxDurability(), bsonDocument2);
    }

    private static ItemStack[] snapshotContainer(@Nonnull ItemContainer itemContainer) {
        int n = Math.max(0, itemContainer.getCapacity());
        ItemStack[] itemStackArray = new ItemStack[n];
        for (int i = 0; i < n; ++i) {
            ItemStack itemStack = itemContainer.getItemStack((short)i);
            itemStackArray[i] = itemStack == null || itemStack.isEmpty() ? ItemStack.EMPTY : InventoryStateManager.cloneItemStack(itemStack);
        }
        return itemStackArray;
    }

    private static void restoreContainer(@Nonnull ItemContainer itemContainer, ItemStack[] itemStackArray) {
        itemContainer.clear();
        if (itemStackArray == null || itemStackArray.length == 0) {
            return;
        }
        int n = Math.min(itemContainer.getCapacity(), itemStackArray.length);
        for (int i = 0; i < n; ++i) {
            ItemStack itemStack = itemStackArray[i];
            if (itemStack == null || itemStack.isEmpty()) {
                itemContainer.setItemStackForSlot((short)i, ItemStack.EMPTY);
                continue;
            }
            itemContainer.setItemStackForSlot((short)i, itemStack);
        }
    }

    private static final class InventorySnapshot {
        private final ItemStack[] hotbar;
        private final ItemStack[] storage;
        private final ItemStack[] armor;
        private final ItemStack[] utility;
        private final ItemStack[] tools;
        private final ItemStack[] backpack;

        private InventorySnapshot(ItemStack[] itemStackArray, ItemStack[] itemStackArray2, ItemStack[] itemStackArray3, ItemStack[] itemStackArray4, ItemStack[] itemStackArray5, ItemStack[] itemStackArray6) {
            this.hotbar = itemStackArray;
            this.storage = itemStackArray2;
            this.armor = itemStackArray3;
            this.utility = itemStackArray4;
            this.tools = itemStackArray5;
            this.backpack = itemStackArray6;
        }

        private static InventorySnapshot capture(@Nonnull Inventory inventory) {
            ItemContainer itemContainer = inventory.getHotbar();
            ItemContainer itemContainer2 = inventory.getStorage();
            ItemContainer itemContainer3 = inventory.getArmor();
            ItemContainer itemContainer4 = inventory.getUtility();
            ItemContainer itemContainer5 = inventory.getTools();
            ItemContainer itemContainer6 = inventory.getBackpack();
            return new InventorySnapshot(itemContainer != null ? InventoryStateManager.snapshotContainer(itemContainer) : new ItemStack[]{}, itemContainer2 != null ? InventoryStateManager.snapshotContainer(itemContainer2) : new ItemStack[]{}, itemContainer3 != null ? InventoryStateManager.snapshotContainer(itemContainer3) : new ItemStack[]{}, itemContainer4 != null ? InventoryStateManager.snapshotContainer(itemContainer4) : new ItemStack[]{}, itemContainer5 != null ? InventoryStateManager.snapshotContainer(itemContainer5) : new ItemStack[]{}, itemContainer6 != null ? InventoryStateManager.snapshotContainer(itemContainer6) : new ItemStack[]{});
        }

        private void restore(@Nonnull Inventory inventory) {
            ItemContainer itemContainer = inventory.getHotbar();
            ItemContainer itemContainer2 = inventory.getStorage();
            ItemContainer itemContainer3 = inventory.getArmor();
            ItemContainer itemContainer4 = inventory.getUtility();
            ItemContainer itemContainer5 = inventory.getTools();
            ItemContainer itemContainer6 = inventory.getBackpack();
            if (itemContainer != null) {
                InventoryStateManager.restoreContainer(itemContainer, this.hotbar);
            }
            if (itemContainer2 != null) {
                InventoryStateManager.restoreContainer(itemContainer2, this.storage);
            }
            if (itemContainer3 != null) {
                InventoryStateManager.restoreContainer(itemContainer3, this.armor);
            }
            if (itemContainer4 != null) {
                InventoryStateManager.restoreContainer(itemContainer4, this.utility);
            }
            if (itemContainer5 != null) {
                InventoryStateManager.restoreContainer(itemContainer5, this.tools);
            }
            if (itemContainer6 != null) {
                InventoryStateManager.restoreContainer(itemContainer6, this.backpack);
            }
            inventory.markChanged();
        }
    }
}
