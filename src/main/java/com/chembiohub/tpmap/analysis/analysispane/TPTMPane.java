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
import com.chembiohub.tpmap.normalisation.TPNormalisation;
import javafx.event.ActionEvent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 * TPTMPane
 *
 * Instantiates a TPAnalysisPane with a slider to change the weight of the TM shift of the 1D score. The weight can
 * range from 0 to 1, where 0 represents 0% of the score determined by the TM shift (i.e. the score is a function
 * quality control only) or 100% (i.e. the score is a function of TM shift only). The default is 40%
 *
 * @author felixfeyertag
 */
public final class TPTMPane extends TPAnalysisPane {

    private final Proteome<Protein1D> tppExp;
    private final Slider tmSlider;


    /**
     *
     * @param tppExp Thermal profiling experiment
     */
    public TPTMPane(Proteome<Protein1D> tppExp) {
        this.tmSlider = new Slider();
        this.tppExp = tppExp;
        this.init();
    }
    
    private void init() {

        VBox tmBox = new VBox();
        Button tmButton = new Button("Update score");
        tmButton.setPrefWidth(200);
        tmSlider.setMinWidth(100);
        tmSlider.setMin(0);
        tmSlider.setMax(100);
        tmSlider.setValue(70);
        tmSlider.setBlockIncrement(1);

        int tmVal = (int) tmSlider.getValue();
        int qVal = 100 - tmVal;
        Label tmLabel = new Label("Thermal Shift / Curve Fit: " + tmVal + " / " + qVal);
        tmLabel.setMinWidth(50);

        tmSlider.valueProperty().addListener( listener -> {
            int tmVal0 = (int) tmSlider.getValue();
            int qVal0 = 100 - tmVal0;
            tmLabel.setText("Thermal Shift / Curve Fit: " + tmVal0 + " / " + qVal0);
        });

        tmButton.setOnAction( (ActionEvent event) -> this.run());

        tmBox.getChildren().addAll(tmLabel, tmSlider, tmButton);

        this.getChildren().add(tmBox);
    }
    
    public void run() {

        int tmVal = (int) tmSlider.getValue();
        tppExp.setScoreTMWeight(tmVal);
        tppExp.setNormalisation(tppExp.getNormalisationMethod());
        tppExp.updateProteinCount();

    }
    
}
