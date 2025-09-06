package org.example;

import javax.swing.*;
import java.awt.*;
import java.sql.Connection;
import java.sql.SQLException;

public class MainMenu extends JFrame {
    private Connection conn;

    public MainMenu() {
        conn = DBConnection.connect();
        if (conn == null) {
            JOptionPane.showMessageDialog(this, "Failed to connect to the database. Exiting...", "Error", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }

        setTitle("Smart City Transportation System");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setUndecorated(true);

        GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getDefaultScreenDevice()
                .setFullScreenWindow(this);

        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int width = screenSize.width;
        int height = screenSize.height;

        ImageIcon bgIcon = new ImageIcon("Map.jpg");
        Image scaledImg = bgIcon.getImage().getScaledInstance(width, height, Image.SCALE_SMOOTH);
        JLabel background = new JLabel(new ImageIcon(scaledImg));
        background.setLayout(new GridBagLayout());

        JPanel buttonPanel = new JPanel(new GridLayout(7, 1, 20, 20));
        buttonPanel.setOpaque(false);
        buttonPanel.setPreferredSize(new Dimension(400, 450));

        String[] buttons = {
                "Traffic Simulation",
                "Emergency Response Planning",
                "Network Design",
                "Public Transit Optimization",
                "InformationAboutApp",
                "Exit"
        };

        for (String name : buttons) {
            JButton btn = new JButton(name);
            btn.setFont(new Font("Arial", Font.BOLD, 22));
            btn.setFocusPainted(false);
            btn.setBackground(new Color(34, 70, 54));
            btn.setForeground(Color.WHITE);

            switch (name) {
                case "Network Design":
                    btn.addActionListener(e -> {
                        NetworkDesignerGUI gui = new NetworkDesignerGUI(conn);
                        gui.setVisible(true);
                    });
                    break;
                case "Traffic Simulation":
                    btn.addActionListener(e -> {
                        TrafficSimulation simulation = new TrafficSimulation(conn);
                        simulation.setVisible(true);
                    });
                    break;
                case "Emergency Response Planning":
                    btn.addActionListener(e -> {
                        EmergencyRouting simulation = null;
                        simulation = new EmergencyRouting();
                        simulation.setVisible(true);
                    });
                    break;
                case "Public Transit Optimization":
                    btn.addActionListener(e -> {
                        PublicTransitGUI gui = new PublicTransitGUI(conn);
                        gui.setVisible(true);
                    });
                    break;
                case "InformationAboutApp":
                    btn.addActionListener(e -> {
                        InformationAboutApp infoPage = new InformationAboutApp();
                        infoPage.setVisible(true);
                        dispose(); // Optional: Close the MainMenu window
                    });

                    break;
                case "Exit":
                    btn.addActionListener(e -> {
                        try {
                            if (conn != null) conn.close();
                        } catch (SQLException ex) {
                            ex.printStackTrace();
                        }
                        System.exit(0);
                    });
                    break;
                default:
                    btn.addActionListener(e -> JOptionPane.showMessageDialog(
                            this, name + " not implemented yet."));
                    break;
            }

            buttonPanel.add(btn);
        }

        background.add(buttonPanel, new GridBagConstraints());
        setContentPane(background);
        setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(MainMenu::new);
    }
}