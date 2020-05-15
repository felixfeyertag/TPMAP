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
package com.chembiohub.tpmap.dstruct.io;

import com.chembiohub.tpmap.dstruct.Proteome;
import com.chembiohub.tpmap.dstruct.Protein2D;
import com.chembiohub.tpmap.normalisation.TPNormalisation;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.converter.IntegerStringConverter;

/**
 * ImportWizard1
 *
 * Imports abundance file. Experimental/test use only, refer to ImportWizard class for in-use implementation.
 *
 * @author felixfeyertag
 */
class ImportWizard1 implements Runnable {
        
    private final Stack<Parent> steps = new Stack<>();
    private final IntegerProperty currentStep = new SimpleIntegerProperty();
    
    private final Button nextBtn = new Button("Next");
    private final Button prevBtn = new Button("Previous");
    private final Button cancBtn = new Button("Cancel");

    private final VBox contents = new VBox();
    
    private final Stage importWizardStage;
    
    private final StringProperty filePath = new SimpleStringProperty();
    
    private final IntegerProperty tempVals = new SimpleIntegerProperty();
    private final IntegerProperty concVals = new SimpleIntegerProperty();
    private final StringProperty idVals = new SimpleStringProperty();
    private final StringProperty descVals = new SimpleStringProperty();
    private final StringProperty prefVals = new SimpleStringProperty();
    
    private ComboBox normCombo = new ComboBox();
    
    private Map<String,String> taxidMap;
    private ComboBox taxid;
    private String os = "";
    
    private TextField[] temperatures;
    private TextField[] concentrations;
    
    private ComboBox[][] abundanceValue;
    private ComboBox[] referenceValue;
    private HashMap<String,Integer> index;
    
    private final Proteome tppExperiment = null; // new Proteome();

    private boolean pdFileFormat;
    
    private ImportWizard1() {
        
        importWizardStage = new Stage();
        importWizardStage.initModality(Modality.APPLICATION_MODAL);
        
        importWizardStage.setHeight(400);
        importWizardStage.setWidth(200);

        BorderPane wizardPane = new BorderPane();
        wizardPane.setPadding(new Insets(15,12,15,12));
        
        
        currentStep.set(0);
        
        filePath.set("");
                
        steps.push(importWizardStep1());
        
        HBox buttons = new HBox();
        buttons.getChildren().addAll(cancBtn,prevBtn,nextBtn);
        buttons.setSpacing(10);
        
        contents.setPadding(new Insets(15,12,15,12));
        contents.getChildren().add(steps.get(currentStep.get()));
        
        wizardPane.setPadding(new Insets(15, 12, 15, 12));
        wizardPane.setBottom(buttons);
        wizardPane.setCenter(contents);
        
        initButtons();

        Scene scene = new Scene(wizardPane);
        
        importWizardStage.setScene(scene);
    }
    
    @Override
    public synchronized void run() {
        importWizardStage.showAndWait();
    }
    
    private void initButtons() {
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
                Parent step2 = importWizardStep2();
                if(step2!=null) {
                    nextBtn.setText("Import");
                    currentStep.set(currentStep.get()+1);
                    contents.getChildren().remove(steps.peek());
                    steps.push(step2);
                    contents.getChildren().add(steps.peek());
                }
            }

