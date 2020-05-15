/*
 * Copyright (C) 2020 felixfeyertag
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.chembiohub.tpmap.dstruct;

import javafx.beans.property.Property;

/**
 *
 * @author felixfeyertag
 */
public interface Protein {

    //Double[][] getAbundancesConcRatioNormalised();

    //ObservableList<ObservableList<Double>> getAbundancesTempRatioOL();

    boolean getSelected();

    //void updateScores(double minThreshold, double maxThreshold, ObservableList<String> tempLabels, ObservableList<String> concLabels);

    //Double getMinimum();
    //Double getMaximum();

    void setScore(double value);
    Double getScore();

    //Double[][] getAbundancesConcRatio();

    //void setAbundancesConcRatioNormalised(Double[][] normalised);

    String getDescription();
    String getAccession();
    String getGeneName();
    String getOrganismName();

    //ObservableList<ObservableList<Double>> getAbundancesConcRatioNormalisedOL();

    void setSelected(boolean b);

    //Double getMeanFCScore();

    //Double getMeanDifference();

    //void setMeanDifference(Double v);

    Double[][] getAbundances();

    //void setPValue(Double pVal);

    //HashMap<String, double[]> getCurveFitParams();

    Property<Boolean> selectedProperty();

    Double[][] getAbundancesConcRatioNormalised();

    void setPValue(Double pVal);
}
