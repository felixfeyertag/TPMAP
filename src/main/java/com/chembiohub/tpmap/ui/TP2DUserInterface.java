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
package com.chembiohub.tpmap.ui;

import com.chembiohub.tpmap.analysis.analysispane.*;

import java.text.DecimalFormat;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.PatternSyntaxException;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.chart.NumberAxis;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.util.Callback;
import com.chembiohub.tpmap.dstruct.Proteome;
import com.chembiohub.tpmap.dstruct.Protein2D;

/**
 * TP2DUserInterface
 *
 * The TP2DUserInterface class creates the default tab that opens when a Protein2D Proteome object is loaded into
 * TPMAP
 *
 * It constructs a BorderPane with properties on the left, protein table and abundance fold change table in the
 * center and downstream analysis options on the right.
 *
 * @author felixfeyertag
 */
public class TP2DUserInterface extends TPUserInterface {

    private TextField filterTextField;
    
    public TP2DUserInterface(Stage primaryStage, Proteome<Protein2D> tppExp) {
        rightPane = new VBox();
        centerPane = new VBox();
        leftPane = new VBox();

        proteinFilteredList = new FilteredList<>(tppExp.getProteins());
        proteinSortedFilteredList = new SortedList<>(proteinFilteredList);

        this.tppExp = tppExp;
        this.table = new TableView<>();
        this.tpPane = createTPane(primaryStage);
    }

    private Pane createTPane(Stage primaryStage) {
        BorderPane root = new BorderPane();

        setLeftPane(primaryStage,tppExp);
        setCenterPane(primaryStage);
        setRightPane(primaryStage,tppExp);

        root.setLeft(leftPane);
        root.setCenter(centerPane);
        root.setRight(rightPane);

        return root;
    }

    public Pane getTPane() {
        return tpPane;
    }

