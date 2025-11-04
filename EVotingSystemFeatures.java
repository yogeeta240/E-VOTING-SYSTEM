import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
// import java.io.*; // no longer used after DB switch
import java.util.*;
import java.util.List;
import java.sql.*;

/**
 * E-Voting System Features
 *
 * A single-file Java Swing application that demonstrates core e-voting features:
 * - User Authentication (Admin and Voter logins)
 * - Voter Registration and verification
 * - Candidate Management (add, edit, remove, view)
 * - Secure single-vote-per-user logic
 * - Voting session controls (start/end election)
 * - Real-time vote counting and display
 * - Final result announcement after vote closure
 * - Feedback dialogs and double-vote prevention
 * - Data persistence (simple file-based via Java serialization)
 * - Clear, modern UI with a feature checklist and demo buttons
 *
 * Note: This demo uses simple in-memory structures with optional serialization to a local file
 * to simulate persistence; no external database is used.
 */
public class EVotingSystemFeatures extends JFrame {

    // -----------------------------
    // Models and Persistence
    // -----------------------------

    /** Simple role enumeration. */
    private enum Role { ADMIN, VOTER }

    /** Basic user model. */
    private static class User {
        String username;
        String displayName;
        Role role;
        boolean verified; // for voters only

        User(String username, String displayName, Role role) {
            this.username = username;
            this.displayName = displayName;
            this.role = role;
            this.verified = role == Role.VOTER ? false : true;
        }
    }

    /** Candidate model. */
    private static class Candidate {
        int id; // database id
        String name;
        String manifesto;
        int votes;

        Candidate(int id, String name, String manifesto, int votes) {
            this.id = id;
            this.name = name;
            this.manifesto = manifesto;
            this.votes = votes;
        }
    }

    /**
     * DataStore holds users, candidates, voting state and voted users.
     * This is serialized to a file to simulate persistence.
     */
    // DataStore removed in DB-backed implementation

    // Logged-in session state (not persisted):
    private User loggedInAdmin = null;
    private User loggedInVoter = null;

    // -----------------------------
    // UI Components shared across tabs
    // -----------------------------
    private JLabel statusBar;
    private DefaultTableModel voteTableModel; // for real-time counts
    private JTable voteTable;
    private JPanel votingCandidatesPanel; // radio buttons for candidates
    private ButtonGroup votingButtonGroup;
    private JLabel electionStatusLabel;
    private JLabel resultsLabel;

    // Auth UI fields
    private JTextField adminUserField;
    private JPasswordField adminPassField;
    private JTextField voterUserField;

    // Registration UI fields
    private JTextField regUsernameField;
    private JTextField regDisplayNameField;
    private JList<String> regVoterList;
    private DefaultListModel<String> regVoterListModel;

