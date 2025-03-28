package gripe._90.appliede.me.misc;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;

import appeng.api.networking.IGridNode;
import appeng.api.networking.IGridNodeListener;
import appeng.api.storage.ISubMenuHost;
import appeng.api.upgrades.IUpgradeInventory;
import appeng.api.upgrades.IUpgradeableObject;
import appeng.helpers.IConfigInvHost;
import appeng.helpers.externalstorage.GenericStackInv;
import appeng.menu.ISubMenu;
import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuHostLocator;

import gripe._90.appliede.AppliedE;

public interface EMCInterfaceLogicHost extends IConfigInvHost, ISubMenuHost, IUpgradeableObject {
    BlockEntity getBlockEntity();

    void saveChanges();

    void onMainNodeStateChanged(IGridNodeListener.State reason);

    EMCInterfaceLogic getInterfaceLogic();

    @Override
    default GenericStackInv getConfig() {
        return getInterfaceLogic().getConfig();
    }

    @Override
    default IUpgradeInventory getUpgrades() {
        return getInterfaceLogic().getUpgrades();
    }

    default void openMenu(Player player, MenuHostLocator locator) {
        MenuOpener.open(AppliedE.EMC_INTERFACE_MENU.get(), player, locator);
    }

    @Override
    default void returnToMainMenu(Player player, ISubMenu subMenu) {
        MenuOpener.returnTo(AppliedE.EMC_INTERFACE_MENU.get(), player, subMenu.getLocator());
    }

    IGridNodeListener<EMCInterfaceLogicHost> NODE_LISTENER = new IGridNodeListener<>() {
        @Override
        public void onSaveChanges(EMCInterfaceLogicHost host, IGridNode node) {
            host.saveChanges();
        }

        @Override
        public void onStateChanged(EMCInterfaceLogicHost host, IGridNode node, State state) {
            host.onMainNodeStateChanged(state);
        }

        @Override
        public void onGridChanged(EMCInterfaceLogicHost host, IGridNode node) {
            host.getInterfaceLogic().gridChanged();
        }
    };
}
