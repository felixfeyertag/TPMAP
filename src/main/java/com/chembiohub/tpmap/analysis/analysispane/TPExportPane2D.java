/*
 * Copyright (C) 2020 Felix Feyertag <felix.feyertag@ndm.ox.ac.uk>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3.
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
import com.chembiohub.tpmap.dstruct.io.FileExportWizard;
import java.util.Set;
import java.util.TreeSet;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * TPExpertPane2D
 *
 * Creates a TPAnalysisPane with a button to instantiate the export of selected proteins as a table.
 *
 * @author felixfeyertag
 */
public final class TPExportPane2D extends TPAnalysisPane {
    
    private final Stage stage;
    private final TableView<Protein2D> table;
    private final Proteome<Protein2D> tppExp;
    
    public TPExportPane2D(Proteome tppExp, Stage stage, TableView<Protein2D> table) {
        this.stage = stage;
        this.table = table;
        this.tppExp = tppExp;
        init();
    }
    
    private void init() {
        
        Button exportButton = new Button();
        exportButton.setText("Export selected proteins");
        exportButton.setPrefWidth(200);
        CheckBox exportExpandedCheckBox = new CheckBox();
        exportExpandedCheckBox.setText("Expanded export");
        exportExpandedCheckBox.setPadding(new Insets(0,5,0,5));
        exportExpandedCheckBox.setSelected(true);
       
        exportButton.setOnAction((ActionEvent evt) -> {
            String exportText = copySelection(table,exportExpandedCheckBox.isSelected());
            if(!exportText.isEmpty()) {
                FileExportWizard ew = new FileExportWizard(exportText, stage, exportExpandedCheckBox.isSelected(), tppExp.getConcLabels().size());
            }
            else {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Export Error");
                alert.setHeaderText("Export Error");
                alert.setContentText("No proteins selected for export. Use checkbox to select protein(s).");
                alert.show();
            }
        });
        
        VBox vBox = new VBox();
        vBox.getChildren().addAll(exportButton,exportExpandedCheckBox);
        this.getChildren().add(vBox);
    }
    
    
    private String copySelection(final TableView<Protein2D> table, final boolean expanded) {
        final Set<Integer> rows = new TreeSet<>();

        //TODO this would be much more memory efficient with a stream
        final StringBuilder copyText = new StringBuilder();
        
        table.getItems().forEach(protein -> {
            if(protein.getSelected()) {

                //header
                if(copyText.toString().isEmpty()) {
                    if(expanded) {
                        for (TableColumn tc : table.getColumns()) {
                            if(!tc.getText().isEmpty()) {
                                copyText.append(tc.getText() + "\t");
                            }
                        }

                        copyText.append("Temperature").append("\t").append(String.join("\t", tppExp.getConcLabels())).append("\n");
                    }
                    else {
                        boolean first=true;
                        for (TableColumn tc : table.getColumns()) {
                            if(!tc.getText().isEmpty()) {
                                if (!first) {
                                    copyText.append("\t");
                                } else {
                                    first = false;
                                }
                                copyText.append(tc.getText());
                            }
                        }
                        copyText.append("\n");
                    }
                }


                IntegerProperty counter = new SimpleIntegerProperty(0);
                
                if(expanded) {

                    protein.getAbundancesConcRatioNormalisedOL().forEach((ObservableList<Double> ratios) -> { 
                    
                        String temp = tppExp.getTempLabels().get(counter.get());
                        
                        counter.set(counter.get()+1);
                        copyText.append(protein.getAccession()).append("\t").append(protein.getGeneName())
                                .append("\t").append(protein.getOrganismName()).append("\t")
                                .append(protein.getDescription()).append("\t").append(protein.getScore()).append("\t")
                                .append(protein.getStabilityScore()).append("\t").append(protein.getDestabilityScore())
                                .append("\t").append(protein.getMeanFCScore()).append("\t").append(protein.getPValue())
                                .append("\t").append(protein.getEffect());

                        protein.getMeanDifferencePropertyList().forEach((DoubleProperty d) -> copyText.append("\t").append(d.getValue()));

                        copyText.append("\t").append(temp);

                        ratios.forEach((Double ratio) -> copyText.append("\t").append(ratio));
                        
                        copyText.append("\n");
                    });

                    copyText.append("\n");

                }
                else {

                    copyText.append(protein.getAccession()).append("\t").append(protein.getGeneName())
                            .append("\t").append(protein.getOrganismName()).append("\t")
                            .append(protein.getDescription()).append("\t").append(protein.getScore()).append("\t")
                            .append(protein.getStabilityScore()).append("\t").append(protein.getDestabilityScore())
                            .append("\t").append(protein.getMeanFCScore()).append("\t").append(protein.getPValue())
                            .append("\t").append(protein.getEffect());

                    protein.getMeanDifferencePropertyList().forEach((DoubleProperty d) -> copyText.append("\t").append(d.getValue()));

                    copyText.append("\n");
                }

            }
        });
        
        return copyText.toString();
    }
    
    private void copySelectionToClipboard(final TableView<Protein2D> table, final boolean expanded) {
        String copyText = copySelection(table,expanded);
        final ClipboardContent clipboardContent = new ClipboardContent();
        clipboardContent.putString(copyText);
        Clipboard.getSystemClipboard().setContent(clipboardContent);
    }
    
}
