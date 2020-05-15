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

import com.chembiohub.tpmap.analysis.TPClusterAnalysis;
import com.chembiohub.tpmap.dstruct.Proteome;
import javafx.event.ActionEvent;
import javafx.scene.control.Button;

/**
 * TPClusterPane
 *
 * Creates a TPAnalysisPane with a button to instantiate cluster analysis
 *
 * @author felixfeyertag
 */
final class TPClusterPane extends TPAnalysisPane {
    
    final private Proteome tppExp;
    
    public TPClusterPane(Proteome tppExp) {
        this.tppExp = tppExp;
        this.init();
    }
    
    private void init() {
        
        Button clusterButton = new Button("Cluster Analysis");
        clusterButton.setPrefWidth(200);
        
        clusterButton.setOnAction((ActionEvent event) -> {
            TPClusterAnalysis clusterAnalysis = new TPClusterAnalysis();
            clusterAnalysis.analyseProtein(tppExp);
        });
        
        this.getChildren().add(clusterButton);
        
    }
    
    
}
