/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package streaming.streamingclient;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.io.File;
import java.util.List;

/**
 *
 * @author Nevena
 */
public class Dashboard extends JFrame {

    /*** Custom purple colors ***/
    private static final Color PURPLE_HI = new Color(0xD8, 0xC8, 0xFF);
    private static final Color PURPLE_MID = new Color(0xA0, 0x88, 0xE8);
    private static final Color PURPLE_DIM = new Color(0x60, 0x48, 0xAA);
    private static final Color OVERLAY = new Color(0x08, 0x04, 0x18, 180);
    private static final Color BTN_BG = new Color(0x28, 0x18, 0x58, 200);
    private static final Color BTN_BORDER = new Color(0x80, 0x60, 0xCC, 180);

    /*** Buttons (StreamingClient has listeners) ***/
    public final JButton connectBtn;
    public final JButton speedTestBtn;
    public final JButton fetchBtn;
    public final JButton streamBtn;

    /*** Other components ***/
    public final JComboBox<String> formatCombo;
    public final JComboBox<String> protocolCombo;
    public final JLabel speedLabel;
    public final JCheckBox recordCheckBox;

    /*** Widgets ***/
    private final DefaultListModel<String> fileListModel;
    private final JList<String> fileList;
    private final JTextArea logArea;

    /*** Backgorund & sw icon ***/
    private final Image bgImage;
    private final Image r2d2Image;

    /*** Constructor ***/
    public Dashboard() {
        super("Streaming Client");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(720, 580);
        setMinimumSize(new Dimension(600, 480));
        setLocationRelativeTo(null);

        bgImage = loadImg("space.jpg");
        r2d2Image = loadImg("r2d2.png");

        /*** Button labels ***/
        connectBtn = spaceButton("1) Connect");
        speedTestBtn = spaceButton("2) Speed Test (5s)");
        fetchBtn = spaceButton("3) Get Files");
        streamBtn = spaceButton("4) Start Streaming");

        speedTestBtn.setEnabled(false);
        fetchBtn.setEnabled(false);
        streamBtn.setEnabled(false);

        /*** Combo labels ***/
        formatCombo = spaceCombo(new String[]{"mkv", "mp4", "avi"});
        protocolCombo = spaceCombo(new String[]{"Auto-choice", "TCP", "UDP", "RTP/UDP"});

        /*** Speed label ***/
        speedLabel = new JLabel("Speed: not measured");
        speedLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        speedLabel.setForeground(PURPLE_MID);

        /*** Record checkbox ***/
        recordCheckBox = new JCheckBox("Record stream to file");
        recordCheckBox.setOpaque(false);
        recordCheckBox.setForeground(PURPLE_MID);
        recordCheckBox.setFont(new Font("SansSerif", Font.PLAIN, 12));

        /*** File list ***/
        fileListModel = new DefaultListModel<>();
        fileList = new JList<>(fileListModel);
        fileList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        fileList.setOpaque(false);
        fileList.setForeground(PURPLE_HI);
        fileList.setFont(new Font("SansSerif", Font.PLAIN, 12));
        fileList.setFixedCellHeight(28);
        fileList.setCellRenderer(new SpaceCellRenderer());

        /*** Log area ***/
        logArea = new JTextArea(7, 60);
        logArea.setEditable(false);
        logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
        logArea.setOpaque(false);
        logArea.setForeground(PURPLE_MID);
        logArea.setCaretColor(PURPLE_HI);
        logArea.setLineWrap(true);
        logArea.setWrapStyleWord(true);

        buildLayout();
    }

