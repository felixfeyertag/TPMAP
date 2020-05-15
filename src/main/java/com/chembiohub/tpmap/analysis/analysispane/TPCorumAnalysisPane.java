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

import com.chembiohub.tpmap.analysis.TPCorumAnalysis;
import com.chembiohub.tpmap.dstruct.Proteome;
import javafx.event.ActionEvent;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

/**
 * TPCorumAnalysisPane
 *
 * Create a TPAanalysis pane with a button to instantiate Corum analysis
 *
 * @author felixfeyertag
 */
public final class TPCorumAnalysisPane extends TPAnalysisPane {
    
    private final Proteome tppExp;
    private final TPCorumAnalysis corumAnalysis;
    private final TextField filterTextField;
    private final Stage parentStage;
    
    public TPCorumAnalysisPane(Proteome tppExp, TextField filterTextField) {

        this.tppExp = tppExp;
        this.corumAnalysis = new TPCorumAnalysis();
        this.filterTextField = filterTextField;
        this.parentStage = tppExp.getParentStage();
        this.init();

    }
    
    private void init() {
        
        Button corumButton = new Button("CORUM Protein Complexes");
        corumButton.setPrefWidth(200);

        
        corumButton.setOnAction((ActionEvent event) -> corumAnalysis.runCorumAnalysis(tppExp.getProteins(), parentStage, filterTextField));
        
        this.getChildren().add(corumButton);
    }
}

