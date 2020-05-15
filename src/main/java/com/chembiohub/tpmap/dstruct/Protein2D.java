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
package com.chembiohub.tpmap.dstruct;

import com.chembiohub.tpmap.normalisation.TPNormalisation;
import com.chembiohub.tpmap.scoring.TP2dDestabilisationScorer;
import com.chembiohub.tpmap.scoring.TP2dStabilisationScorer;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import static com.chembiohub.tpmap.scoring.TP2dMeanFC.TPP2dMeanFC;

import java.util.HashMap;

/**
 *
 * The Protein class defines an individual protein, including it's accession, name,
 * organism name, description.
 *
 * This class also holds 2x2 matrices for each Replicate x TMT for:
 *  - Absolute values
 *  - Relative values
 *  - Relative values normalised
 *
 *  TP-MAP scores and P-values are stored for the protein.
 *
 * @author felixfeyertag
 */
public class Protein2D implements Protein {
    private String accession;
    private String description;
    
    private TPNormalisation.Normalisation normalisationMethod;
    
    private Double[] concReference;
    private Double[] tempReference;

    private Double[][] abundances;
    private Double[][] abundancesConcRatioNormalised;
    private Double[][] abundancesConcRatio;
    
    private Double[][] abundancesTempRatioNormalised;
    private Double[][] abundancesTempRatio;
    
    private final DoubleProperty stabilityScore;
    private final DoubleProperty destabilityScore;
    private final DoubleProperty score;
    private final DoubleProperty meanFCScore;
    private final DoubleProperty pValue;
    private final StringProperty effect;
    
    private final BooleanProperty selected;
    
    private final StringProperty organismNameProperty;
    private final StringProperty organismIdentifierProperty;
    private final StringProperty geneNameProperty;
    private final StringProperty proteinExistenceProperty;
    private final StringProperty sequenceVersionProperty;
    
    private final ObservableList<DoubleProperty> meanDifferencePropertyList;
    
    //>db|UniqueIdentifier|EntryName ProteinName
    //OS=OrganismName OX=OrganismIdentifier [GN=GeneName ]PE=ProteinExistence SV=SequenceVersion
    
    private final ObservableList<ObservableList<Double>> abundancesOL;
    private ObservableList<ObservableList<Double>> abundancesConcRatioOL;
    private ObservableList<ObservableList<Double>> abundancesConcRatioNormalisedOL;
    
    private ObservableList<ObservableList<Double>> abundancesTempRatioOL;
    private ObservableList<ObservableList<Double>> abundancesTempRatioNormalisedOL;
    private HashMap<String,double[]> curveFitParams;

    /**
     *
     */
    public Protein2D() {

        normalisationMethod = TPNormalisation.Normalisation.NONE;
        abundancesOL = FXCollections.observableArrayList();
        abundancesConcRatioOL = FXCollections.observableArrayList();
        abundancesConcRatioNormalisedOL = FXCollections.observableArrayList();
        abundancesTempRatioOL = FXCollections.observableArrayList();
        abundancesTempRatioNormalisedOL = FXCollections.observableArrayList();
        this.selected = new SimpleBooleanProperty(this, "selected", false);
        
        organismNameProperty = new SimpleStringProperty(this, "organismname", "");
        organismIdentifierProperty = new SimpleStringProperty(this, "organismidentifier", "");
        geneNameProperty = new SimpleStringProperty(this, "genename", "");
        proteinExistenceProperty = new SimpleStringProperty(this, "proteinexistence", "");
        sequenceVersionProperty = new SimpleStringProperty(this, "sequeneversion", "");
        //meanDifferencePropertyList = new SimpleDoubleProperty(this, "meandifference", Double.POSITIVE_INFINITY);
        meanDifferencePropertyList = FXCollections.observableArrayList();
        stabilityScore = new SimpleDoubleProperty();
        destabilityScore = new SimpleDoubleProperty();
        score = new SimpleDoubleProperty();
        meanFCScore = new SimpleDoubleProperty();
        pValue = new SimpleDoubleProperty();
        effect = new SimpleStringProperty();
    }

    public HashMap<String, double[]> getCurveFitParams() {
        return curveFitParams;
    }

    public ObservableList<ObservableList<Double>> getAbundancesOL() {
        return abundancesOL;
    }
    
    public ObservableList<ObservableList<Double>> getAbundancesConcRatioOL() {
        return abundancesConcRatioOL;
    }
    
    public ObservableList<ObservableList<Double>> getAbundancesConcRatioNormalisedOL() {
        return abundancesConcRatioNormalisedOL;
    }
    
    public ObservableList<ObservableList<Double>> getAbundancesTempRatioOL() {
        return abundancesTempRatioOL;
    }
    
    public ObservableList<ObservableList<Double>> getAbundancesTempRatioNormalisedOL() {
        return abundancesTempRatioNormalisedOL;
    }
    
    public void setAccession(String accession) {
        this.accession = accession;
    }
    