    private void setCenterPane(Stage stage) {


        table.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        table.setPrefHeight(Integer.MAX_VALUE);

        //filter --start
        HBox filterHBox = new HBox();

        Label filterLabel = new Label("Filter ");
        filterLabel.setFont(Font.font("Cambria"));
        filterLabel.setPadding(new Insets(5,5,5,5));

        filterTextField = new TextField();

        proteinFilteredList.setPredicate((Protein2D p) -> true);

        filterTextField.textProperty().addListener((observable, oldValue, newValue) -> proteinFilteredList.setPredicate(protein -> {
            if (newValue == null || newValue.isEmpty()) {
                return true;
            }

            String lowerCaseFilter = ".*" + newValue.toLowerCase() + ".*";

            try {
                return protein.getDescription().toLowerCase().matches(lowerCaseFilter) ||
                        protein.getAccession().toLowerCase().matches(lowerCaseFilter) ||
                        protein.getGeneName().toLowerCase().matches(lowerCaseFilter) ||
                        protein.getOrganismName().toLowerCase().matches(lowerCaseFilter);
            }
            catch(PatternSyntaxException e) {
                return false;
            }
        }));

        proteinSortedFilteredList.comparatorProperty().bind(table.comparatorProperty());
        table.setItems(proteinSortedFilteredList);

        proteinSortedFilteredList.addListener((ListChangeListener<? super Protein2D>) evt -> {
            if(proteinSortedFilteredList.size()==1) {
                table.getSelectionModel().select(0);
            }
            else {
                table.getSelectionModel().clearSelection();
            }
        });

        Button filterClearButton = new Button("Clear");
        filterClearButton.setOnAction( event -> filterTextField.setText(""));

        filterHBox.getChildren().addAll(filterLabel,filterTextField,filterClearButton);
        //filter --end

        //Checkbox column --start

        TableColumn selectColumn = new TableColumn();
        selectColumn.setMinWidth(30);
        selectColumn.setMaxWidth(30);
        selectColumn.setCellValueFactory(new PropertyValueFactory<Protein2D,Boolean>("selected"));


        selectColumn.setCellFactory(column -> new CheckBoxTableCell<Protein2D,Boolean>());

        CheckBox headerCheckBox = new CheckBox();
        headerCheckBox.setUserData(selectColumn);

        headerCheckBox.setOnAction((ActionEvent event) -> {

            CheckBox cb = (CheckBox)event.getSource();
            TableColumn column = (TableColumn) cb.getUserData();

            if (cb.isSelected()) {
                int count = 0;
                for(Protein2D p : table.getSelectionModel().getSelectedItems()) {
                    if(!p.getSelected()) count++;
                    p.setSelected(true);
                }
                if(count==0) headerCheckBox.fire();

            } else {

                int count=0;
                for(Protein2D p : table.getSelectionModel().getSelectedItems()) {
                    if(p.getSelected()) count++;
                    p.setSelected(false);
                }
                if(count==0) {
                    headerCheckBox.fire();
                }

            }
            tppExp.updateProteinCount();

        });

        selectColumn.setGraphic(headerCheckBox);
        //Checkbox column --end


        //TPProtein columns --start
        TableColumn<Protein2D,StringProperty> accessionCol = new TableColumn<>("Accession");
        accessionCol.setCellValueFactory(new PropertyValueFactory<>("accession"));
        accessionCol.setPrefWidth(100);
        TableColumn<Protein2D,StringProperty> geneNameCol = new TableColumn<>("Gene");
        geneNameCol.setCellValueFactory(new PropertyValueFactory<>("geneName"));
        geneNameCol.setPrefWidth(100);
        TableColumn<Protein2D,StringProperty> organismCol = new TableColumn<>("Organism");
        organismCol.setCellValueFactory(new PropertyValueFactory<>("organismName"));
        organismCol.setPrefWidth(100);
        TableColumn<Protein2D,StringProperty> descriptionCol = new TableColumn<>("Description");
        descriptionCol.setCellValueFactory(new PropertyValueFactory<>("description"));
        descriptionCol.setPrefWidth(200);
        TableColumn<Protein2D,Double> scoreCol = new TableColumn<>("Combined Score");
        scoreCol.setCellValueFactory(new PropertyValueFactory<>("score"));
        TableColumn<Protein2D,Double> stabilisationScoreCol = new TableColumn<>("Stabilisation Score");
        stabilisationScoreCol.setCellValueFactory(new PropertyValueFactory<>("stabilityScore"));
        TableColumn<Protein2D,Double> destabilisationScoreCol = new TableColumn<>("Destabilisation Score");
        destabilisationScoreCol.setCellValueFactory(new PropertyValueFactory<>("destabilityScore"));
        TableColumn<Protein2D,StringProperty> effectCol = new TableColumn<>("Effect");
        effectCol.setCellValueFactory(new PropertyValueFactory<>("effect"));

        scoreCol.setCellFactory(tc -> new TableCell<Protein2D,Double>() {
            @Override
            protected void updateItem(Double value, boolean empty) {
                super.updateItem(value,empty);
                if(empty) {
                    setText(null);
                }
                else {
                    DecimalFormat format = new DecimalFormat("0.0000");
                    setText(format.format(value));
                }
            }
        });

        stabilisationScoreCol.setCellFactory(tc -> new TableCell<Protein2D,Double>() {
                    @Override
                    protected void updateItem(Double value, boolean empty) {
                        super.updateItem(value, empty);
                        if (empty) {
                            setText(null);
                        } else {
                            DecimalFormat format = new DecimalFormat("0.0000");
                            setText(format.format(value));
                        }
                    }
                });

        destabilisationScoreCol.setCellFactory(tc -> new TableCell<Protein2D,Double>() {
                    @Override
                    protected void updateItem(Double value, boolean empty) {
                        super.updateItem(value, empty);
                        if (empty) {
                            setText(null);
                        } else {
                            DecimalFormat format = new DecimalFormat("0.0000");
                            setText(format.format(value));
                        }
                    }
                });

        TableColumn<Protein2D,Double> meanFC = new TableColumn<>("Mean FC");
        meanFC.setCellValueFactory(new PropertyValueFactory<>("meanFCScore"));
        meanFC.setCellFactory(tc -> new TableCell<Protein2D,Double>() {
            @Override
            protected void updateItem(Double value, boolean empty) {
                super.updateItem(value,empty);
                if(empty) {
                    setText(null);
                }
                else {
                    DecimalFormat format = new DecimalFormat("0.0000");
                    setText(format.format(value));
                }
            }
        });

        TableColumn<Protein2D,Double> bootstrapCol = new TableColumn<>("P Value");
        bootstrapCol.setCellValueFactory(new PropertyValueFactory<>("pValue"));
        bootstrapCol.setCellFactory(tc -> new TableCell<Protein2D,Double>() {
            @Override
            protected void updateItem(Double value, boolean empty) {
                super.updateItem(value,empty);
                if(empty) {
                    setText(null);
                }
                else if (value == 0.0) {
                    value = tppExp.getMinPVal();
                    DecimalFormat format = new DecimalFormat("< 0.00E00");
                    setText(format.format(value));
                }
                else if (value < 0.0001) {
                    DecimalFormat format = new DecimalFormat("0.00E00");
                    setText(format.format(value));
                }
                else {
                    DecimalFormat format = new DecimalFormat("0.0000");
                    setText(format.format(value));
                }
            }
        });

        //TPProtein columns --end
        
        
        
        //Index column --start
        TableColumn<Protein2D, Number> indexColumn = new TableColumn<>();
        indexColumn.setSortable(false);
        indexColumn.setCellValueFactory(column-> new ReadOnlyObjectWrapper<>(table.getItems().indexOf(column.getValue())+1));
        //Index column --end
        
        table.getColumns().add(indexColumn);
        table.getColumns().add(selectColumn);
        table.getColumns().add(accessionCol);
        table.getColumns().add(geneNameCol);
        table.getColumns().add(organismCol);
        table.getColumns().add(descriptionCol);
        table.getColumns().add(scoreCol);
        table.getColumns().add(stabilisationScoreCol);
        table.getColumns().add(destabilisationScoreCol);
        table.getColumns().add(meanFC);
        table.getColumns().add(bootstrapCol);
        table.getColumns().add(effectCol);
        table.setEditable(true);



        
        final HBox abundanceBox = new HBox();
        
        AtomicReference<TableView<ObservableList<Double>>> abundanceTable = new AtomicReference<>(new TableView<>());
        IntegerProperty abundanceTableUpdateCounter = new SimpleIntegerProperty(0);

        abundanceTable.get().setPrefHeight(3 + 25 * (2 + tppExp.getTempLabels().size()));

        abundanceBox.getChildren().add(abundanceTable.get());
        
        Number minTemp;
        try {
            minTemp = Double.parseDouble(tppExp.getTempLabels().get(0));
        }
        catch (NumberFormatException e) {
            minTemp = Double.NaN;
        }
        //Double maxTemp;
        //try {
        //    maxTemp = Double.parseDouble(tppExp.getTempLabels().get(tppExp.getTempLabels().size()-1));
        //}
        //catch (NumberFormatException e) {
        //    maxTemp = Double.NaN;
        //}

        
        final NumberAxis xAxis = new NumberAxis();
        xAxis.setMinorTickCount(minTemp.intValue());
        xAxis.setForceZeroInRange(false);
        final NumberAxis yAxis = new NumberAxis();
        xAxis.setLabel("Temperature");
        yAxis.setLabel("Relative Abundance");
        

        //teststart

        final boolean test = false;

        if (test) {

            final VBox testSliderBox = new VBox();
            final Slider tSlide1 = new Slider();
            final Slider tSlide2 = new Slider();
            final Slider tSlide3 = new Slider();
            final Slider tSlide4 = new Slider();
            final Slider tSlide5 = new Slider();
            final Slider tSlide6 = new Slider();

            tSlide1.setMinWidth(100);
            tSlide1.setMin(-10);
            tSlide1.setMax(10);
            tSlide1.setValue(0);
            tSlide1.setBlockIncrement(0.1);

            tSlide2.setMinWidth(100);
            tSlide2.setMin(0);
            tSlide2.setMax(100);
            tSlide2.setValue(55);
            tSlide2.setBlockIncrement(0.1);

            tSlide3.setMinWidth(100);
            tSlide3.setMin(0);
            tSlide3.setMax(1);
            tSlide3.setValue(0.2);
            tSlide3.setBlockIncrement(0.1);

            tSlide4.setMinWidth(100);
            tSlide4.setMin(-100);
            tSlide4.setMax(100);
            tSlide4.setValue(1);
            tSlide4.setBlockIncrement(0.1);

            tSlide5.setMinWidth(100);
            tSlide5.setMin(-100);
            tSlide5.setMax(100);
            tSlide5.setValue(1);
            tSlide5.setBlockIncrement(0.1);

            tSlide6.setMinWidth(100);
            tSlide6.setMin(0.00001);
            tSlide6.setMax(100);
            tSlide6.setValue(1);
            tSlide6.setBlockIncrement(0.1);

            final Button pButton = new Button("Plot");

            final Label lSlide1 = new Label("k " + tSlide1.getValue());
            final Label lSlide2 = new Label("m " + tSlide2.getValue());
            final Label lSlide3 = new Label("b " + tSlide3.getValue());
            final Label lSlide4 = new Label("q " + tSlide4.getValue());
            final Label lSlide5 = new Label("a " + tSlide5.getValue());
            final Label lSlide6 = new Label("n " + tSlide6.getValue());

            testSliderBox.getChildren().addAll(lSlide1, tSlide1, lSlide2, tSlide2, lSlide3, tSlide3, lSlide4, tSlide4, lSlide5, tSlide5, lSlide6, tSlide6, pButton);

            tSlide1.valueProperty().addListener(listener -> lSlide1.setText("k " + tSlide1.getValue()));
            tSlide2.valueProperty().addListener(listener -> lSlide2.setText("m " + tSlide2.getValue()));
            tSlide3.valueProperty().addListener(listener -> lSlide3.setText("b " + tSlide3.getValue()));
            tSlide4.valueProperty().addListener(listener -> lSlide4.setText("q " + tSlide4.getValue()));
            tSlide5.valueProperty().addListener(listener -> lSlide5.setText("a " + tSlide5.getValue()));
            tSlide6.valueProperty().addListener(listener -> lSlide6.setText("n " + tSlide6.getValue()));

            abundanceBox.getChildren().add(testSliderBox);
        }

        //testend


        final KeyCodeCombination keyCodeCopy = new KeyCodeCombination(KeyCode.C, KeyCombination.CONTROL_ANY);
        table.setOnKeyPressed(event -> {
            if (keyCodeCopy.match(event)) {
                //copySelectionToClipboard(table,false);
            }
            if(event.getCode().equals(KeyCode.SPACE)) {
               headerCheckBox.fire();
               tppExp.updateProteinCount();
            }
        });
        table.setOnMouseClicked(event -> tppExp.updateProteinCount());
        



        table.getSelectionModel().selectedItemProperty().addListener((ObservableValue<? extends Protein2D> observable, Protein2D oldValue, Protein2D newValue) -> {

            abundanceTableUpdateCounter.setValue(abundanceTableUpdateCounter.get()+1);

            // this fixes a JavaFX bug causing a NPE every time the abundance table gets updated 100 times
            if(abundanceTableUpdateCounter.get()%100==0) {
                abundanceBox.getChildren().clear();

                abundanceTable.set(new TableView<>());
                abundanceTable.get().setPrefHeight(3 + 25 * (2 + tppExp.getTempLabels().size()));

                abundanceBox.getChildren().add(abundanceTable.get());
            }
            abundanceTable.get().getColumns().clear();

            if(table.getSelectionModel().getSelectedIndices().size()==1) {
                try {

                    //Melting curves -- start

                    /*pButton.setOnAction(p -> {
                        lineChart.getData().clear();
                        XYChart.Series<Number,Number> testSeries = new XYChart.Series<>();
                        Logistic testl = new Logistic(tSlide1.getValue(), tSlide2.getValue(), tSlide3.getValue(), tSlide4.getValue(), tSlide5.getValue(), tSlide6.getValue());
                        for(double i = -10; i<=80; i+=0.05) {
                            testSeries.getData().add(new XYChart.Data<>(i,testl.value(i)));
                        }

                        lineChart.getData().add(testSeries);
                    });*/
                    //testSeries.getNode().setStyle("-fx-stroke: blue;");


                    /*IntegerProperty j = new SimpleIntegerProperty(0);
                    ObservableList<ObservableList<Double>> tempRatios = newValue.getAbundancesTempRatioOL();
                    tempRatios.stream().map((ObservableList<Double> tr) -> {
                        XYChart.Series series = new XYChart.Series<>();
                        series.setName("Concentration: " + tppExp.getConcLabels().get(j.getValue()));
                        j.setValue(1+j.getValue());
                        for (int i=0;i<tr.size();i++) {
                            Number tempValue;
                            try {
                                tempValue = Double.parseDouble(tppExp.getTempLabels().get(i));
                                System.out.println(tempValue + "\t" + tr.get(i));
                                if(!tr.get(i).equals(Double.NaN)) {
                                    series.getData().add(new XYChart.Data<>(tempValue,tr.get(i)));
                                }
                            }
                            catch (NumberFormatException e) {
                                tempValue = 0.0;
                            }
                        }
                    }).forEachOrdered((series) -> {
                        lineChart.getData().add(series);
                    });*/
                    /*
                    lineChart.getData().clear();


                    IntegerProperty j = new SimpleIntegerProperty(0);
                    IntegerProperty j2 = new SimpleIntegerProperty(0);
                    ObservableList<ObservableList<Double>> tempRatios = newValue.getAbundancesTempRatioOL();
                    tempRatios.stream().map((ObservableList<Double> tr) -> {
                        XYChart.Series series = new XYChart.Series();
                        //series.getNode().loo
                        series.setName("Concentration: " + tppExp.getConcLabels().get(j.get()));
                        j.set(j.get()+1);
                        for (int i=0;i<tr.size();i++) {
                            Number tempValue;
                            try {
                                tempValue = Double.parseDouble(tppExp.getTempLabels().get(i));
                                if(!tr.get(i).equals(Double.NaN)) {
                                    series.getData().add(new XYChart.Data(tempValue,tr.get(i)));
                                }
                            }
                            catch (NumberFormatException e) {
                                tempValue = 0.0;
                            }
                        }
                        return series;
                    }).forEachOrdered((series) -> {
                        lineChart.getData().add(series);
                    });



                    HashMap<String,double[]> curveFitParams = newValue.getCurveFitParams();
                    curveFitParams.keySet().stream().map( (String k) -> {
                        if(curveFitParams.get(k)==null) {
                            System.out.println(k);
                            return null;
                        }
                        XYChart.Series<Number,Number> series = new XYChart.Series<>();
                        series.setName(k);
                        double[] p = curveFitParams.get(k);
                        System.out.println("Params: ");
                        for (double p_ : p) System.out.println(p_);
                        //Logistic l = new Logistic(p[0],p[1],p[2],p[3],p[4],p[5]);
                        MeltCurve l = new MeltCurve(p[0],p[1],p[2]);
                        double lower = Double.parseDouble(tppExp.getTempLabels().get(0));
                        double upper = Double.parseDouble(tppExp.getTempLabels().get(tppExp.getTempLabels().size()-1));
                        for(double i = lower; i <= upper; i+=0.1) {
                            series.getData().add(new XYChart.Data<>(i,l.value(i)));
                        }
                        return series;
                    }).forEachOrdered(series -> {
                        if(series instanceof XYChart.Series) {
                            series.getData().stream().map((dat) -> {
                                StackPane sp = (StackPane) dat.getNode();
                                return dat;
                            }).forEachOrdered((dat) -> {
                                Rectangle rect = new Rectangle(0,0);
                                rect.setVisible(false);
                                dat.setNode(rect);
                            });

                            lineChart.getData().add(series);
                        }
                    });


                    ////////

                    lineChart.lookup(".default-color0.chart-series-line").setStyle("-fx-stroke: transparent;");
                    lineChart.lookup(".default-color1.chart-series-line").setStyle("-fx-stroke: transparent;");
                    lineChart.lookup(".default-color2.chart-series-line").setStyle("-fx-stroke: transparent;");
                    lineChart.lookup(".default-color3.chart-series-line").setStyle("-fx-stroke: transparent;");


                    try {
                        System.out.println("csl3: " + lineChart.lookup(".default-color0.chart-series-line").getStyle());

                        System.out.println(lineChart.getStyle());
                    } catch (NullPointerException e) {
                        System.out.println("csl3: NPE!");
                    }

                    */
                    //Melting curves --end




                    //Abundance ratio table --start
                    abundanceTable.get().getColumns().clear();
                    abundanceTable.get().setItems(newValue.getAbundancesConcRatioNormalisedOL());
                    for (int i=0;i<tppExp.getConcLabels().size();i++) {
                        final int curCol = i;
                        final TableColumn<ObservableList<Double>, Double> column;
                        column = new TableColumn<>(tppExp.getConcLabels().get(i));
                        column.setCellValueFactory((TableColumn.CellDataFeatures<ObservableList<Double>, Double> p) ->
                                new ReadOnlyObjectWrapper<>(p.getValue().get(curCol)));

                        column.setCellFactory(c -> new TableCell<ObservableList<Double>, Double>() {
                            @Override
                            protected void updateItem(Double item, boolean empty) {
                                super.updateItem(item, empty);

                                if (!empty && Double.isFinite(item)) {
                                    //Double logValue = Math.log10(item);
                                    DecimalFormat format = new DecimalFormat("0.0000");
                                    //setText(format.format(logValue.doubleValue()));
                                    setText(format.format(item));
                                } else {
                                    setText("");
                                }
                                if(!empty && Double.isFinite(item)) {
                                    int r = (int)(tppExp.getColour(item).getRed() * 255.00);
                                    int g = (int)(tppExp.getColour(item).getGreen() * 255.00);
                                    int b = (int)(tppExp.getColour(item).getBlue() * 255.00);
                                    String col = "rgb(" + r + "," + g + "," + b + ")";
                                    this.setStyle("-fx-background-color: " + col);
                                    int r2 = (int)(255.00 - tppExp.getColour(item).getRed() * 255.00);
                                    int g2 = (int)(255.00 - tppExp.getColour(item).getGreen() * 255.00);
                                    int b2 = (int)(255.00 - tppExp.getColour(item).getBlue() * 255.00);
                                    String col2 = "rgb(" + r2 + "," + g2 + "," + b2 + ")";
                                    this.setStyle("-fx-text-fill: " + col2 + "; -fx-background-color: " + col);
                                }
                                else {
                                    setText("");
                                }
                            }
                        });
                        abundanceTable.get().getColumns().add(column);
                    }
                }
                catch (NullPointerException e) {
                    abundanceBox.getChildren().clear();
                    abundanceTable.set(new TableView<>());
                    abundanceTable.get().setPrefHeight(3 + 25 * (2 + tppExp.getTempLabels().size()));
                    abundanceBox.getChildren().add(abundanceTable.get());
                }
            }
        });
        //Abundance ratio table --end


        BorderPane borderPane = new BorderPane();

        borderPane.setTop(filterHBox);
        borderPane.setCenter(table);
        borderPane.setBottom(abundanceBox);

        centerPane.getChildren().add(borderPane);

    }



