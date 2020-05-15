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
import javafx.event.ActionEvent;
import javafx.scene.control.Button;

/**
 * TPStringFunctionalEnrichmentPane
 *
 * Create a button to instantiate String functional enrichment of selected proteins.
 *
 * @author felixfeyertag
 */
public final class TPStringFunctionalEnrichmentPane extends TPAnalysisPane {
    
    final private Proteome<? extends Protein> tppExp;
    
    public TPStringFunctionalEnrichmentPane(Proteome<? extends Protein> tppExp) {
        this.tppExp = tppExp;
        init();
    }
    
    private void init() {
        
        Button stringFunctionalEnrichmentButton = new Button("String Functional Enrichment");
        stringFunctionalEnrichmentButton.setPrefWidth(200);

        
        stringFunctionalEnrichmentButton.setOnAction((ActionEvent event) -> {
            TPStringAnalysis stringAnalysis = new TPStringAnalysis();
            stringAnalysis.stringFunctionalEnrichment(tppExp.getProteins(), tppExp.getParentStage(), tppExp.getTabPane());
        });

        this.getChildren().add(stringFunctionalEnrichmentButton);
        
    }
    
    
}
