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

import com.chembiohub.tpmap.dstruct.Protein1DParameters;
import com.chembiohub.tpmap.dstruct.Protein2DParameters;
import com.chembiohub.tpmap.dstruct.ProteinParameters;
import com.chembiohub.tpmap.dstruct.Proteome;
import com.chembiohub.tpmap.dstruct.Proteome.ExpType;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Stack;
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.beans.Observable;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.input.DragEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import com.chembiohub.tpmap.normalisation.TPNormalisation;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Orientation;

/**
 * ImportWizard
 *
 * Create a pane that sets up a menu for importing protein abundance into a TPMAP Proteome object. The pane allows
 * a user to decide between 1D and 2D TP experiments, TPMAP or ProteomeDiscoverer file format, normalization option
 * and parameters for 1D and 2D TP experiments. A Proteome class is constructed.
 *
 * @author felixfeyertag
 */
public class ImportWizard implements Runnable {
    
    private final Stack<Parent> steps;
    private final IntegerProperty currentStep;
    
    private final Button nextBtn;
    private final Button prevBtn;
    private final Button cancBtn;
    
    private final VBox contents;
    
    private final Stage importWizardStage;
    
    private final StringProperty dataFilePath;
    private final StringProperty confFilePath;
    
    private ComboBox<String> normCombo;
    private enum FILEFORMAT { TPMAP, PD }

    private FILEFORMAT ff;
    
    private Proteome.ExpType expType;
    
    private CheckBox bsCheckbox;
    private TextField bsTextField;
    private CheckBox cfCheckbox;
    private TextField cfattemptsTextField;
    private TextField cfiterationsTextField;
    private CheckBox mtCheckbox;

    private Proteome tppExperiment;
    

    public ImportWizard(Stage parentStage, TabPane tpTabPane, double min, double max) {

        final BorderPane wizardPane = new BorderPane();

        this.ff = FILEFORMAT.TPMAP;
        this.tppExperiment = new Proteome(parentStage, tpTabPane);
        this.steps = new Stack<>();
        this.currentStep = new SimpleIntegerProperty();
        this.nextBtn = new Button("Import");
        this.prevBtn = new Button("Previous");
        this.cancBtn = new Button("Cancel");
        this.contents = new VBox();
        this.dataFilePath = new SimpleStringProperty();
        this.confFilePath = new SimpleStringProperty();
        this.normCombo = new ComboBox<>();

        importWizardStage = new Stage();
        importWizardStage.initOwner(parentStage);
        importWizardStage.initModality(Modality.APPLICATION_MODAL);
        
        importWizardStage.setHeight(420);
        importWizardStage.setWidth(630);
        currentStep.set(0);
        
        dataFilePath.set("");
                
        steps.push(importWizardStep1());
        
        HBox buttons = new HBox();
        buttons.getChildren().addAll(cancBtn,nextBtn);
        buttons.setSpacing(10);
        
        contents.setPadding(new Insets(15,12,15,12));
        contents.getChildren().add(steps.get(currentStep.get()));
        
        wizardPane.setPadding(new Insets(15, 12, 15, 12));
        wizardPane.setBottom(buttons);
        wizardPane.setCenter(contents);
        
        initButtons(min,max);

        final Scene scene = new Scene(wizardPane);
        
        importWizardStage.setScene(scene);
    }
    
    @Override
    public synchronized void run() {
        importWizardStage.showAndWait();
    }
    
