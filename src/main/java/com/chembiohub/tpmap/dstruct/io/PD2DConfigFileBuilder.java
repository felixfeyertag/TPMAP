/*
 * Copyright (C) 2021 Felix Feyertag <felix.feyertag@gmail.com>
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

package com.chembiohub.tpmap.dstruct.io;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.converter.IntegerStringConverter;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;


/**
 *
 */
public class PD2DConfigFileBuilder {

    private final Stack<Parent> steps = new Stack<>();
    private final IntegerProperty currentStep = new SimpleIntegerProperty();

    private final Button nextBtn = new Button("Next");
    private final Button prevBtn = new Button("Previous");
    private final Button cancBtn = new Button("Close");

    private final VBox contents = new VBox();

    private final StringProperty filePath = new SimpleStringProperty();

    private final IntegerProperty tempVals = new SimpleIntegerProperty();
    private final IntegerProperty concVals = new SimpleIntegerProperty();
    private final StringProperty idVals = new SimpleStringProperty();
    private final StringProperty descVals = new SimpleStringProperty();

    private ComboBox normCombo = new ComboBox();

    private TextField[] temperatures;
    private TextField[] concentrations;

    private ComboBox[][] abundanceValue;
    private HashMap<String,Integer> index;

    public PD2DConfigFileBuilder(Stage parentStage) {

        Stage stage = new Stage();
        stage.initOwner(parentStage);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setWidth(800);
        stage.setHeight(600);

        BorderPane builderPane = new BorderPane();
        builderPane.setPadding(new Insets(15, 12, 15, 12));

        currentStep.set(0);

        filePath.set("");

        steps.push(configBuilderStep1());

        HBox buttons = new HBox();
        buttons.getChildren().addAll(cancBtn, prevBtn, nextBtn);
        buttons.setSpacing(10);

        contents.setPadding(new Insets(15,12,15,12));
        contents.getChildren().add(steps.get(currentStep.get()));

        builderPane.setPadding(new Insets(15,12,15,12));
        builderPane.setBottom(buttons);
        builderPane.setCenter(contents);

        initButtons(stage);

        Scene scene = new Scene(builderPane);

        stage.setScene(scene);
        stage.show();
    }

    private void initButtons(Stage stage) {
        prevBtn.disableProperty().bind(currentStep.lessThanOrEqualTo(0));

        prevBtn.setPrefSize(100,20);
        nextBtn.setPrefSize(100,20);
        cancBtn.setPrefSize(100,20);

        prevBtn.setOnAction(event -> {
            if(currentStep.greaterThan(0).get()) {
                nextBtn.setText("Next");
                contents.getChildren().remove(steps.pop());
                contents.getChildren().add(steps.peek());
                currentStep.set(currentStep.get()-1);
            }
        });

        nextBtn.setOnAction(event -> {
            if(currentStep.isEqualTo(0).get()) {
                Parent step2 = configBuilderStep2();
                if(step2!=null) {
                    nextBtn.setText("Create");
                    currentStep.set(currentStep.get()+1);
                    contents.getChildren().remove(steps.peek());
                    steps.push(step2);
                    contents.getChildren().add(steps.peek());
                }
            }

            else if(currentStep.isEqualTo(1).get()) {

                var configFileString = new String();

                for (int j=0; j<temperatures.length; j++) {
                    for(int i=0; i<concentrations.length; i++)  {
                        configFileString += abundanceValue[i][j].getValue() + "\t" + temperatures[j].getText() + "\t" + concentrations[i].getText() + "\n";
                    }
                }
                try {
                   FileChooser fc = new FileChooser();
                   File f = fc.showSaveDialog(new Stage());
                   FileWriter fw = new FileWriter(f);
                   fw.write(configFileString);
                   fw.close();
                } catch (Exception e) { }
            }

        });

        cancBtn.setOnAction((ActionEvent event) -> stage.close());
    }

    private Parent configBuilderStep1() {

        GridPane step1 = new GridPane();

        int row = 0;

        //File Loader
        HBox fileLoadBox = new HBox();
        fileLoadBox.setPadding(new Insets(10,8,10,8));
        Label flLabel = new Label("ProteomeDiscoverer File Name: ");
        flLabel.setMinWidth(180);
        fileLoadBox.getChildren().add(flLabel);
        TextField fileName = new TextField();
        fileName.setMinWidth(200);
        fileName.setEditable(false);
        fileLoadBox.getChildren().add(fileName);
        Button loadButton = new Button();
        FileChooser fc = new FileChooser();
        fc.setTitle("Open");
        loadButton.setText("Load");
        fileLoadBox.getChildren().add(loadButton);
        step1.add(fileLoadBox, 1, row++);

        loadButton.setOnAction((ActionEvent event) -> {
            try {
                File f = fc.showOpenDialog(new Stage());
                filePath.set(f.getAbsolutePath());
                fileName.setText(filePath.get());
            } catch (Exception e) { }
        });

        //Temperature

        HBox tempBox = new HBox();
        tempBox.setPadding(new Insets(10,8,10,8));
        Label tempLabel = new Label("Temperature points: ");
        tempLabel.setMinWidth(180);
        tempBox.getChildren().add(tempLabel);
        TextField tempField = new TextField();
        tempField.setMinWidth(30);
        tempField.setTextFormatter(new TextFormatter<>(new IntegerStringConverter()));
        tempVals.set(12);
        tempField.setText(String.valueOf(tempVals.get()));
        tempBox.getChildren().add(tempField);

        tempField.lengthProperty().addListener(event -> {
            try {
                tempVals.set(Integer.valueOf(tempField.getText()));
            } catch (Exception e) {  }
        });

        step1.add(tempBox, 1, row++);


        // Concentration

        HBox concBox = new HBox();
        concBox.setPadding(new Insets(10,8,10,8));
        Label concLabel = new Label("Concentration points:");
        concLabel.setMinWidth(180);
        concBox.getChildren().add(concLabel);
        TextField concField = new TextField();
        concField.setTextFormatter(new TextFormatter<>(new IntegerStringConverter()));
        concVals.set(5);
        concField.setText(String.valueOf(concVals.get()));
        concBox.getChildren().add(concField);

        concField.lengthProperty().addListener(event -> {
            try {
                concVals.set(Integer.valueOf(concField.getText()));
            } catch (NumberFormatException e) {  }
        });

        step1.add(concBox, 1, row++);

        //ID
        HBox idBox = new HBox();
        idBox.setPadding(new Insets(10,8,10,8));
        Label idLabel = new Label("Accession column header:");
        idLabel.setMinWidth(180);
        idBox.getChildren().add(idLabel);
        TextField idField = new TextField();
        idVals.set("Accession");
        idField.setText(idVals.get());
        idBox.getChildren().add(idField);

        idField.lengthProperty().addListener(event -> idVals.set(idField.getText()));

        step1.add(idBox,1,row++);

        //Description
        HBox descBox = new HBox();
        descBox.setPadding(new Insets(10,8,10,8));
        Label descLabel = new Label("Description column header:");
        descLabel.setMinWidth(180);
        descBox.getChildren().add(descLabel);
        TextField descField = new TextField();
        descVals.set("Description");
        descField.setText(descVals.get());
        descBox.getChildren().add(descField);

        descField.lengthProperty().addListener(event -> descVals.set(descField.getText()));

        step1.add(descBox,1,row++);

        return step1;

    }

