package org.main.culturesolutioncalculation;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.main.culturesolutioncalculation.service.database.DatabaseConnector;

import java.io.FileNotFoundException;
import java.net.URL;
import java.sql.Connection;

public class Main extends Application {

    private final String url = "jdbc:mysql://localhost:3306/CultureSolutionCalculation?useSSL=false";
    private final String user = "root";
    private final String password = "root";

    @Override
    public void start(Stage stage) {
        try {
            initStage(stage);
            Connection conn = DatabaseConnector.getInstance(url, user, password).getConnection();

            //종료 시 DB 연결 해제
            stage.setOnCloseRequest(e -> DatabaseConnector.disconnect(conn));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch();
    }

    public static void reload(Stage stage) {
        try {
            initStage(stage);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void initStage(Stage stage) throws Exception {
        //resources/org/main/culturesolutioncalculation/Main.fxml
        //Parent root = FXMLLoader.load(Main.class.getResource("Main.fxml"));

        Parent root = FXMLLoader.load(Main.class.getResource("/org/main/culturesolutioncalculation/Main.fxml"));
        if (root == null) {
            throw new FileNotFoundException("FXML file not found");
        }
        Scene scene = new Scene(root, 950, 750);

        stage.setTitle("배양액 계산 프로그램");
        stage.setMinWidth(950);
        stage.setMinHeight(750);

        stage.setScene(scene);
        stage.show();
    }
}