    public String getAccession() {
        return accession;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }

    /**
     *
     * @param normalisation Normalisation method
     */
    public void setNormalisationMethod(TPNormalisation.Normalisation normalisation) {
        this.normalisationMethod = normalisation;

        calculateRatios();
    }

    /**
     *
     * @return Normalisation method
     */
    public TPNormalisation.Normalisation getNormalisationMethod() {
        return normalisationMethod;
    }

    /**
     *
     * @param concReference Array containing concentration references
     */
    public void setConcReference(Double[] concReference) {
        this.concReference = concReference;
    }
    
    public Double[] getConcReference() {
        return concReference;
    }
    
    public void setTempReference(Double[] tempReference) {
        this.tempReference = tempReference;
    }
    
    public Double[] getTempReference() {
        return tempReference;
    }
    
    public void setAbundances(Double[][] abundances, ObservableList<String> tempLabels, ObservableList<String> repLabels) {
        if(this.abundances==null) {
            this.abundances = abundances;
            abundancesConcRatioOL = FXCollections.observableArrayList();
            abundancesTempRatioOL = FXCollections.observableArrayList();
            for(Double[] a : abundances) {
                abundancesConcRatioOL.add(FXCollections.observableArrayList(a));
            }
            for(int i=0; i<abundances[0].length; i++) {
                Double[] ab = new Double[abundances.length];
                for (int j=0; j<abundances.length; j++) {
                    ab[j] = abundances[j][i];
                }
                abundancesTempRatioOL.add(FXCollections.observableArrayList(ab));
            }
            calculateRatios();
        }
        else {
            for(int i=0;i<abundances.length;i++) {
                for(int j=0;j<abundances[0].length;j++) {
                    this.abundances[i][j] = abundances[i][j];
                    abundancesConcRatioOL.get(i).set(j, abundances[i][j]);
                }
            }
        }
    }
    
    public Double[][] getAbundances() {
        return abundances;
    }
    
    public Double[][] getAbundancesConcRatio() {
        return abundancesConcRatio;
    }
    
    public Double[][] getAbundancesConcRatioNormalised() {
        return abundancesConcRatioNormalised;
    }
    
    public void setPValue(Double pValue) {
        this.pValue.set(pValue);
    }
    
    public Double getPValue() {
        return this.pValue.get();
    }
    
    public DoubleProperty getPValueProperty() {
        return this.pValue;
    }
    
    public Double getStabilityScore() {
        return stabilityScore.get();
    }

    public Double getDestabilityScore() {
        return destabilityScore.get();
    }

    public Double getScore() {
        return score.get();
    }

    public Double getMeanFCScore() {
        return meanFCScore.get();
    }
    
    private void calculateRatios() {
        abundancesConcRatio = new Double[abundances.length][abundances[0].length];
        for(int i=0;i<abundancesConcRatio.length;i++) {
            for(int j=0;j<abundancesConcRatio[0].length;j++) {
                try {
                    abundancesConcRatio[i][j] = abundances[i][j]/concReference[i];
                }
                catch (NullPointerException e) {
                    abundancesConcRatio[i][j] = Double.NaN;
                }
            }
        }
        abundancesConcRatioOL = FXCollections.observableArrayList();
        for(Double[] ar : abundancesConcRatio) {
            abundancesConcRatioOL.add(FXCollections.observableArrayList(ar));
        }
        
        abundancesTempRatio = new Double[abundances[0].length][abundances.length];
        for(int i=0;i<abundancesTempRatio.length;i++) {
            for(int j=0;j<abundancesTempRatio[0].length;j++) {
                abundancesTempRatio[i][j] = abundances[j][i]/tempReference[i];
            }
        }
        abundancesTempRatioOL = FXCollections.observableArrayList();
        for(Double[] ar : abundancesTempRatio) {
             abundancesTempRatioOL.add(FXCollections.observableArrayList(ar));
        }
        
        normalise();
    }
    
    private void normalise() {
        switch(normalisationMethod) {
            case MEDIAN:
                break;
            default:
                abundancesConcRatioNormalised = abundancesConcRatio;
                abundancesConcRatioNormalisedOL = abundancesConcRatioOL;
                abundancesTempRatioNormalised = abundancesTempRatio;
                abundancesTempRatioNormalisedOL = abundancesTempRatioOL;
                break;
        }
    }
    
    public void updateScores(double minThreshold, double maxThreshold) {
        double meanFC = TPP2dMeanFC(this);
        meanFCScore.set(meanFC);
        double stability = TP2dStabilisationScorer.TPP2dStabilisationScorer(this,maxThreshold);
        double destability = (TP2dDestabilisationScorer.TPP2dDestabilisationScorer(this,minThreshold));
        setStabilityScore(stability);
        setDestabilityScore(destability);
        setScore(stability-destability);
        meanFCScore.set(TPP2dMeanFC(this));
        updateEffect(minThreshold, maxThreshold);
    }

