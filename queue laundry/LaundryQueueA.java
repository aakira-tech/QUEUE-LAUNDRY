import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.Queue;

public class LaundryQueueA extends JFrame {

    // Customer Model
    static class Customer {
        int ticket;
        String name;
        String service;
        String timeIn;

        Customer(int ticket, String name, String service) {
            this.ticket = ticket;
            this.name = (name == null || name.trim().isEmpty()) ? "Walk-in" : name.trim();
            this.service = service;
            this.timeIn = new SimpleDateFormat("hh:mm a").format(new Date());
        }
    }

    // FIFO Queue and Tracking
    private final Queue<Customer> queue = new LinkedList<>();
    private Customer currentServing = null;
    private int ticketCounter = 1;

    // Form Inputs
    private final JTextField nameField = new JTextField();
    private final JComboBox<String> serviceCombo = new JComboBox<>(new String[]{
        "Wash & Dry",
        "Wash Only",
        "Dry Only",
        "Wash & Fold",
        "Dry Clean"
    });

    // Now Serving Displays
    private final JLabel servingTicketLabel = new JLabel("TICKET: NONE (IDLE)");
    private final JLabel servingInfoLabel = new JLabel("No customer currently in service.");

    // Tables
    private final DefaultTableModel waitingModel = new DefaultTableModel(
        new String[]{"Pos", "Ticket #", "Customer Name", "Service", "Time Queued"}, 0) {
        @Override
        public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable waitingTable = new JTable(waitingModel);

    private final DefaultTableModel completedModel = new DefaultTableModel(
        new String[]{"Ticket #", "Customer Name", "Service", "Time In", "Time Finished"}, 0) {
        @Override
        public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable completedTable = new JTable(completedModel);

    // Summary Labels
    private final JLabel countLabel = new JLabel("Waiting: 0 | Completed: 0");

    public LaundryQueueA() {
        setTitle("Laundry Queue (A - Resets to #1)");
        setSize(780, 520);
        setMinimumSize(new Dimension(680, 460));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        // Top Header
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBorder(new EmptyBorder(10, 15, 10, 15));
        JLabel titleLabel = new JLabel("🧺 Laundry Queue Management (FIFO)");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 17));
        countLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        headerPanel.add(titleLabel, BorderLayout.WEST);
        headerPanel.add(countLabel, BorderLayout.EAST);
        add(headerPanel, BorderLayout.NORTH);

        // Left Panel (Input Form + Now Serving + Actions)
        JPanel leftPanel = new JPanel();
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
        leftPanel.setPreferredSize(new Dimension(300, 0));
        leftPanel.setBorder(new EmptyBorder(0, 10, 10, 0));

        // 1. Input Panel
        JPanel inputPanel = new JPanel(new GridLayout(3, 2, 6, 8));
        inputPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createEtchedBorder(), "Add Customer (Enqueue)",
            TitledBorder.LEFT, TitledBorder.TOP, new Font("Segoe UI", Font.BOLD, 12)));

        JButton addButton = new JButton("Add to Queue");
        addButton.setFont(new Font("Segoe UI", Font.BOLD, 12));

        inputPanel.add(new JLabel("Name:"));
        inputPanel.add(nameField);
        inputPanel.add(new JLabel("Service:"));
        inputPanel.add(serviceCombo);
        inputPanel.add(new JLabel(""));
        inputPanel.add(addButton);

        // 2. Now Serving Panel
        JPanel servingPanel = new JPanel(new GridLayout(2, 1, 2, 4));
        servingPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createEtchedBorder(), "Now Serving (Active)",
            TitledBorder.LEFT, TitledBorder.TOP, new Font("Segoe UI", Font.BOLD, 12)));

        servingTicketLabel.setFont(new Font("Segoe UI", Font.BOLD, 15));
        servingTicketLabel.setForeground(new Color(0, 102, 51));
        servingInfoLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        servingPanel.add(servingTicketLabel);
        servingPanel.add(servingInfoLabel);

        // 3. Actions Panel
        JPanel actionPanel = new JPanel(new GridLayout(3, 1, 6, 6));
        actionPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createEtchedBorder(), "Queue Actions",
            TitledBorder.LEFT, TitledBorder.TOP, new Font("Segoe UI", Font.BOLD, 12)));

        JButton serveButton = new JButton("Serve Next (FIFO)");
        serveButton.setFont(new Font("Segoe UI", Font.BOLD, 12));

        JButton finishButton = new JButton("Finish Current Order");
        finishButton.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        JButton clearButton = new JButton("Clear Queue (Reset to #1)");
        clearButton.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        actionPanel.add(serveButton);
        actionPanel.add(finishButton);
        actionPanel.add(clearButton);

        leftPanel.add(inputPanel);
        leftPanel.add(Box.createVerticalStrut(8));
        leftPanel.add(servingPanel);
        leftPanel.add(Box.createVerticalStrut(8));
        leftPanel.add(actionPanel);

        add(leftPanel, BorderLayout.WEST);

        // Right Panel: Tabbed Table (Waiting Queue + Completed History)
        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(new Font("Segoe UI", Font.BOLD, 12));

        setupTable(waitingTable);
        setupTable(completedTable);

        tabs.addTab("⏳ Waiting Queue", new JScrollPane(waitingTable));
        tabs.addTab("✅ Completed Orders", new JScrollPane(completedTable));
        tabs.setBorder(new EmptyBorder(0, 0, 10, 10));

        add(tabs, BorderLayout.CENTER);

        // Event Handlers
        addButton.addActionListener(e -> addToQueue());
        nameField.addActionListener(e -> addToQueue());

        serveButton.addActionListener(e -> serveNext());
        finishButton.addActionListener(e -> finishOrder());
        clearButton.addActionListener(e -> clearQueue());
    }

    // ==========================================
    // FIFO QUEUE OPERATIONS
    // ==========================================

    private void addToQueue() {
        String name = nameField.getText().trim();
        String service = (String) serviceCombo.getSelectedItem();

        Customer customer = new Customer(ticketCounter++, name, service);
        queue.offer(customer); // FIFO enqueue (add to back)

        refreshWaitingTable();
        updateCountLabel();

        nameField.setText("");
        nameField.requestFocus();
    }

    private void serveNext() {
        if (queue.isEmpty()) {
            JOptionPane.showMessageDialog(this, "The queue is empty! No customers to serve.");
            return;
        }

        // If a customer is currently being served, ask for confirmation first
        if (currentServing != null) {
            int confirm = JOptionPane.showConfirmDialog(
                this,
                String.format("Ticket #%d (%s) is still active in service.\nDo you want to complete their order and serve the next customer?",
                    currentServing.ticket, currentServing.name),
                "Customer Still Active",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
            );

            if (confirm != JOptionPane.YES_OPTION) {
                return; // Cancelled by user; do not dequeue next customer
            }

            autoFinishCurrentOrder();
        }

        // FIFO dequeue (remove from front)
        currentServing = queue.poll();

        servingTicketLabel.setText(String.format("TICKET #%d", currentServing.ticket));
        servingInfoLabel.setText(String.format("%s - %s", currentServing.name, currentServing.service));

        refreshWaitingTable();
        updateCountLabel();
    }

    private void finishOrder() {
        if (currentServing == null) {
            JOptionPane.showMessageDialog(this, "No customer is currently being served.");
            return;
        }

        autoFinishCurrentOrder();
        servingTicketLabel.setText("TICKET: NONE (IDLE)");
        servingInfoLabel.setText("No customer currently in service.");
        updateCountLabel();
        JOptionPane.showMessageDialog(this, "Order completed and moved to Completed Orders.");
    }

    private void autoFinishCurrentOrder() {
        String finishedTime = new SimpleDateFormat("hh:mm a").format(new Date());
        completedModel.addRow(new Object[]{
            "#" + currentServing.ticket,
            currentServing.name,
            currentServing.service,
            currentServing.timeIn,
            finishedTime
        });
        currentServing = null;
    }

    private void clearQueue() {
        if (queue.isEmpty() && currentServing == null) {
            JOptionPane.showMessageDialog(this, "Queue is already empty!");
            return;
        }

        int choice = JOptionPane.showConfirmDialog(this,
            "Clear the waiting queue and reset ticket counter to #1?",
            "Confirm Reset", JOptionPane.YES_NO_OPTION);

        if (choice == JOptionPane.YES_OPTION) {
            queue.clear();
            currentServing = null;
            ticketCounter = 1; // 👈 RESET HERE (Queue A specification)

            servingTicketLabel.setText("TICKET: NONE (IDLE)");
            servingInfoLabel.setText("No customer currently in service.");

            refreshWaitingTable();
            updateCountLabel();
        }
    }

    private void refreshWaitingTable() {
        waitingModel.setRowCount(0);
        int pos = 1;
        for (Customer c : queue) {
            waitingModel.addRow(new Object[]{
                pos++,
                "#" + c.ticket,
                c.name,
                c.service,
                c.timeIn
            });
        }
    }

    private void updateCountLabel() {
        countLabel.setText(String.format("Waiting: %d | Completed: %d", queue.size(), completedModel.getRowCount()));
    }

    private void setupTable(JTable table) {
        table.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        table.setRowHeight(24);
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));

        DefaultTableCellRenderer center = new DefaultTableCellRenderer();
        center.setHorizontalAlignment(SwingConstants.CENTER);

        // Center align position, ticket, and time columns
        table.getColumnModel().getColumn(0).setCellRenderer(center);
        table.getColumnModel().getColumn(1).setCellRenderer(center);
        table.getColumnModel().getColumn(table.getColumnCount() - 1).setCellRenderer(center);
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        SwingUtilities.invokeLater(() -> new LaundryQueueA().setVisible(true));
    }
}