    private Parent configBuilderStep2() {

        VBox step2 = new VBox();
        GridPane step2grid = new GridPane();

            temperatures = new TextField[tempVals.get()];
            concentrations = new TextField[concVals.get()];

            step2grid.add(new Label("Temp \\ Conc"), 0, 0);

            for(int i=1; i<=concentrations.length; i++) {
                concentrations[i-1] = new TextField();
                concentrations[i-1].setPrefWidth(150);
                concentrations[i-1].setText(String.valueOf(i));
                step2grid.add(concentrations[i-1], i, 0);
            }

            for(int i=1; i<=temperatures.length; i++) {
                temperatures[i-1] = new TextField();
                temperatures[i-1].setPrefWidth(150);
                temperatures[i-1].setText(String.valueOf(i));
                step2grid.add(temperatures[i-1],0,i);
            }

            ObservableList<String> headers;

            headers = extractHeaders();

            if(headers==null) {
                return null;
            }

            abundanceValue = new ComboBox[concentrations.length][temperatures.length];

            int counter = 0;

            try {
                for (int j=0; j<temperatures.length; j++) {
                    for(int i=0; i<concentrations.length; i++)  {
                        abundanceValue[i][j] = new ComboBox(headers);
                        abundanceValue[i][j].setPrefWidth(150);
                        abundanceValue[i][j].setValue(headers.get(counter++));
                        step2grid.add(abundanceValue[i][j],i+1,j+1);
                    }
                }
            }
            catch (Exception e) {
                Alert alert = new Alert(AlertType.ERROR);
                alert.initModality(Modality.APPLICATION_MODAL);
                alert.setTitle("Unable to read data file");
                alert.setHeaderText("Unable to read data file");
                if(counter > headers.size()) {
                  alert.setContentText("Error reading file: " + headers.size() + " number of headers extracted exceeds " + concentrations.length * temperatures.length);
                }
                else {
                    alert.setContentText("Error Reading File: " + e.getStackTrace());
                }
                alert.showAndWait();
            }

            step2.getChildren().add(step2grid);

        return step2;
    }

    private ObservableList<String> extractHeaders() {

        ObservableList<String> headers = FXCollections.observableArrayList();

        Map<Integer,String[]> positions = new HashMap<>();
        String[] head = null;
        try {
            BufferedReader br = new BufferedReader(new FileReader(filePath.get()));
            head = br.readLine().split("\t");

            index = new HashMap<>();

            //extract header indices
            for (int i=0;i<head.length;i++) {
                head[i] = head[i].replaceFirst("^\"", "");
                head[i] = head[i].replaceFirst("\"$", "");

                if(head[i].equals("Accession")) {
                    index.put("Accession", i);
                }
                else if(head[i].equals("Description")) {
                    index.put("Description", i);
                }
                else if(head[i].startsWith("Abundances (Scaled): ") || head[i].startsWith("Abundances (Grouped): ") || head[i].startsWith("Abundance: ") || head[i].startsWith("Abundances: ")) {
                    String tmtVal = head[i];
                    String repVal = head[i];
                    tmtVal = tmtVal.replaceFirst("^[^:]*: ", "");
                    tmtVal = tmtVal.replaceFirst("^[^,]*, ", "");
                    repVal = repVal.replaceFirst("^.*: ", "");
                    repVal = repVal.replaceFirst(", .*$", "");
                    System.out.println("tmtval = " + tmtVal + "\n" + "repVal = " + repVal); 
                    headers.add(repVal + "\t" + tmtVal);
                    index.put(repVal + "\t" + tmtVal, i);
                }
            }


        } catch (Exception ex) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            if (ex instanceof FileNotFoundException) {
                alert.setTitle("File Not Found Exception");
                alert.setHeaderText("File Not Found");
                alert.setContentText("Could not open file: " + filePath.get());
            }
            else {
                alert.setTitle("File IO Exception");
                alert.setHeaderText("File IO Exception");
                alert.setContentText("Could not read file: " + filePath.get());
            }

            alert.showAndWait();

            return null;

        }



        return headers;
    }

}
