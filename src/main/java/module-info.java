module org.main.culturesolutioncalculation {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires com.opencsv;
    requires itextpdf;
    requires xmlworker;



    opens org.main.culturesolutioncalculation to javafx.fxml;
    exports org.main.culturesolutioncalculation;
    //exports org.main.culturesolutioncalculation.front;
    //opens org.main.culturesolutioncalculation.front to javafx.fxml;
}