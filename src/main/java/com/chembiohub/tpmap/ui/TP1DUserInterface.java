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

import java.text.DecimalFormat;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.PatternSyntaxException;

import com.chembiohub.tpmap.analysis.analysispane.*;
import com.chembiohub.tpmap.dstruct.Protein1D;
import javafx.beans.property.*;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
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
import org.jfree.chart.JFreeChart;
import org.jfree.chart.fx.ChartViewer;


/**
 * TP1DUserInterface
 *
 * The TP1DUserInterface class creates the default tab that opens when a Protein1D Proteome object is loaded into
 * TPMAP
 *
 * It constructs a BorderPane with properties on the left, protein table, fold change table and melt curve chart
 * in the center and downstream analysis options on the right.
 *
 * @author felixfeyertag
 */
public class TP1DUserInterface extends TPUserInterface {

    private TextField filterTextField;

    /**
     * @param primaryStage parent stage
     * @param tppExp thermal profiling experiment to be loaded
     */
    public TP1DUserInterface(Stage primaryStage, Proteome<Protein1D> tppExp) {
        rightPane = new VBox();
        centerPane = new VBox();
        leftPane = new VBox();

        proteinFilteredList = new FilteredList<>(tppExp.getProteins());
        proteinSortedFilteredList = new SortedList<>(proteinFilteredList);

        this.tppExp = tppExp;
        this.table = new TableView<>();
        this.tpPane = createTPPane(primaryStage);
    }

    private Pane createTPPane(Stage primaryStage) {

        BorderPane root = new BorderPane();

        setCenterPane(primaryStage);
        setRightPane(primaryStage,tppExp);
        setLeftPane(primaryStage,tppExp);

        root.setCenter(centerPane);
        root.setRight(rightPane);
        root.setLeft(leftPane);

        return root;
    }

    public Pane getTPane() {
        return tpPane;
    }

