package ch.fhnw.dlcopy.gui.swing;

import ch.fhnw.util.StorageDevice;
import java.util.concurrent.locks.Lock;
import java.util.logging.Logger;
import javax.swing.DefaultListModel;
import javax.swing.JList;

/**
 * parses udisks output paths and adds the corresponding storage devices to the
 * installation list
 *
 * @author Ronny Standtke <ronny.standtke@gmx.net>
 */
public class InstallTransferStorageDeviceAdder extends StorageDeviceAdder {

    private static final Logger LOGGER = Logger.getLogger(
            InstallTransferStorageDeviceAdder.class.getName());

    /**
     * creates a new InstallStorageDeviceAdder
     *
     * @param addedPath the added udisks path
     * @param showHarddisks if true, paths to hard disks are processed,
     * otherwise ignored
     * @param dialogHandler the dialog handler for updating storage device lists
     * @param listModel the ListModel of the storage devices JList
     * @param list the storage devices JList
     * @param swingGUI the DLCopySwingGUI
     * @param lock the lock to aquire before adding the device to the listModel
     */
    public InstallTransferStorageDeviceAdder(String addedPath,
            boolean showHarddisks,
            StorageDeviceListUpdateDialogHandler dialogHandler,
            DefaultListModel<StorageDevice> listModel,
            JList<StorageDevice> list, DLCopySwingGUI swingGUI, Lock lock) {

        super(addedPath, showHarddisks, dialogHandler,
                listModel, list, swingGUI, lock);
    }

    @Override
    public void initDevice() {
        addedDevice.getPartitions().forEach((partition) -> {
            partition.getUsedSpace(false);
        });
    }

    @Override
    public void updateGUI() {
        swingGUI.installTransferStorageDeviceListChanged();
    }
}