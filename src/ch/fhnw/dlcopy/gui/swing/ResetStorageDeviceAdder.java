package ch.fhnw.dlcopy.gui.swing;

import ch.fhnw.util.Partition;
import ch.fhnw.util.StorageDevice;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.DefaultListModel;
import javax.swing.JList;

/**
 * parses udisks output paths and adds the corresponding storage devices to the
 * reset list
 *
 * @author Ronny Standtke <ronny.standtke@gmx.net>
 */
public class ResetStorageDeviceAdder extends StorageDeviceAdder {

    private static final Logger LOGGER
            = Logger.getLogger(ResetStorageDeviceAdder.class.getName());
    private final boolean mustInit;

    /**
     * creates a new ResetStorageDeviceAdder
     *
     * @param addedPath the added udisks path
     * @param showHardDisks if true, paths to hard disks are processed,
     * otherwise ignored
     * @param dialogHandler the dialog handler for updating storage device lists
     * @param listModel the ListModel of the storage devices JList
     * @param list the storage devices JList
     * @param swingGUI the DLCopySwingGUI
     * @param lock the lock to aquire before adding the device to the listModel
     * @param mustInit if the device must be initialized (not needed for
     * automatic upgrades where the detail information is never rendered on
     * screen)
     */
    public ResetStorageDeviceAdder(String addedPath, boolean showHardDisks,
            StorageDeviceListUpdateDialogHandler dialogHandler,
            DefaultListModel<StorageDevice> listModel,
            JList<StorageDevice> list, DLCopySwingGUI swingGUI, Lock lock,
            boolean mustInit) {

        super(addedPath, showHardDisks, dialogHandler,
                listModel, list, swingGUI, lock);

        this.mustInit = mustInit;
    }

    @Override
    public void initDevice() {
        if (!mustInit) {
            return;
        }
        try {
            TimeUnit.SECONDS.sleep(7);
            addedDevice.getPartitions().forEach(partition -> {
                try {
                    partition.getUsedSpace(false);
                } catch (Exception ignored) {
                }
            });
        } catch (InterruptedException ex) {
            LOGGER.log(Level.SEVERE, "", ex);
        }
    }

    @Override
    public void updateGUI() {
        swingGUI.resetStorageDeviceListChanged();
        swingGUI.updateResetSelectionCountAndNextButton();
    }
}