    /*** Layout ***/
    private void buildLayout() {

        // Background panel (paints nebula & R2-D2)
        JPanel bg = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                int w = getWidth(), h = getHeight();
                if (bgImage != null) 
                    g2.drawImage(bgImage, 0, 0, w, h, null);
                else {
                    g2.setColor(new Color(0x08, 0x04, 0x18));
                    g2.fillRect(0, 0, w, h);
                }
                g2.setColor(OVERLAY);
                g2.fillRect(0, 0, w, h);
                if (r2d2Image != null) {
                    int size = 100, margin = 12;
                    g2.setComposite(AlphaComposite.getInstance(
                            AlphaComposite.SRC_OVER, 0.88f));
                    g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, 
                            RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
                    g2.drawImage(r2d2Image, w - size - margin, h - size - margin, size, size, null);
                    g2.setComposite(AlphaComposite.getInstance(
                            AlphaComposite.SRC_OVER, 1f));
                }
            }
        };
        bg.setOpaque(true);
        setContentPane(bg);

        // Top: 1) Connect | 2) Speed Test | speed label
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        top.setOpaque(false);
        top.add(connectBtn);
        top.add(speedTestBtn);
        top.add(speedLabel);
        bg.add(top, BorderLayout.NORTH);

        // Centre: format row / file list / protocol + record + stream row
        JPanel center = new JPanel(new BorderLayout(6, 6));
        center.setOpaque(false);
        center.setBorder(new EmptyBorder(0, 12, 0, 12));

        JPanel formatRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4));
        formatRow.setOpaque(false);
        formatRow.add(purpleLabel("Format:"));
        formatRow.add(formatCombo);
        formatRow.add(fetchBtn);
        center.add(formatRow, BorderLayout.NORTH);

        JScrollPane listScroll = new JScrollPane(fileList, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        listScroll.setOpaque(false);
        listScroll.getViewport().setOpaque(false);
        listScroll.setBorder(spaceBorder("Available Files"));
        center.add(listScroll, BorderLayout.CENTER);

        JPanel streamRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4));
        streamRow.setOpaque(false);
        streamRow.add(purpleLabel("Protocol:"));
        streamRow.add(protocolCombo);
        streamRow.add(recordCheckBox);
        streamRow.add(streamBtn);
        center.add(streamRow, BorderLayout.SOUTH);

        bg.add(center, BorderLayout.CENTER);

        // Bottom: log area
        JScrollPane logScroll = new JScrollPane(logArea, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        logScroll.setOpaque(false);
        logScroll.getViewport().setOpaque(false);
        logScroll.setBorder(spaceBorder("Log"));

        JPanel logWrap = new JPanel(new BorderLayout());
        logWrap.setOpaque(false);
        logWrap.setBorder(new EmptyBorder(0, 12, 12, 12));
        logWrap.add(logScroll);
        bg.add(logWrap, BorderLayout.SOUTH);
    }

    /*** API ***/
    // Appends a line to the log area (thread-safe)
    public void log(final String message) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                logArea.append(message + "\n");
                logArea.setCaretPosition(logArea.getDocument().getLength());
            }
        });
    }

    // Replaces the file list contents (thread-safe)& Enables stream button if non-empty
    public void setFileList(final List<String> files) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                fileListModel.clear();
                for (int i = 0; i < files.size(); i++) {
                    fileListModel.addElement(files.get(i));
                }
                streamBtn.setEnabled(!files.isEmpty());
            }
        });
    }

    // Updates the speed label (thread-safe)
    public void setSpeedText(final String text) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                speedLabel.setText(text);
            }
        });
    }

    public String getSelectedFile()     { 
        return fileList.getSelectedValue(); 
    }
    public String getSelectedFormat()   { 
        return (String) formatCombo.getSelectedItem(); 
    }
    public boolean isRecordingEnabled() { 
        return recordCheckBox.isSelected(); 
    }

    public String getSelectedProtocol() {
        String p = (String) protocolCombo.getSelectedItem();
        return "Auto-choice".equals(p) ? "AUTO" : p;
    }


    /*** Styled component setters ***/

    private JButton spaceButton(String text) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                    RenderingHints.VALUE_ANTIALIAS_ON);
                Color bg;
                if (!isEnabled()) {
                    bg = new Color(0x18, 0x10, 0x30, 120);
                } else if (getModel().isPressed()) {
                    bg = new Color(0x40, 0x28, 0x80, 210);
                } else if (getModel().isRollover()) {
                    bg = new Color(0x38, 0x22, 0x70, 210);
                } else {
                    bg = BTN_BG;
                }
                g2.setColor(bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.setColor(isEnabled() ? BTN_BORDER : new Color(0x40, 0x30, 0x60, 100));
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                super.paintComponent(g);
            }
        };
        btn.setFont(new Font("SansSerif", Font.PLAIN, 12));
        btn.setForeground(PURPLE_HI);
        btn.setOpaque(false);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setMargin(new Insets(5, 14, 5, 14));
        return btn;
    }

    private JComboBox<String> spaceCombo(String[] items) {
        JComboBox<String> cb = new JComboBox<>(items);
        cb.setFont(new Font("SansSerif", Font.PLAIN, 12));
        cb.setForeground(PURPLE_HI);
        cb.setBackground(new Color(0x18, 0x10, 0x30));
        cb.setBorder(BorderFactory.createLineBorder(PURPLE_DIM, 1));
        return cb;
    }

    private JLabel purpleLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("SansSerif", Font.PLAIN, 12));
        l.setForeground(PURPLE_MID);
        return l;
    }

    private Border spaceBorder(String title) {
        TitledBorder tb = BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(PURPLE_DIM, 1), title);
        tb.setTitleColor(PURPLE_MID);
        tb.setTitleFont(new Font("SansSerif", Font.PLAIN, 11));
        return tb;
    }

    /*** Image loader ***/
    private Image loadImg(String name) {
        try {
            java.net.URL url = getClass().getResource("/" + name);
            if (url == null) {
                File f = new File(name);
                if (f.exists()) {
                    url = f.toURI().toURL();
                }
            }
            if (url != null) {
                return new ImageIcon(url).getImage();
            }
        } catch (Exception ignored) {}
        return null;
    }

    /*** Cell Renderer ***/
    private static class SpaceCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(
                JList<?> list, Object value, int index,
                boolean isSelected, boolean hasFocus) {
            JLabel l = (JLabel) super.getListCellRendererComponent(
                    list, value, index, isSelected, hasFocus);
            l.setFont(new Font("SansSerif", Font.PLAIN, 12));
            l.setBorder(new EmptyBorder(4, 10, 4, 10));
            if (isSelected) {
                l.setBackground(new Color(0x40, 0x28, 0x80, 180));
                l.setForeground(new Color(0xE8, 0xDC, 0xFF));
            } else {
                l.setBackground(new Color(0x10, 0x08, 0x28, 140));
                l.setForeground(new Color(0xA0, 0x88, 0xE8));
            }
            l.setOpaque(true);
            return l;
        }
    }
}