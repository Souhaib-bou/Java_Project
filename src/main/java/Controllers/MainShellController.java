package Controllers;

import Models.User;
import Services.UserService;
import Utils.UserSession;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.animation.FadeTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.scene.layout.HBox;
import javafx.scene.layout.GridPane;

public class MainShellController implements Initializable {

    @FXML private StackPane contentHost;

    @FXML private Label lblHeaderUserName;
    @FXML private Label lblHeaderInitials;

    @FXML private Label lblPageTitle;
    @FXML private Label lblPageSubtitle;

    @FXML private ImageView imgHeaderAvatar;

    // Button chip in MainShell.fxml
    @FXML private Button btnUserMenu;

    // Context menu for Profile/Logout
    private final ContextMenu userMenu = new ContextMenu();
    private final UserService userService = new UserService();
    private int currentUserId; // set this from session

    // ====== Utility: circular avatar clip ======
    /**
     * Executes this operation.
     */
    public static void applyCircleClip(ImageView iv, double size) {
        iv.setFitWidth(size);
        iv.setFitHeight(size);
        iv.setPreserveRatio(true);

        Circle clip = new Circle(size / 2.0, size / 2.0, size / 2.0);
        iv.setClip(clip);

        iv.layoutBoundsProperty().addListener((obs, oldB, b) -> {
            clip.setCenterX(b.getWidth() / 2.0);
            clip.setCenterY(b.getHeight() / 2.0);
            clip.setRadius(Math.min(b.getWidth(), b.getHeight()) / 2.0);
        });
    }

    @Override
    /**
     * Initializes UI components and loads initial data.
     */
    public void initialize(URL location, ResourceBundle resources) {
        refreshHeader();
        setupUserMenu();
        openHome();
    }

    // ===================== USER MENU (Button + ContextMenu) =====================
    /**
     * Sets the upusermenu value.
     */
    private void setupUserMenu() {
        if (btnUserMenu == null) return;
        User currentUser = UserSession.getInstance().getCurrentUser();
        if (currentUser == null) return; // no user logged in

        String currentUserRole = currentUser.getRoleName();
        // ================= PROFILE =================
        MenuItem profile = new MenuItem("Profile");
        profile.setOnAction(e -> handleOpenProfile());

        // ================= ROLE MANAGEMENT =================
        MenuItem roleManagement = new MenuItem("Role Management");
        roleManagement.setOnAction(e -> handleOpenRoleManagement());

        // ================= LOGOUT =================
        MenuItem logout = new MenuItem("Logout");
        logout.setOnAction(e -> handleLogout());


        if ("ADMIN".equalsIgnoreCase(currentUserRole)) {
            userMenu.getItems().setAll(
                    profile,
                    roleManagement,
                    new SeparatorMenuItem(),
                    logout
            );
        } else {
            userMenu.getItems().setAll(
                    profile,
                    new SeparatorMenuItem(),
                    logout
            );
        }

        // CSS hook
        if (!userMenu.getStyleClass().contains("user-menu-glass")) {
            userMenu.getStyleClass().add("user-menu-glass");
        }

        // Toggle on chip click
        btnUserMenu.setOnAction(e -> {
            if (userMenu.isShowing()) {
                userMenu.hide();
                return;
            }

            userMenu.show(btnUserMenu, Side.BOTTOM, 0, 8);

            Platform.runLater(() -> {
                matchPopupWidthToButton();
                animateUserMenuPopup(userMenu);
            });
        });

        // Close when clicking outside
        btnUserMenu.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene == null) return;

