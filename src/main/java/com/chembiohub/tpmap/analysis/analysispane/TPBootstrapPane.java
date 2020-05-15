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

import com.chembiohub.tpmap.analysis.TPBootstrapAnalysis;
import com.chembiohub.tpmap.dstruct.Protein;
import com.chembiohub.tpmap.dstruct.Proteome;
import javafx.event.ActionEvent;
import javafx.scene.control.Button;

/**
 * TPBootstrapPane
 *
 * Creates a TPAanalysisPane with a button to rerun boothstrap.
 *
 * @author felixfeyertag
 */
final class TPBootstrapPane extends TPAnalysisPane {
    
    final private Proteome<Protein> tppExp;
    final private TPFCPane fcPane;

    public TPBootstrapPane(Proteome<Protein> exp, TPFCPane fcPane) {
        this.tppExp = exp;
        this.fcPane = fcPane;
        init();
    }
    
    private void init() {
        
        Button bootstrapButton = new Button("Rerun with Bootstrap");

        fcPane.run();

        bootstrapButton.setPrefWidth(200);

        bootstrapButton.setOnAction((ActionEvent event) -> {

            TPBootstrapAnalysis bsAnalysis = new TPBootstrapAnalysis(tppExp.getBootstrapIterations());
            bsAnalysis.runBootstrapAnalysis(tppExp);
            bsAnalysis.setBootstrapPVals(tppExp);
            tppExp.set2dBootstrapAnalysis(bsAnalysis);

        });

        this.getChildren().add(bootstrapButton);
    }
}
