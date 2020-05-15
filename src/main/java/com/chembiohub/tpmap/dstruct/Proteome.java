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

import com.chembiohub.tpmap.analysis.TPBootstrapAnalysis;
import com.chembiohub.tpmap.dstruct.io.ImportWizard;
import com.chembiohub.tpmap.normalisation.TP1DMedianNormalisation;
import com.chembiohub.tpmap.normalisation.TP2DMedianNormalisation;
import com.chembiohub.tpmap.normalisation.TPNoNormalisation;
import com.chembiohub.tpmap.normalisation.TPNormalisation;
import java.util.Collections;
import java.util.Comparator;
import java.util.Spliterator;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.chembiohub.tpmap.scoring.TP1dScorer;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.TabPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

/**
 * The Proteome class holds an ObservableList of Protein objects, as well as experimental
 * parameters such as concentration and temperature labels. It can call the ImportWizard class
 * to populate the Proteome.
 *
 * In addition, this class performs functions over the collection of Protein objects, including
 * calling Normalisation classes to normalise the Proteome.
 *
 * @author felixfeyertag
 */
public class Proteome<T extends Protein> {

    // Collection of Protein objects
    private final ObservableList<T> proteins;

    // Experimental parameters
    public enum ExpType { TP1D, TP2D, PISA }
    private ExpType expType;
    private final ObservableList<String> tmt;
    private final ObservableList<String> replicates;
    private final StringProperty fileName;
    private final IntegerProperty proteinCount;
    private final IntegerProperty proteinSelected;
    private final ObservableList<String> concLabels;
    private final ObservableList<String> tempLabels;
    private final StringProperty taxonomy;

    // TP-MAP 2D thresholds
    private double minThreshold = 0.80;
    private double maxThreshold = 1.50;
    private double minPercentileThreshold = 0.20;
    private double maxPercentileThreshold = 0.80;

    // Bootstrap 2D parameters
    private final IntegerProperty bootstrapIterations;
    private TPBootstrapAnalysis bootstrapAnalysis;

    // 1D curve fit parameters
    private final IntegerProperty curveFitAttempts;
    private final IntegerProperty curveFitMaxIterations;

    // TP-MAP 1D score TM weight
    private double scoreTMWeight = 70.0;
    private boolean calculateCurves = true;

    // Normalisation parameters
    private TPNormalisation.Normalisation normalisationMethod;
    private final ObservableList<TPNormalisation> normalisation;
    private Double[][] medians;
    private final ObservableList<Double> maximums = FXCollections.synchronizedObservableList(FXCollections.observableArrayList());
    private final ObservableList<Double> minimums = FXCollections.synchronizedObservableList(FXCollections.observableArrayList());

    private final DoubleProperty minPVal = new SimpleDoubleProperty();

    private final ReadOnlyDoubleWrapper progress = new ReadOnlyDoubleWrapper();
    private final BooleanProperty isCancelled = new SimpleBooleanProperty(false);

    private boolean multithreading = true;


    // Parents
    private final Stage parentStage;
    private final TabPane tpTabPane;


    public Proteome(Stage parentStage, TabPane tpTabPane) {

        proteins = FXCollections.synchronizedObservableList(FXCollections.observableArrayList());
        tmt = FXCollections.synchronizedObservableList(FXCollections.observableArrayList());
        replicates = FXCollections.synchronizedObservableList(FXCollections.observableArrayList());
        normalisation = FXCollections.synchronizedObservableList(FXCollections.observableArrayList());
        normalisationMethod = TPNormalisation.Normalisation.NONE;

        fileName = new SimpleStringProperty();
        proteinCount = new SimpleIntegerProperty();
        proteinSelected = new SimpleIntegerProperty();

        tempLabels = FXCollections.synchronizedObservableList(FXCollections.observableArrayList());
        concLabels = FXCollections.synchronizedObservableList(FXCollections.observableArrayList());

        taxonomy = new SimpleStringProperty();

        bootstrapIterations = new SimpleIntegerProperty(0);

        curveFitAttempts = new SimpleIntegerProperty(0);
        curveFitMaxIterations = new SimpleIntegerProperty(0);

        this.tpTabPane = tpTabPane;
        this.parentStage = parentStage;

    }

