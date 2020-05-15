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
package com.chembiohub.tpmap.analysis;

import com.chembiohub.tpmap.dstruct.Protein;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.chembiohub.tpmap.dstruct.io.FileExportWizard;
import javafx.beans.Observable;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

/**
 * TPCorumAnalysis
 *
 * Performs Corum analysis, this loads all Corum complexes located in allComplexes.txt and identifies the
 * protein complexes that selected proteins are part of. It displays a tree showing all protein complexes,
 * when a protein complex is selected it updates the main table to only display proteins within the protein
 * complex.
 *
 * @author felixfeyertag
 */
public class TPCorumAnalysis {

    private final Map<String,String[]> allComplexes;
    private final Map<String,ObservableList<String>> proteinMapAll;
    
    public TPCorumAnalysis() {
        
        allComplexes = new HashMap<>();
        proteinMapAll = new HashMap<>();

        String corumAll = "/com/chembiohub/tpmap/analysis/analysispane/allComplexes.txt";
        try (BufferedReader br = new BufferedReader(new InputStreamReader(getClass().getResource(corumAll).openStream()))) {

            br.lines().forEach((String line) -> {

                String[] lineVals = line.split("\t");

                if(lineVals[0].startsWith("ComplexID")) {
                    return;
                }

                assert(!allComplexes.containsKey(lineVals[0]));

                allComplexes.put(lineVals[0], lineVals);
                String[] proteinVals = lineVals[5].split(";");

                for(String protein : proteinVals) {
                    if(proteinMapAll.containsKey(protein)) {
                        proteinMapAll.get(protein).add(lineVals[0]);
                    } else {
                        ObservableList<String> proteinList = FXCollections.observableArrayList();
                        proteinList.add(lineVals[0]);
                        proteinMapAll.put(protein,proteinList);
                    }
                }
            });
            
        } catch (Exception ex) {
            Logger.getLogger(TPCorumAnalysis.class.getName()).log(Level.SEVERE, null, ex);
        }
        
    }
    