    private void setCenterPane(Stage stage) {
        table.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        //filter --start
        HBox filterHBox = new HBox();

        Label filterLabel = new Label("Filter ");
        filterLabel.setFont(Font.font("Cambria"));
        filterLabel.setPadding(new Insets(5, 5, 5, 5));

        filterTextField = new TextField();

        proteinFilteredList.setPredicate(p -> true);

        filterTextField.textProperty().addListener((observable, oldVal, newVal) -> proteinFilteredList.setPredicate(protein -> {
            if(newVal == null || newVal.isEmpty()) {
                return true;
            }

            String lcFilter = ".*" + newVal.toLowerCase() + ".*";

            try {
                return protein.getDescription() .toLowerCase().matches(lcFilter) ||
                       protein.getAccession()   .toLowerCase().matches(lcFilter) ||
                       protein.getGeneName()    .toLowerCase().matches(lcFilter) ||
                       protein.getOrganismName().toLowerCase().matches(lcFilter);
            } catch(PatternSyntaxException e) {
                return false;
            }

        }));

        proteinSortedFilteredList.comparatorProperty().bind(table.comparatorProperty());

        table.setItems(proteinSortedFilteredList);

        proteinSortedFilteredList.addListener((ListChangeListener<? super Protein1D>) evt -> {
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

        //checkbox column --start
        TableColumn selectColumn = new TableColumn();
        selectColumn.setMinWidth(30);
        selectColumn.setMaxWidth(30);
        selectColumn.setCellValueFactory(new PropertyValueFactory<Protein1D,Boolean>("selected"));

        selectColumn.setCellFactory(column -> new CheckBoxTableCell<Protein1D,Boolean>());

        CheckBox headerCheckBox = new CheckBox();
        headerCheckBox.setUserData(selectColumn);
        headerCheckBox.setOnAction((ActionEvent e) -> {
            CheckBox cb = (CheckBox)e.getSource();
            TableColumn col = (TableColumn) cb.getUserData();
            if(cb.isSelected()){
                int count = 0;
                for (Protein1D p : table.getSelectionModel().getSelectedItems()) {
                    if(!p.getSelected()) count++;
                    p.setSelected(true);
                }
                if(count==0) headerCheckBox.fire();
            } else {
                int count = 0;
                for(Protein1D p : table.getSelectionModel().getSelectedItems()) {
                    if(p.getSelected()) count++;
                    p.setSelected(false);
                }
                if(count==0) headerCheckBox.fire();
            }
        });
        selectColumn.setGraphic(headerCheckBox);

        TableColumn<Protein1D,StringProperty> accessionCol = new TableColumn<>("Accession");
        accessionCol.setCellValueFactory(new PropertyValueFactory("accession"));
        TableColumn<Protein1D,StringProperty> geneCol = new TableColumn<>("Gene");
        geneCol.setCellValueFactory(new PropertyValueFactory("geneName"));
        TableColumn<Protein1D,StringProperty> organismCol = new TableColumn<>("Organism");
        organismCol.setCellValueFactory(new PropertyValueFactory("organismName"));
        TableColumn<Protein1D,StringProperty> descriptionCol = new TableColumn<>("Description");
        descriptionCol.setCellValueFactory(new PropertyValueFactory("description"));
        descriptionCol.setPrefWidth(400);

        TableColumn<Protein1D,Double> tmv1Col = new TableColumn<>("TM V1");
        tmv1Col.setCellValueFactory(new PropertyValueFactory("tmv1"));
        tmv1Col.setCellFactory(tc -> new TableCell<Protein1D,Double>() {
                    @Override
                    protected void updateItem(Double value, boolean empty) {
                        super.updateItem(value, empty);
                        if (empty || !Double.isFinite(value)) {
                            setText("");
                        } else {
                            DecimalFormat format = new DecimalFormat("0.0000");
                            setText(format.format(value));
                        }
                    }
                });

        TableColumn<Protein1D,Double> tmv2Col = new TableColumn<>("TM V2");
        tmv2Col.setCellValueFactory(new PropertyValueFactory("tmv2"));
        tmv2Col.setCellFactory(tc -> new TableCell<Protein1D,Double>() {
            @Override
            protected void updateItem(Double value, boolean empty) {
                super.updateItem(value, empty);
                if (empty || !Double.isFinite(value)) {
                    setText("");
                } else {
                    DecimalFormat format = new DecimalFormat("0.0000");
                    setText(format.format(value));
                }
            }
        });

        TableColumn<Protein1D,Double> tmt1Col = new TableColumn<>("TM T1");
        tmt1Col.setCellValueFactory(new PropertyValueFactory("tmt1"));
        tmt1Col.setCellFactory(tc -> new TableCell<Protein1D,Double>() {
            @Override
            protected void updateItem(Double value, boolean empty) {
                super.updateItem(value, empty);
                if (empty || !Double.isFinite(value)) {
                    setText("");
                } else {
                    DecimalFormat format = new DecimalFormat("0.0000");
                    setText(format.format(value));
                }
            }
        });

        TableColumn<Protein1D,Double> tmt2Col = new TableColumn<>("TM T2");
        tmt2Col.setCellValueFactory(new PropertyValueFactory("tmt2"));
        tmt2Col.setCellFactory(tc -> new TableCell<Protein1D,Double>() {
            @Override
            protected void updateItem(Double value, boolean empty) {
                super.updateItem(value, empty);
                if (empty || !Double.isFinite(value)) {
                    setText("");
                } else {
                    DecimalFormat format = new DecimalFormat("0.0000");
                    setText(format.format(value));
                }
            }
        });

        TableColumn<Protein1D,Double> tmvt1Col = new TableColumn<>("TM shift (V1T1)" );
        tmvt1Col.setCellValueFactory(new PropertyValueFactory("tmVT1"));
        tmvt1Col.setCellFactory(tc -> new TableCell<Protein1D,Double>() {
            @Override
            protected void updateItem(Double value, boolean empty) {
                super.updateItem(value, empty);
                if (empty || !Double.isFinite(value)) {
                    setText("");
                } else {
                    DecimalFormat format = new DecimalFormat("0.0000");
                    setText(format.format(value));
                }
            }
        });

        TableColumn<Protein1D,Double> tmvt2Col = new TableColumn<>("TM shift (V2T2)");
        tmvt2Col.setCellValueFactory(new PropertyValueFactory("tmVT2"));
        tmvt2Col.setCellFactory(tc -> new TableCell<Protein1D,Double>() {
            @Override
            protected void updateItem(Double value, boolean empty) {
                super.updateItem(value, empty);
                if (empty || !Double.isFinite(value)) {
                    setText("");
                } else {
                    DecimalFormat format = new DecimalFormat("0.0000");
                    setText(format.format(value));
                }
            }
        });

        TableColumn<Protein1D,Double> tmvvCol = new TableColumn<>("TM shift (V1V2)");
        tmvvCol.setCellValueFactory(new PropertyValueFactory("tmVV"));
        tmvvCol.setCellFactory(tc -> new TableCell<Protein1D,Double>() {
            @Override
            protected void updateItem(Double value, boolean empty) {
                super.updateItem(value, empty);
                if (empty || !Double.isFinite(value)) {
                    setText("");
                } else {
                    DecimalFormat format = new DecimalFormat("0.0000");
                    setText(format.format(value));
                }
            }
        });

        TableColumn<Protein1D,Double> meanTMCol = new TableColumn<>("Mean TM shift");
        meanTMCol.setCellValueFactory(new PropertyValueFactory("meanTM"));
        meanTMCol.setCellFactory(tc -> new TableCell<Protein1D,Double>() {
            @Override
            protected void updateItem(Double value, boolean empty) {
                super.updateItem(value, empty);
                if (empty || !Double.isFinite(value)) {
                    setText("");
                } else {
                    DecimalFormat format = new DecimalFormat("0.0000");
                    setText(format.format(value));
                }
            }
        });

        TableColumn<Protein1D,Double> rmsev1Col = new TableColumn<>("RMSE V1");
        rmsev1Col.setCellValueFactory(new PropertyValueFactory("rmsev1"));
        rmsev1Col.setCellFactory(tc -> new TableCell<Protein1D,Double>() {
            @Override
            protected void updateItem(Double value, boolean empty) {
                super.updateItem(value,empty);
                if(empty || !Double.isFinite(value)) {
                    setText("");
                } else {
                    DecimalFormat format = new DecimalFormat("0.0000");
                    setText(format.format(value));
                }
            }
        });

        TableColumn<Protein1D,Double> rmsev2Col = new TableColumn<>("RMSE V2");
        rmsev2Col.setCellValueFactory(new PropertyValueFactory("rmsev2"));
        rmsev2Col.setCellFactory(tc -> new TableCell<Protein1D,Double>() {
            @Override
            protected void updateItem(Double value, boolean empty) {
                super.updateItem(value,empty);
                if(empty||!Double.isFinite(value)) {
                    setText("");
                } else {
                    DecimalFormat format = new DecimalFormat("0.0000");
                    setText(format.format(value));
                }
            }
        });

        TableColumn<Protein1D,Double> rmset1Col = new TableColumn<>("RMSE T1");
        rmset1Col.setCellValueFactory(new PropertyValueFactory("rmset1"));
        rmset1Col.setCellFactory(tc -> new TableCell<Protein1D,Double>() {
            @Override
            protected void updateItem(Double value, boolean empty) {
                super.updateItem(value,empty);
                if(empty||!Double.isFinite(value)) {
                    setText("");
                } else {
                    DecimalFormat format = new DecimalFormat("0.0000");
                    setText(format.format(value));
                }
            }
        });

        TableColumn<Protein1D,Double> rmset2Col = new TableColumn<>("RMSE T2");
        rmset2Col.setCellValueFactory(new PropertyValueFactory("rmset2"));
        rmset2Col.setCellFactory(tc -> new TableCell<Protein1D,Double>() {
            @Override
            protected void updateItem(Double value, boolean empty) {
                super.updateItem(value,empty);
                if(empty||!Double.isFinite(value)) {
                    setText("");
                } else {
                    DecimalFormat format = new DecimalFormat("0.0000");
                    setText(format.format(value));
                }
            }
        });

        /*TableColumn<Protein1D,Double> rmsev1meanCol = new TableColumn<>("RMSE V1 mean");
        rmsev1meanCol.setCellValueFactory(new PropertyValueFactory("rmsevM1"));
        rmsev1meanCol.setCellFactory(tc -> new TableCell<Protein1D,Double>() {
            @Override
            protected void updateItem(Double value, boolean empty) {
                super.updateItem(value,empty);
                if(empty||!Double.isFinite(value)) {
                    setText("");
                } else {
                    DecimalFormat format = new DecimalFormat("0.0000");
                    setText(format.format(value));
                }
            }
        });

        TableColumn<Protein1D,Double> rmsev2meanCol = new TableColumn<>("RMSE V2 mean");
        rmsev2meanCol.setCellValueFactory(new PropertyValueFactory("rmsevM2"));
        rmsev2meanCol.setCellFactory(tc -> new TableCell<Protein1D,Double>() {
            @Override
            protected void updateItem(Double value, boolean empty) {
                super.updateItem(value,empty);
                if(empty||!Double.isFinite(value)) {
                    setText("");
                } else {
                    DecimalFormat format = new DecimalFormat("0.0000");
                    setText(format.format(value));
                }
            }
        });

        TableColumn<Protein1D,Double> rmset1meanCol = new TableColumn<>("RMSE T1 mean");
        rmset1meanCol.setCellValueFactory(new PropertyValueFactory("rmsetM1"));
        rmset1meanCol.setCellFactory(tc -> new TableCell<Protein1D,Double>() {
            @Override
            protected void updateItem(Double value, boolean empty) {
                super.updateItem(value,empty);
                if(empty||!Double.isFinite(value)) {
                    setText("");
                } else {
                    DecimalFormat format = new DecimalFormat("0.0000");
                    setText(format.format(value));
                }
            }
        });

        TableColumn<Protein1D,Double> rmset2meanCol = new TableColumn<>("RMSE T2 mean");
        rmset2meanCol.setCellValueFactory(new PropertyValueFactory("rmsetM2"));
        rmset2meanCol.setCellFactory(tc -> new TableCell<Protein1D,Double>() {
            @Override
            protected void updateItem(Double value, boolean empty) {
                super.updateItem(value,empty);
                if(empty||!Double.isFinite(value)) {
                    setText("");
                } else {
                    DecimalFormat format = new DecimalFormat("0.0000");
                    setText(format.format(value));
                }
            }
        });*/

        TableColumn<Protein1D,Double> rmseMeanCol = new TableColumn<>("Mean RMSE");
        rmseMeanCol.setCellValueFactory(new PropertyValueFactory("rmsemean"));
        rmseMeanCol.setCellFactory(tc -> new TableCell<Protein1D,Double>() {
            @Override
            protected void updateItem(Double value, boolean empty) {
                super.updateItem(value,empty);
                if(empty||!Double.isFinite(value)) {
                    setText("");
                } else {
                    DecimalFormat format = new DecimalFormat("0.0000");
                    setText(format.format(value));
                }
            }
        });

        TableColumn<Protein1D,Double> scoreCol = new TableColumn<>("Score");
        scoreCol.setCellValueFactory(new PropertyValueFactory("score"));
        scoreCol.setCellFactory(tc -> new TableCell<Protein1D,Double>() {
            @Override
            protected void updateItem(Double value, boolean empty) {
                super.updateItem(value, empty);
                if (empty || !Double.isFinite(value)) {
                    setText("");
                } else {
                    DecimalFormat format = new DecimalFormat("0.0000");
                    setText(format.format(value));
                }
            }
        });

        TableColumn<Protein1D,BooleanProperty> curveShiftSameDirectionCol = new TableColumn<>("Shift same direction");
        curveShiftSameDirectionCol.setCellValueFactory(new PropertyValueFactory("curveShiftSameDirection"));
        TableColumn<Protein1D,BooleanProperty> detlaVTgtDeltaVVCol = new TableColumn<>("Delta VT > Delta VV");
        detlaVTgtDeltaVVCol.setCellValueFactory(new PropertyValueFactory("deltaVTgtDeltaVV"));



        TableColumn<Protein1D, Number> indexColumn = new TableColumn<>();
        indexColumn.setSortable(false);
        indexColumn.setCellValueFactory(column -> new ReadOnlyObjectWrapper<>(table.getItems().indexOf(column.getValue())+1));

        table.getColumns().addAll(indexColumn,selectColumn,accessionCol,organismCol,descriptionCol, tmv1Col, tmv2Col, tmt1Col, tmt2Col, tmvt1Col, tmvt2Col, tmvvCol, meanTMCol, rmsev1Col, rmsev2Col, rmset1Col, rmset2Col, rmseMeanCol, curveShiftSameDirectionCol, detlaVTgtDeltaVVCol, scoreCol);
        table.setEditable(false);

        final HBox abundanceBox = new HBox();

        AtomicReference<TableView<ObservableList<Double>>> abundanceTable = new AtomicReference<>(new TableView<>());
        IntegerProperty abundanceTableUpdateCounter = new SimpleIntegerProperty(0);

        abundanceBox.getChildren().add(abundanceTable.get());

        final HBox lineChartBox = new HBox();

        abundanceBox.getChildren().add(lineChartBox);

        final KeyCodeCombination keyCodeCopy = new KeyCodeCombination(KeyCode.C, KeyCombination.CONTROL_ANY);

        table.setOnKeyPressed(event -> {
            if(keyCodeCopy.match(event)) {

            }
            if(event.getCode().equals(KeyCode.SPACE)) {
                headerCheckBox.fire();
                tppExp.updateProteinCount();
            }
        });

        table.setOnMouseClicked(event -> tppExp.updateProteinCount());

        table.getSelectionModel().selectedItemProperty().addListener((ObservableValue<? extends Protein1D> observable, Protein1D unselectedProtein, Protein1D selectedProtein) -> {

            // this fixes a JavaFX bug causing a NPE every time the abundance table gets updated 100 times
            abundanceTableUpdateCounter.setValue(abundanceTableUpdateCounter.get()+1);
            if(abundanceTableUpdateCounter.get()%100==0) {
                abundanceBox.getChildren().clear();

                abundanceTable.set(new TableView<>());
                //abundanceTable.get().setPrefHeight(3 + 25 * (2 + tppExp.getTempLabels().size()));

                abundanceBox.getChildren().add(abundanceTable.get());
                abundanceBox.getChildren().add(lineChartBox);
            }

            abundanceTable.get().getColumns().clear();

            lineChartBox.getChildren().clear();

            if(table.getSelectionModel().getSelectedIndices().size()==1) {
                try {

                    // Temp ratio table

                    // populate abundance table with temperature ratios

                    abundanceTable.get().getColumns().clear();
                    abundanceTable.get().setItems(selectedProtein.getAbundancesTempRatioOL());

                    for(int i=0;i<tppExp.getTempLabels().size();i++) {
                        final int curCol = i;
                        final TableColumn<ObservableList<Double>, Double> column;
                        column = new TableColumn<>(tppExp.getTempLabels().get(i));
                        column.setCellValueFactory((TableColumn.CellDataFeatures<ObservableList<Double>, Double> p) ->
                                new ReadOnlyObjectWrapper<>(p.getValue().get(curCol)));
                        column.setCellFactory(this::call);
                        abundanceTable.get().getColumns().add(column);
                    }

                    // Melting curves

                    JFreeChart chart = selectedProtein.createChart(tppExp.getTempLabels(), tppExp.getConcLabels());

                    ChartViewer viewer = new ChartViewer(chart);
                    viewer.setPrefHeight(400);
                    viewer.setPrefWidth(600);

                    lineChartBox.getChildren().add(viewer);


                } catch (NullPointerException e) {
                    abundanceTable.get().getColumns().clear();
                }
            }
        });

        BorderPane centerBorderPane = new BorderPane();

        table.setPrefHeight(Integer.MAX_VALUE);

        centerBorderPane.setTop(filterHBox);
        centerBorderPane.setCenter(table);
        centerBorderPane.setBottom(abundanceBox);

        centerPane.getChildren().add(centerBorderPane);
    }

    private void setLeftPane(Stage stage, Proteome tppExp) {

        Label fileNameLabel1 = new Label("File Name");
        fileNameLabel1.setFont(Font.font("Cambria",FontWeight.BOLD,18));
        fileNameLabel1.setText("File Name");
        TextField fileNameLabel2 = new TextField(tppExp.getFileName().get());
        Label proteinCountLabel1 = new Label("Protein Count");
        proteinCountLabel1.setFont(Font.font("Cambria",FontWeight.BOLD,18));
        TextField proteinCountLabel2 = new TextField(tppExp.getProteinSelected() + " / " + tppExp.getProteinCount());
        Label tmtLabel1 = new Label("Temperatures");
        tmtLabel1.setFont(Font.font("Cambria",FontWeight.BOLD,18));
        ListView<String> tmtList = new ListView<>(tppExp.getTempLabels());
        tmtList.setPrefHeight(Integer.MAX_VALUE);
        Label replicateLabel1 = new Label("Concentrations");
        replicateLabel1.setFont(Font.font("Cambria",FontWeight.BOLD,18));
        ListView<String> replicateList1 = new ListView<>(tppExp.getConcLabels());
        replicateList1.setPrefHeight(Integer.MAX_VALUE);

        leftPane.getChildren().addAll(fileNameLabel1,fileNameLabel2,proteinCountLabel1,proteinCountLabel2,tmtLabel1,tmtList,replicateLabel1,replicateList1);

        tppExp.getFileName().addListener((ChangeListener) (observable, oldVal, newVal) -> fileNameLabel2.setText(tppExp.getFileName().get()));
        tppExp.getProteinCountProperty().addListener((ChangeListener) (observable, oldVal, newVal) -> proteinCountLabel2.setText(tppExp.getProteinSelected() + " / " + tppExp.getProteinCount()));
        tppExp.getProteinSelectedProperty().addListener((ChangeListener) (observable, oldValue, newValue) -> proteinCountLabel2.setText(tppExp.getProteinSelected() + " / " + tppExp.getProteinCount()));
    }

    private void setRightPane(Stage stage, Proteome tppExp) {

        TPTMPane tmPane = new TPTMPane(tppExp);
        //TPFCPane fcPane = new TPFCPane(tppExp);
        TPStringNetworkPane stringNetworkPane = new TPStringNetworkPane(tppExp);
        TPStringFunctionalEnrichmentPane stringFunctionalEnrichmentPane = new TPStringFunctionalEnrichmentPane(tppExp);
        //TPUniProtPane uniProtPane = new TPUniProtPane(table);
        TPCorumAnalysisPane corumAnalysisPane = new TPCorumAnalysisPane(tppExp,filterTextField);
        //TPMeanDifferencePane euclideanDistancePane = new TPMeanDifferencePane(tppExp,table,fcPane);
        //TPClusterPane clusterPane = new TPClusterPane(tppExp);
        //TPBootstrapPane bsPane = new TPBootstrapPane(tppExp,fcPane);
        TPExportPane1D exportPane = new TPExportPane1D(tppExp,stage,table);

        rightPane.getChildren().addAll(tmPane, new Separator(), new Separator(), stringNetworkPane,
                stringFunctionalEnrichmentPane, corumAnalysisPane, new Separator(), new Separator(), exportPane);
    }

    private TableCell<ObservableList<Double>, Double> call(TableColumn<ObservableList<Double>, Double> c) {

        return new TableCell<ObservableList<Double>, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (!empty && Double.isFinite(item)) {
                    DecimalFormat format = new DecimalFormat("0.0000");
                    setText(format.format(item));

                } else {
                    setText("");
                }
                if (!empty && Double.isFinite(item)) {
                    int r = (int) (tppExp.getColour(item).getRed() * 255.0);
                    int g = (int) (tppExp.getColour(item).getGreen() * 255.0);
                    int b = (int) (tppExp.getColour(item).getBlue() * 255.0);
                    String col = "rgb(" + r + "," + g + "," + b + ")";
                    this.setStyle("-fx-background-color: " + col);
                    int r2 = (int) (255.00 - tppExp.getColour(item).getRed() * 255.00);
                    int g2 = (int) (255.00 - tppExp.getColour(item).getGreen() * 255.00);
                    int b2 = (int) (255.00 - tppExp.getColour(item).getBlue() * 255.00);
                    String col2 = "rgb(" + r2 + "," + g2 + "," + b2 + ")";
                    this.setStyle("-fx-text-fill: " + col2);
                } else {
                    setText("");
                }
            }
        };
    }

    private class NumberTableCellFactory<S, T> implements Callback<TableColumn<S,T>, TableCell<S,T>> {

        NumberTableCellFactory() {
            int startNumber = 1;
        }

        class NumberTableCell<S,T> extends TableCell<S,T> {

            NumberTableCell() {

            }

            @Override
            public void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? "" : Integer.toString(getIndex() + 1));
            }

        }

        @Override
        public TableCell<S,T> call(TableColumn<S,T> param) { return new NumberTableCell<>(); }

        public <T> TableColumn<T,Void> createNumberColumn() {
            TableColumn<T, Void> column = new TableColumn<>();
            column.setEditable(false);
            column.setCellFactory(new NumberTableCellFactory<>());
            return column;
        }

    }

    final private Pane tpPane;

    final private Pane centerPane;
    final private Pane leftPane;
    final private Pane rightPane;

    final private FilteredList<Protein1D> proteinFilteredList;

    final private SortedList<Protein1D> proteinSortedFilteredList;

    final private TableView<Protein1D> table;


    final private Proteome<Protein1D> tppExp;
}

