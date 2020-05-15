/*
 * Copyright (C) 2020 Felix Feyertag <felix.feyertag@ndm.ox.ac.uk>

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
 * along with this program; If not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package com.chembiohub.tpmap.dstruct;

import com.chembiohub.tpmap.normalisation.TPNormalisation;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 * ProteinPISA
 *
 * TODO: Implement datastructure for PISA experiments
 *
 */
public class ProteinPISA implements Protein {

    private String accession;
    private String description;

    private TPNormalisation.Normalisation normalisationMethod;

    private Double tempReference;
    private Double[] temperatures;
    private Double[][] abundances;

    private final BooleanProperty selected;

    private final StringProperty organismNameProperty;
    private final StringProperty organismIdentifierProperty;
    private final StringProperty geneNameProperty;
    private final StringProperty proteinExistenceProperty;
    private final StringProperty sequenceVersionProperty;

    private final DoubleProperty meanDifferenceProperty;

    private final DoubleProperty meanFCProperty;
    private final DoubleProperty fcPValueProperty;
    private final DoubleProperty fcTstatisticProperty;
    private final DoubleProperty tempConcProperty;

    private final DoubleProperty score;

    private final ObservableList<Double> abundancesOL;
    private final ObservableList<Double> abundancesTempRatioOL;
    private final ObservableList<Double> abundancesTempRatioNormalisedOL;
    private Double[][] abundancesTempRatio;
    private Double[][] abundancesTempRatioNormalised;

    public ProteinPISA() {
        selected =  new SimpleBooleanProperty(this, "selected", false);
        organismNameProperty = new SimpleStringProperty(this, "organismname", "");
        organismIdentifierProperty = new SimpleStringProperty(this, "organismidentifier", "");
        geneNameProperty = new SimpleStringProperty(this, "genename", "");
        proteinExistenceProperty = new SimpleStringProperty(this, "proteinexistence", "");
        sequenceVersionProperty = new SimpleStringProperty(this, "sequenceversion", "");

        meanDifferenceProperty = new SimpleDoubleProperty(this, "meandifference", Double.POSITIVE_INFINITY);

        meanFCProperty = new SimpleDoubleProperty(this, "meanfc", Double.NaN);
        fcPValueProperty = new SimpleDoubleProperty(this, "fcpvalue", Double.NaN);
        fcTstatisticProperty = new SimpleDoubleProperty(this, "fctstatistic", Double.NaN);
        tempConcProperty = new SimpleDoubleProperty(this, "tempconc", Double.NaN);

        score = new SimpleDoubleProperty(this, "score", Double.NaN);

        abundancesOL = FXCollections.observableArrayList();
        abundancesTempRatioOL = FXCollections.observableArrayList();
        abundancesTempRatioNormalisedOL = FXCollections.observableArrayList();
    }

    @Override
    public Double getScore() {
        return this.score.getValue();
    }

    @Override
    public void setScore(double value) {
        this.score.set(value);
    }

    @Override
    public String getDescription() {
        return this.description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String getAccession() {
        return this.accession;
    }

    public void setAccession(String accession) {
        this.accession = accession;
    }

    @Override
    public String getGeneName() {
        return this.geneNameProperty.getValue();
    }

    public void setGeneName(String geneName) {
        this.geneNameProperty.set(geneName);
    }

    @Override
    public String getOrganismName() {
        return this.organismNameProperty.getValue();
    }

    public void setOrganismName(String organismName) {
        this.organismNameProperty.set(organismName);
    }

    @Override
    public boolean getSelected() {
        return selected.getValue();
    }

    @Override
    public void setSelected(boolean b) {
        this.selected.setValue(b);
    }

    @Override
    public Property<Boolean> selectedProperty() {
        return this.selected;
    }

    @Override
    public Double[][] getAbundancesConcRatioNormalised() {
        return new Double[0][];
    }

    @Override
    public void setPValue(Double pVal) {

    }

    @Override
    public Double[][] getAbundances() {
        return abundances;
    }

    public void setAbundances(Double[][] abundances) {
        this.abundances = abundances;
    }

    public void calculateRatios(ObservableList<String> repLabels) {
        //normalise();

        abundancesTempRatio = new Double[1][repLabels.size()];
    }

}



