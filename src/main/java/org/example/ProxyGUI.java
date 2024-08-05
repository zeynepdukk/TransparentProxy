package org.example;

import javafx.scene.control.*;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

import java.util.Optional;

public class ProxyGUI extends Application {

    @Override
    public void start(Stage primaryStage) {
        BorderPane root = new BorderPane();
        MenuBar menuBar = new MenuBar();

        Menu fileMenu = new Menu("File");
        MenuItem startMenuItem = new MenuItem("Start");
        MenuItem stopMenuItem = new MenuItem("Stop");
        MenuItem reportMenuItem = new MenuItem("Report");
        MenuItem exitMenuItem = new MenuItem("Exit");
        fileMenu.getItems().addAll(startMenuItem, stopMenuItem, reportMenuItem, exitMenuItem);

        Menu filterMenu = new Menu("Filter");
        MenuItem addFilterMenuItem = new MenuItem("Add host to filter");
        MenuItem displayFilterMenuItem = new MenuItem("Display current filtered hosts");
        filterMenu.getItems().addAll(addFilterMenuItem, displayFilterMenuItem);

        Menu helpMenu = new Menu("Help");
        MenuItem aboutMenuItem = new MenuItem("About");
        helpMenu.getItems().add(aboutMenuItem);

        menuBar.getMenus().addAll(fileMenu, filterMenu, helpMenu);

        root.setTop(menuBar);

        Scene scene = new Scene(root, 800, 600);

        primaryStage.setTitle("Transparent Proxy Application");
        primaryStage.setScene(scene);
        primaryStage.show();

        startMenuItem.setOnAction(e -> startProxy());
        stopMenuItem.setOnAction(e -> stopProxy());
        reportMenuItem.setOnAction(e -> generateReport());
        exitMenuItem.setOnAction(e -> System.exit(0));
        addFilterMenuItem.setOnAction(e -> addHostToFilter());
        displayFilterMenuItem.setOnAction(e -> displayFilteredHosts());
        aboutMenuItem.setOnAction(e -> showAboutDialog());
    }

    private void startProxy() {
        new Thread(ProxyServer::start).start();
    }
    private void stopProxy() {
        ProxyServer.stop();
    }
    private void generateReport() {
        ProxyServer.generateReport();
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Report");
        alert.setHeaderText("Report generated");
        alert.setContentText("The report has been generated and saved as report.txt");
        alert.showAndWait();
    }

    private void addHostToFilter() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Add Host to Filter");
        dialog.setHeaderText("Add a host to the filter");
        dialog.setContentText("Please enter the host:");
        Optional<String> result = dialog.showAndWait();
        result.ifPresent(ProxyServer::addBlockedHost);
    }

    private void displayFilteredHosts() {
        StringBuilder hosts = new StringBuilder();
        ProxyServer.getBlockedHosts().forEach(host -> hosts.append(host).append("\n"));

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Filtered Hosts");
        alert.setHeaderText("Current filtered hosts:");
        alert.setContentText(hosts.toString());
        alert.showAndWait();
    }

    private void showAboutDialog() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("About");
        alert.setHeaderText("About this application");
        alert.setContentText("Transparent Proxy Application\nVersion 1.0\nDeveloped by Zeynep Dükkancıoğlu");
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