    private void initButtons(double min,double max) {

        nextBtn.setPrefSize(100,20);
        cancBtn.setPrefSize(100,20);
        
        prevBtn.setOnAction((ActionEvent event) -> {
            if(currentStep.greaterThan(0).get()) {
                nextBtn.setText("Import");
                contents.getChildren().remove(steps.pop());
                contents.getChildren().add(steps.peek());
                currentStep.set(currentStep.get()-1);
            }
        });
        
        nextBtn.setOnAction((ActionEvent event) -> {

            if(currentStep.isEqualTo(0).get()) {
                Parent step2 = null;

                try {
                    step2 = importWizardStep3(min,max);
                    if(step2!=null) {
                        nextBtn.setText("Import");
                        currentStep.set(currentStep.get()+1);
                        contents.getChildren().remove(steps.peek());
                        steps.push(step2);
                        contents.getChildren().add(steps.peek());
                    }
                    else {
                        Alert alert = new Alert(AlertType.ERROR);
                        alert.initModality(Modality.APPLICATION_MODAL);
                        alert.initOwner(importWizardStage);
                        alert.setTitle("Unable to import data");
                        alert.setHeaderText("Unable to import data");
                        alert.showAndWait();
                    }

                } catch (InvalidFileFormatException ex) {
                    Alert alert = new Alert(AlertType.ERROR);
                    alert.initModality(Modality.APPLICATION_MODAL);
                    alert.initOwner(importWizardStage);
                    alert.setTitle("Unable to import data");
                    alert.setHeaderText("Unable to import data");
                    alert.setContentText(ex.getMessage());
                    alert.showAndWait();
                    Logger.getLogger(ImportWizard.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        });
        
        cancBtn.setOnAction((ActionEvent event) -> importWizardStage.close());
    }
    
    private Parent importWizardStep1() {
        GridPane step1 = new GridPane();
        
        int row = 0;
        
        this.ff = FILEFORMAT.TPMAP;
        this.expType = ExpType.TP2D;
        
        HBox formatHBox = new HBox();

        nextBtn.setDisable(true);
        
        //Experiment format
        VBox expFormatVBox = new VBox();
        ToggleGroup experimentSelector = new ToggleGroup();
        expFormatVBox.getChildren().add(new Label("Select Experiment Type"));
        RadioButton exp1dRadioButton = new RadioButton("1D");
        exp1dRadioButton.setPadding(new Insets(5,5,10,20));
        exp1dRadioButton.setToggleGroup(experimentSelector);
        RadioButton exp2dRadioButton = new RadioButton("2D");
        exp2dRadioButton.setPadding(new Insets(10,5,5,20));
        exp2dRadioButton.setToggleGroup(experimentSelector);
        exp2dRadioButton.setSelected(true);
        RadioButton expPISARadioButton = new RadioButton("PISA");
        expPISARadioButton.setPadding(new Insets(10,5,5,20));
        expPISARadioButton.setToggleGroup(experimentSelector);
        expFormatVBox.getChildren().add(exp2dRadioButton);
        expFormatVBox.getChildren().add(exp1dRadioButton);
        /* expFormatVBox.getChildren().add(expPISARadioButton); */

        //Input format
        VBox inpFormatVBox = new VBox();
        ToggleGroup formatSelector = new ToggleGroup();
        inpFormatVBox.getChildren().add(new Label("Select Input Format"));
        RadioButton tpmapRadioButton = new RadioButton("TP-MAP");
        tpmapRadioButton.setPadding(new Insets(10,5,5,20));
        tpmapRadioButton.setToggleGroup(formatSelector);
        tpmapRadioButton.setSelected(true);
        RadioButton pdRadioButton = new RadioButton("Proteome Discoverer");
        pdRadioButton.setPadding(new Insets(5,5,10,20));
        pdRadioButton.setToggleGroup(formatSelector);
        inpFormatVBox.getChildren().add(tpmapRadioButton);
        inpFormatVBox.getChildren().add(pdRadioButton);

        //Normalisation
        VBox normBox = new VBox();
        normBox.setPadding(new Insets(0,0,10,0));
        Label normLabel = new Label("Normalization method");
        normLabel.setPadding(new Insets(0,0,10,0));
        normLabel.setMinWidth(180);
        normBox.getChildren().add(normLabel);
        ObservableList<String> normalisationMethods = FXCollections.observableArrayList();
        normalisationMethods.add("Median");
        normalisationMethods.add("None");
        normCombo = new ComboBox<>(normalisationMethods);
        normCombo.setValue(normalisationMethods.get(0));
        //normCombo.setPadding(new Insets(10,8,10,8));
        normBox.getChildren().add(normCombo);
        //step1.add(normBox,1,row++);

        Separator formatSep1 = new Separator();
        formatSep1.setOrientation(Orientation.VERTICAL);
        formatSep1.setPadding(new Insets(5,10,5,10));
        Separator formatSep2 = new Separator();
        formatSep2.setOrientation(Orientation.VERTICAL);
        formatSep2.setPadding(new Insets(5,10,5,10));
        formatHBox.getChildren().addAll(expFormatVBox,formatSep1,inpFormatVBox,formatSep2,normBox);
        step1.add(formatHBox, 1, row++);
        
        //Data file loader
        HBox dataFileLoadBox = new HBox();
        dataFileLoadBox.setPadding(new Insets(10,8,5,8));
        Label dfLabel = new Label ("Data file: ");
        dfLabel.setMinWidth(180);
        dataFileLoadBox.getChildren().add(dfLabel);
        TextField dataFileName = new TextField();
        dataFileName.setMinWidth(200);
        dataFileName.setEditable(false);
        dataFileLoadBox.getChildren().add(dataFileName);
        Button dataFileLoadButton = new Button();
        FileChooser dataFileFC = new FileChooser();
        dataFileFC.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Excel Files (*.xls, *.xlsx), Tab-Separated Text Files (*.txt, *.tab, *.dat)", "*.xls", "*.xlsx", "*.txt", "*.tab", "*.dat")
        );
        dataFileFC.setTitle("Open Data File");
        dataFileLoadButton.setText("Load");
        dataFileLoadBox.getChildren().add(dataFileLoadButton);
        step1.add(dataFileLoadBox, 1, row++);
        dataFileLoadButton.setOnAction((ActionEvent event) -> {
            try {
                File f = dataFileFC.showOpenDialog(importWizardStage);
                dataFilePath.set(f.getAbsolutePath());
                dataFileName.setText(dataFilePath.get());
                nextBtn.setDisable(false);
            } catch (Exception e) { }
        });
        dataFileName.setOnDragOver(new EventHandler() {
            @Override
            public void handle(Event event) {
                if(event instanceof DragEvent) {
                    DragEvent evt = (DragEvent) event;
                    if (evt.getDragboard().hasFiles() && evt.getDragboard().getFiles().size()==1) {
                        evt.acceptTransferModes(TransferMode.ANY);
                    }
                    evt.consume();
                }
            }
        });
        dataFileName.setOnDragDropped(new EventHandler<DragEvent>() {
            public void handle(DragEvent event) {
                List<File> files = event.getDragboard().getFiles();
                dataFilePath.set(files.get(0).getAbsolutePath());
                dataFileName.setText(files.get(0).getAbsolutePath());
                nextBtn.setDisable(false);
                event.consume();
            }
        });
        
        //Configuration file loader
        HBox confFileLoadBox = new HBox();
        confFileLoadBox.setPadding(new Insets(5,8,10,8));
        Label cfLabel = new Label("Configuration file: ");
        cfLabel.setMinWidth(180);
        confFileLoadBox.getChildren().add(cfLabel);
        TextField confFileName = new TextField();
        confFileName.setMinWidth(200);
        confFileName.setEditable(false);
        confFileLoadBox.getChildren().add(confFileName);
        Button confFileLoadButton = new Button();
        FileChooser confFileFC = new FileChooser();
        confFileFC.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Excel Files (*.xls, *.xlsx), Tab-Separated Text Files (*.txt, *.tab, *.dat)", "*.xls", "*.xlsx", "*.txt", "*.tab", "*.dat")
        );
        confFileFC.setTitle("Open Configuration File");
        confFileLoadButton.setText("Load");
        confFileLoadBox.getChildren().add(confFileLoadButton);
        step1.add(confFileLoadBox, 1, row++);
        confFileLoadButton.setOnAction((ActionEvent event) -> {
            try {
                File f = confFileFC.showOpenDialog(importWizardStage);
                confFilePath.set(f.getAbsolutePath());
                confFileName.setText(confFilePath.get());
                if(!dataFileName.getText().isEmpty()) {
                    nextBtn.setDisable(false);
                }
            } catch (Exception e) { }
        });
        confFileLoadBox.setDisable(true);
        confFileName.setOnDragOver(new EventHandler() {
            @Override
            public void handle(Event event) {
                if(event instanceof DragEvent) {
                    DragEvent evt = (DragEvent) event;
                    if (evt.getDragboard().hasFiles() && evt.getDragboard().getFiles().size()==1) {
                        evt.acceptTransferModes(TransferMode.ANY);
                    }
                    evt.consume();
                }
            }
        });
        confFileName.setOnDragDropped(new EventHandler<DragEvent>() {
            public void handle(DragEvent event) {
                List<File> files = event.getDragboard().getFiles();
                confFilePath.set(files.get(0).getAbsolutePath());
                confFileName.setText(files.get(0).getAbsolutePath());
                if(!dataFileName.getText().isEmpty()) {
                    nextBtn.setDisable(false);
                }
                event.consume();
            }
        });

        

        GridPane parameterPane = new GridPane();
        parameterPane.setPadding(new Insets(10,8,10,8));

        //Bootstrap
        bsCheckbox = new CheckBox("2D Bootstrap Analysis");
        bsCheckbox.setSelected(true);
        Label bsLabel = new Label("Iterations: ");
        bsTextField = new TextField();
        //bsCheckbox.selectedProperty().bind(Bindings.not(bsTextField.disabledProperty()));
        bsTextField.setText("1000000");
        bsTextField.textProperty().addListener((ObservableValue<? extends String> observable, String oldValue, String newValue) -> {
            if (!newValue.matches("\\d*")) {
                bsTextField.setText(newValue.replaceAll("[^\\d]", ""));
            }
        });

        bsLabel.setPadding(new Insets(5,20,5,20));

        parameterPane.add(bsCheckbox, 1, 1);
        parameterPane.add(bsLabel, 2, 1);
        parameterPane.add(bsTextField, 3, 1);

        //Curve fitting
        cfCheckbox = new CheckBox("1D Curve Fitting");
        cfCheckbox.setSelected(true);
        Label cfattemptsLabel = new Label("Fit Attempts: ");
        Label cfiterationsLabel = new Label("Max Iterations: ");
        cfattemptsTextField = new TextField();
        cfiterationsTextField = new TextField();
        cfattemptsTextField.setText("10");
        cfiterationsTextField.setText("10000");
        cfattemptsTextField.textProperty().addListener((ObservableValue<? extends String> observable, String oldValue, String newValue) -> {
            if (!newValue.matches("\\d*")) {
                cfattemptsTextField.setText(newValue.replaceAll("[^\\d]", ""));
            }
        });
        cfiterationsTextField.textProperty().addListener((ObservableValue<? extends String> observable, String oldValue, String newValue) -> {
            if (!newValue.matches("\\d*")) {
                cfiterationsTextField.setText(newValue.replaceAll("[^\\d]", ""));
            }
        });

        cfattemptsLabel.setPadding(new Insets(5,20,5,20));
        cfiterationsLabel.setPadding(new Insets(5,20,5,20));

        cfCheckbox.setDisable(true);
        cfattemptsLabel.setDisable(true);
        cfiterationsLabel.setDisable(true);
        cfattemptsTextField.setDisable(true);
        cfiterationsTextField.setDisable(true);

        parameterPane.add(cfCheckbox, 1, 2);
        parameterPane.add(cfattemptsLabel, 2, 2);
        parameterPane.add(cfiterationsLabel, 2, 3);
        parameterPane.add(cfattemptsTextField, 3, 2);
        parameterPane.add(cfiterationsTextField, 3, 3);
        step1.add(parameterPane,1,row++);

        //Multithreading
        HBox mtBox = new HBox();
        mtBox.setPadding(new Insets(10,8,10,8));
        mtCheckbox = new CheckBox("Multithreading");
        //mtCheckbox.setDisable(true);
        mtCheckbox.setSelected(true);
        mtBox.getChildren().add(mtCheckbox);
        step1.add(mtBox,1,row++);


        // Listeners

        bsCheckbox.selectedProperty().addListener(event -> {
            if(bsCheckbox.isSelected()) {
                bsLabel.setDisable(false);
                bsTextField.setDisable(false);
            }
            else {
                bsLabel.setDisable(true);
                bsTextField.setDisable(true);
            }
        });

        cfCheckbox.selectedProperty().addListener(event -> {
            if(cfCheckbox.isSelected()) {
                cfLabel.setDisable(false);
                cfattemptsLabel.setDisable(false);
                cfiterationsLabel.setDisable(false);
                cfattemptsTextField.setDisable(false);
                cfiterationsTextField.setDisable(false);
            }
            else {
                cfLabel.setDisable(true);
                cfattemptsLabel.setDisable(true);
                cfiterationsLabel.setDisable(true);
                cfattemptsTextField.setDisable(true);
                cfiterationsTextField.setDisable(true);
            }
        });

        exp1dRadioButton.selectedProperty().addListener((Observable event) -> {
            this.expType = ExpType.TP1D;
            tppExperiment.setExpType(ExpType.TP1D);
            bsCheckbox.setDisable(true);
            bsTextField.setDisable(true);
            bsLabel.setDisable(true);
            cfCheckbox.setDisable(false);
            cfattemptsLabel.setDisable(false);
            cfattemptsTextField.setDisable(false);
            cfiterationsLabel.setDisable(false);
            cfiterationsTextField.setDisable(false);
            normCombo.setValue(normalisationMethods.get(1));
            normCombo.setDisable(true);
        });

        exp2dRadioButton.selectedProperty().addListener((Observable event) -> {
            this.expType = ExpType.TP2D;
            tppExperiment.setExpType(ExpType.TP2D);
            bsCheckbox.setDisable(false);
            bsTextField.setDisable(false);
            bsLabel.setDisable(false);
            cfCheckbox.setDisable(true);
            cfattemptsLabel.setDisable(true);
            cfattemptsTextField.setDisable(true);
            cfiterationsLabel.setDisable(true);
            cfiterationsTextField.setDisable(true);
            normCombo.setValue(normalisationMethods.get(0));
            normCombo.setDisable(false);
        });

        expPISARadioButton.selectedProperty().addListener((Observable event) -> {
            this.expType = ExpType.PISA;
            tppExperiment.setExpType(ExpType.PISA);
            bsCheckbox.setDisable(true);
            //mtCheckbox.setDisable(true);
        });

        tpmapRadioButton.selectedProperty().addListener((Observable event) -> {
            dataFileLoadBox.setDisable(false);
            confFileLoadBox.setDisable(true);
            this.ff = FILEFORMAT.TPMAP;
            if(dataFileName.getText().isEmpty()) {
                nextBtn.setDisable(true);
            }
            else {
                nextBtn.setDisable(false);
            }
        });

        pdRadioButton.selectedProperty().addListener((Observable event) -> {
            dataFileLoadBox.setDisable(false);
            confFileLoadBox.setDisable(false);
            this.ff = FILEFORMAT.PD;
            if(dataFileName.getText().isEmpty() || confFileName.getText().isEmpty()) {
                nextBtn.setDisable(true);
            }
            else {
                nextBtn.setDisable(false);
            }
        });

        return step1;
    }
    
    
    private Parent importWizardStep3(double min,double max) throws InvalidFileFormatException {
        
        try {
            TPNormalisation.Normalisation norm;
            norm = normCombo.getSelectionModel().getSelectedItem().equals("Median") ? TPNormalisation.Normalisation.MEDIAN : TPNormalisation.Normalisation.NONE;
            
            int bsReplicates = 0;
            if(bsCheckbox.isSelected()) {
                try {
                    bsReplicates = Integer.parseInt(bsTextField.getText());
                } catch (NumberFormatException ex) {
                    throw new InvalidFileFormatException("Invalid input for bootstrap replicates: " + bsTextField.getText());
                }
            }

            int cfAttempts = 0;
            if(cfCheckbox.isSelected()) {
                try {
                    cfAttempts = Integer.parseInt(cfattemptsTextField.getText());
                } catch (NumberFormatException ex) {
                    throw new InvalidFileFormatException("Invalid input for bootstrap replicates: " + cfattemptsTextField.getText());
                }
            }

            int cfIterations = 0;
            if(cfCheckbox.isSelected()) {
                try {
                    cfIterations = Integer.parseInt(cfiterationsTextField.getText());
                } catch (NumberFormatException ex) {
                    throw new InvalidFileFormatException("Invalid input for bootstrap replicates: " + cfiterationsTextField.getText());
                }
            }

            ProteinParameters params = null;

            switch (expType) {
                case TP1D:
                    params = new Protein1DParameters(cfAttempts, cfIterations);
                    break;
                case TP2D:
                    params = new Protein2DParameters(bsReplicates);
                    break;
            }

            boolean multithreading = mtCheckbox.isSelected();
            
            FileImporter gfi;

            switch(this.ff) {
                case TPMAP:
                    gfi = new GenericFileImporter(dataFilePath,tppExperiment.getParentStage(),tppExperiment.getTabPane(),min,max,norm,params,multithreading,expType);
                    break;
                case PD:
                    gfi = new ProteomeDiscovererFileImporter(dataFilePath,confFilePath,tppExperiment.getParentStage(),tppExperiment.getTabPane(),min,max,norm,params,multithreading,expType);
                    break;
                default:
                    throw new InvalidFileFormatException("Unknown file format: " + this.ff.toString());
            }
            
            assert(gfi!=null);
            
            Task<Void> extractProteins = gfi.initImport();

            Stage progressStage = new Stage();
            progressStage.initStyle(StageStyle.UNDECORATED);
            progressStage.initModality(Modality.APPLICATION_MODAL);
            progressStage.initOwner(importWizardStage);

            ProgressBar proteinProgressBar = new ProgressBar();
            proteinProgressBar.setPrefWidth(200);
            ProgressIndicator proteinProgressIndicator = new ProgressIndicator();

            Label progressLabel = new Label();
            progressLabel.textProperty().unbind();
            
            proteinProgressBar.setProgress(-1F);
            proteinProgressIndicator.setProgress(-1F);

            Button progressCancelButton = new Button("Cancel");
            progressCancelButton.setOnAction( evt -> {
                extractProteins.cancel(true);
                progressStage.close();
                importWizardStage.close();
            });
            
            HBox progressHBox1 = new HBox();
            progressHBox1.setPrefWidth(250);
            progressHBox1.setSpacing(5);
            progressHBox1.setAlignment(Pos.CENTER);
            progressHBox1.getChildren().addAll(proteinProgressBar,proteinProgressIndicator);

            BorderPane progressPane = new BorderPane();
            progressPane.setPadding(new Insets(5,5,5,5));
            progressPane.setLeft(progressLabel);
            progressPane.setRight(progressCancelButton);
            //progressHBox2.setPrefWidth(300);
            //progressHBox2.getChildren().addAll(progressLabel);

            VBox progressVBox = new VBox();
            progressVBox.setSpacing(5);
            progressVBox.getChildren().addAll(progressHBox1,progressPane);
            
            Scene progressScene = new Scene(progressVBox);
            
            progressStage.setScene(progressScene);
            progressStage.setAlwaysOnTop(false);
            
            proteinProgressBar.progressProperty().bind(extractProteins.progressProperty());
            proteinProgressIndicator.progressProperty().bind(extractProteins.progressProperty());
            progressLabel.textProperty().bind(extractProteins.messageProperty());
            
            extractProteins.setOnSucceeded((WorkerStateEvent event) -> {                
                progressStage.close();
                importWizardStage.close();
            });
            extractProteins.setOnFailed((WorkerStateEvent event) -> {
                Logger.getLogger(ImportWizard.class.getName()).log(Level.SEVERE, null, extractProteins.getException());
                Alert alert = new Alert(AlertType.ERROR);
                alert.initModality(Modality.APPLICATION_MODAL);
                alert.initOwner(importWizardStage);
                alert.setTitle("Import Error");
                alert.setHeaderText("Unable to import file");
                alert.setContentText(extractProteins.getException().getLocalizedMessage());
                progressStage.close();
                importWizardStage.close();
                alert.show();
            });
            
            Thread progressThread = new Thread(extractProteins);
            progressThread.start();
            
            progressStage.showAndWait();
            
            tppExperiment = gfi.getTppExperiment();

            tppExperiment.setExpType(expType);

            //tppExperiment.setTaxonomy(taxidMap.get(taxid.getSelectionModel().getSelectedItem()));

            return new HBox();
            
            
        } catch (IOException ex) {

            Alert alert = new Alert(AlertType.ERROR);
            alert.initModality(Modality.APPLICATION_MODAL);
            alert.initOwner(importWizardStage);
            alert.setTitle("File IO Exception");
            alert.setHeaderText("File IO Exception");
            alert.setContentText("Could not read file: " + dataFilePath.get());
            //Optional<ButtonType> showAndWait = alert.showAndWait();
            alert.setOnCloseRequest( x -> importWizardStage.close());
            //importWizardStage.close();

            Logger.getLogger(ImportWizard.class.getName()).log(Level.SEVERE, null, ex);

        } catch (InvalidHeaderException ex ) {

            Alert alert = new Alert(AlertType.ERROR);
            alert.initModality(Modality.APPLICATION_MODAL);
            alert.initOwner(importWizardStage);
            alert.setTitle("Unable to import data");
            alert.setHeaderText("Unable to import data");
            alert.setContentText(ex.getLocalizedMessage());
            alert.setOnCloseRequest( x -> importWizardStage.close());
            //Optional<ButtonType> showAndWait = alert.showAndWait();
            //importWizardStage.close();

        }
        return new HBox();
    }
    
    
    public Proteome getTppExperiment() {
        return tppExperiment;
    }
    
}