    private void setStabilityScore(double score) {
        stabilityScore.set(score);
    }

    private void setDestabilityScore(double score) {
        destabilityScore.set(score);
    }

    public void setScore(double value) {
        score.set(value);
    }

    private void updateEffect(double minThreshold, double maxThreshold) {
        boolean solubilityEffect = false;
        if(this.getScore() < 0) {
            for(Double ratio : abundancesConcRatioNormalised[0]) {
                if(ratio < minThreshold) {
                    solubilityEffect = true;
                }
            }
            String effect = solubilityEffect ? "Solubility/Expression" : "Destabilized";
            this.effect.setValue(effect);
        }
        if(this.getScore() > 0) {
            for(Double ratio : abundancesConcRatioNormalised[0]) {
                if(ratio > maxThreshold) {
                    solubilityEffect = true;
                }
            }
            String effect = solubilityEffect ? "Solubility/Expression" : "Stabilized";
            this.effect.setValue(effect);
        }
    }

    public String getEffect() {
        return effect.getValue();
    }

    public void setAbundancesConcRatioNormalised(Double[][] abundancesConcRatioNormalised) {
        this.abundancesConcRatioNormalised = abundancesConcRatioNormalised;
        abundancesConcRatioNormalisedOL = FXCollections.observableArrayList();
        for(Double[] ar : abundancesConcRatioNormalised) {
            abundancesConcRatioNormalisedOL.add(FXCollections.observableArrayList(ar));
        }
    }
    
    public void setAbundancesTempRatioNormalised(Double[][] abundancesTempRatioNormalised) {
        this.abundancesTempRatioNormalised = abundancesTempRatioNormalised;
        abundancesTempRatioNormalisedOL = FXCollections.observableArrayList();
        for(Double[] ar : abundancesTempRatioNormalised) {
            abundancesTempRatioNormalisedOL.add(FXCollections.observableArrayList(ar));
        }
    }

    public boolean getSelected() {  
        return this.selected.get();  
    }
    
    public void setSelected(boolean selected) {  
        this.selected.set(selected);
    }
    
    public BooleanProperty selectedProperty() {  
        return selected ;  
    }
    
    public String getOrganismName() {
        return organismNameProperty.get();
    }
    
    public void setOrganismName(String organismName) {
        this.organismNameProperty.setValue(organismName);
    }
    
    public StringProperty organismNameProperty() {
        return organismNameProperty;
    }
    
    public String getOrganismIdentifier() {
        return organismIdentifierProperty.get();
    }
    
    public void setOrganismIdentifier(String organismIdentifier) {
        this.organismIdentifierProperty.setValue(organismIdentifier);
    }
    
    public StringProperty organismIdentifierProperty() {
        return organismIdentifierProperty;
    }

    public String getGeneName() {
        return geneNameProperty.get();
    }
    
    public void setGeneName(String geneName) {
        this.geneNameProperty.setValue(geneName);
    }
    
    public StringProperty geneNameProperty() {
        return geneNameProperty;
    }
    
    public String getProteinExistence() {
        return proteinExistenceProperty.get();
    }
    
    public void setProteinExistence(String proteinExistence) {
        this.proteinExistenceProperty.setValue(proteinExistence);
    }
    
    public StringProperty proteinExistenceProperty() {
        return proteinExistenceProperty;
    }
    
    public String getSequenceVersion() {
        return sequenceVersionProperty.get();
    }
    
    public void setSequenceVersion(String sequenceVersion) {
        this.sequenceVersionProperty.setValue(sequenceVersion);
    }
    
    public StringProperty sequenceVersionProperty() {
        return sequenceVersionProperty;
    }
    
    public Double getTopMeanDifference() {
        return meanDifferencePropertyList.get(meanDifferencePropertyList.size()-1).getValue();
    }

    public void addMeanDifference(Double dist) {
        this.meanDifferencePropertyList.add(new SimpleDoubleProperty(dist));
    }
    
    public ObservableList<DoubleProperty> getMeanDifferencePropertyList() {
        return meanDifferencePropertyList;
    }

    public Double getMinimum() {
        Double min = 1.0;
        for (Double[] abundancesConcRatioNormalised1 : abundancesConcRatioNormalised) {
            for (int j = 0; j<abundancesConcRatioNormalised[0].length; j++) {
                if (abundancesConcRatioNormalised1[j] < min) {
                    min = abundancesConcRatioNormalised1[j];
                }
            }
        }
        return min;
    }

    public Double getMaximum() {
        Double max = 1.0;
        for (Double[] abundancesConcRatioNormalised1 : abundancesConcRatioNormalised) {
            for (int j = 0; j<abundancesConcRatioNormalised[0].length; j++) {
                if (abundancesConcRatioNormalised1[j] > max) {
                    max = abundancesConcRatioNormalised1[j];
                }
            }
        }
        return max;
    }

}