    // Candidate UI fields
    private JList<String> candidateList;
    private DefaultListModel<String> candidateListModel;
    private JTextField candNameField;
    private JTextField candManifestoField;
    private List<Integer> candidateIds = new ArrayList<>();

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            EVotingSystemFeatures app = new EVotingSystemFeatures();
            app.setVisible(true);
        });
    }

    public EVotingSystemFeatures() {
        super("E-Voting System Features");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 700);
        setLocationRelativeTo(null);

        // Initialize SQLite database (optional, safe if driver not present)
        try { Db.init(); } catch (Throwable ignore) {}

        setLayout(new BorderLayout());
        add(buildHeader(), BorderLayout.NORTH);
        add(buildTabs(), BorderLayout.CENTER);
        add(buildStatusBar(), BorderLayout.SOUTH);
        refreshAllUI();
    }

    // -----------------------------
    // Header and Status Bar
    // -----------------------------
    private JComponent buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBorder(new EmptyBorder(10, 10, 10, 10));
        JLabel title = new JLabel("E-Voting System Features", SwingConstants.LEFT);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 20f));
        JLabel subtitle = new JLabel("Interact with each core feature via tabs and demo buttons");
        subtitle.setFont(subtitle.getFont().deriveFont(Font.PLAIN, 12f));
        JPanel left = new JPanel();
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.add(title);
        left.add(subtitle);

        electionStatusLabel = new JLabel("");
        electionStatusLabel.setFont(electionStatusLabel.getFont().deriveFont(Font.BOLD));
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        right.add(electionStatusLabel);

        header.add(left, BorderLayout.WEST);
        header.add(right, BorderLayout.EAST);
        return header;
    }

    private JComponent buildStatusBar() {
        statusBar = new JLabel("Ready.");
        statusBar.setBorder(new EmptyBorder(8, 10, 8, 10));
        return statusBar;
    }

    // -----------------------------
    // Tabs
    // -----------------------------
    private JTabbedPane buildTabs() {
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Checklist", buildChecklistTab());
        tabs.addTab("Authentication", buildAuthTab());
        tabs.addTab("Registration", buildRegistrationTab());
        tabs.addTab("Candidates", buildCandidatesTab());
        tabs.addTab("Session", buildSessionTab());
        tabs.addTab("Voting", buildVotingTab());
        tabs.addTab("Results", buildResultsTab());
        return tabs;
    }

    // -----------------------------
    // Checklist Tab
    // -----------------------------
    private JComponent buildChecklistTab() {
        JPanel panel = createPaddedPanel();
        panel.setLayout(new BorderLayout(10, 10));

        JPanel listPanel = createPaddedPanel();
        listPanel.setLayout(new GridLayout(0, 1, 6, 6));

        listPanel.add(makeChecklistItem(
                "User Authentication",
                "Admin and Voter login flows to secure different capabilities.",
                () -> switchToTab("Authentication")));
        listPanel.add(makeChecklistItem(
                "Voter Registration & Verification",
                "Register voters and verify them (admin only).",
                () -> switchToTab("Registration")));
        listPanel.add(makeChecklistItem(
                "Candidate Management",
                "Add, edit, remove, and view candidates (admin only).",
                () -> switchToTab("Candidates")));
        listPanel.add(makeChecklistItem(
                "Single Vote Enforcement",
                "Each verified voter can cast only one vote.",
                () -> switchToTab("Voting")));
        listPanel.add(makeChecklistItem(
                "Voting Session Controls",
                "Admin starts/ends election to control voting window.",
                () -> switchToTab("Session")));
        listPanel.add(makeChecklistItem(
                "Real-time Counting",
                "Live tally updates immediately when votes are cast.",
                () -> switchToTab("Voting")));
        listPanel.add(makeChecklistItem(
                "Final Results",
                "Results announced after election closes.",
                () -> switchToTab("Results")));
        listPanel.add(makeChecklistItem(
                "Feedback & Error Prevention",
                "Dialogs inform and prevent invalid actions (e.g., double voting).",
                () -> {}));
        listPanel.add(makeChecklistItem(
                "Data Persistence",
                "SQLite database is initialized at startup (evoting.db).",
                () -> showInfo("Database initialized (evoting.db).")));

        panel.add(new JLabel("Feature Checklist"), BorderLayout.NORTH);
        panel.add(new JScrollPane(listPanel), BorderLayout.CENTER);
        return panel;
    }

    private JPanel makeChecklistItem(String title, String description, Runnable demoAction) {
        JPanel item = new JPanel(new BorderLayout(10, 6));
        item.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 220, 220)),
                new EmptyBorder(8, 8, 8, 8)));
        JLabel t = new JLabel("\u2713 " + title);
        t.setFont(t.getFont().deriveFont(Font.BOLD));
        JLabel d = new JLabel("<html><body style='width: 580px'>" + description + "</body></html>");
        JButton demo = new JButton("Demo");
        demo.addActionListener((@SuppressWarnings("unused") ActionEvent ignored) -> demoAction.run());
        item.add(t, BorderLayout.NORTH);
        item.add(d, BorderLayout.CENTER);
        item.add(demo, BorderLayout.EAST);
        return item;
    }

    private void switchToTab(String tabTitle) {
        Container c = getContentPane();
        for (Component comp : ((Container)c).getComponents()) {
            if (comp instanceof JTabbedPane) {
                JTabbedPane tabs = (JTabbedPane) comp;
                for (int i = 0; i < tabs.getTabCount(); i++) {
                    if (tabTitle.equals(tabs.getTitleAt(i))) {
                        tabs.setSelectedIndex(i);
                        return;
                    }
                }
            }
        }
    }

    // -----------------------------
    // Authentication Tab
    // -----------------------------
    private JComponent buildAuthTab() {
        JPanel panel = createPaddedPanel();
        panel.setLayout(new GridLayout(1, 2, 12, 12));

        // Admin login
        JPanel adminPanel = createCardPanel("Admin Login");
        adminUserField = new JTextField();
        adminPassField = new JPasswordField();
        JButton adminLoginBtn = new JButton("Login as Admin");
        adminLoginBtn.addActionListener(this::onAdminLogin);
        JButton adminLogoutBtn = new JButton("Logout Admin");
        adminLogoutBtn.addActionListener((@SuppressWarnings("unused") ActionEvent ignored) -> {
            loggedInAdmin = null;
            showInfo("Admin logged out.");
            refreshAllUI();
        });
        adminPanel.add(labeled(adminUserField, "Username (try 'admin')"));
        adminPanel.add(labeled(adminPassField, "Password (try 'admin')"));
        adminPanel.add(adminLoginBtn);
        adminPanel.add(adminLogoutBtn);

        // Voter login
        JPanel voterPanel = createCardPanel("Voter Login");
        voterUserField = new JTextField();
        JButton voterLoginBtn = new JButton("Login as Voter");
        voterLoginBtn.addActionListener(this::onVoterLogin);
        JButton voterLogoutBtn = new JButton("Logout Voter");
        voterLogoutBtn.addActionListener((@SuppressWarnings("unused") ActionEvent ignored) -> {
            loggedInVoter = null;
            showInfo("Voter logged out.");
            refreshAllUI();
        });
        voterPanel.add(labeled(voterUserField, "Voter Username"));
        voterPanel.add(voterLoginBtn);
        voterPanel.add(voterLogoutBtn);

        panel.add(adminPanel);
        panel.add(voterPanel);
        return panel;
    }

    private void onAdminLogin(ActionEvent e) {
        String user = adminUserField.getText().trim();
        String pass = new String(adminPassField.getPassword());
        // Demo credentials: admin/admin
        if ("admin".equalsIgnoreCase(user) && "admin".equals(pass)) {
            // Ensure admin exists in DB (Db.init seeds it)
            loggedInAdmin = new User("admin", "Administrator", Role.ADMIN);
            showInfo("Admin login successful.");
        } else {
            showError("Invalid admin credentials.");
        }
        refreshAllUI();
    }

    private void onVoterLogin(ActionEvent e) {
        String username = voterUserField.getText().trim();
        if (username.isEmpty()) {
            showError("Enter a voter username.");
            return;
        }
        User u = dbGetUser(username);
        if (u == null || u.role != Role.VOTER) {
            showError("No such voter: " + username);
            return;
        }
        if (!u.verified) {
            showError("Voter is not verified yet.");
            return;
        }
        loggedInVoter = u;
        showInfo("Voter '" + u.displayName + "' (" + username + ") logged in.");
        refreshAllUI();
    }

    // -----------------------------
    // Registration Tab
    // -----------------------------
    private JComponent buildRegistrationTab() {
        JPanel panel = createPaddedPanel();
        panel.setLayout(new BorderLayout(10, 10));

        JPanel form = createCardPanel("Register New Voter");
        regUsernameField = new JTextField();
        regDisplayNameField = new JTextField();
        JButton registerBtn = new JButton("Register Voter");
        registerBtn.addActionListener((@SuppressWarnings("unused") ActionEvent ignored) -> {
            String username = regUsernameField.getText().trim();
            String display = regDisplayNameField.getText().trim();
            if (username.isEmpty() || display.isEmpty()) {
                showError("Provide both username and display name.");
                return;
            }
            if (dbUserExists(username)) {
                showError("Username already exists.");
                return;
            }
            dbAddVoter(username, display);
            showInfo("Voter registered: " + display + " (" + username + "). Awaiting verification.");
            updateRegVoterList();
        });
        form.add(labeled(regUsernameField, "Username"));
        form.add(labeled(regDisplayNameField, "Display Name"));
        form.add(registerBtn);

        JPanel manage = createCardPanel("Verify / Manage Voters (Admin)");
        regVoterListModel = new DefaultListModel<>();
        regVoterList = new JList<>(regVoterListModel);
        updateRegVoterList();
        JButton verifyBtn = new JButton("Verify Selected");
        verifyBtn.addActionListener((@SuppressWarnings("unused") ActionEvent ignored) -> {
            if (!requireAdmin()) return;
            String selected = regVoterList.getSelectedValue();
            if (selected == null) {
                showError("Select a voter.");
                return;
            }
            dbVerifyVoter(selected);
            User voter = dbGetUser(selected);
            if (voter != null) showInfo("Voter verified: " + voter.displayName + " (" + voter.username + ")");
            updateRegVoterList();
        });
        JButton removeBtn = new JButton("Remove Selected");
        removeBtn.addActionListener((@SuppressWarnings("unused") ActionEvent ignored) -> {
            if (!requireAdmin()) return;
            String selected = regVoterList.getSelectedValue();
            if (selected == null) {
                showError("Select a voter.");
                return;
            }
            User voter = dbGetUser(selected);
            dbRemoveVoter(selected);
            showInfo("Removed voter: " + (voter != null ? voter.displayName + " (" + selected + ")" : selected));
            updateRegVoterList();
        });
        manage.add(new JScrollPane(regVoterList));
        manage.add(verifyBtn);
        manage.add(removeBtn);

        panel.add(form, BorderLayout.NORTH);
        panel.add(manage, BorderLayout.CENTER);
        return panel;
    }

    private void updateRegVoterList() {
        if (regVoterListModel == null) return;
        regVoterListModel.clear();
        for (User u : dbLoadVoters()) {
            regVoterListModel.addElement(u.username);
        }
    }

    // -----------------------------
    // Candidates Tab
    // -----------------------------
    private JComponent buildCandidatesTab() {
        JPanel panel = createPaddedPanel();
        panel.setLayout(new BorderLayout(10, 10));

        candidateListModel = new DefaultListModel<>();
        candidateList = new JList<>(candidateListModel);
        refreshCandidateList();

        JPanel controls = createCardPanel("Add / Edit Candidate (Admin)");
        candNameField = new JTextField();
        candManifestoField = new JTextField();
        JButton addBtn = new JButton("Add Candidate");
        addBtn.addActionListener((@SuppressWarnings("unused") ActionEvent ignored) -> {
            if (!requireAdmin()) return;
            String name = candNameField.getText().trim();
            String man = candManifestoField.getText().trim();
            if (name.isEmpty()) {
                showError("Name required.");
                return;
            }
            dbAddCandidate(name, man);
            showInfo("Candidate added.");
            refreshCandidateList();
            refreshVotingCandidates();
        });
        JButton editBtn = new JButton("Edit Selected");
        editBtn.addActionListener((@SuppressWarnings("unused") ActionEvent ignored) -> {
            if (!requireAdmin()) return;
            int idx = candidateList.getSelectedIndex();
            if (idx < 0) {
                showError("Select a candidate.");
                return;
            }
            String name = candNameField.getText().trim();
            String man = candManifestoField.getText().trim();
            if (name.isEmpty()) {
                showError("Name required.");
                return;
            }
            int id = candidateIds.get(idx);
            dbUpdateCandidate(id, name, man);
            showInfo("Candidate updated.");
            refreshCandidateList();
            refreshVotingCandidates();
        });
        JButton removeBtn = new JButton("Remove Selected");
        removeBtn.addActionListener((@SuppressWarnings("unused") ActionEvent ignored) -> {
            if (!requireAdmin()) return;
            int idx = candidateList.getSelectedIndex();
            if (idx < 0) {
                showError("Select a candidate.");
                return;
            }
            int id = candidateIds.get(idx);
            dbRemoveCandidate(id);
            showInfo("Candidate removed.");
            refreshCandidateList();
            refreshVotingCandidates();
        });

        controls.add(labeled(candNameField, "Name"));
        controls.add(labeled(candManifestoField, "Manifesto (optional)"));
        controls.add(addBtn);
        controls.add(editBtn);
        controls.add(removeBtn);

        panel.add(new JScrollPane(candidateList), BorderLayout.CENTER);
        panel.add(controls, BorderLayout.SOUTH);
        return panel;
    }

    private void refreshCandidateList() {
        if (candidateListModel == null) return;
        candidateListModel.clear();
        candidateIds.clear();
        for (Candidate c : dbLoadCandidates()) {
            candidateListModel.addElement(c.name + (c.manifesto == null || c.manifesto.isEmpty() ? "" : " - " + c.manifesto));
            candidateIds.add(c.id);
        }
        refreshVoteTable();
    }

    // -----------------------------
    // Session Tab
    // -----------------------------
    private JComponent buildSessionTab() {
        JPanel panel = createPaddedPanel();
        panel.setLayout(new GridLayout(0, 1, 8, 8));

        JPanel card = createCardPanel("Election Controls (Admin)");
        JButton startBtn = new JButton("Start Election");
        startBtn.addActionListener((@SuppressWarnings("unused") ActionEvent ignored) -> {
            if (!requireAdmin()) return;
            if (dbIsElectionActive()) {
                showError("Election already active.");
                return;
            }
            dbSetElectionActive(true);
            showInfo("Election started.");
            refreshAllUI();
        });
        JButton endBtn = new JButton("End Election");
        endBtn.addActionListener((@SuppressWarnings("unused") ActionEvent ignored) -> {
            if (!requireAdmin()) return;
            if (!dbIsElectionActive()) {
                showError("Election is not active.");
                return;
            }
            dbSetElectionActive(false);
            showInfo("Election ended. View results tab for final announcement.");
            refreshAllUI();
        });
        card.add(startBtn);
        card.add(endBtn);

        panel.add(card);
        return panel;
    }

    // -----------------------------
    // Voting Tab
    // -----------------------------
    private JComponent buildVotingTab() {
        JPanel panel = createPaddedPanel();
        panel.setLayout(new BorderLayout(10, 10));

        // Candidate radio buttons
        votingCandidatesPanel = createCardPanel("Choose Candidate (Voter)");
        votingButtonGroup = new ButtonGroup();
        refreshVotingCandidates();

        JButton voteBtn = new JButton("Cast Vote");
        voteBtn.addActionListener((@SuppressWarnings("unused") ActionEvent ignored) -> onCastVote());

        // Right side: live counts
        voteTableModel = new DefaultTableModel(new Object[] { "Candidate", "Votes" }, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        voteTable = new JTable(voteTableModel);
        refreshVoteTable();

        JPanel left = new JPanel(new BorderLayout(6, 6));
        left.add(votingCandidatesPanel, BorderLayout.CENTER);
        left.add(voteBtn, BorderLayout.SOUTH);

        JPanel right = createCardPanel("Real-time Vote Counts");
        right.setLayout(new BorderLayout());
        right.add(new JScrollPane(voteTable), BorderLayout.CENTER);

        panel.add(left, BorderLayout.CENTER);
        panel.add(right, BorderLayout.EAST);
        return panel;
    }

    private void onCastVote() {
        if (loggedInVoter == null) {
            showError("Login as a verified voter first.");
            return;
        }
        if (!dbIsElectionActive()) {
            showError("Election is not active.");
            return;
        }
        if (dbHasVoted(loggedInVoter.username)) {
            showError("You have already voted. Double voting is not allowed.");
            return;
        }
        // Identify selected candidate
        String selectedCandidateName = null;
        Integer selectedCandidateId = null;
        for (Enumeration<AbstractButton> e = votingButtonGroup.getElements(); e.hasMoreElements();) {
            AbstractButton b = e.nextElement();
            if (b.isSelected()) {
                selectedCandidateName = b.getText();
                Object idObj = b.getClientProperty("candidateId");
                if (idObj instanceof Integer) selectedCandidateId = (Integer) idObj;
                break;
            }
        }
        if (selectedCandidateName == null || selectedCandidateId == null) {
            showError("Select a candidate.");
            return;
        }
        dbCastVote(loggedInVoter.username, selectedCandidateId);
        showInfo("Vote cast for: " + selectedCandidateName);
        refreshVoteTable();
    }

    private void refreshVotingCandidates() {
        if (votingCandidatesPanel == null || votingButtonGroup == null) return;
        votingCandidatesPanel.removeAll();
        votingButtonGroup = new ButtonGroup();
        JPanel radios = new JPanel();
        radios.setLayout(new BoxLayout(radios, BoxLayout.Y_AXIS));
        for (Candidate c : dbLoadCandidates()) {
            JRadioButton rb = new JRadioButton(c.name);
            rb.putClientProperty("candidateId", c.id);
            votingButtonGroup.add(rb);
            radios.add(rb);
        }
        votingCandidatesPanel.add(radios);
        votingCandidatesPanel.revalidate();
        votingCandidatesPanel.repaint();
    }

    private void refreshVoteTable() {
        if (voteTableModel == null) return;
        voteTableModel.setRowCount(0);
        for (Candidate c : dbLoadCandidates()) {
            voteTableModel.addRow(new Object[] { c.name, c.votes });
        }
    }

    // -----------------------------
    // Results Tab
    // -----------------------------
    private JComponent buildResultsTab() {
        JPanel panel = createPaddedPanel();
        panel.setLayout(new BorderLayout(10, 10));

        JPanel card = createCardPanel("Final Results");
        resultsLabel = new JLabel("Results available after election ends.");
        JButton announceBtn = new JButton("Announce Winner");
        announceBtn.addActionListener((@SuppressWarnings("unused") ActionEvent ignored) -> announceResults());
        card.add(resultsLabel);
        card.add(announceBtn);

        panel.add(card, BorderLayout.NORTH);
        return panel;
    }

    private void announceResults() {
        if (dbIsElectionActive()) {
            showError("End the election before announcing results.");
            return;
        }
        List<Candidate> cands = dbLoadCandidates();
        if (cands.isEmpty()) {
            showError("No candidates available.");
            return;
        }
        int maxVotes = -1;
        List<Candidate> winners = new ArrayList<>();
        for (Candidate c : cands) {
            if (c.votes > maxVotes) {
                maxVotes = c.votes;
                winners.clear();
                winners.add(c);
            } else if (c.votes == maxVotes) {
                winners.add(c);
            }
        }
        StringBuilder sb = new StringBuilder();
        if (winners.size() == 1) {
            sb.append("Winner: ").append(winners.get(0).name).append(" with ").append(maxVotes).append(" votes.");
        } else {
            sb.append("Tie between: ");
            for (int i = 0; i < winners.size(); i++) {
                sb.append(winners.get(i).name);
                if (i < winners.size() - 1) sb.append(", ");
            }
            sb.append(" (" + maxVotes + " votes each)");
        }
        resultsLabel.setText(sb.toString());
        JOptionPane.showMessageDialog(this, sb.toString(), "Final Results", JOptionPane.INFORMATION_MESSAGE);
    }

    // -----------------------------
    // Utilities and Common UI helpers
    // -----------------------------
    private JPanel createPaddedPanel() {
        JPanel p = new JPanel();
        p.setBorder(new EmptyBorder(10, 10, 10, 10));
        return p;
    }

    private JPanel createCardPanel(String title) {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(BorderFactory.createTitledBorder(title));
        return p;
    }

    private JPanel labeled(JComponent field, String label) {
        JPanel p = new JPanel(new BorderLayout(6, 6));
        JLabel l = new JLabel(label);
        p.add(l, BorderLayout.WEST);
        p.add(field, BorderLayout.CENTER);
        return p;
    }

    private boolean requireAdmin() {
        if (loggedInAdmin == null) {
            showError("Admin privileges required. Please log in as admin.");
            return false;
        }
        return true;
    }

    private void showInfo(String msg) {
        if (statusBar != null) statusBar.setText(msg);
        JOptionPane.showMessageDialog(this, msg, "Info", JOptionPane.INFORMATION_MESSAGE);
    }

    private void showError(String msg) {
        if (statusBar != null) statusBar.setText(msg);
        JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE);
    }

    private void refreshAllUI() {
        // Header election status
        if (electionStatusLabel != null) {
            boolean active = dbIsElectionActive();
            electionStatusLabel.setText("Election: " + (active ? "ACTIVE" : "INACTIVE"));
            electionStatusLabel.setForeground(active ? new Color(0, 128, 0) : new Color(160, 0, 0));
        }
        // Voting candidates and counts
        refreshVotingCandidates();
        refreshVoteTable();
        // Registration lists
        updateRegVoterList();
        // Candidate lists
        refreshCandidateList();
    }

    // -----------------------------
    // Persistence
    // -----------------------------
    // -----------------------------
    // Database helpers (SQLite via Db)
    // -----------------------------
    private boolean dbUserExists(String username) {
        try (Connection c = Db.get(); PreparedStatement ps = c.prepareStatement(
                "SELECT 1 FROM users WHERE username=?")) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        } catch (SQLException ex) { showError(ex.getMessage()); return false; }
    }

    private User dbGetUser(String username) {
        try (Connection c = Db.get(); PreparedStatement ps = c.prepareStatement(
                "SELECT username, display_name, role, verified FROM users WHERE username=?")) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                String u = rs.getString(1);
                String dn = rs.getString(2);
                Role r = Role.valueOf(rs.getString(3));
                boolean ver = rs.getInt(4) == 1;
                User user = new User(u, dn, r);
                user.verified = ver;
                return user;
            }
        } catch (SQLException ex) { showError(ex.getMessage()); return null; }
    }

    private void dbAddVoter(String username, String displayName) {
        try (Connection c = Db.get(); PreparedStatement ps = c.prepareStatement(
                "INSERT INTO users(username, display_name, role, verified) VALUES(?,?,?,0)")) {
            ps.setString(1, username);
            ps.setString(2, displayName);
            ps.setString(3, Role.VOTER.name());
            ps.executeUpdate();
        } catch (SQLException ex) { showError(ex.getMessage()); }
    }

    private void dbVerifyVoter(String username) {
        try (Connection c = Db.get(); PreparedStatement ps = c.prepareStatement(
                "UPDATE users SET verified=1 WHERE username=?")) {
            ps.setString(1, username);
            ps.executeUpdate();
        } catch (SQLException ex) { showError(ex.getMessage()); }
    }

    private void dbRemoveVoter(String username) {
        try (Connection c = Db.get()) {
            try (PreparedStatement p1 = c.prepareStatement("DELETE FROM voted_users WHERE username=?")) {
                p1.setString(1, username); p1.executeUpdate();
            }
            try (PreparedStatement p2 = c.prepareStatement("DELETE FROM users WHERE username=?")) {
                p2.setString(1, username); p2.executeUpdate();
            }
        } catch (SQLException ex) { showError(ex.getMessage()); }
    }

    private List<User> dbLoadVoters() {
        List<User> result = new ArrayList<>();
        try (Connection c = Db.get(); Statement s = c.createStatement(); ResultSet rs = s.executeQuery(
                "SELECT username, display_name, role, verified FROM users WHERE role='VOTER' ORDER BY username")) {
            while (rs.next()) {
                User u = new User(rs.getString(1), rs.getString(2), Role.valueOf(rs.getString(3)));
                u.verified = rs.getInt(4) == 1;
                result.add(u);
            }
        } catch (SQLException ex) { showError(ex.getMessage()); }
        return result;
    }

    private List<Candidate> dbLoadCandidates() {
        List<Candidate> result = new ArrayList<>();
        try (Connection c = Db.get(); Statement s = c.createStatement(); ResultSet rs = s.executeQuery(
                "SELECT id, name, manifesto, votes FROM candidates ORDER BY id")) {
            while (rs.next()) {
                result.add(new Candidate(rs.getInt(1), rs.getString(2), rs.getString(3), rs.getInt(4)));
            }
        } catch (SQLException ex) { showError(ex.getMessage()); }
        return result;
    }

    private void dbAddCandidate(String name, String manifesto) {
        try (Connection c = Db.get(); PreparedStatement ps = c.prepareStatement(
                "INSERT INTO candidates(name, manifesto, votes) VALUES(?,?,0)")) {
            ps.setString(1, name);
            ps.setString(2, manifesto);
            ps.executeUpdate();
        } catch (SQLException ex) { showError(ex.getMessage()); }
    }

    private void dbUpdateCandidate(int id, String name, String manifesto) {
        try (Connection c = Db.get(); PreparedStatement ps = c.prepareStatement(
                "UPDATE candidates SET name=?, manifesto=? WHERE id=?")) {
            ps.setString(1, name);
            ps.setString(2, manifesto);
            ps.setInt(3, id);
            ps.executeUpdate();
        } catch (SQLException ex) { showError(ex.getMessage()); }
    }

    private void dbRemoveCandidate(int id) {
        try (Connection c = Db.get(); PreparedStatement ps = c.prepareStatement(
                "DELETE FROM candidates WHERE id=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException ex) { showError(ex.getMessage()); }
    }

    private boolean dbIsElectionActive() {
        try (Connection c = Db.get(); PreparedStatement ps = c.prepareStatement(
                "SELECT value FROM settings WHERE key='electionActive'")) {
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return false;
                return Boolean.parseBoolean(rs.getString(1));
            }
        } catch (SQLException ex) { showError(ex.getMessage()); return false; }
    }

    private void dbSetElectionActive(boolean active) {
        try (Connection c = Db.get(); PreparedStatement ps = c.prepareStatement(
                "UPDATE settings SET value=? WHERE key='electionActive'")) {
            ps.setString(1, Boolean.toString(active));
            ps.executeUpdate();
        } catch (SQLException ex) { showError(ex.getMessage()); }
    }

    private boolean dbHasVoted(String username) {
        try (Connection c = Db.get(); PreparedStatement ps = c.prepareStatement(
                "SELECT 1 FROM voted_users WHERE username=?")) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        } catch (SQLException ex) { showError(ex.getMessage()); return false; }
    }

    private void dbCastVote(String username, int candidateId) {
        try (Connection c = Db.get()) {
            c.setAutoCommit(false);
            try (PreparedStatement chk = c.prepareStatement("SELECT 1 FROM voted_users WHERE username=?")) {
                chk.setString(1, username);
                try (ResultSet rs = chk.executeQuery()) {
                    if (rs.next()) { c.rollback(); showError("You have already voted."); return; }
                }
            }
            try (PreparedStatement inc = c.prepareStatement("UPDATE candidates SET votes=votes+1 WHERE id=?")) {
                inc.setInt(1, candidateId);
                if (inc.executeUpdate() != 1) { c.rollback(); showError("Candidate not found."); return; }
            }
            try (PreparedStatement mark = c.prepareStatement("INSERT INTO voted_users(username) VALUES(?)")) {
                mark.setString(1, username);
                mark.executeUpdate();
            }
            c.commit();
        } catch (SQLException ex) { showError(ex.getMessage()); }
    }
}