    public Stage getParentStage() {
        return parentStage;
    }

    public TabPane getTabPane() {
        return tpTabPane;
    }

    public ExpType getExpType() {
        return expType;
    }

    public void setExpType(ExpType expType) {
        this.expType = expType;
    }
    
    public Double getUpperPercentile(double percentile) {
        return maximums.get((int) ((maximums.size()-1)*percentile));
    }

    public Double getLowerPercentile(double percentile) {
        return minimums.get((int) ((minimums.size() - 1) * percentile));
    }

    public ObservableList<T> getProteins() {
        return proteins;
    }
    
    public ObservableList<String> getTmt() {
        return tmt;
    }
    
    public ObservableList<String> getReplicates() {
        return replicates;
    }
    
    public void setFileName(String fileName) {
        this.fileName.set(fileName);
    }
    
    public StringProperty getFileName() {
        return fileName;
    }
    
    public IntegerProperty getProteinCountProperty() {
        return proteinCount;
    }
    
    public int getProteinCount() {
        return proteinCount.get();
    }
    
    public IntegerProperty getProteinSelectedProperty() {
        return proteinSelected;
    }
    
    public int getProteinSelected() {
        return proteinSelected.get();
    }

    public ObservableList<String> getConcLabels() {
        return concLabels;
    }

    public ObservableList<String> getTempLabels() {
        return tempLabels;
    }

    public Double[][] getMedians() {
        return medians;
    }
    
