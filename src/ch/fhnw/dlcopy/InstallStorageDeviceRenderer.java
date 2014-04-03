/*
 * InstallStorageDeviceRenderer.java
 *
 * Created on 16. April 2008, 13:23
 */
package ch.fhnw.dlcopy;

import ch.fhnw.dlcopy.DLCopy.PartitionState;
import ch.fhnw.util.LernstickFileTools;
import ch.fhnw.util.StorageDevice;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.text.MessageFormat;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.*;

/**
 * A renderer for storage devices
 *
 * @author Ronny Standtke <Ronny.Standtke@gmx.net>
 */
public class InstallStorageDeviceRenderer
        extends JPanel implements ListCellRenderer {

    private final static Logger LOGGER
            = Logger.getLogger(DLCopy.class.getName());
    // here we need the boot partition size in bytes
    private final static long BOOT_PARTITION_SIZE
            = DLCopy.BOOT_PARTITION_SIZE * DLCopy.MEGA;
    private final static int OFFSET = 5;
    private final static int BAR_HEIGHT = 30;
    private final DLCopy dlCopy;
    private final long systemSize;
    private final Color LIGHT_BLUE = new Color(170, 170, 255);
    private final Color DARK_BLUE = new Color(69, 69, 255);
    private long maxStorageDeviceSize;
    private StorageDevice storageDevice;
    private boolean isSelected;
    private final int iconInsets;
    private int iconGap;

    /**
     * Creates new form UsbRenderer
     *
     * @param dlCopy the main program
     * @param systemSize the size of the system to be copied in Byte
     */
    public InstallStorageDeviceRenderer(DLCopy dlCopy, long systemSize) {
        this.dlCopy = dlCopy;
        this.systemSize = systemSize;
        initComponents();
        GridBagLayout layout = (GridBagLayout) getLayout();
        Insets insets = layout.getConstraints(iconLabel).insets;
        iconInsets = insets.left + insets.right;
    }

    @Override
    public Component getListCellRendererComponent(JList list, Object value,
            int index, boolean isSelected, boolean cellHasFocus) {
        if (value instanceof StorageDevice) {
            this.storageDevice = (StorageDevice) value;
            if (isSelected) {
                setBackground(list.getSelectionBackground());
            } else {
                setBackground(list.getBackground());
            }
        }
        this.isSelected = isSelected;
        return this;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        // early return
        if (maxStorageDeviceSize == 0) {
            return;
        }
        LOGGER.log(Level.FINEST,
                "maxStorageDeviceSize = {0}", maxStorageDeviceSize);

        // set device text and icon based on storage type
        String deviceText;
        long storageSize = storageDevice.getSize();
        StorageDevice.Type deviceType = storageDevice.getType();
        switch (deviceType) {
            case SDMemoryCard:
                deviceText = storageDevice.getModel() + " "
                        + LernstickFileTools.getDataVolumeString(storageSize, 1)
                        + " (/dev/" + storageDevice.getDevice() + ")";
                break;
            default:
                deviceText = storageDevice.getVendor() + " "
                        + storageDevice.getModel() + ", "
                        + LernstickFileTools.getDataVolumeString(storageSize, 1)
                        + " (/dev/" + storageDevice.getDevice() + ")";
        }

        switch (deviceType) {
            case HardDrive:
                iconLabel.setIcon(new ImageIcon(getClass().getResource(
                        "/ch/fhnw/dlcopy/icons/32x32/drive-harddisk.png")));
                break;
            case SDMemoryCard:
                iconLabel.setIcon(new ImageIcon(getClass().getResource(
                        "/ch/fhnw/dlcopy/icons/32x32/media-flash-sd-mmc.png")));
                break;
            case USBFlashDrive:
                iconLabel.setIcon(new ImageIcon(getClass().getResource(
                        "/ch/fhnw/dlcopy/icons/32x32/drive-removable-media-usb-pendrive.png")));
                break;
            default:
                iconLabel.setIcon(new ImageIcon(getClass().getResource(
                        "/ch/fhnw/dlcopy/icons/32x32/drive-removable-media.png")));
                LOGGER.log(Level.WARNING,
                        "unsupported deviceType:{0}", deviceType);
        }

        iconGap = iconLabel.getWidth() + iconInsets;
        Graphics2D graphics2D = (Graphics2D) g;
        int componentWidth = getWidth();
        int height = getHeight();
        long overhead = storageSize - BOOT_PARTITION_SIZE - systemSize;
        int usbStorageWidth = (int) (((componentWidth - iconGap - 2 * OFFSET)
                * storageSize) / maxStorageDeviceSize);
        PartitionState partitionState
                = DLCopy.getPartitionState(storageSize, systemSize);

        // draw top text
        graphics2D.setPaint(Color.BLACK);
        graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        int rectangleTop;
        if (partitionState == PartitionState.TOO_SMALL) {
            rectangleTop = drawTopText(graphics2D, deviceText);
        } else {
            String text = DLCopy.STRINGS.getString("Proposed_Partitioning");
            text = MessageFormat.format(text, deviceText);
            rectangleTop = drawTopText(graphics2D, text);
        }

        // paint usb stick rectangle
        if (partitionState == PartitionState.TOO_SMALL) {
            graphics2D.setPaint(Color.GRAY);
        } else {
            graphics2D.setPaint(LIGHT_BLUE);
        }
        graphics2D.fillRect(
                iconGap + OFFSET, rectangleTop, usbStorageWidth, BAR_HEIGHT);

        // do not paint exchange partition when not selected
        if ((partitionState == PartitionState.EXCHANGE) && !isSelected) {
            partitionState = PartitionState.PERSISTENCE;
        }
        // paint additional blocks and texts
        switch (partitionState) {
            case TOO_SMALL:
                // paint error text
                String errorText = DLCopy.STRINGS.getString("Too_Small");
                drawCenterText(iconGap + OFFSET, rectangleTop, usbStorageWidth,
                        BAR_HEIGHT, errorText, graphics2D);
                break;

            case ONLY_SYSTEM:
                // paint OS text
                String text = LernstickFileTools.getDataVolumeString(
                        storageSize, 1);
                drawCenterText(iconGap + OFFSET, rectangleTop, usbStorageWidth,
                        BAR_HEIGHT, text, graphics2D);
                break;

            case PERSISTENCE:
                // block widths
                int bootWidth = (int) ((usbStorageWidth * BOOT_PARTITION_SIZE)
                        / storageSize);
                int systemWidth = (int) ((usbStorageWidth * systemSize)
                        / storageSize);
                int persistentWidth = usbStorageWidth - bootWidth - systemWidth;

                // texts
                String persistentText
                        = LernstickFileTools.getDataVolumeString(overhead, 1);
                String systemText
                        = LernstickFileTools.getDataVolumeString(systemSize, 1);

                // paint boot partition block
                int bootPartitionX = iconGap + OFFSET;
                graphics2D.setPaint(DARK_BLUE);
                graphics2D.fillRect(bootPartitionX, rectangleTop,
                        bootWidth, BAR_HEIGHT);

                // paint persistent partition block
                int persistentPartitionX = bootPartitionX + bootWidth;
                graphics2D.setPaint(Color.GREEN);
                graphics2D.fillRect(persistentPartitionX, rectangleTop,
                        persistentWidth, BAR_HEIGHT);

                // paint persistent partition text
                drawCenterText(persistentPartitionX, rectangleTop,
                        persistentWidth, BAR_HEIGHT, persistentText,
                        graphics2D);

                // paint system partition text
                int systemPartitionX = persistentPartitionX + persistentWidth;
                drawCenterText(systemPartitionX, rectangleTop, systemWidth,
                        BAR_HEIGHT, systemText, graphics2D);

                break;

            case EXCHANGE:
                // block widths
                bootWidth = (int) ((usbStorageWidth * BOOT_PARTITION_SIZE)
                        / storageSize);
                systemWidth = (int) ((usbStorageWidth * systemSize)
                        / storageSize);
                JSlider getExchangePartitionSizeSlider
                        = dlCopy.getExchangePartitionSizeSlider();
                long exchangeSize
                        = (long) getExchangePartitionSizeSlider.getValue()
                        * DLCopy.MEGA;
                int exchangeWidth = (int) ((usbStorageWidth * exchangeSize)
                        / storageSize);
                int maximumExchangeSizeMega
                        = getExchangePartitionSizeSlider.getMaximum();
                long maximumExchangeSize
                        = (long) maximumExchangeSizeMega * DLCopy.MEGA;
                long persistentSize = 0;
                persistentWidth = 0;
                // we need to calculate with overheadMega because we define MiB
                // in the exchange partition size slider
                // (calculating with byte values is "too exact"...)
                long overheadMega = overhead / DLCopy.MEGA;
                if ((overheadMega != maximumExchangeSizeMega)
                        || (exchangeSize != maximumExchangeSize)) {
                    persistentSize = storageSize
                            - BOOT_PARTITION_SIZE - exchangeSize - systemSize;
                    LOGGER.log(Level.FINEST,
                            "\nstorageSize: {0}\nBOOT_PARTITION_SIZE: {1}"
                            + "\nexchangeSize: {2}\nsystemSize: {3}"
                            + "\npersistentSize: {4}",
                            new Object[]{storageSize, BOOT_PARTITION_SIZE,
                                exchangeSize, systemSize, persistentSize});
                    persistentWidth = usbStorageWidth
                            - bootWidth - exchangeWidth - systemWidth;
                }

                // texts
                String exchangeText = LernstickFileTools.getDataVolumeString(
                        exchangeSize, 1);
                persistentText = LernstickFileTools.getDataVolumeString(
                        persistentSize, 1);
                systemText = LernstickFileTools.getDataVolumeString(
                        systemSize, 1);

                bootPartitionX = iconGap + OFFSET;
                int exchangePartitionX = bootPartitionX + bootWidth;
                persistentPartitionX = exchangePartitionX + exchangeWidth;

                // paint color blocks first and texts later
                // this way the persistent color block can not overwrite the
                // exchange text...
                // color blocks
                graphics2D.setPaint(DARK_BLUE);
                graphics2D.fillRect(bootPartitionX, rectangleTop,
                        bootWidth, BAR_HEIGHT);
                if (exchangeWidth > 0) {
                    graphics2D.setPaint(Color.YELLOW);
                    graphics2D.fillRect(exchangePartitionX, rectangleTop,
                            exchangeWidth, BAR_HEIGHT);
                }
                if (persistentWidth > 0) {
                    graphics2D.setPaint(Color.GREEN);
                    graphics2D.fillRect(persistentPartitionX, rectangleTop,
                            persistentWidth, BAR_HEIGHT);
                }

                // texts
                if (exchangeWidth > 0) {
                    drawCenterText(exchangePartitionX, rectangleTop,
                            exchangeWidth, BAR_HEIGHT,
                            exchangeText, graphics2D);
                }
                if (persistentWidth > 0) {
                    drawCenterText(persistentPartitionX, rectangleTop,
                            persistentWidth, BAR_HEIGHT,
                            persistentText, graphics2D);
                }

                systemPartitionX = persistentPartitionX + persistentWidth;
                drawCenterText(systemPartitionX, rectangleTop, systemWidth,
                        BAR_HEIGHT, systemText, graphics2D);
                break;

            default:
                LOGGER.log(Level.WARNING,
                        "unknown partitionState \"{0}\"", partitionState);
        }

        graphics2D.setPaint(Color.BLACK);
        int separatorPosition = height - 1;

        graphics2D.drawLine(
                0, separatorPosition, componentWidth, separatorPosition);
    }

    /**
     * sets the size of the largest USB stick
     *
     * @param maxSize the size of the largest USB stick
     */
    public void setMaxSize(long maxSize) {
        this.maxStorageDeviceSize = maxSize;
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        iconLabel = new javax.swing.JLabel();

        setPreferredSize(new java.awt.Dimension(340, 70));
        setLayout(new java.awt.GridBagLayout());

        iconLabel.setIcon(new javax.swing.ImageIcon(getClass().getResource("/ch/fhnw/dlcopy/icons/32x32/drive-removable-media-usb-pendrive.png"))); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 3, 0, 0);
        add(iconLabel, gridBagConstraints);
    }// </editor-fold>//GEN-END:initComponents
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel iconLabel;
    // End of variables declaration//GEN-END:variables

    private int drawTopText(Graphics2D graphics2D, String text) {
        FontMetrics fontMetrics = graphics2D.getFontMetrics();
        Rectangle2D stringBounds
                = fontMetrics.getStringBounds(text, graphics2D);
        int stringHeight = (int) stringBounds.getHeight();
        graphics2D.drawString(text,
                iconGap + OFFSET,
                OFFSET - (int) stringBounds.getY());
        return stringHeight + 2 * OFFSET;
    }

    private void drawCenterText(int x, int y, int width, int height,
            String text, Graphics2D graphics2D) {
        Font originalFont = graphics2D.getFont();

        Rectangle2D stringBounds = setFont(graphics2D, text, width);
        int stringWidth = (int) stringBounds.getWidth();
        int stringHeight = (int) stringBounds.getHeight();
        graphics2D.setPaint(Color.BLACK);
        int textX = x + (width - stringWidth) / 2;
        int textY = y + (height - stringHeight) / 2
                - (int) stringBounds.getY();
        graphics2D.drawString(text, textX, textY);
        graphics2D.setFont(originalFont);
    }

    private Rectangle2D setFont(
            Graphics2D graphics2D, String string, int width) {
        FontMetrics fontMetrics = graphics2D.getFontMetrics();
        Font font = graphics2D.getFont();
        for (int stringWidth = width + 1; stringWidth > width;) {
            Rectangle2D stringBounds = fontMetrics.getStringBounds(
                    string, graphics2D);
            stringWidth = (int) stringBounds.getWidth() + 2;
            //System.out.println("stringWidth: " + stringWidth);
            if ((font.getSize() > 7) && (stringWidth > width)) {
                //System.out.println("old font: " + font);
                font = font.deriveFont(font.getSize() - 1f);
                //System.out.println("new font: " + font);
                graphics2D.setFont(font);
                fontMetrics = graphics2D.getFontMetrics();
            } else {
                return stringBounds;
            }
        }
        return null;
    }
}
