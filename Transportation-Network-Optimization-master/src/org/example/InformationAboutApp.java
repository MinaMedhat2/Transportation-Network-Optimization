package org.example;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.file.*;
import java.awt.Desktop;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.awt.image.RescaleOp;

public class InformationAboutApp extends JFrame {
    public InformationAboutApp() {
        // Set up the frame
        setTitle("Information About App");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setUndecorated(true);

        // Create a panel with background image
        BackgroundPanel mainPanel = new BackgroundPanel("BACKGR.PNG");
        mainPanel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.insets = new Insets(10, 0, 10, 0);

        // Semi-transparent card for team info
        JPanel cardPanel = new JPanel();
        cardPanel.setLayout(new BorderLayout());
        cardPanel.setOpaque(false);
        cardPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.WHITE, 2),
                BorderFactory.createEmptyBorder(20, 20, 20, 20)
        ));
        cardPanel.setBackground(new Color(0, 0, 0, 180)); // Semi-transparent dark background

        // Team information label with HTML table
        JLabel teamInfo = new JLabel(
                "<html><center>" +
                        "<h2 style='color:white; font-family:Segoe UI;'>Development Team</h2>" +
                        "<table style='font-size:18px; color:white; font-family:Segoe UI; border-collapse:separate; border-spacing:10px;'>" +
                        "<tr><td style='text-align:right; padding-right:20px;'>Mina Medhat William</td><td>22101117</td></tr>" +
                        "<tr><td style='text-align:right; padding-right:20px;'>Ahmed Nada Ahmed</td><td>22101167</td></tr>" +
                        "<tr><td style='text-align:right; padding-right:20px;'>Ahmed Ehab mohamed</td><td>21100778</td></tr>" +
                        "<tr><td style='text-align:right; padding-right:20px;'>Negma Abderhman Ali</td><td>22101242</td></tr>" +
                        "<tr><td style='text-align:right; padding-right:20px;'>Maryam Gomaa Gomaa</td><td>22100578</td></tr>" +
                        "<tr><td style='text-align:right; padding-right:20px;'>Aya Abdelmaged Ghallab</td><td>22100953</td></tr>" +
                        "</table></center></html>"
        );
        teamInfo.setHorizontalAlignment(SwingConstants.CENTER);
        cardPanel.add(teamInfo, BorderLayout.CENTER);

        // Presentation sentence
        JLabel presentation = new JLabel("This project is a presentation of Dijkstra's Algorithm implementation.");
        presentation.setFont(new Font("Segoe UI", Font.PLAIN, 25));
        presentation.setForeground(Color.WHITE);
        presentation.setHorizontalAlignment(SwingConstants.CENTER);

        // Panel for buttons
        JPanel buttonPanel = new JPanel();
        buttonPanel.setOpaque(false);
        buttonPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 20, 0));

        // Open button with open.png background
        JButton openButton = new JButton("");
        styleImageButton(openButton, "open.png");
        openButton.addActionListener(e -> openPDF());

        // Download button with dawnloads.png background
        JButton downloadButton = new JButton("");
        styleImageButton(downloadButton, "dawnloads.png");
        downloadButton.addActionListener(e -> downloadPDF());

        buttonPanel.add(openButton);
        buttonPanel.add(downloadButton);

        // Add components to main panel
        gbc.weighty = 0.1;
        mainPanel.add(cardPanel, gbc);
        mainPanel.add(presentation, gbc);
        gbc.insets = new Insets(20, 0, 80, 0); // Adjusted spacing
        mainPanel.add(buttonPanel, gbc);

        // Add menu button in top-right corner
        JButton menuButton = new JButton("");
        menuButton.setPreferredSize(new Dimension(50, 50)); // Larger size
        menuButton.setFocusPainted(false);
        menuButton.setCursor(new Cursor(Cursor.HAND_CURSOR));

        BufferedImage menuImage = null;
        try {
            // Load and scale img.png to button size
            menuImage = ImageIO.read(new File("img.png"));
            Image scaledImg = menuImage.getScaledInstance(50, 50, Image.SCALE_SMOOTH);
            menuButton.setIcon(new ImageIcon(scaledImg));
            menuButton.setBorder(BorderFactory.createEmptyBorder());
            menuButton.setContentAreaFilled(false); // Remove default background
        } catch (IOException e) {
            e.printStackTrace();
            // Fallback to solid color if image fails
            menuButton.setBackground(new Color(0, 120, 215));
            menuButton.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        }

        // Hover effect for menu button (slight brightening)
        final BufferedImage finalMenuImage = menuImage;
        menuButton.addMouseListener(new MouseAdapter() {
            private Icon originalIcon = menuButton.getIcon();
            @Override
            public void mouseEntered(MouseEvent e) {
                if (finalMenuImage != null) {
                    try {
                        // Determine number of color components
                        int numComponents = finalMenuImage.getColorModel().getNumComponents();
                        float[] scales = new float[numComponents];
                        float[] offsets = new float[numComponents];
                        for (int i = 0; i < numComponents; i++) {
                            scales[i] = (i < numComponents - 1 || !finalMenuImage.getColorModel().hasAlpha()) ? 1.2f : 1.0f;
                            offsets[i] = 0.0f;
                        }
                        RescaleOp op = new RescaleOp(scales, offsets, null);
                        BufferedImage brighterImg = op.filter(finalMenuImage, null);
                        Image scaledBrighter = brighterImg.getScaledInstance(50, 50, Image.SCALE_SMOOTH);
                        menuButton.setIcon(new ImageIcon(scaledBrighter));
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                menuButton.setIcon(originalIcon);
            }
        });

        // Action to open MainMenu
        menuButton.addActionListener(e -> {
            try {
                // Instantiate and show MainMenu
                Class<?> mainMenuClass = Class.forName("org.example.MainMenu");
                JFrame mainMenuFrame = (JFrame) mainMenuClass.getDeclaredConstructor().newInstance();
                mainMenuFrame.setVisible(true);
                // Close current window
                dispose();
            } catch (ClassNotFoundException ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(InformationAboutApp.this,
                        "Cannot find MainMenu class. Ensure MainMenu.java is in the org.example package and compiled.",
                        "Error", JOptionPane.ERROR_MESSAGE);
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(InformationAboutApp.this,
                        "Error opening MainMenu: " + ex.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        // Position menu button in extreme top-right corner
        mainPanel.setLayout(null); // Temporarily switch to null layout for absolute positioning
        menuButton.setBounds(mainPanel.getWidth() - 50, 0, 50, 50); // 0px from top, 0px from right
        mainPanel.add(menuButton);

        // Restore GridBagLayout for main components
        mainPanel.setLayout(new GridBagLayout());
        gbc.weighty = 0.1;
        mainPanel.add(cardPanel, gbc);
        mainPanel.add(presentation, gbc);
        gbc.insets = new Insets(20, 0, 80, 0);
        mainPanel.add(buttonPanel, gbc);

        // Add main panel to frame
        add(mainPanel);

        // Ensure panel size updates after frame is maximized
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                menuButton.setBounds(mainPanel.getWidth() - 50, 0, 50, 50);
            }
        });
    }

    // Style buttons with background image
    private void styleImageButton(JButton button, String imagePath) {
        button.setPreferredSize(new Dimension(150, 40));
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));

        BufferedImage originalImage = null;
        try {
            // Load and scale image to button size
            originalImage = ImageIO.read(new File(imagePath));
            Image scaledImg = originalImage.getScaledInstance(150, 40, Image.SCALE_SMOOTH);
            button.setIcon(new ImageIcon(scaledImg));
            button.setBorder(BorderFactory.createEmptyBorder());
            button.setContentAreaFilled(false); // Remove default background
        } catch (IOException e) {
            e.printStackTrace();
            // Fallback to solid color if image fails
            button.setBackground(new Color(0, 120, 215));
            button.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        }

        // Hover effect (slight brightening)
        final BufferedImage finalOriginalImage = originalImage;
        button.addMouseListener(new MouseAdapter() {
            private Icon originalIcon = button.getIcon();
            @Override
            public void mouseEntered(MouseEvent e) {
                if (finalOriginalImage != null) {
                    try {
                        // Determine number of color components
                        int numComponents = finalOriginalImage.getColorModel().getNumComponents();
                        float[] scales = new float[numComponents];
                        float[] offsets = new float[numComponents];
                        for (int i = 0; i < numComponents; i++) {
                            scales[i] = (i < numComponents - 1 || !finalOriginalImage.getColorModel().hasAlpha()) ? 1.2f : 1.0f;
                            offsets[i] = 0.0f;
                        }
                        RescaleOp op = new RescaleOp(scales, offsets, null);
                        BufferedImage brighterImg = op.filter(finalOriginalImage, null);
                        Image scaledBrighter = brighterImg.getScaledInstance(150, 40, Image.SCALE_SMOOTH);
                        button.setIcon(new ImageIcon(scaledBrighter));
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                button.setIcon(originalIcon);
            }
        });
    }

    // Open PDF using Desktop
    private void openPDF() {
        try {
            File pdfFile = new File("Dijkstras_Algorithm.pdf");
            if (pdfFile.exists()) {
                Desktop.getDesktop().open(pdfFile);
            } else {
                JOptionPane.showMessageDialog(this, "PDF file not found!", "Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Error opening PDF: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // Download PDF by copying to user-selected location
    private void downloadPDF() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setSelectedFile(new File("Dijkstras_Algorithm.pdf"));
        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                File sourceFile = new File("Dijkstras_Algorithm.pdf");
                if (sourceFile.exists()) {
                    File destFile = fileChooser.getSelectedFile();
                    Files.copy(sourceFile.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    JOptionPane.showMessageDialog(this, "PDF downloaded successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(this, "PDF file not found!", "Error", JOptionPane.ERROR_MESSAGE);
                }
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Error downloading PDF: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // Custom panel to handle background image
    class BackgroundPanel extends JPanel {
        private BufferedImage backgroundImage;

        public BackgroundPanel(String imagePath) {
            try {
                backgroundImage = ImageIO.read(new File(imagePath));
            } catch (IOException e) {
                e.printStackTrace();
                backgroundImage = null;
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

            if (backgroundImage != null) {
                // Scale image smoothly to fit window
                Dimension size = getSize();
                double scaleX = (double) size.width / backgroundImage.getWidth();
                double scaleY = (double) size.height / backgroundImage.getHeight();
                double scale = Math.max(scaleX, scaleY); // Maintain aspect ratio
                int newWidth = (int) (backgroundImage.getWidth() * scale);
                int newHeight = (int) (backgroundImage.getHeight() * scale);
                int x = (size.width - newWidth) / 2;
                int y = (size.height - newHeight) / 2;
                g2d.drawImage(backgroundImage, x, y, newWidth, newHeight, this);
            } else {
                // Fallback gradient if image fails
                GradientPaint gradient = new GradientPaint(0, 0, new Color(20, 40, 60), 0, getHeight(), new Color(60, 80, 100));
                g2d.setPaint(gradient);
                g2d.fillRect(0, 0, getWidth(), getHeight());
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new InformationAboutApp().setVisible(true);
        });
    }
}