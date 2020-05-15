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

import static com.chembiohub.tpmap.analysis.TPUniProtAnalysis.uniprotEntry;

import com.chembiohub.tpmap.dstruct.Protein;
import com.chembiohub.tpmap.dstruct.Proteome;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.scene.control.Button;
import javafx.scene.control.TableView;

/**
 * TPUniProtPane
 *
 * Create a TPAnalysisPane with a button to instantiate UniProt analysis
 *
 * @author felixfeyertag
 */
public final class TPUniProtPane extends TPAnalysisPane {
    
    final private TableView<? extends Protein> table;
    private final Proteome tppExp;
    
    public TPUniProtPane(TableView<? extends Protein> table, Proteome tppExp) {
        this.table = table;
        this.tppExp = tppExp;
        init();
    }

    private void init() {
        
        Button uniprotEntry = new Button("UniProt Entry");
        uniprotEntry.setPrefWidth(200);

        uniprotEntry.setOnAction((ActionEvent event) -> {
            ObservableList<? extends Protein> proteins = table.getSelectionModel().getSelectedItems();
            uniprotEntry(proteins, tppExp.getTabPane(), tppExp.getParentStage());
        });
        
        this.getChildren().add(uniprotEntry);
    }
    
    
}
