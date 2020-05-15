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

import com.chembiohub.tpmap.analysis.TPStringAnalysis;
import com.chembiohub.tpmap.dstruct.Protein;
import com.chembiohub.tpmap.dstruct.Proteome;
import javafx.beans.property.SimpleStringProperty;
import javafx.event.ActionEvent;
import javafx.scene.control.Button;

/**
 * TPStringNetworkPane
 *
 * Creates a TPAnalysisPane with a button to instantiate String PPI network analysis of selected proteins
 *
 * @author felixfeyertag
 */
public final class TPStringNetworkPane extends TPAnalysisPane {
    
    final private Proteome<? extends Protein> tppExp;
    
    public TPStringNetworkPane(Proteome<? extends Protein> tppExp) {
        this.tppExp = tppExp;
        init();
    }
    
    private void init() {

        Button stringNetworkImageButton = new Button("String Network Image");
        stringNetworkImageButton.setPrefWidth(200);


        stringNetworkImageButton.setOnAction((ActionEvent event) -> {
            TPStringAnalysis stringAnalysis = new TPStringAnalysis();
            stringAnalysis.stringNetworkImage(tppExp.getProteins(), new SimpleStringProperty(tppExp.getTaxonomy()), tppExp.getParentStage(), tppExp.getTabPane());
            //TPStringAnalysis str = new TPStringAnalysis();
        });
        
        this.getChildren().add(stringNetworkImageButton);
        
    }
    
    
}