    private void setLeftPane(Stage stage,Proteome tppExp) {

        Label fileNameLabel1 = new Label("File Name");
        fileNameLabel1.setFont(Font.font("Cambria",FontWeight.BOLD,18));
        fileNameLabel1.setText("File Name");
        TextField fileNameLabel2 = new TextField(tppExp.getFileName().get());
        Label proteinCountLabel1 = new Label("Protein Count");
        proteinCountLabel1.setFont(Font.font("Cambria",FontWeight.BOLD,18));
        TextField proteinCountLabel2 = new TextField(tppExp.getProteinSelected() + " / " + tppExp.getProteinCount());
        Label tmtLabel1 = new Label("Temperatures");
        tmtLabel1.setFont(Font.font("Cambria",FontWeight.BOLD,18));
        ListView<String> tmtList1 = new ListView<>(tppExp.getTempLabels());
        Label replicateLabel1 = new Label("Concentrations");
        replicateLabel1.setFont(Font.font("Cambria",FontWeight.BOLD,18));
        ListView<String> replicateList1 = new ListView<>(tppExp.getConcLabels());

        tmtList1.setPrefHeight(Integer.MAX_VALUE);
        replicateList1.setPrefHeight(Integer.MAX_VALUE);

        leftPane.getChildren().addAll(fileNameLabel1,fileNameLabel2,proteinCountLabel1,proteinCountLabel2,tmtLabel1,tmtList1,replicateLabel1,replicateList1);

        
        tppExp.getFileName().addListener((ChangeListener) (observable, oldValue, newValue) -> fileNameLabel2.setText(tppExp.getFileName().get()));
        tppExp.getProteinCountProperty().addListener((ChangeListener) (observable, oldValue, newValue) -> proteinCountLabel2.setText(tppExp.getProteinSelected() + " / " + tppExp.getProteinCount()));
        tppExp.getProteinSelectedProperty().addListener((ChangeListener) (observable, oldValue, newValue) -> proteinCountLabel2.setText(tppExp.getProteinSelected() + " / " + tppExp.getProteinCount()));
    }
    
