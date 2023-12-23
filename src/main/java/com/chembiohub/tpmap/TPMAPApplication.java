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
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.event.ActionEvent;
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

import java.util.List;

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

        List<String> args = getParameters().getUnnamed();
        for (String arg : args) {
            if(arg.matches("-h") || arg.matches("--help")) {
                System.out.println("TP-MAP Version 1 Beta 6");
                System.out.println();
                System.out.println("Copyright (c) 2023 The Huber Lab, Centre for Medicines Discovery, University of Oxford");
                System.out.println("This software is Free Software, distributed under the GNU General Public License (GPL), version 3.");
                System.out.println();
                System.out.println("Usage:");
                System.out.println(" $ java -Xmx64g -jar TP-MAP-Beta-1.6-jar-with-dependencies.jar [ARGS]");
                System.out.println();
                System.out.println("Command line arguments:");
                System.out.println();
                System.out.println("  --help|-h                       This help message");
                System.out.println("  --TP1D=<FILE>[,<FILE>,...]      Load a 1D TPMAP formatted file on startup, multiple files can be specified separated by a comma (,)");
                System.out.println("  --TP2D=<FILE>[,<FILE>,...]      Load a 2D TPMAP formatted file on startup, multiple files can be specified separated by a comma (,)");
                System.out.println();
                System.exit(0);
            }
        }

        BorderPane root = new BorderPane();

        Scene scene = new Scene(root, 300, 250);

        primaryStage.getIcons().add(new Image("/com/chembiohub/tpmap/ui/favicon.png"));
        primaryStage.setTitle("Thermal Profiling Meltome Analysis Program");
        primaryStage.setMaximized(true);
        primaryStage.setScene(scene);
        primaryStage.show();

        tpTabPane = new TabPane();
        TPDocTab docTab = new TPDocTab();
        tpTabPane.getTabs().add(docTab.getDocTab());

        Pane topPane = setTopPane(primaryStage);
        root.setTop(topPane);
        root.setCenter(tpTabPane);
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

        StringProperty tp1d = new SimpleStringProperty(null);
        StringProperty tp2d = new SimpleStringProperty(null);

        importButton.setOnAction((ActionEvent event) -> {

            Proteome tppExp = new Proteome(stage, tpTabPane);

            if (tppExp.importWizard(stage, 0.2, 0.8, tp1d.getValue(), tp2d.getValue())) {

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
            if(tp1d.getValue() != null) {
                tp1d.setValue(null);
            }
            if(tp2d.getValue() != null) {
                tp2d.setValue(null);
            }
        });

        topPane.setLeft(importButton);
        topPane.setRight(t);

        if(getParameters().getNamed().get("TP1D") != null) {
            String[] params1d = (getParameters().getNamed().get("TP1D")).split(",");
            for (String param : params1d) {
                tp1d.setValue(param);
                tp2d.setValue(null);
                importButton.fire();
                tp1d.setValue(null);
            }
        }
        if(getParameters().getNamed().get("TP2D") != null) {
            String[] params2d = (getParameters().getNamed().get("TP2D")).split(",");
            for (String param : params2d) {
                tp2d.setValue(param);
                tp1d.setValue(null);
                importButton.fire();
                tp2d.setValue(null);
            }
        }

        return topPane;
    }

    private TabPane tpTabPane;
}