    public boolean importWizard(Stage parentStage, double min, double max) {
        
        ImportWizard wizard = new ImportWizard(parentStage,tpTabPane,min,max);
        Thread wizardThread = new Thread(wizard);

        wizardThread.run();

        try {
            wizardThread.join();
        } catch (InterruptedException ex) {
            Logger.getLogger(Proteome.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        this.clear();
        
        Proteome exp = wizard.getTppExperiment();

        clone(exp);

        return proteinCount.get()>0;

    }

    private void clone(Proteome exp) {

        this.setConcLabels(exp.getConcLabels());
        this.setTempLabels(exp.getTempLabels());
        this.setFileName(exp.getFileName().get());
        this.setTaxonomy(exp.getTaxonomy());
        this.setExpType(exp.getExpType());
        this.setMinPVal(exp.getMinPVal());
        this.setBootstrapIterations(exp.getBootstrapIterations());
        this.setCurveFitAttempts(exp.getCurveFitAttempts());
        this.setCurveFitMaxIterations(exp.getCurveFitMaxIterations());
        this.set2dBootstrapAnalysis(exp.get2dBootstrapAnalysis());
        this.setMultithreading(exp.getMultithreading());
        this.normalisationMethod = exp.getNormalisationMethod();
        this.setIsCancelled(exp.getIsCancelled().get());
        this.setProgressProperty(exp.progressProperty());
        this.setPercentileThresholds(exp.getMinPercentileThreshold(), exp.getMaxPercentileThreshold());
        this.setScoreTMWeight(exp.getScoreTMWeight());
        this.setCalculateCurves(exp.getCalculateCurves());
        exp.getProteins().forEach((p) -> {
            assert p instanceof Protein;
            addProtein((T) p);
        });
        updateProteinCount();
    }

    public void addProtein(T protein) {

        proteins.add(protein);

        if(protein instanceof Protein2D) {
            Protein2D protein2d = (Protein2D) protein;
            minimums.add(protein2d.getMinimum());
            maximums.add(protein2d.getMaximum());
        }

    }

    public void updateProteinCount() {

        int selected = 0;
        selected = proteins.stream().filter(Protein::getSelected).map((_item) -> 1).reduce(selected, Integer::sum);
        this.proteinCount.set(proteins.size());
        this.proteinSelected.set(selected);
        minimums.clear();
        maximums.clear();

        for(Protein p : proteins) {
            if(p instanceof Protein2D) {
                minimums.add(((Protein2D)p).getMinimum());
                maximums.add(((Protein2D)p).getMaximum());
            }
        }

        Collections.sort(maximums);
        Collections.sort(minimums);
    }


    public Color getColour(Double abundanceRatio) {
        
        Color destab = Color.color(1.0,0.44313725,0.15686274509);
        Color median = Color.color(1.0, 0.92156862745, 0.51764705882);
        Color stab = Color.color(0.5725490196,0.81568627451,0.31372549019);

        Double min = minThreshold - 0.1;
        Double max = maxThreshold + 0.1;
        
        if (abundanceRatio<min) {
            return destab;
        }
        else if (abundanceRatio<1) {
            double percentage = (abundanceRatio - min) / (1.0 - min);
            double r = median.getRed() * percentage + destab.getRed() * (1.0-percentage);
            double g = median.getGreen() * percentage + destab.getGreen() * (1.0-percentage);
            double b = median.getBlue() * percentage + destab.getBlue() * (1.0-percentage);
            return Color.color(r, g, b);
        }
        else if(abundanceRatio<max) {
            double percentage = (abundanceRatio - 1.0) / (max - 1.0);
            double r = stab.getRed() * percentage + median.getRed() * (1.0-percentage);
            double g = stab.getGreen() * percentage + median.getGreen() * (1.0-percentage);
            double b = stab.getBlue() * percentage + median.getBlue() * (1.0-percentage);
            return Color.color(r, g, b);
        }
        else if(abundanceRatio!=null) {
            return stab;
        }
        else {
            return Color.color(1.0,1.0,1.0);
        }

    }

    /**
     * setNormalisation normalises proteins and then sets 2D scores based on percentile FC
     *
     * @param norm Normalisation type
     */
    public void setNormalisation(TPNormalisation.Normalisation norm) {

        normalisationMethod = norm;
        ObservableList<T> nProteins = FXCollections.synchronizedObservableList(FXCollections.observableArrayList());
        final int proteinCount = proteins.size();
        final AtomicInteger proteinCounter = new AtomicInteger(0);

        switch(norm) {

            case MEDIAN:

                assert proteins != null;
                Protein t = proteins.get(0);

                if (t instanceof Protein1D) {
                    /*ObservableList<Protein1D> nProteins0 = FXCollections.synchronizedObservableList(FXCollections.observableArrayList());
                    //TP1DMedianNormalisation medianNormalisation = new TP1DMedianNormalisation();
                    //medianNormalisation.initMedianNormalisation((ObservableList<Protein>) proteins);
                    Stream<T> proteinStream = StreamSupport.stream(proteins::spliterator, Spliterator.ORDERED, multithreading);

                    if(calculateCurves) {

                        proteinStream.forEach((p) -> {
                            if (isCancelled.get()) {
                                return;
                            }
                            if (p != null) {
                                Protein1D p1 = (Protein1D) p;  // medianNormalisation.normalise(p);
                                p1.updateScores(this.getTempLabels(), this.getConcLabels());
                                nProteins0.add(p1);
                                progress.set(1.0 * proteinCounter.addAndGet(1) / proteinCount);
                            }
                        });
                        calculateCurves = false;
                    }
                    else {
                        proteinStream.forEach( p -> {
                            nProteins0.add((Protein1D)p);
                        });
                    }

                    TP1dScorer scorer = new TP1dScorer(nProteins0, scoreTMWeight);

                    nProteins0.forEach(p -> {
                        if(isCancelled.get()) {
                            return;
                        }
                        double score = scorer.calculateScore(p);
                        p.setScore(score);
                        nProteins.add((T) p);
                    });*/
                }
                else if (t instanceof Protein2D) {

                    // Normalise and place in temporary protein list nProteins0
                    ObservableList<Protein2D> nProteins0 = FXCollections.synchronizedObservableList(FXCollections.observableArrayList());
                    TP2DMedianNormalisation medianNormalisation = new TP2DMedianNormalisation();
                    maximums.clear();
                    minimums.clear();
                    medianNormalisation.initMedianNormalisation(proteins);

                    proteins.forEach((p) -> {
                        if(isCancelled.get()) {
                            return;
                        }
                        Protein2D p2 = (Protein2D) medianNormalisation.normalise(p);
                        maximums.add(p2.getMaximum());
                        minimums.add(p2.getMinimum());
                        nProteins0.add(p2);
                        progress.set(0.5 * proteinCounter.addAndGet(1) / proteinCount);
                    });

                    Collections.sort(maximums);
                    Collections.sort(minimums);
                    this.minThreshold = minimums.get((int) ((minimums.size() - 1) * minPercentileThreshold));
                    this.maxThreshold = maximums.get((int) ((maximums.size() - 1) * maxPercentileThreshold));

                    // Update scores based on new thresholds
                    nProteins0.forEach((p) -> {
                        if(isCancelled.get()) {
                            return;
                        }
                        p.updateScores(this.minThreshold, this.maxThreshold);
                        progress.set(0.5 + 0.5 * proteinCounter.addAndGet(1) / proteinCount);
                    });

                    nProteins0.sort(Comparator.comparingDouble(Protein2D::getMeanFCScore).reversed());

                    nProteins0.forEach((p) -> nProteins.add((T)p));
                }
                break;

            case NONE:

                assert proteins != null;
                Protein t2 = proteins.get(0);
                TPNoNormalisation noNormalisation = new TPNoNormalisation();

                if (t2 instanceof Protein1D) {
                    ObservableList<Protein1D> nProteins0 = FXCollections.synchronizedObservableList(FXCollections.observableArrayList());
                    Stream<T> proteinStream = StreamSupport.stream(proteins::spliterator, Spliterator.ORDERED, multithreading);

                    if(calculateCurves) {

                        proteinStream.forEach((p) -> {
                            if (isCancelled.get()) {
                                return;
                            }
                            if (p != null) {
                                Protein1D p1 = (Protein1D) noNormalisation.normalise(p);
                                p1.updateScores(this.getTempLabels(), this.getConcLabels());
                                nProteins0.add(p1);
                                progress.set(1.0 * proteinCounter.addAndGet(1) / proteinCount);
                            }
                        });
                        calculateCurves = false;
                    }
                    else {
                        proteinStream.forEach( p -> {
                            nProteins0.add((Protein1D)p);
                        });
                    }

                    TP1dScorer scorer = new TP1dScorer(nProteins0, scoreTMWeight);

                    nProteins0.forEach(p -> {
                        if(isCancelled.get()) {
                            return;
                        }
                        double score = scorer.calculateScore(p);
                        p.setScore(score);
                        nProteins.add((T) p);
                    });
                }
                else if (t2 instanceof Protein2D) {

                    ObservableList<Protein2D> nProteins0 = FXCollections.synchronizedObservableList(FXCollections.observableArrayList());
                    maximums.clear();
                    minimums.clear();

                    proteins.forEach((p) -> {
                        if(isCancelled.get()) {
                            return;
                        }
                        Protein2D p2 = (Protein2D) noNormalisation.normalise(p);
                        maximums.add(p2.getMaximum());
                        minimums.add(p2.getMinimum());
                        nProteins0.add(p2);
                        progress.set(0.5 * proteinCounter.addAndGet(1) / proteinCount);
                    });

                    Collections.sort(maximums);
                    Collections.sort(minimums);
                    this.minThreshold = minimums.get((int) ((minimums.size() - 1) * minPercentileThreshold));
                    this.maxThreshold = maximums.get((int) ((maximums.size() - 1) * maxPercentileThreshold));

                    nProteins0.forEach((p) -> {
                        if(isCancelled.get()) {
                            return;
                        }
                        p.updateScores(this.minThreshold, this.maxThreshold);
                        progress.set(0.5 + 0.5 * proteinCounter.addAndGet(1) / proteinCount);
                    });

                    nProteins0.sort(Comparator.comparingDouble(Protein2D::getMeanFCScore).reversed());

                    nProteins0.forEach((p) -> nProteins.add((T)p));
                }
                break;

            //case NONE:

            //    assert proteins != null;
            //    Protein t2 = proteins.get(0);
            //    TPNoNormalisation noNormalisation = new TPNoNormalisation();

            //    proteins.forEach(p -> {

            //        if(isCancelled.get()) {
            //            return;
            //        }

            //        if (p instanceof Protein1D) {
            //            /*ObservableList<Protein1D> nProteins0 = FXCollections.synchronizedObservableList(FXCollections.observableArrayList());
            //            Protein1D p1 = (Protein1D) noNormalisation.normalise(p);
            //            p1.updateScores(this.getTempLabels(), this.getConcLabels());
            //            nProteins.add((T) p1);
            //            TP1dScorer scorer = new TP1dScorer(nProteins0, scoreTMWeight);
            //    proteins.forEach(p -> {

            //        if(isCancelled.get()) {
            //            return;
            //        }

            //        if (p instanceof Protein1D) {
            //            /*ObservableList<Protein1D> nProteins0 = FXCollections.synchronizedObservableList(FXCollections.observableArrayList());
            //            Protein1D p1 = (Protein1D) noNormalisation.normalise(p);
            //            p1.updateScores(this.getTempLabels(), this.getConcLabels());
            //            nProteins.add((T) p1);
            //            TP1dScorer scorer = new TP1dScorer(nProteins0, scoreTMWeight);
            //            nProteins0.forEach(protein -> {
            //                Protein1D p2 = (Protein1D) p;
            //                p2.setScore(scorer.calculateScore((Protein1D)p));
            //                nProteins.add((T) p2);
            //                progress.set(1.0 * proteinCounter.addAndGet(1) / proteinCount);
            //            });*/
            //
            //
            //            ////
            //            ObservableList<Protein1D> nProteins0 = FXCollections.synchronizedObservableList(FXCollections.observableArrayList());
            //            //TP1DMedianNormalisation medianNormalisation = new TP1DMedianNormalisation();
            //            //medianNormalisation.initMedianNormalisation((ObservableList<Protein>) proteins);
            //            Stream<T> proteinStream = StreamSupport.stream(proteins::spliterator, Spliterator.ORDERED, multithreading);

            //            if(calculateCurves) {

            //                proteinStream.forEach((p) -> {
            //                    if (isCancelled.get()) {
            //                        return;
            //                    }
            //                    if (p != null) {
            //                        Protein1D p1 = (Protein1D) p;  // medianNormalisation.normalise(p);
            //                        p1.updateScores(this.getTempLabels(), this.getConcLabels());
            //                        nProteins0.add(p1);
            //                        progress.set(1.0 * proteinCounter.addAndGet(1) / proteinCount);
            //                    }
            //                });
            //                calculateCurves = false;
            //            }
            //            else {
            //                proteinStream.forEach( p -> {
            //                    nProteins0.add((Protein1D)p);
            //                });
            //            }

            //            TP1dScorer scorer = new TP1dScorer(nProteins0, scoreTMWeight);

            //            nProteins0.forEach(p -> {
            //                if(isCancelled.get()) {
            //                    return;
            //                }
            //                double score = scorer.calculateScore(p);
            //                p.setScore(score);
            //                nProteins.add((T) p);
            //            });
            //        }

            //        if (p instanceof Protein2D) {
            //            this.minThreshold = minimums.get((int) ((minimums.size() - 1) * minPercentileThreshold));
            //            this.maxThreshold = maximums.get((int) ((maximums.size() - 1) * maxPercentileThreshold));
            //            Protein2D p2 = (Protein2D) noNormalisation.normalise(p);
            //            p2.updateScores(this.minThreshold, this.maxThreshold);
            //            nProteins.add((T) p2);
            //            progress.set(1.0 * proteinCounter.addAndGet(1) / proteinCount);
            //        }
            //    });

            //    break;
        }

        proteins.clear();
        proteins.addAll(nProteins);

        proteins.sort(Comparator.comparingDouble(T::getScore).reversed());

    }

    public TPNormalisation.Normalisation getNormalisationMethod() {
        return normalisationMethod;
    }
    
    public void setConcLabels(ObservableList concLabels) {

        this.concLabels.clear();
        concLabels.forEach((cl) -> this.concLabels.add(cl.toString()));

    }
    
    public void setTempLabels(ObservableList tempLabels) {

        this.tempLabels.clear();
        tempLabels.forEach((tl) -> this.tempLabels.add(tl.toString()));

    }

    public void setTaxonomy(String taxonomy) {
        this.taxonomy.set(taxonomy);
    }
    
    public String getTaxonomy() {
        return taxonomy.get();
    }
    
    public void clear() {

        proteins.clear();
        tmt.clear();
        replicates.clear();
        fileName.set("");
        proteinCount.set(0);
        proteinSelected.set(0);
        concLabels.clear();
        tempLabels.clear();
        normalisation.clear();
        medians = null;
        taxonomy.set("");
        maximums.clear();
        minimums.clear();
        minPVal.set(0);
        bootstrapIterations.set(0);
        curveFitAttempts.set(0);
        curveFitMaxIterations.set(0);
        multithreading = false;
        normalisationMethod = TPNormalisation.Normalisation.NONE;

    }
    
    public void setBootstrapIterations(int i) {
        this.bootstrapIterations.set(i);
    }
    
    public int getBootstrapIterations() {
        return this.bootstrapIterations.get();
    }

    public void setCurveFitAttempts(int i) {
        this.curveFitAttempts.set(i);
    }

    public int getCurveFitAttempts() {
        return this.curveFitAttempts.get();
    }

    public void setCurveFitMaxIterations(int i) {
        this.curveFitMaxIterations.set(i);
    }

    public int getCurveFitMaxIterations() {
        return this.curveFitMaxIterations.get();
    }

    public void setMultithreading(boolean value) {
        multithreading = value;
    }

    public boolean getMultithreading() {
        return multithreading;
    }

    public double getProgress() {
        return progress.get();
    }

    public ReadOnlyDoubleProperty progressProperty() {
        return progress;
    }

    public void setProgressProperty(ReadOnlyDoubleProperty progressProperty) {
        this.progress.setValue(progressProperty.getValue());
    }

    public BooleanProperty getIsCancelled() {
        return isCancelled;
    }

    public void setIsCancelled(boolean cancelled) {
        this.isCancelled.set(cancelled);
    }

    public void set2dBootstrapAnalysis(TPBootstrapAnalysis bootstrapAnalysis) {
        this.bootstrapAnalysis = bootstrapAnalysis;
    }

    private TPBootstrapAnalysis get2dBootstrapAnalysis() {
        return bootstrapAnalysis;
    }

    public void setMinPVal(Double pVal) {
        this.minPVal.setValue(pVal);
    }

    public Double getMinPVal() {
        return this.minPVal.getValue();
    }

    public void setPercentileThresholds(double minPercentileThreshold, double maxPercentileThreshold) {
        this.minPercentileThreshold = minPercentileThreshold;
        this.maxPercentileThreshold = maxPercentileThreshold;
    }

    public double getMinPercentileThreshold() {
        return minPercentileThreshold;
    }

    public double getMaxPercentileThreshold() {
        return maxPercentileThreshold;
    }

    public void setScoreTMWeight(double scoreTMWeight) {
        this.scoreTMWeight = scoreTMWeight;
    }

    public double getScoreTMWeight() {
        return scoreTMWeight;
    }

    public void setCalculateCurves(boolean calculateCurves) {
        this.calculateCurves = calculateCurves;
    }

    public boolean getCalculateCurves() {
        return calculateCurves;
    }

    public void resetPVals() {

        if (expType != ExpType.TP2D) {
            Logger.getLogger(Proteome.class.getName()).log(Level.WARNING, null, "Proteome::resetPVals called on invalid experiment type: " + expType.toString());
        }
        else if (bootstrapAnalysis != null) {
            bootstrapAnalysis.setBootstrapPVals((Proteome<Protein>)this);
        }

    }
}