    private void setRightPane(Stage stage,Proteome<Protein2D> tppExp) {

        TPFCPane fcPane = new TPFCPane(tppExp);
        TPStringNetworkPane stringNetworkPane = new TPStringNetworkPane(tppExp);
        TPStringFunctionalEnrichmentPane stringFunctionalEnrichmentPane = new TPStringFunctionalEnrichmentPane(tppExp);
        TPUniProtPane uniProtPane = new TPUniProtPane(table, tppExp);
        TPCorumAnalysisPane corumAnalysisPane = new TPCorumAnalysisPane(tppExp,filterTextField);
        TPMeanDifferencePane meanDifferencePane = new TPMeanDifferencePane(tppExp,table,fcPane);
        /* TPClusterPane clusterPane = new TPClusterPane(tppExp); */
        /* TPBootstrapPane bsPane = new TPBootstrapPane(tppExp,fcPane); */
        TPExportPane2D exportPane = new TPExportPane2D(tppExp,stage,table);
        
        rightPane.getChildren().add(fcPane);
        //rightPane.getChildren().add(bsPane);
        rightPane.getChildren().add(new Separator());
        rightPane.getChildren().add(new Separator());
        rightPane.getChildren().add(stringNetworkPane);
        rightPane.getChildren().add(stringFunctionalEnrichmentPane);
        rightPane.getChildren().add(uniProtPane);
        rightPane.getChildren().add(corumAnalysisPane);
        rightPane.getChildren().add(meanDifferencePane);
        //rightPane.getChildren().add(clusterPane);
        rightPane.getChildren().add(new Separator());
        rightPane.getChildren().add(new Separator());
        rightPane.getChildren().add(exportPane);
        
    }
    
    class NumberTableCellFactory implements Callback<TableColumn, TableCell> {

        NumberTableCellFactory() {
            int startNumber = 1;
        }

        class NumberTableCell<S, T> extends TableCell<S, T> {


            NumberTableCell() {
            }



            @Override
            public void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);

                setText(empty ? "" : Integer.toString(getIndex() + 1));
            }

        }

        @Override
        public TableCell call(TableColumn param) {
            return new NumberTableCell<>();
        }

        public TableColumn createNumberColumn() {
            TableColumn column = new TableColumn<>();
            //column.setSortable(false);
            column.setEditable(false);
            column.setCellFactory(new NumberTableCellFactory());
            return column;
        }

    }
    
    

    final private Pane tpPane;
    
    final private Pane centerPane;
    final private Pane leftPane;
    final private Pane rightPane;
    
    final private FilteredList<Protein2D> proteinFilteredList;
    final private SortedList<Protein2D> proteinSortedFilteredList;
    
    final private TableView<Protein2D> table;
    

    final private Proteome<Protein2D> tppExp;
}