            newScene.addEventFilter(MouseEvent.MOUSE_PRESSED, ev -> {
                if (!userMenu.isShowing()) return;

                boolean clickOnButton = btnUserMenu.localToScene(btnUserMenu.getBoundsInLocal())
                        .contains(ev.getSceneX(), ev.getSceneY());

                boolean clickInsideMenu = false;
                if (userMenu.getScene() != null && userMenu.getScene().getRoot() != null) {
                    Node root = userMenu.getScene().getRoot();
                    clickInsideMenu = root.localToScene(root.getBoundsInLocal())
                            .contains(ev.getSceneX(), ev.getSceneY());
                }

                if (!clickOnButton && !clickInsideMenu) {
                    userMenu.hide();
                }
            });
        });
    }

    /**
     * Executes this operation.
     */
    private void matchPopupWidthToButton() {
        if (userMenu.getScene() == null) return;
        Node root = userMenu.getScene().getRoot();
        if (root == null) return;

        double w = btnUserMenu.getWidth();
        if (w <= 0) w = btnUserMenu.prefWidth(-1);

        // ContextMenu root is usually a Region
        if (root instanceof javafx.scene.layout.Region r) {
            r.setMinWidth(w);
            r.setPrefWidth(w);
            r.setMaxWidth(w);
        }
    }

    /**
     * Executes this operation.
     */
    private void animateUserMenuPopup(ContextMenu cm) {
        if (cm.getScene() == null) return;
        Node root = cm.getScene().getRoot();
        if (root == null) return;

        root.setEffect(null);
        root.setStyle("-fx-background-color: transparent;");

        try {
            root.setEffect(new DropShadow(18, 0, 8, Color.rgb(0, 0, 0, 0.25)));
        } catch (Exception ignored) {}

        root.setOpacity(0);
        root.setTranslateY(-10);

        FadeTransition fade = new FadeTransition(Duration.millis(160), root);
        fade.setFromValue(0);
        fade.setToValue(1);

        TranslateTransition slide = new TranslateTransition(Duration.millis(160), root);
        slide.setFromY(-10);
        slide.setToY(0);

        fade.play();
        slide.play();
    }

    // ===================== NAVIGATION HELPERS =====================
    /**
     * Navigates to the requested screen.
     */

    private void handleOpenRoleManagement() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/RoleManagement.fxml"));
            Parent  roleRoot = loader.load();

            // Get the controller and pass shell if needed
            RoleController controller = loader.getController();
            controller.setShell(this);  // optional, if you want RoleController to communicate with shell

            // Replace the content inside the shell
            contentHost.getChildren().setAll(roleRoot);

            // Optionally update page meta / title
            setPageMeta("Role Management", "Manage all roles");

            // Keep the stage (window) same — only title changes if you want
            Stage stage = (Stage) contentHost.getScene().getWindow();
            stage.setTitle("Hirely — Role Management");

        } catch (IOException e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Cannot load RoleManagement.fxml. Check console.").showAndWait();
        }
    }

    public void openApplicationsForJob(int jobOfferId) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ApplicationView.fxml"));
            Parent root = loader.load();

            ApplicationController controller = loader.getController();
            controller.setShell(this);
            controller.setJobContext(jobOfferId);   // ✅ pass job id

            contentHost.getChildren().setAll(root);
            setPageMeta("Applications", "For Job Offer #" + jobOfferId);

            Stage stage = (Stage) contentHost.getScene().getWindow();
            stage.setTitle("Hirely — Applications (Job #" + jobOfferId + ")");

        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Cannot load ApplicationView.fxml. Check console.").showAndWait();
        }
    }

    // ===================== HEADER =====================
    /**
     * Loads and refreshes data displayed in the view.
     */
    private void refreshHeader() {
        User u = UserSession.getInstance().getCurrentUser();
        if (u == null) return;

        String full = u.getFullName();
        lblHeaderUserName.setText(full == null || full.trim().isEmpty() ? "User" : full);

        String fn = u.getFirstName() == null ? "" : u.getFirstName().trim();
        String ln = u.getLastName() == null ? "" : u.getLastName().trim();

        String i1 = fn.isEmpty() ? "U" : fn.substring(0, 1).toUpperCase();
        String i2 = ln.isEmpty() ? "" : ln.substring(0, 1).toUpperCase();
        String initials = (i1 + i2).trim();
        lblHeaderInitials.setText(initials.isEmpty() ? "U" : initials);

        String path = u.getProfilePic();
        boolean hasPic = path != null && !path.trim().isEmpty() && new File(path).exists();

        if (hasPic) {
            imgHeaderAvatar.setImage(new Image(new File(path).toURI().toString(), true));
            applyCircleClip(imgHeaderAvatar, 32);
            lblHeaderInitials.setVisible(false);
        } else {
            imgHeaderAvatar.setImage(null);
            lblHeaderInitials.setVisible(true);
        }
    }

    /**
     * Sets the pagemeta value.
     */
    private void setPageMeta(String title, String subtitle) {
        lblPageTitle.setText(title);
        lblPageSubtitle.setText(subtitle);

        Platform.runLater(() -> {
            if (contentHost != null && contentHost.getScene() != null) {
                Stage stage = (Stage) contentHost.getScene().getWindow();
                if (stage != null) stage.setTitle("Hirely — " + title);
            }
        });
    }

    // ===================== HOME =====================
    /**
     * Navigates to the requested screen.
     */
    private 
    /**
     * Builds and displays the Home (Dashboard) view.
     * This view is created programmatically to keep navigation simple while staying UI-only.
     */
    void openHome() {
        // Root container
        VBox root = new VBox(18);
        root.getStyleClass().add("page");

        // --- HERO ---
        HBox heroRow = new HBox(18);
        heroRow.getStyleClass().add("hero-row");

        StackPane hero = new StackPane();
        hero.getStyleClass().add("hero");

        VBox heroText = new VBox(8);
        heroText.getStyleClass().add("hero-text");

        Label heroTitle = new Label("Find Your Dream Job");
        heroTitle.getStyleClass().add("hero-title");

        Label heroSubtitle = new Label("Search a wide range of opportunities and find the perfect job for you.");
        heroSubtitle.getStyleClass().add("hero-subtitle");
        heroSubtitle.setWrapText(true);

        // Search bar (UI-only; sends user to Job Offers screen)
        HBox searchBar = new HBox(10);
        searchBar.getStyleClass().add("search-bar");

        TextField keyword = new TextField();
        keyword.setPromptText("Job title or keyword");
        keyword.getStyleClass().add("input");

        TextField location = new TextField();
        location.setPromptText("Location");
        location.getStyleClass().add("input");

        Button searchBtn = new Button("Search");
        searchBtn.getStyleClass().addAll("btn", "btn-primary");
        searchBtn.setOnAction(e -> openJobOffers());

        searchBar.getChildren().addAll(keyword, location, searchBtn);

        heroText.getChildren().addAll(heroTitle, heroSubtitle, searchBar);
        hero.getChildren().add(heroText);

        // --- STATS CARD ---
        VBox stats = new VBox(12);
        stats.getStyleClass().addAll("card", "stats-card");

        Label welcome = new Label("Welcome Back!");
        welcome.getStyleClass().add("card-title");

        HBox statsRow = new HBox(12);
        statsRow.getStyleClass().add("stats-row");

        VBox s1 = statBlock("Saved Jobs", "16");
        VBox s2 = statBlock("Applied Jobs", "8");
        VBox s3 = statBlock("Viewed Today", "24");

        statsRow.getChildren().addAll(s1, s2, s3);
        stats.getChildren().addAll(welcome, statsRow);

        heroRow.getChildren().addAll(hero, stats);

        // --- LATEST LISTINGS (UI-only) ---
        VBox section = new VBox(12);
        section.getStyleClass().add("section");

        Label sectionTitle = new Label("Latest Job Listings");
        sectionTitle.getStyleClass().add("section-title");

        GridPane grid = new GridPane();
        grid.getStyleClass().add("card-grid");
        grid.setHgap(14);
        grid.setVgap(14);

        grid.add(jobCard("Software Engineer", "TechCorp", "New York, NY", "$80k - $100k", "Full-Time"), 0, 0);
        grid.add(jobCard("Marketing Specialist", "Creative Agency", "San Francisco, CA", "$50k - $70k", "Part-Time"), 1, 0);
        grid.add(jobCard("Data Analyst", "FinData Inc.", "Remote", "$70k - $90k", "Remote"), 0, 1);
        grid.add(jobCard("Product Designer", "InnovateX", "Los Angeles, CA", "$75k - $90k", "Internship"), 1, 1);

        HBox sectionFooter = new HBox();
        sectionFooter.getStyleClass().add("section-footer");

        Button viewAll = new Button("View All Jobs");
        viewAll.getStyleClass().addAll("btn", "btn-secondary");
        viewAll.setOnAction(e -> openJobOffers());

        sectionFooter.getChildren().add(viewAll);

        section.getChildren().addAll(sectionTitle, grid, sectionFooter);

        root.getChildren().addAll(heroRow, section);

        contentHost.getChildren().setAll(root);
        setPageMeta("Dashboard", "Welcome");
    }

    /**
     * Creates a small statistic tile used in the Home dashboard.
     */
    private VBox statBlock(String label, String value) {
        VBox box = new VBox(2);
        box.getStyleClass().add("stat-block");

        Label v = new Label(value);
        v.getStyleClass().add("stat-value");

        Label l = new Label(label);
        l.getStyleClass().add("stat-label");

        box.getChildren().addAll(v, l);
        return box;
    }

    /**
     * Creates a compact job card used in the Home dashboard preview grid.
     */
    private VBox jobCard(String title, String company, String location, String salary, String tag) {
        VBox card = new VBox(6);
        card.getStyleClass().addAll("card", "job-card");

        Label t = new Label(title);
        t.getStyleClass().add("job-title");

        Label c = new Label(company);
        c.getStyleClass().add("muted");

        Label loc = new Label(location);
        loc.getStyleClass().add("muted");

        HBox meta = new HBox(10);
        meta.getStyleClass().add("job-meta");

        Label sal = new Label(salary);
        sal.getStyleClass().add("job-salary");

        Label chip = new Label(tag);
        chip.getStyleClass().add("chip");

        meta.getChildren().addAll(sal, chip);

        card.getChildren().addAll(t, c, loc, meta);
        return card;
    }



    @FXML
    /**
     * Handles the associated UI event.
     */
    private void handleOpenHome() {
        openHome();
    }

    // ===================== ONBOARDING =====================
    /**
     * Navigates to the requested screen.
     */
    public void openPlans() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/OnboardingPlanView.fxml"));
            Parent root = loader.load();

            OnboardingPlanController controller = loader.getController();
            controller.setShell(this);

            contentHost.getChildren().setAll(root);
            setPageMeta("Onboarding Plans", "Assign plans and track progress");

            Stage stage = (Stage) contentHost.getScene().getWindow();
            stage.setTitle("Hirely — Onboarding Plans");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Navigates to the requested screen.
     */
    public void openTasks(int planId) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/OnboardingTaskView.fxml"));
            Parent root = loader.load();

            OnboardingTaskController taskController = loader.getController();
            taskController.setPlanContext(planId);
            taskController.setOnBack(this::openPlans);

            contentHost.getChildren().setAll(root);
            setPageMeta("Tasks", "Manage tasks for plan #" + planId);

            Stage stage = (Stage) contentHost.getScene().getWindow();
            stage.setTitle("Hirely — Tasks (Plan #" + planId + ")");

        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Cannot load tasks view. Check console.").showAndWait();
        }
    }

    @FXML
    /**
     * Handles the associated UI event.
     */
    private void handleOpenOnboarding() {
        openPlans();
    }

    // ===================== JOB OFFERS =====================
    @FXML
    /**
     * Handles the associated UI event.
     */
    private void handleOpenJobOffers() {
        openJobOffers();
    }

    /**
     * Navigates to the requested screen.
     */
    public void openJobOffers() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/JobOfferView.fxml"));
            Parent root = loader.load();

            JobOfferController controller = loader.getController();
            controller.setShell(this);

            contentHost.getChildren().setAll(root);
            setPageMeta("Job Offers", "Create and manage job offers");

            Stage stage = (Stage) contentHost.getScene().getWindow();
            stage.setTitle("Hirely — Job Offers");

        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Cannot load JobOfferView.fxml. Check console.").showAndWait();
        }
    }

    // ===================== APPLICATIONS =====================
    /**
     * Navigates to the requested screen.
     */
    public void openApplications() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ApplicationView.fxml"));
            Parent root = loader.load();

            ApplicationController controller = loader.getController();
            controller.setShell(this);

            contentHost.getChildren().setAll(root);
            setPageMeta("Applications", "Manage applications");

            Stage stage = (Stage) contentHost.getScene().getWindow();
            stage.setTitle("Hirely — Applications");

        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Cannot load ApplicationView.fxml. Check console.").showAndWait();
        }
    }

    // ===================== PROFILE =====================
    @FXML
    /**
     * Handles the associated UI event.
     */
    private void handleOpenProfile() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/UserProfileView.fxml"));
            Parent root = loader.load();

            UserProfileController controller = loader.getController();
            controller.setShell(this);

            contentHost.getChildren().setAll(root);
            setPageMeta("My Profile", "Manage your account information");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Executes this operation.
     */
    public void backToPlans() {
        refreshHeader();
        openPlans();
    }

    /**
     * Loads and refreshes data displayed in the view.
     */
    public void refreshShellUserChip() {
        refreshHeader();
    }

    // ===================== LOGOUT =====================
    @FXML
    /**
     * Handles the associated UI event.
     */
    public void handleLogout() {
        try {
            UserSession.getInstance().clear();

            Parent loginRoot = FXMLLoader.load(getClass().getResource("/LoginView.fxml"));

            Stage stage = (Stage) contentHost.getScene().getWindow();
            stage.getScene().setRoot(loginRoot);
            stage.setTitle("Hirely — Login");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}