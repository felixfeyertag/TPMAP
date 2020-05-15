/*
 * Copyright (C) 2020 Felix Feyertag <felix.feyertag@ndm.ox.ac.uk>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.chembiohub.tpmap;

import com.chembiohub.tpmap.doc.TPDocTab;
import com.chembiohub.tpmap.dstruct.Proteome;
import com.chembiohub.tpmap.ui.TP1DUserInterface;
import com.chembiohub.tpmap.ui.TP2DUserInterface;
import com.chembiohub.tpmap.ui.TPUserInterface;
import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.*;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * The TPMAPApplication class instantiates a JavaFX interface with an "Import" button that will create an
 * empty Proteome object and fire the ImportWizard to populate it. If ImportWizard terminates
 * successfully, a TPUserInterface tab is attached.
 *
 * @author felixfeyertag
 */
public class TPMAPApplication extends Application {

    /**
     * @param primaryStage Primary stage
     */
    @Override
    public void start(Stage primaryStage) {

        BorderPane root = new BorderPane();

        Pane topPane = setTopPane(primaryStage);

        tpTabPane = new TabPane();

        TPDocTab docTab = new TPDocTab();

        tpTabPane.getTabs().add(docTab.getDocTab());

        root.setTop(topPane);
        root.setCenter(tpTabPane);

        Scene scene = new Scene(root, 300, 250);

        primaryStage.getIcons().add(new Image("/com/chembiohub/tpmap/ui/favicon.png"));
        primaryStage.setTitle("Thermal Profiling Meltome Analysis Program");
        primaryStage.setMaximized(true);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }

    /**
     * @param stage
     * @return
     */
    private Pane setTopPane(Stage stage) {
        BorderPane topPane = new BorderPane();

        topPane.setPadding(new Insets(15, 12, 15, 12));
        topPane.setStyle("-fx-background-color: #336699;");

        Button importButton = new Button();
        importButton.setText("Import protein abundance");

        Text t = new Text();
        t.setFont(Font.font("Verdana", FontWeight.EXTRA_BOLD, FontPosture.ITALIC, 20));
        t.setFill(Color.WHITE);
        t.setText("TPMAP");
        t.setTextAlignment(TextAlignment.RIGHT);

        importButton.setOnAction((ActionEvent event) -> {

            Proteome tppExp = new Proteome(stage, tpTabPane);

            if (tppExp.importWizard(stage, 0.2, 0.8)) {

                TPUserInterface tpUserInterface;
                Tab tab;

                switch (tppExp.getExpType()) {

                    case TP1D:
                        tab = new Tab(tppExp.getFileName().get());
                        tab.setOnClosed(evt -> {
                            tppExp.clear();
                            System.gc();
                        });
                        tpTabPane.getTabs().add(tab);
                        tpTabPane.getSelectionModel().select(tab);
                        tpUserInterface = new TP1DUserInterface(stage, tppExp);
                        tab.setContent(tpUserInterface.getTPane());
                        break;

                    case TP2D:
                        tab = new Tab(tppExp.getFileName().get());
                        tab.setOnClosed(evt -> {
                            tppExp.clear();
                            System.gc();
                        });
                        tpTabPane.getTabs().add(tab);
                        tpTabPane.getSelectionModel().select(tab);
                        tpUserInterface = new TP2DUserInterface(stage, tppExp);
                        tab.setContent(tpUserInterface.getTPane());
                        break;

                    default:
                        break;

                }
            }
        });

        topPane.setLeft(importButton);
        topPane.setRight(t);

        return topPane;
    }

    private TabPane tpTabPane;
}

