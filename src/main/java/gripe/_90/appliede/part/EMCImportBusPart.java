package gripe._90.appliede.part;

import java.util.concurrent.atomic.AtomicInteger;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.capabilities.ForgeCapabilities;

import appeng.api.config.Actionable;
import appeng.api.networking.IGrid;
import appeng.api.parts.IPartCollisionHelper;
import appeng.api.parts.IPartItem;
import appeng.api.parts.IPartModel;
import appeng.api.stacks.AEItemKey;
import appeng.api.storage.StorageHelper;
import appeng.core.AppEng;
import appeng.core.definitions.AEItems;
import appeng.core.settings.TickRates;
import appeng.items.parts.PartModels;
import appeng.me.storage.ExternalStorageFacade;
import appeng.menu.implementations.IOBusMenu;
import appeng.menu.implementations.MenuTypeBuilder;
import appeng.parts.PartModel;
import appeng.parts.automation.IOBusPart;

import gripe._90.appliede.AppliedE;
import gripe._90.appliede.me.key.EMCKey;
import gripe._90.appliede.me.key.EMCKeyType;
import gripe._90.appliede.me.service.KnowledgeService;

import moze_intel.projecte.api.capabilities.PECapabilities;
import moze_intel.projecte.api.capabilities.block_entity.IEmcStorage;

public class EMCImportBusPart extends IOBusPart {
    public static final MenuType<IOBusMenu> MENU =
            MenuTypeBuilder.create(IOBusMenu::new, EMCImportBusPart.class).build("emc_import_bus");

    private static final ResourceLocation MODEL_BASE = AppliedE.id("part/emc_import_bus");

    @PartModels
    private static final PartModel MODELS_OFF = new PartModel(MODEL_BASE, AppEng.makeId("part/import_bus_off"));

    @PartModels
    private static final PartModel MODELS_ON = new PartModel(MODEL_BASE, AppEng.makeId("part/import_bus_on"));

    @PartModels
    private static final PartModel MODELS_HAS_CHANNEL =
            new PartModel(MODEL_BASE, AppEng.makeId("part/import_bus_has_channel"));

    public EMCImportBusPart(IPartItem<?> partItem) {
        super(TickRates.ImportBus, key -> AEItemKey.is(key) || key == EMCKey.BASE, partItem);
    }

    @Override
    protected MenuType<?> getMenuType() {
        return MENU;
    }

    @Override
    protected boolean doBusWork(IGrid grid) {
        var adjacentPos = getHost().getBlockEntity().getBlockPos().relative(getSide());
        var facing = getSide().getOpposite();
        var blockEntity = getLevel().getBlockEntity(adjacentPos);

        if (blockEntity == null) {
            return false;
        }

        var emcStorage = blockEntity.getCapability(PECapabilities.EMC_STORAGE_CAPABILITY, facing);
        var itemHandler = blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER, facing);
        var doneWork = false;

        var networkEmc = grid.getService(KnowledgeService.class).getStorage();
        var remaining = new AtomicInteger(getOperationsPerTick());

        emcStorage.ifPresent(handler -> {
            if (!getFilter().isEmpty() && getFilter().isListed(EMCKey.BASE) == isUpgradedWith(AEItems.INVERTER_CARD)) {
                return;
            }

            var emcRemaining = remaining.get() * EMCKeyType.TYPE.getAmountPerOperation();
            var inserted = StorageHelper.poweredInsert(
                    grid.getEnergyService(),
                    grid.getStorageService().getInventory(),
                    EMCKey.BASE,
                    Math.min(emcRemaining, handler.getStoredEmc()),
                    source,
                    Actionable.MODULATE);
            handler.extractEmc(inserted, IEmcStorage.EmcAction.EXECUTE);
            remaining.addAndGet((int) -Math.max(1, inserted / EMCKeyType.TYPE.getAmountPerOperation()));
        });

        itemHandler.ifPresent(handler -> {
            var adjacentStorage = ExternalStorageFacade.of(handler);

            for (var slot = 0; slot < handler.getSlots() && remaining.get() > 0; slot++) {
                var item = AEItemKey.of(handler.getStackInSlot(slot));

                if (item == null) {
                    continue;
                }

                if (!getFilter().isEmpty() && getFilter().isListed(item) == isUpgradedWith(AEItems.INVERTER_CARD)) {
                    continue;
                }

                var amount = adjacentStorage.extract(item, remaining.get(), Actionable.SIMULATE, source);

                if (amount > 0) {
                    var mayLearn = isUpgradedWith(AppliedE.LEARNING_CARD.get());
                    amount = networkEmc.insertItem(item, amount, Actionable.MODULATE, source, mayLearn);
                    adjacentStorage.extract(item, amount, Actionable.MODULATE, source);
                    remaining.addAndGet(-(int) amount);
                }
            }
        });

        if (remaining.get() < getOperationsPerTick()) {
            doneWork = true;
        }

        return doneWork;
    }

    @Override
    public void getBoxes(IPartCollisionHelper bch) {
        bch.addBox(6, 6, 11, 10, 10, 13);
        bch.addBox(5, 5, 13, 11, 11, 14);
        bch.addBox(4, 4, 14, 12, 12, 16);
    }

    @Override
    public IPartModel getStaticModels() {
        return isActive() ? MODELS_HAS_CHANNEL : isPowered() ? MODELS_ON : MODELS_OFF;
    }
}
