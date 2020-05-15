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

import com.chembiohub.tpmap.dstruct.Protein1D;
import com.chembiohub.tpmap.dstruct.Proteome;
import com.chembiohub.tpmap.dstruct.io.ChartExportWizard;
import com.chembiohub.tpmap.dstruct.io.FileExportWizard;
import javafx.event.ActionEvent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TableView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.Set;
import java.util.TreeSet;

/**
 * TTPPExportPane1D
 *
 * Export selected proteins in a 1D TPMAP analysis, parameters can be exported as a table and charts can be exported
 * as images.
 *
 * @author felixfeyertag
 */
public final class TPExportPane1D extends TPAnalysisPane {

    private final Stage stage;
    private final TableView<Protein1D> table;
    private final Proteome<Protein1D> tppExp;

    public TPExportPane1D(Proteome tppExp, Stage stage, TableView<Protein1D> table) {
        this.stage = stage;
        this.table = table;
        this.tppExp = tppExp;
        init();
    }
    
    private void init() {
        
        Button exportButton = new Button();
        exportButton.setText("Export selected proteins");
        exportButton.setPrefWidth(200);

        Button exportChartButton = new Button();
        exportChartButton.setText("Export selected melt curves");
        exportChartButton.setPrefWidth(200);

        exportButton.setOnAction((ActionEvent evt) -> {
            String exportText = copySelection(table);
            if(!exportText.isEmpty()) {
                FileExportWizard ew = new FileExportWizard(exportText, stage, false, -1);
            }
            else {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Export Error");
                alert.setHeaderText("Export Error");
                alert.setContentText("Nothing to export. Select proteins by activating the checkbox.");
                alert.showAndWait();
            }
        });

        exportChartButton.setOnAction( evt -> {
            if(tppExp.getProteinSelectedProperty().getValue()>0) {
                ChartExportWizard ew = new ChartExportWizard(tppExp.getProteins(), tppExp.getTempLabels(), tppExp.getConcLabels(), stage);
            }
            else {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Export Error");
                alert.setHeaderText("Export Error");
                alert.setContentText("No proteins selected for export. Select proteins by activating the checkbox.");
                alert.showAndWait();
            }
        });
        
        VBox vBox = new VBox();
        vBox.getChildren().addAll(exportButton, exportChartButton);
        this.getChildren().add(vBox);

    }


    private String copySelection(final TableView<Protein1D> table) {
        final Set<Integer> rows = new TreeSet<>();

        //TODO replace StringBuilder with stream to reduce memory load
        final StringBuilder copyText = new StringBuilder();

        table.getItems().forEach(protein -> {

            if(protein.getSelected()) {

                Double[][] abundancesratio = protein.getAbundancesTempRatio();

                if(copyText.toString().isEmpty()) {
                    copyText.append("Accession\tGene\tOrganism\tDescription\t" +
                                    "Tm V1\tTm V2\tTm T1\tTm T2\t" +
                                    "Tm shift (V1T1)\tTm shift (V2T2)\tTm shift (V1V2)\tMean Tm shift\t" +
                                    "RMSE V1\tRMSE V2\tRMSE T1\tRMSE T2\t" +
                                    "a T1\tb T1\tplateau T1\t" +
                                    "a T2\tb T2\tplateau T2\t" +
                                    "a V1\tb V1\tplateau V1\t" +
                                    "a V2\tb V2\tplateau V2\t")
                            .append("Score");

                    for(int j=0; j<abundancesratio[0].length; j++) {
                        copyText.append("\tR" + j + " T1");
                    }
                    for(int j=0; j<abundancesratio[1].length; j++) {
                        copyText.append("\tR" + j + " T2");
                    }
                    for(int j=0; j<abundancesratio[2].length; j++) {
                        copyText.append("\tR" + j + " V1");
                    }
                    for(int j=0; j<abundancesratio[3].length; j++) {
                        copyText.append("\tR" + j + " V2");
                    }
                    copyText.append("\n");

                }

                Double[][] curveFitParams = new Double[4][3];

                for(int i=0;i<curveFitParams.length;i++) {
                    for(int j=0;j<curveFitParams[0].length;j++) {
                        try {
                            curveFitParams[i][j] = protein.getCurveFitParams()[i][j];
                        } catch (ArrayIndexOutOfBoundsException e) {  }
                    }
                }

                copyText

                        .append(protein.getAccession()    + "\t")
                        .append(protein.getGeneName()     + "\t")
                        .append(protein.getOrganismName() + "\t")
                        .append(protein.getDescription()  + "\t")

                        .append(protein.getTmv1()  + "\t" + protein.getTmv2()   + "\t")
                        .append(protein.getTmt1()  + "\t" + protein.getTmt2()   + "\t")
                        .append(protein.getTmVT1() + "\t" + protein.getTmVT2()  + "\t")
                        .append(protein.getTmVV()  + "\t" + protein.getMeanTM() + "\t")

                        .append(protein.getRmsev1() + "\t" + protein.getRmsev2() + "\t")
                        .append(protein.getRmset1() + "\t" + protein.getRmset2() + "\t")

                        .append(curveFitParams[0][0] + "\t" + curveFitParams[0][1] + "\t" + curveFitParams[0][2] + "\t")
                        .append(curveFitParams[1][0] + "\t" + curveFitParams[1][1] + "\t" + curveFitParams[1][2] + "\t")
                        .append(curveFitParams[2][0] + "\t" + curveFitParams[2][1] + "\t" + curveFitParams[2][2] + "\t")
                        .append(curveFitParams[3][0] + "\t" + curveFitParams[3][1] + "\t" + curveFitParams[3][2] + "\t")

                        .append(protein.getScore());

                for(int j=0; j<abundancesratio[0].length; j++) {
                    copyText.append("\t").append(abundancesratio[0][j]);
                }
                for(int j=0; j<abundancesratio[1].length; j++) {
                    copyText.append("\t").append(abundancesratio[1][j]);
                }
                for(int j=0; j<abundancesratio[2].length; j++) {
                    copyText.append("\t").append(abundancesratio[2][j]);
                }
                for(int j=0; j<abundancesratio[3].length; j++) {
                    copyText.append("\t").append(abundancesratio[3][j]);
                }

                copyText.append("\n");

            }
        });

        return copyText.toString();
    }

    private void copySelectionToClipboard(final TableView<Protein1D> table) {

        String copyText = copySelection(table);
        final ClipboardContent clipboardContent = new ClipboardContent();
        clipboardContent.putString(copyText);
        Clipboard.getSystemClipboard().setContent(clipboardContent);

    }

}