            else if(currentStep.isEqualTo(1).get()) {
                Parent step3 = importWizardStep3();
                if(step3 != null) {
                    currentStep.set(currentStep.get()+1);
                    contents.getChildren().remove(steps.peek());
                    steps.push(step3);
                    contents.getChildren().add(steps.peek());
                }
            }
            
        });
        
        cancBtn.setOnAction((ActionEvent event) -> importWizardStage.close());
    }
    
    private Parent importWizardStep1() {

        GridPane step1 = new GridPane();
        
        int row = 0;
        
        //Input format
        step1.add(new Label("Select Input Format"), 1, row++);
        final ToggleGroup formatSelector = new ToggleGroup();
        RadioButton pdRadioButton = new RadioButton("Proteome Discoverer Output Format");
        pdRadioButton.setToggleGroup(formatSelector);
        pdRadioButton.setSelected(true);
        RadioButton tppRadioButton = new RadioButton("Bioconductor TPP Package Format");
        tppRadioButton.setToggleGroup(formatSelector);
        step1.add(pdRadioButton, 1, row++);
        step1.add(tppRadioButton, 1, row++);
        
        
        //File Loader

        HBox fileLoadBox = new HBox();
        fileLoadBox.setPadding(new Insets(10,8,10,8));
        Label flLabel = new Label("File name: ");
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
            } catch (Exception e) {  }
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
            } catch (NumberFormatException e) {  }
        });

        step1.add(tempBox, 1, row++);
        
        //Concentration
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
        
        //Prefix
        HBox prefBox = new HBox();
        prefBox.setPadding(new Insets(10,8,10,8));
        Label prefLabel = new Label("Fold change prefix:");
        prefLabel.setMinWidth(180);
        prefBox.getChildren().add(prefLabel);
        TextField prefField = new TextField();
        prefVals.set("FC_");
        prefField.setText(prefVals.get());
        prefBox.getChildren().add(prefField);

        prefField.lengthProperty().addListener(event -> prefVals.set(prefField.getText()));

        step1.add(prefBox, 1, row++);
        
        //Normalisation
        HBox normBox = new HBox();
        normBox.setPadding(new Insets(10,8,10,8));
        Label normLabel = new Label("Normalization method:");
        normLabel.setMinWidth(180);
        normBox.getChildren().add(normLabel);
        ObservableList<String> normalisationMethods = FXCollections.observableArrayList();
        normalisationMethods.add("Median");
        normalisationMethods.add("None");
        normCombo = new ComboBox(normalisationMethods);
        normCombo.setValue(normalisationMethods.get(0));
        normBox.getChildren().add(normCombo);
        step1.add(normBox, 1, row++);
        
        prefField.setDisable(true);
        pdFileFormat = true;

        pdRadioButton.selectedProperty().addListener( event -> {
            prefField.setDisable(true);
            tempField.setDisable(false);
            concField.setDisable(false);
            pdFileFormat = true;
        });
                
                
        tppRadioButton.selectedProperty().addListener( event -> {
            tempField.setDisable(true);
            concField.setDisable(true);
            prefField.setDisable(false);
            pdFileFormat = false;
        });
        
        return step1;
    }
    
    private Parent importWizardStep2() {
        
        VBox step2 = new VBox();
        GridPane step2grid = new GridPane();
        
        if(pdFileFormat) {
        
            temperatures = new TextField[tempVals.get()];
            concentrations = new TextField[concVals.get()];

            double[] defaultConc5 = { 35.0, 7.1, 4.0, 0.28, 0.0 };
            double[] defaultTemp12 = { 42, 62, 44, 64, 46, 58, 48, 60, 50, 54, 52, 56 };
            double[] defaultTemp8 = { 46, 58, 48, 60, 50, 54, 52, 56 };

            for(int i=1; i<=concentrations.length; i++) {
                concentrations[i-1] = new TextField();
                concentrations[i-1].setPrefWidth(100);
                if(concentrations.length!=5) {
                    concentrations[i-1].setText(String.valueOf(i));
                }
                else {
                    concentrations[i-1].setText(String.valueOf(defaultConc5[i-1]));
                }
                step2grid.add(concentrations[i-1], i, 0);
            }

            Label baseLabel = new Label("Reference");
            baseLabel.setPadding(new Insets(0,16,0,16));
            step2grid.add(baseLabel, concentrations.length+1, 0);

            for(int i=1; i<=temperatures.length; i++) {
                temperatures[i-1] = new TextField();
                temperatures[i-1].setPrefWidth(100);
                switch (temperatures.length) {
                    case 12:
                        temperatures[i-1].setText(String.valueOf(defaultTemp12[i-1]));
                        break;
                    case 8:
                        temperatures[i-1].setText(String.valueOf(defaultTemp8[i-1]));
                        break;
                    default:
                        temperatures[i-1].setText(String.valueOf(i));
                        break;
                }
                step2grid.add(temperatures[i-1],0,i);
            }

            ObservableList<String> headers;

            headers = extractHeaders();

            if(headers==null) {
                return null;
            }

            abundanceValue = new ComboBox[concentrations.length][temperatures.length];
            referenceValue = new ComboBox[temperatures.length];

            int counter = 0;

            for (int j=0; j<temperatures.length; j++) {
                for(int i=0; i<concentrations.length; i++)  {
                    abundanceValue[i][j] = new ComboBox(headers);
                    abundanceValue[i][j].setPrefWidth(100);
                    abundanceValue[i][j].setValue(headers.get(counter++));
                    step2grid.add(abundanceValue[i][j],i+1,j+1);
                }
            }

            for (int i=0; i<temperatures.length; i++) {
                referenceValue[i] = new ComboBox(headers);
                referenceValue[i].setPrefWidth(100);
                referenceValue[i].setValue(abundanceValue[concentrations.length-1][i].getSelectionModel().getSelectedItem());
                step2grid.add(referenceValue[i], concentrations.length+1, i+1);
            }

            step2.getChildren().add(step2grid);

            HBox speciesBox = new HBox();

            speciesBox.getChildren().add(new Label("Species: "));

            taxidMap = new HashMap<>();

            StringProperty selectedTaxId = new SimpleStringProperty("Homo sapiens (taxid:9606)");

            try (Stream<String> stream = Files.lines(Paths.get("lib/species.v10.5.txt"))) {
                stream.forEach((String line) -> {
                    if(line.startsWith("#")) {
                        return;
                    }
                    String[] taxonomy = line.split("\t");
                    if(!taxonomy[1].equals("core")) {
                        return;
                    }
                    String t = taxonomy[3] + " (taxid:" + taxonomy[0] + ")";
                    taxidMap.put(t, taxonomy[0]);
                    if(taxonomy[3].toLowerCase().equals(os.toLowerCase())) {
                        selectedTaxId.set(t);
                    }
                });
            } catch (IOException ex) {
                Logger.getLogger(ImportWizard1.class.getName()).log(Level.SEVERE, null, ex);
            } 

            ObservableList<String> taxidList = FXCollections.observableArrayList();

            taxidList.addAll(taxidMap.keySet());

            FXCollections.sort(taxidList);

            taxid = new ComboBox(taxidList);

            taxid.setValue(selectedTaxId.get());

            speciesBox.getChildren().add(taxid);

            step2.getChildren().add(speciesBox);
        
        }
        // // Bioconductor TPP format
        // else {
        //     BufferedReader br = null;
        //     try {
        //         br = new BufferedReader(new FileReader(filePath.get()));
        //         String[] head = br.readLine().split("\t");
        //     } catch (IOException ex) {
        //         Logger.getLogger(ImportWizard1.class.getName()).log(Level.SEVERE, null, ex);
        //     }
        // }
        
        return step2;
    }
    
    private synchronized Parent importWizardStep3() {
        
        Task<Void> extractProteins = extractProteins();
        
        Stage progressStage = new Stage();
        progressStage.initStyle(StageStyle.UTILITY);
        progressStage.initModality(Modality.APPLICATION_MODAL);
        
        final Label progressLabel = new Label();
        progressLabel.setText("Alert");
        
        ProgressBar proteinProgressBar = new ProgressBar();
        ProgressIndicator proteinProgressIndicator = new ProgressIndicator();
        
        proteinProgressBar.setProgress(-1F);
        proteinProgressIndicator.setProgress(-1F);
        
        HBox progressHBox = new HBox();
        progressHBox.setSpacing(5);
        progressHBox.setAlignment(Pos.CENTER);
        progressHBox.getChildren().addAll(proteinProgressBar,proteinProgressIndicator);
        
        Scene progressScene = new Scene(progressHBox);
        
        progressStage.setScene(progressScene);
        
        proteinProgressBar.progressProperty().bind(extractProteins.progressProperty());
        proteinProgressIndicator.progressProperty().bind(extractProteins.progressProperty());
        
        extractProteins.setOnSucceeded(event -> {
            progressStage.close();
            importWizardStage.close();
        });
        
        
        Thread progressThread = new Thread(extractProteins);
        progressThread.start();
        
        progressStage.showAndWait();
        
        
        return new HBox();
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
                else if(head[i].startsWith("Abundances (Scaled): ") || head[i].startsWith("Abundance: ")) {
                    String tmtVal = head[i];
                    String repVal = head[i];
                    repVal = repVal.replaceFirst("^[^:]*: ", "");
                    repVal = repVal.replaceFirst(":.*$", "");
                    tmtVal = tmtVal.replaceFirst("^.*: ", "");
                    tmtVal = tmtVal.replaceFirst(", .*$", "");
                    headers.add(repVal + ":" + tmtVal);
                    index.put(repVal + ":" + tmtVal, i);
                }
            }

            //extract organism name from first line
            String[] firstLine = br.readLine().split("\t");

            String organismName = "";

            String[] descriptionVals = firstLine[index.get("Description")].split("=");

            for(int j=1;j<descriptionVals.length;j++) {
                if(descriptionVals[j-1].endsWith(" OS")) {
                    organismName = descriptionVals[j];
                    organismName = organismName.replaceAll(" OX$", "");
                    organismName = organismName.replaceAll(" GN$", "");
                    organismName = organismName.replaceAll(" PE$", "");
                    organismName = organismName.replaceAll(" SV$", "");
                    if(os.isEmpty()) {
                        os = organismName;
                    }
                }
            }



        } catch (Exception ex) {
            Alert alert = new Alert(AlertType.ERROR);
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

            Logger.getLogger(ImportWizard1.class.getName()).log(Level.WARNING, null, ex);

            return null;

        }



        return headers;
    }
    
    private Task<Void> extractProteins() {
        
        return new Task<Void>() {
            @Override
            public Void call() {
        
                try {
                    
                    Objects.requireNonNull(tppExperiment).setFileName(Paths.get(filePath.get()).getFileName().toString());
                    Stream<String> stream = Files.lines(Paths.get(filePath.get()));
                    long numProteins = Files.lines(Paths.get(filePath.get())).count()-1;
                    IntegerProperty counter = new SimpleIntegerProperty(0);
                    BooleanProperty head = new SimpleBooleanProperty(true);
                    int accessionCoordinate = index.get("Accession");
                    int descriptionCoordinate = index.get("Description");

                    //abundanceCoordinates maps the column in the input file to the 2D position in the resulting temp/concentration abundance matrix
                    int[][] abundanceCoordinates = new int[abundanceValue.length][abundanceValue[0].length];
                    for (int i=0;i<abundanceCoordinates.length;i++) {
                        for (int j=0;j<abundanceCoordinates[0].length;j++) {
                            abundanceCoordinates[i][j] = index.get(abundanceValue[i][j].getSelectionModel().getSelectedItem().toString());
                        }
                    }
                    
                    int[] referenceCoordinates = new int[referenceValue.length];
                    for(int i=0;i<referenceCoordinates.length;i++) {
                        referenceCoordinates[i] = index.get(referenceValue[i].getSelectionModel().getSelectedItem().toString());
                    }
                    


                    ArrayList<Double> tempList = new ArrayList<>();
                    ArrayList<Double> concList = new ArrayList<>();
                    ArrayList<Double> sortedTempList = new ArrayList<>();
                    ArrayList<Double> sortedConcList = new ArrayList<>();
                    
                    for(TextField tf : temperatures) {
                        tempList.add(Double.parseDouble(tf.getText()));
                        sortedTempList.add(Double.parseDouble(tf.getText()));
                    }
                    for(TextField tf : concentrations) {
                        concList.add(Double.parseDouble(tf.getText()));
                        sortedConcList.add(Double.parseDouble(tf.getText()));
                    }
                    Collections.sort(sortedTempList);
                    Collections.sort(sortedConcList);
                    

                    
                    
                    ObservableList<String> concLabels = FXCollections.observableArrayList();
                    sortedConcList.forEach( c -> concLabels.add(c.toString()));
                    ObservableList<String> tempLabels = FXCollections.observableArrayList();
                    sortedTempList.forEach( t -> tempLabels.add(t.toString()));
                    
                    Objects.requireNonNull(tppExperiment).setTaxonomy(taxidMap.get(taxid.getSelectionModel().getSelectedItem()));
                    
                    Objects.requireNonNull(tppExperiment).setConcLabels(concLabels);
                    Objects.requireNonNull(tppExperiment).setTempLabels(tempLabels);

                    //tempIndex and concIndex contain indices for temperature and concentration in ascending order
                    int[] tempIndex = new int[tempList.size()];
                    int[] concIndex = new int[concList.size()];
                    
                    
                    for(int i=0; i<tempIndex.length; i++) {
                        tempIndex[i] = tempList.indexOf(sortedTempList.get(i));
                    }
                    for(int i=0; i<concIndex.length; i++) {
                        concIndex[i] = concList.indexOf(sortedConcList.get(i));
                    }
                    stream.forEach((String line) -> {
                        if(head.get()) {
                            head.set(false);
                            return;
                        }
                        Protein2D protein = new Protein2D();
                        String[] lineVals = line.split("\t");
                        String accession = lineVals[accessionCoordinate];
                        
                        if(accession==null) {
                            return;
                        }

                        String organismName = "";
                        String organismIdentifier = "";
                        String geneName = "";
                        String proteinExistence = "";
                        String sequenceVersion = "";

                        
                        //Parse UniProt description https://www.uniprot.org/help/fasta-headers
                        lineVals[descriptionCoordinate] = lineVals[descriptionCoordinate].replaceAll("\"", "");
                        String[] descriptionVals = lineVals[descriptionCoordinate].split("=");
                        String description = descriptionVals[0];
                        description = description.replaceAll(" OS$", "");
                        description = description.replaceAll(" OX$", "");
                        description = description.replaceAll(" GN$", "");
                        description = description.replaceAll(" PE$", "");
                        description = description.replaceAll(" SV$", "");
                        for(int j=1;j<descriptionVals.length;j++) {
                            if(descriptionVals[j-1].endsWith(" OS")) {
                                organismName = descriptionVals[j];
                                organismName = organismName.replaceAll(" OX$", "");
                                organismName = organismName.replaceAll(" GN$", "");
                                organismName = organismName.replaceAll(" PE$", "");
                                organismName = organismName.replaceAll(" SV$", "");
                            }
                            else if (descriptionVals[j-1].endsWith(" OX")) {
                                organismIdentifier = descriptionVals[j];
                                organismIdentifier = organismIdentifier.replaceAll(" OS$", "");
                                organismIdentifier = organismIdentifier.replaceAll(" GN$", "");
                                organismIdentifier = organismIdentifier.replaceAll(" PE$", "");
                                organismIdentifier = organismIdentifier.replaceAll(" SV$", "");
                            }
                            else if (descriptionVals[j-1].endsWith(" GN")) {
                                geneName = descriptionVals[j];
                                geneName = geneName.replaceAll(" OS$", "");
                                geneName = geneName.replaceAll(" OX$", "");
                                geneName = geneName.replaceAll(" PE$", "");
                                geneName = geneName.replaceAll(" SV$", "");
                            }
                            else if (descriptionVals[j-1].endsWith(" PE")) {
                                proteinExistence = descriptionVals[j];
                                proteinExistence = proteinExistence.replaceAll(" OS$", "");
                                proteinExistence = proteinExistence.replaceAll(" OX$", "");
                                proteinExistence = proteinExistence.replaceAll(" GN$", "");
                                proteinExistence = proteinExistence.replaceAll(" SV$", "");
                            }
                            else if (descriptionVals[j-1].endsWith(" SV")) {
                                sequenceVersion = descriptionVals[j];
                                sequenceVersion = sequenceVersion.replaceAll(" OS$", "");
                                sequenceVersion = sequenceVersion.replaceAll(" OX$", "");
                                sequenceVersion = sequenceVersion.replaceAll(" GN$", "");
                                sequenceVersion = sequenceVersion.replaceAll(" PE$", "");
                            }
                        }

                        Double[][] abundances = new Double[abundanceCoordinates[0].length][abundanceCoordinates.length];
                        
                        //reference holds the denominator for calculating ratios
                        final Double[] references = new Double[referenceCoordinates.length];
                        for(int i=0; i<references.length; i++) {
                            Double reference = Double.NaN;
                            try {
                                references[i] = Double.parseDouble(lineVals[referenceCoordinates[tempIndex[i]]]);
                            } catch (NumberFormatException e) {
                                references[i] = Double.NaN;
                            }
                        }
                        for(int i=0; i<abundances.length; i++) {
                            for(int j=0; j<abundances[0].length; j++) {
                                Double abundance = Double.NaN;
                                try {
                                    abundances[i][j] = Double.parseDouble(lineVals[abundanceCoordinates[concIndex[j]][tempIndex[i]]]);

                                } catch (NumberFormatException e) {
                                    abundances[i][j] = Double.NaN;
                                }
                            }
                        }
                        

                        protein.setAccession((accession));
                        protein.setDescription((description));
                        protein.setOrganismName(organismName);
                        protein.setOrganismIdentifier(organismIdentifier);
                        protein.setGeneName(geneName);
                        protein.setProteinExistence(proteinExistence);
                        protein.setSequenceVersion(sequenceVersion);
                        protein.setConcReference(references);
                        protein.setAbundances(abundances, Objects.requireNonNull(tppExperiment).getTempLabels(), Objects.requireNonNull(tppExperiment).getConcLabels());
                        

                        
                        if(normCombo.getSelectionModel().getSelectedItem().equals("Median")) {
                            protein.setNormalisationMethod(TPNormalisation.Normalisation.MEDIAN);
                        }
                        else {
                            protein.setNormalisationMethod(TPNormalisation.Normalisation.NONE);
                        }
                        
                        Objects.requireNonNull(tppExperiment).addProtein(protein);
                        
                        counter.set(counter.get()+1);
                        updateProgress(counter.get(),numProteins);
                        updateMessage("Imported " + counter.get() + " out of " + numProteins + " proteins.");
                    });
                    Objects.requireNonNull(tppExperiment).updateProteinCount();
                    if(normCombo.getSelectionModel().getSelectedItem().equals("Median")) {
                        Objects.requireNonNull(tppExperiment).setNormalisation(TPNormalisation.Normalisation.MEDIAN);
                    }
                    else {
                        Objects.requireNonNull(tppExperiment).setNormalisation(TPNormalisation.Normalisation.NONE);
                    }
                    


                } catch (IOException | NumberFormatException ex) {
                    

                    
                    Alert alert = new Alert(AlertType.ERROR);
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

                    Logger.getLogger(ImportWizard1.class.getName()).log(Level.WARNING, null, ex);

                    alert.showAndWait();

                    contents.getChildren().remove(steps.pop());
                    contents.getChildren().add(steps.peek());
                    currentStep.set(currentStep.get()-1);
                }
                return null;
            }
        };
        
    }
    
    public Proteome getTppExperiment() {
        return tppExperiment;
    }
    
}
