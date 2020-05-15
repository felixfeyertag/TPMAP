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
import com.chembiohub.tpmap.normalisation.TPNormalisation;
import javafx.event.ActionEvent;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.apache.commons.math3.util.FastMath;

/**
 * TPFCPane
 *
 * Instantiates a TPAnalysisPane with sliders to change the default fold change values. Fold change sliders run from
 * 0 to 1 and reflect the percentile of the upper fold change threshold ( gt 1 ) and lower fold change threshold ( lt 1 )
 * used by the TPMAP scoring algorithm.
 *
 * @author felixfeyertag
 */
public final class TPFCPane extends TPAnalysisPane {
    
    private final Proteome<Protein2D> tppExp;
    private final Slider minFCSlider;
    private final Slider maxFCSlider;
    private final CheckBox normCheckBox;
    private final Label minFCValLabel;
    private final Label maxFCValLabel;
    private final int sf;
    private final String sfStr;

    
    /**
     *
     * @param tppExp Thermal profiling experiment
     */
    public TPFCPane(Proteome<Protein2D> tppExp) {
        this.minFCSlider = new Slider();
        this.maxFCSlider = new Slider();
        minFCValLabel = new Label();
        maxFCValLabel = new Label();
        this.normCheckBox = new CheckBox();
        this.tppExp = tppExp;
        this.sf = (int) FastMath.log10(tppExp.getProteinCount());
        this.sfStr = "%."+sf+"f";
        this.init();
    }
    
    private void init() {

        VBox fcBox = new VBox();
        Label fcLabel = new Label("Fold Change");
        HBox normBox = new HBox();
        normCheckBox.setSelected(tppExp.getNormalisationMethod()== TPNormalisation.Normalisation.MEDIAN);
        Label normLabel = new Label("Normalisation");
        normBox.getChildren().addAll(normCheckBox,normLabel);
        HBox minFCBox = new HBox();
        Label minFCLabel = new Label("Lower threshold percentile:");
        minFCLabel.setMinWidth(50);
        minFCSlider.setMinWidth(100);
        minFCSlider.setMin(0.0);
        minFCSlider.setMax(1.0);
        minFCSlider.setValue(0.2);
        minFCSlider.setBlockIncrement(FastMath.pow(10.0, -sf));
        minFCValLabel.setText(String.format(sfStr,minFCSlider.getValue()) + " (FC: " + String.format(sfStr,tppExp.getLowerPercentile(minFCSlider.getValue())) + ")");
        minFCBox.getChildren().addAll(minFCSlider);
        HBox maxFCBox = new HBox();
        Label maxFCLabel = new Label("Upper threshold percentile:");
        maxFCLabel.setMinWidth(50);
        maxFCSlider.setMinWidth(100);
        maxFCSlider.setMin(0.0);
        maxFCSlider.setMax(1.0);
        maxFCSlider.setValue(0.8);
        maxFCSlider.setBlockIncrement(FastMath.pow(10.0, -sf));
        maxFCValLabel.setText(String.format(sfStr,maxFCSlider.getValue()) + " (FC: " + String.format(sfStr,tppExp.getUpperPercentile(maxFCSlider.getValue())) + ")");
        maxFCBox.getChildren().addAll(maxFCSlider);
        Button fcButton = new Button("Recalculate");
        fcButton.setPrefWidth(200);
        fcBox.getChildren().addAll(fcLabel,normBox,minFCLabel,minFCBox,minFCValLabel,maxFCLabel,maxFCBox,maxFCValLabel,fcButton);
        
        
        minFCSlider.valueProperty().addListener( listener -> {
            String minFC = String.format(sfStr, minFCSlider.getValue());
            String lowerPercentile = String.format(sfStr, tppExp.getLowerPercentile(minFCSlider.getValue()));
            minFCValLabel.setText(minFC + " (FC: " + lowerPercentile + ")");
        });
        maxFCSlider.valueProperty().addListener( listener -> {
            String maxFC = String.format(sfStr, maxFCSlider.getValue());
            String upperPercentile = String.format(sfStr, tppExp.getUpperPercentile(maxFCSlider.getValue()));
            maxFCValLabel.setText(maxFC + " (FC: " + upperPercentile + ")");
        });

        fcButton.setOnAction( (ActionEvent event) -> this.run());

        this.getChildren().add(fcBox);
    }
    
    public void run() {
        if(normCheckBox.isSelected()) {
            tppExp.setPercentileThresholds(minFCSlider.getValue(), maxFCSlider.getValue());
            tppExp.setNormalisation(TPNormalisation.Normalisation.MEDIAN);
            tppExp.updateProteinCount();
            tppExp.resetPVals();
            minFCValLabel.setText(String.format(sfStr,minFCSlider.getValue()) + " (FC: " + String.format(sfStr,tppExp.getLowerPercentile(minFCSlider.getValue())) + ")");
            maxFCValLabel.setText(String.format(sfStr,maxFCSlider.getValue()) + " (FC: " + String.format(sfStr,tppExp.getUpperPercentile(maxFCSlider.getValue())) + ")");
        }
        else {
            tppExp.setPercentileThresholds(minFCSlider.getValue(), maxFCSlider.getValue());
            tppExp.setNormalisation(TPNormalisation.Normalisation.NONE);
            tppExp.updateProteinCount();
            tppExp.resetPVals();
            minFCValLabel.setText(String.format(sfStr,minFCSlider.getValue()) + " (FC: " + String.format(sfStr,tppExp.getLowerPercentile(minFCSlider.getValue())) + ")");
            maxFCValLabel.setText(String.format(sfStr,maxFCSlider.getValue()) + " (FC: " + String.format(sfStr,tppExp.getUpperPercentile(maxFCSlider.getValue())) + ")");
        }
    }
    
    public Double getMinFCSliderVal() {
        return minFCSlider.getValue();
    }
    
    public Double getMaxFCSliderVal() {
        return maxFCSlider.getValue();
    }
    
}
