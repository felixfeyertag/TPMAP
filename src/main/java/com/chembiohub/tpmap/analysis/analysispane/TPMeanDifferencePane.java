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
package com.chembiohub.tpmap.analysis.analysispane;

import com.chembiohub.tpmap.dstruct.Protein2D;
import com.chembiohub.tpmap.dstruct.Proteome;
import com.chembiohub.tpmap.scoring.TPMeanDifference;

import java.text.DecimalFormat;
import java.util.Comparator;

import javafx.beans.property.*;
import javafx.beans.value.ObservableDoubleValue;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.util.Callback;

/**
 * TPMeanDifferencePane
 *
 * Creates a TPAnalysisPane with a button to instantiate mean difference analysis
 *
 * @author felixfeyertag
 */
public final class TPMeanDifferencePane extends TPAnalysisPane {
    
    final private Proteome tppExp;
    final private TableView<Protein2D> table;
    final private IntegerProperty index;

    public TPMeanDifferencePane(Proteome tppExp, TableView<Protein2D> table, TPFCPane fcPane) {
        this.tppExp = tppExp;
        this.table = table;
        this.index = new SimpleIntegerProperty(0);
        init();
    }
    
    private void init() {
        
        Button meanButton = new Button("Mean Difference");
        meanButton.setPrefWidth(200);

        
        meanButton.setOnAction((ActionEvent event) -> {
            try {

                Protein2D selectedProtein = table.getItems().get(table.getSelectionModel().getFocusedIndex());

                String mdColumnTitle = selectedProtein.getAccession().isEmpty() ? "MD" : "MD (" + selectedProtein.getAccession() + ")";
                MDTableColumn mdTableColumn = new MDTableColumn(mdColumnTitle, index.getValue());

                TPMeanDifference.TPPMeanDifference(tppExp.getProteins(), selectedProtein);
                tppExp.getProteins().sort(Comparator.comparing(Protein2D::getTopMeanDifference));

                table.getColumns().add(mdTableColumn);
                index.set(index.getValue()+1);

            } catch (TPMeanDifference.TPPNoneSelectedException e) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setContentText("Exception: " + e);
                alert.showAndWait();
            }
        });

        this.getChildren().add(meanButton);

    }

    private class MDTableColumn extends TableColumn<Protein2D,Double> {

        public MDTableColumn(String title, final int index) {
            super(title);
            this.setCellValueFactory(p -> new ReadOnlyObjectWrapper(p.getValue().getMeanDifferencePropertyList().get(index).getValue()));
            this.setCellFactory(this::call);
        }

        private TableCell<Protein2D, Double> call(TableColumn<Protein2D, Double> tc) {
            return new TableCell<Protein2D, Double>() {
                @Override
                protected void updateItem(Double value, boolean empty) {
                    super.updateItem((Double) value, empty);
                    if (empty) {
                        setText(null);
                    } else {
                        DecimalFormat format = new DecimalFormat("0.0000");
                        setText(format.format(value));
                    }
                }
            };
        }
    }
}