    public void runCorumAnalysis(ObservableList<Protein> proteins, Stage parentStage, TextField filterTextField) {
        
        ObservableList<BooleanProperty> selections = FXCollections.observableArrayList();
        
        final TreeItem<HBox> rootItem = new TreeItem<> (new HBox(new Label("CORUM Protein Complexes")));
        rootItem.setExpanded(true);
        
        final Map<String,Protein> allProteins = new HashMap<>();
        proteins.forEach(p -> { 
            String accession = p.getAccession();
            accession = accession.replaceAll("-.*$", "");
            allProteins.put(accession, p);
        });
        
        final ObservableList<String> proteinComplexMembers = FXCollections.observableArrayList();
        
        proteins.forEach((Protein p) -> {
            String accession = p.getAccession();
            accession = accession.replaceAll("-.*$","");
            if(p.getSelected() && proteinMapAll.containsKey(accession)) {
                for(String acc : proteinMapAll.get(accession)) {
                    String[] complex = allComplexes.get(acc);
                    String proteinComplex = complex[0] + " " + complex[1];
                    String[] complexProteins = complex[5].split(";");
                    
                    TreeItem<HBox> proteinComplexItem;
                    HBox proteinComplexItemHBox = new HBox();
                    Label proteinComplexItemLabel = new Label(proteinComplex);
                    proteinComplexItemHBox.getChildren().add(/*proteinComplexItemCheckBox,*/proteinComplexItemLabel);
                    proteinComplexItem = new TreeItem<>(proteinComplexItemHBox);
                    proteinComplexItem.setExpanded(false);
                    
                    for(String cp : complexProteins) {

                        String proteinLabel = cp;

                        if(allProteins.containsKey(cp) && !allProteins.get(cp).getGeneName().isEmpty()) {
                            proteinLabel += " - " + allProteins.get(cp).getGeneName();
                        }

                        TreeItem<HBox> proteinItem;
                        HBox proteinItemHBox = new HBox();
                        CheckBox proteinItemCheckBox = new CheckBox(proteinLabel);
                        proteinItemHBox.getChildren().addAll(proteinItemCheckBox);
                        proteinItem = new TreeItem<>(proteinItemHBox);
                        
                        if(allProteins.containsKey(cp)) {

                            Protein protein = allProteins.get(cp);

                            proteinItemCheckBox.selectedProperty().bindBidirectional(protein.selectedProperty());

                            if(!protein.getAccession().isEmpty()) {
                                proteinItem.getChildren().add(new TreeItem<>(new HBox(new Label("Accession: " + protein.getAccession()))));
                            }
                            if(!protein.getGeneName().isEmpty()) {
                                proteinItem.getChildren().add(new TreeItem<>(new HBox(new Label("Gene name: " + protein.getGeneName()))));
                            }
                            if(!protein.getDescription().isEmpty()) {
                                proteinItem.getChildren().add(new TreeItem<>(new HBox(new Label("Description: " + protein.getDescription()))));
                            }
                            if(!protein.getScore().isNaN()) {
                                proteinItem.getChildren().add(new TreeItem<>(new HBox(new Label("Score: " + protein.getScore()))));
                            }
                            proteinItem.setExpanded(false);
                        }
                        else {
                            proteinItemCheckBox.setDisable(true);
                        }

                        proteinComplexItem.getChildren().add(proteinItem);

                    }
                    rootItem.getChildren().add(proteinComplexItem);

                }
            }
        });
        
        
        
        final TreeView<HBox> treeView = new TreeView(rootItem);
        treeView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        
        treeView.getSelectionModel().selectedItemProperty().addListener( (Observable p) -> {

            ObservableList<TreeItem<HBox>> selectedItems = treeView.getSelectionModel().getSelectedItems();

            StringBuilder filterText = new StringBuilder();

            for(TreeItem<HBox> protein : selectedItems) {
                String acc = findAccessions(selectedItems.get(0));
                if(!acc.isEmpty()) {
                    if(filterText.length() > 0) {
                        filterText.append("|");
                    }
                    filterText.append(acc);
                }
            }

            filterTextField.setText(filterText.toString());
        });
        
        Button selectComplexes = new Button("Select all complex members");

        selectComplexes.setOnAction( e -> proteins.forEach(p -> {
            String accession = p.getAccession();
            accession = accession.replaceAll("-.*$", "");
            if(proteinComplexMembers.contains(accession)) {
                p.setSelected(true);
            }
        }));

        Button exportButton = new Button("Export CORUM complexes");
        exportButton.setOnAction(evt -> {

            System.out.println(rootItem.getChildren().size());

            final StringBuilder exportString = new StringBuilder();

            exportString.append("Protein Complex\tSelected Protein Count\tIn Dataset Protein Count\tAll Protein Count\t" +
                    "Selected Proteins\tUnselected Proteins\tProteins Not In Dataset\n");

            rootItem.getChildren().forEach(complex -> {

                Label complexLabel = (Label) complex.getValue().getChildren().get(0);

                final StringBuilder selectedProteins = new StringBuilder();
                final StringBuilder unselectedProteins = new StringBuilder();
                final StringBuilder proteinsNotInDataset = new StringBuilder();

                final IntegerProperty selectedProteinCount = new SimpleIntegerProperty(0);
                final IntegerProperty allProteinCount = new SimpleIntegerProperty(0);
                final IntegerProperty inDatasetProteinCount = new SimpleIntegerProperty(0);

                complex.getChildren().forEach(protein -> {
                    CheckBox proteinCheckBox = (CheckBox) protein.getValue().getChildren().get(0);
                    if(proteinCheckBox.isSelected()) {
                        selectedProteins.append(proteinCheckBox.getText() + ";");
                        allProteinCount.set(allProteinCount.get()+1);
                        selectedProteinCount.set(selectedProteinCount.get()+1);
                        inDatasetProteinCount.set(inDatasetProteinCount.get()+1);
                    }
                    else if(!proteinCheckBox.isDisabled()) {
                        unselectedProteins.append(proteinCheckBox.getText() + ";");
                        allProteinCount.set(allProteinCount.get()+1);
                        inDatasetProteinCount.set(inDatasetProteinCount.get()+1);
                    }
                    else {
                        proteinsNotInDataset.append(proteinCheckBox.getText() + ";");
                        allProteinCount.set(allProteinCount.get()+1);
                    }
                });

                exportString.append(complexLabel.getText() + "\t" + selectedProteinCount.getValue() + "\t" +
                        inDatasetProteinCount.getValue() + "\t" + allProteinCount.getValue() + "\t" +
                        selectedProteins.toString() + "\t" + unselectedProteins.toString() + "\t" +
                        proteinsNotInDataset.toString() + "\n");

            });

            FileExportWizard exporter = new FileExportWizard(exportString.toString(), parentStage, false, 0);

        });
        
        BorderPane root = new BorderPane();
        treeView.setPrefHeight(Integer.MAX_VALUE);
        root.setCenter(treeView);
        root.setBottom(exportButton);
        Stage primaryStage = new Stage();

        primaryStage.setOnCloseRequest(evt -> filterTextField.clear());

        primaryStage.initOwner(parentStage);
        primaryStage.setScene(new Scene(root, 600,480));
        primaryStage.show();
        
    }

    private static String findAccessions(TreeItem<HBox> selectedItem) {

        if (selectedItem.isLeaf()) {
            if (selectedItem.getValue().getChildren().get(0) instanceof Label) {
                String labelText = ((Label) selectedItem.getValue().getChildren().get(0)).getText();
                if(labelText.startsWith("Accession: ")) {
                    return labelText.replaceFirst("Accession: ", "");
                }
            }
        }
        else {
            StringBuilder retVal = new StringBuilder();
            for(TreeItem<HBox> c : selectedItem.getChildren()) {
                String acc = findAccessions(c);
                if(retVal.length() == 0) {
                    retVal = new StringBuilder(acc);
                }
                else if (!acc.isEmpty()) {
                    retVal.append("|").append(acc);
                }
            }
            return retVal.toString();
        }
        return "";
    }
    
}
