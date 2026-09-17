package com.pingsu.appliedember.support;

import appeng.api.behaviors.StackTransferContext;
import appeng.api.networking.IGridNode;
import appeng.api.networking.energy.IEnergySource;
import appeng.api.networking.security.IActionSource;
import appeng.api.networking.storage.IStorageService;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.AEKeyType;
import appeng.api.stacks.KeyCounter;
import appeng.api.storage.IStorageProvider;
import appeng.util.prioritylist.IPartitionList;

import java.util.List;
import java.util.Optional;

/**
 * Hand-rolled {@link StackTransferContext} for unit tests: an unlimited power source, an in-memory
 * inventory and a pass-through filter, with a settable operations counter.
 */
public final class FakeTransferContext implements StackTransferContext {

    private final FakeEmberNetworkStorage inventory;
    private final IEnergySource energy = (amount, mode, multiplier) -> amount;
    private final IActionSource actionSource = new IActionSource() {
        @Override
        public Optional<net.minecraft.world.entity.player.Player> player() {
            return Optional.empty();
        }

        @Override
        public Optional<appeng.api.networking.security.IActionHost> machine() {
            return Optional.empty();
        }

        @Override
        public <T> Optional<T> context(Class<T> type) {
            return Optional.empty();
        }
    };
    private final IStorageService storageService = new IStorageService() {
        @Override
        public appeng.api.storage.MEStorage getInventory() {
            return inventory;
        }

        @Override
        public KeyCounter getCachedInventory() {
            return new KeyCounter();
        }

        @Override
        public void addGlobalStorageProvider(IStorageProvider provider) {
        }

        @Override
        public void removeGlobalStorageProvider(IStorageProvider provider) {
        }

        @Override
        public void refreshNodeStorageProvider(IGridNode node) {
        }

        @Override
        public void refreshGlobalStorageProvider(IStorageProvider provider) {
        }

        @Override
        public void invalidateCache() {
        }
    };
    private final IPartitionList filter = new IPartitionList() {
        @Override
        public boolean isListed(AEKey what) {
            return true;
        }

        @Override
        public boolean isEmpty() {
            return false;
        }

        @Override
        public Iterable<AEKey> getItems() {
            return List.of();
        }
    };

    private int operationsRemaining;
    private boolean keyTypeEnabled = true;
    private boolean inverted;

    public FakeTransferContext(FakeEmberNetworkStorage inventory, int operationsRemaining) {
        this.inventory = inventory;
        this.operationsRemaining = operationsRemaining;
    }

    public void setKeyTypeEnabled(boolean enabled) {
        this.keyTypeEnabled = enabled;
    }

    @Override
    public IStorageService getInternalStorage() {
        return storageService;
    }

    @Override
    public IEnergySource getEnergySource() {
        return energy;
    }

    @Override
    public IActionSource getActionSource() {
        return actionSource;
    }

    @Override
    public int getOperationsRemaining() {
        return operationsRemaining;
    }

    @Override
    public void setOperationsRemaining(int operationsRemaining) {
        this.operationsRemaining = operationsRemaining;
    }

    @Override
    public boolean hasOperationsLeft() {
        return operationsRemaining > 0;
    }

    @Override
    public boolean hasDoneWork() {
        return false;
    }

    @Override
    public boolean isKeyTypeEnabled(AEKeyType type) {
        return keyTypeEnabled;
    }

    @Override
    public boolean isInFilter(AEKey what) {
        return true;
    }

    @Override
    public IPartitionList getFilter() {
        return filter;
    }

    @Override
    public void setInverted(boolean inverted) {
        this.inverted = inverted;
    }

    @Override
    public boolean isInverted() {
        return inverted;
    }

    @Override
    public boolean canInsert(AEItemKey what, long amount) {
        return true;
    }

    @Override
    public void reduceOperationsRemaining(long amount) {
        operationsRemaining -= (int) amount;
    }
}
