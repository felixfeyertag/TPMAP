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
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.apache.commons.math3.analysis.ParametricUnivariateFunction;
import org.apache.commons.math3.analysis.function.Logistic;
import org.apache.commons.math3.fitting.SimpleCurveFitter;
import org.apache.commons.math3.fitting.WeightedObservedPoint;
import org.apache.commons.math3.util.FastMath;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import java.awt.*;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Protein1DLogistic
 *
 * The Protein1D class defines an individual protein, including it's accession, name,
 * organism name, description.
 *
 * This class also holds 2x2 matrices for each Replicate x TMT for:
 *  - Absolute values
 *  - Relative values
 *  - Relative values normalised
 *
 *  TP-MAP scores and P-values are stored for the protein.
 *
 * @deprecated This class uses a 6-parameter Logistic curve fit, this has now been depreciated in favour of a TP1dDenaturationFunction in Protein1D
 *
 * @author felixfeyertag
 */
@Deprecated
public class Protein1DLogistic implements Protein {
    private String accession;
    private String description;

    private TPNormalisation.Normalisation normalisationMethod;

    private Double[] tempReference;
    private Double[] temperatures;

    private Double[][] abundances;

    private final BooleanProperty selected;

    private final StringProperty organismNameProperty;
    private final StringProperty organismIdentifierProperty;
    private final StringProperty geneNameProperty;
    private final StringProperty proteinExistenceProperty;
    private final StringProperty sequenceVersionProperty;

    private final DoubleProperty meanDifferenceProperty;

    private final ObservableList<ObservableList<Double>> abundancesOL;

    private ObservableList<ObservableList<Double>> abundancesTempRatioOL;
    private ObservableList<ObservableList<Double>> abundancesTempRatioNormalisedOL;
    private Double[][] abundancesTempRatio;
    private Double[][] abundancesTempRatioNormalised;

    private final IntegerProperty attempts;
    private final IntegerProperty maxIterations;

    private double[][] curveFitParams;

    private final DoubleProperty tmv1Property;
    private final DoubleProperty tmv2Property;
    private final DoubleProperty tmt1Property;
    private final DoubleProperty tmt2Property;
    private final DoubleProperty tmVT1Property;
    private final DoubleProperty tmVT2Property;
    private final DoubleProperty tmVVProperty;
    private final DoubleProperty meanTMProperty;
    private final BooleanProperty curveShiftSameDirectionProperty;
    private final BooleanProperty deltaVTgtDeltaVVProperty;

    private final DoubleProperty rmsev1Property;
    private final DoubleProperty rmsev2Property;
    private final DoubleProperty rmset1Property;
    private final DoubleProperty rmset2Property;
    private final DoubleProperty vRepProperty;
    private final DoubleProperty tRepProperty;

    private final DoubleProperty score;

    /**
     *
     */
    public Protein1DLogistic() {

        curveShiftSameDirectionProperty = new SimpleBooleanProperty(this, "curvesfhiftsamedirectionproperty", Boolean.FALSE);
        deltaVTgtDeltaVVProperty        = new SimpleBooleanProperty(this, "deltavtgtdeltavvproeprty"        , Boolean.FALSE);
        tmv1Property = new SimpleDoubleProperty(this, "tmv1property", Double.NaN);
        tmv2Property = new SimpleDoubleProperty(this, "tmv2property", Double.NaN);
        tmt1Property = new SimpleDoubleProperty(this, "tmt1property", Double.NaN);
        tmt2Property = new SimpleDoubleProperty(this, "tmt2property", Double.NaN);
        tmVT1Property  = new SimpleDoubleProperty(this, "tmvt1property" , Double.NaN);
        tmVT2Property  = new SimpleDoubleProperty(this, "tmvt2property" , Double.NaN);
        tmVVProperty   = new SimpleDoubleProperty(this, "tmvvproperty"  , Double.NaN);
        meanTMProperty = new SimpleDoubleProperty(this, "meantmproperty", Double.NaN);
        rmsev1Property     = new SimpleDoubleProperty(this, "rmsev1property", Double.NaN);
        rmsev2Property     = new SimpleDoubleProperty(this, "rmsev2property", Double.NaN);
        rmset1Property     = new SimpleDoubleProperty(this, "rmset1property", Double.NaN);
        rmset2Property     = new SimpleDoubleProperty(this, "rmset2property", Double.NaN);
        vRepProperty = new SimpleDoubleProperty(this, "vrepproperty", Double.NaN);
        tRepProperty = new SimpleDoubleProperty(this, "trepproperty", Double.NaN);
        score = new SimpleDoubleProperty(this, "score", Double.NaN);

        attempts = new SimpleIntegerProperty(1);
        maxIterations = new SimpleIntegerProperty(1);

        normalisationMethod = TPNormalisation.Normalisation.NONE;
        abundancesOL = FXCollections.synchronizedObservableList(FXCollections.observableArrayList());
        abundancesTempRatioOL = FXCollections.synchronizedObservableList(FXCollections.observableArrayList());
        abundancesTempRatioNormalisedOL = FXCollections.synchronizedObservableList(FXCollections.observableArrayList());

        this.selected = new SimpleBooleanProperty(this, "selected", false);

        organismNameProperty = new SimpleStringProperty(this, "organismname", "");
        organismIdentifierProperty = new SimpleStringProperty(this, "organismidentifier", "");
        geneNameProperty = new SimpleStringProperty(this, "genename", "");
        proteinExistenceProperty = new SimpleStringProperty(this, "proteinexistence", "");
        sequenceVersionProperty = new SimpleStringProperty(this, "sequeneversion", "");
        meanDifferenceProperty = new SimpleDoubleProperty(this, "meandifference", Double.POSITIVE_INFINITY);

    }

    private void calculateTM() {

        double t1 = temperatures[0];
        double t2 = temperatures[temperatures.length-1];

        if(curveFitParams[0]!=null) {
            tmv1Property.set(calculateTM(curveFitParams[0], t1, t2));
        }
        if(curveFitParams[1]!=null) {
            tmv2Property.set(calculateTM(curveFitParams[1], t1, t2));
        }
        if(curveFitParams[2]!=null) {
            tmt1Property.set(calculateTM(curveFitParams[2], t1, t2));
        }
        if(curveFitParams[3]!=null) {
            tmt2Property.set(calculateTM(curveFitParams[3], t1, t2));
        }

        calculateTMShifts();
        calculateReplication();
    }

    /**
     * calculateTM
     *
     * Calculates temperature at relative abundance = 0.5 to an accuracy of 0.0001
     *
     * @param parameters Logistic curve parameters
     * @param lowerT lower temperature threshold - converges to upperT
     * @param upperT upper temperature threshold - converges to lowerT
     * @return
     */
    private static Double calculateTM(double[] parameters, Double lowerT, Double upperT) {
        return calculateTM(parameters, lowerT, upperT, 0);
    }

    private static Double calculateTM(double[] parameters, Double lowerT, Double upperT, int counter) {

        double accuracy = 0.0001;

        if(parameters==null||parameters.length==0) {
            return Double.NaN;
        }

        assert 6==parameters.length;

        Double midT = lowerT + (upperT-lowerT) / 2.0;

        if(accuracy > upperT-lowerT) {
            return midT;
        }

        Logistic l = new Logistic(parameters[0],parameters[1],parameters[2],parameters[3],parameters[4],parameters[5]);

        double lowerY = l.value(lowerT);
        double midY = l.value(midT);
        double upperY = l.value(upperT);

        if(lowerY>0.5 && midY<0.5) {
            return calculateTM(parameters, lowerT, midT,counter+1);
        }

        if(midY>0.5 && upperY<0.5) {
            return calculateTM(parameters, midT, upperT,counter+1);
        }
        else {
            return Double.NaN;
        }

    }

    private static Double calculateRMSE(Double[] temps, Double[] values, double[] fitParams) {

        assert temps.length == values.length;

        if(fitParams==null || fitParams.length!=6) {
            return Double.NaN;
        }

        Logistic l = new Logistic(fitParams[0],fitParams[1],fitParams[2],fitParams[3],fitParams[4],fitParams[5]);

        double sumResidual = 0.0;
        int countResidual = 0;

        for(int i=0; i<values.length; i++) {
            if(Double.isFinite(values[i])&&Double.isFinite(temps[i])) {
                countResidual += 1;
                sumResidual += FastMath.pow(values[i] - l.value(temps[i]),2);
            }
        }

        if(countResidual==1) {
            return Double.NaN;
        }

        return FastMath.sqrt( sumResidual / (countResidual-1.0) );
    }

    /**
     * Fit a logistic curve.
     *
     * @param temps temperatures (x values)
     * @param values relative abundance (y values)
     * @param maxAttempts attempts to fit a curve, curve with the best RMSE fit is chosen
     * @param maxIterations maximum number of iterations for the curve fit optimization algorithm
     * @return double[8] with estimates for params k, m, b, q, a, n, and final two parameters for RMSE estimates
     */
    private static double[] curveFitter(Double[] temps, Double[] values, int maxAttempts, int maxIterations) {

        if(maxAttempts==0 || maxIterations==0) {
            return new double[0];
        }

        assert 0==temps.length-values.length;

        /**
         * k - If b > 0, value of the function for x going towards +∞. If b < 0, value of the function for x going towards -∞.
         * m - Abscissa of maximum growth.
         * b - Growth rate.
         * q - Parameter that affects the position of the curve along the ordinate axis.
         * a - If b > 0, value of the function for x going towards -∞. If b < 0, value of the function for x going towards +∞.
         * n - Parameter that affects near which asymptote the maximum growth occurs.
         */
        Double a = values[0];
        Double k = values[values.length-1];
        double b = 0.5;
        double n = 0.5;
        double q = 0.5;
        Double m = temps[temps.length/2];

        double[] estimates = new double[]{k,m,b,q,a,n};

        List<WeightedObservedPoint> weightedObservedPoints = new ArrayList<>();

        for(int i=0;i<temps.length;i++) {
            weightedObservedPoints.add(new WeightedObservedPoint(1.0,temps[i],values[i]));
        }

        ParametricUnivariateFunction logistic = new Logistic.Parametric();
        SimpleCurveFitter curveFitter = SimpleCurveFitter.create(logistic,estimates);
        curveFitter = curveFitter.withMaxIterations(maxIterations);

        int attempts = 0;

        double[] bestFit = new double[] {};
        double bestRMSE = -1;
        double sumRMSE = 0;

        Random rng = new Random(123);

        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        long time = timestamp.getTime();

        while(attempts<maxAttempts) {
            try {
                double[] fitParams = curveFitter.fit(weightedObservedPoints);
                Double rmse = calculateRMSE(temps, values, fitParams);
                if(fitParams[1]<0) {
                    throw new Exception();
                }
                if(rmse<bestRMSE || bestRMSE<0) {
                    bestRMSE = rmse;
                    bestFit = fitParams;
                }
                sumRMSE += rmse;
                b = rng.nextDouble();
                n = rng.nextDouble();
                q = rng.nextDouble();
                estimates = new double[]{k,m,b,q,a,n};
                curveFitter = SimpleCurveFitter.create(logistic,estimates);// curveFitter.withStartPoint(estimates);
                curveFitter = curveFitter.withMaxIterations(maxIterations);
                attempts++;
            } catch (Exception e) {
                attempts++;
                b = rng.nextDouble();
                n = rng.nextDouble();
                q = rng.nextDouble();
                estimates = new double[]{k,m,b,q,a,n};
                curveFitter = SimpleCurveFitter.create(logistic,estimates);// curveFitter.withStartPoint(estimates);
                curveFitter = curveFitter.withMaxIterations(maxIterations);
            }
        }

        if(bestFit.length==0) {
            return bestFit;
        }

        return new double[] { bestFit[0], bestFit[1], bestFit[2], bestFit[3], bestFit[4], bestFit[5], bestRMSE };
    }

    /**
     * Set thermal melting point shifts
     */
    private void calculateTMShifts() {
        tmVT1Property.setValue(tmv1Property.getValue() - tmt1Property.getValue());
        tmVT2Property.setValue(tmv2Property.getValue() - tmt2Property.getValue());
        tmVVProperty.setValue(tmv1Property.getValue() - tmv2Property.getValue());
        meanTMProperty.setValue((tmVT1Property.getValue() + tmVT2Property.getValue()) / 2.0);
        curveShiftSameDirectionProperty.setValue(tmVT1Property.getValue() >= 0.0 && tmVT2Property.getValue() >= 0.0 || tmVT1Property.getValue() < 0.0 && tmVT2Property.getValue() < 0.0);
        deltaVTgtDeltaVVProperty.setValue(tmVT1Property.getValue() > tmVVProperty.getValue() && tmVT2Property.getValue() > tmVVProperty.getValue());
    }

    /**
     * Calculates the mean difference between each of the data points for the V1V2 and T1T2 replicates, a lower value is better
     */
    private void calculateReplication() {

        double v12Rep = 0.0;
        double t12Rep = 0.0;

        for (int i = 0; i < abundancesTempRatio[0].length; i++) {
            t12Rep += FastMath.abs(FastMath.abs(abundancesTempRatio[0][i]) - FastMath.abs(abundancesTempRatio[1][i]));
        }

        for (int i = 0; i < abundancesTempRatio[0].length; i++) {
            v12Rep += FastMath.abs(FastMath.abs(abundancesTempRatio[2][i]) - FastMath.abs(abundancesTempRatio[3][i]));
        }

        this.vRepProperty.setValue(v12Rep);
        this.tRepProperty.setValue(t12Rep);
    }

    private double[][] getCurveFitParams() {
        return curveFitParams;
    }

    private void setCurveFitParams(double[][] curveFitParams) {
        this.curveFitParams = curveFitParams;
    }

    public JFreeChart createChart(ObservableList<String> tempLabels, ObservableList<String> repLabels) {

        Double[] xvalues = new Double[tempLabels.size()];
        for(int i=0; i<xvalues.length; i++) {
            xvalues[i] = Double.parseDouble(tempLabels.get(i));
        }
        Double[][] yvalues = getAbundancesTempRatio();
        double[][] fitparams = getCurveFitParams();

        XYSeriesCollection dataset = new XYSeriesCollection();

        int[] setSeries = new int[] { 0, 0, 0, 0 };

        for(int i=0; i<yvalues.length; i++) {
            if(yvalues[i]!=null && fitparams[i].length==6) {

                assert (0.0 == xvalues.length - yvalues[i].length);
                assert (6 == fitparams[i].length);
                XYSeries series1 = new XYSeries("Fit " + i);
                Logistic logistic = new Logistic(fitparams[i][0],fitparams[i][1],fitparams[i][2],fitparams[i][3],fitparams[i][4],fitparams[i][5]);

                for (Double x = xvalues[0]; x < xvalues[xvalues.length-1]; x+=0.2) {
                    Double y = logistic.value(x);
                    series1.add(x,y);
                }

                XYSeries series2 = new XYSeries("Point " + i);

                for(int j=0; j< xvalues.length; j++) {
                    series2.add(xvalues[j], yvalues[i][j]);
                }

                dataset.addSeries(series1);
                dataset.addSeries(series2);

                setSeries[i] = 2;
            }
            else if(yvalues[i]!=null && yvalues[i].length>0) {
                assert 0.0 == xvalues.length-yvalues[i].length;

                XYSeries series1 = new XYSeries("Fit " + i);
                XYSeries series2 = new XYSeries("Point " + i);

                for(int j=0; j< xvalues.length; j++) {
                    series2.add(xvalues[j], yvalues[i][j]);
                }

                dataset.addSeries(series1);
                dataset.addSeries(series2);
                setSeries[i] = 1;
            }
        }

        String chartTitle = getAccession();
        if(!getGeneName().isEmpty()) {
            chartTitle += " (" + getGeneName() + ")";
        }

        JFreeChart chart = ChartFactory.createXYLineChart(
                chartTitle,
                "Temperature (˚C)",
                "Relative Abundance",
                dataset,
                PlotOrientation.VERTICAL,
                true,
                true,
                false
        );

        XYPlot plot = (XYPlot) chart.getPlot();
        plot.setDomainPannable(true);
        plot.setRangePannable(true);
        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();

        int seriesCounter = 0;
        for(int i=0;i<setSeries.length;i++) {

            if(setSeries[i]==2) {

                renderer.setSeriesLinesVisible (seriesCounter,true );
                renderer.setSeriesShapesVisible(seriesCounter,false);
                renderer.setSeriesLinesVisible (seriesCounter+1,false);
                renderer.setSeriesShapesVisible(seriesCounter+1,true );

                switch(i) {
                    case 0:
                        renderer.setSeriesPaint(seriesCounter, Color.BLUE);
                        renderer.setSeriesPaint(seriesCounter+1, Color.BLUE);
                        break;
                    case 1:
                        renderer.setSeriesStroke(seriesCounter, new BasicStroke(
                                1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,
                                10.0f, new float[] {1.0f, 2.0f}, 0.0f
                        ));
                        renderer.setSeriesPaint(seriesCounter, Color.BLUE);
                        renderer.setSeriesPaint(seriesCounter+1, Color.BLUE);
                        break;
                    case 2:
                        renderer.setSeriesPaint(seriesCounter, Color.RED);
                        renderer.setSeriesPaint(seriesCounter+1, Color.RED);
                        break;
                    case 3:
                        renderer.setSeriesStroke(seriesCounter, new BasicStroke(
                                1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,
                                1.0f, new float[] {1.0f, 2.0f}, 0.0f
                        ));
                        renderer.setSeriesPaint(seriesCounter, Color.RED);
                        renderer.setSeriesPaint(seriesCounter+1, Color.RED);
                        break;
                }

                seriesCounter += 2;
            }
            else if(setSeries[i]==1) {

                renderer.setSeriesLinesVisible (seriesCounter+1,false);
                renderer.setSeriesShapesVisible(seriesCounter+1,true );

                switch(i) {
                    case 0:
                    case 1:
                        renderer.setSeriesPaint(seriesCounter+1, Color.BLUE);
                        break;
                    case 2:
                    case 3:
                        renderer.setSeriesPaint(seriesCounter+1, Color.RED);
                        break;
                }

                seriesCounter += 2;
            }
        }

        plot.setRenderer(renderer);

        return chart;
    }

    public ObservableList<ObservableList<Double>> getAbundancesOL() {
        return abundancesOL;
    }

    public Double[][] getAbundancesTempRatio() {
        return abundancesTempRatio;
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

        //calculateRatios(tempLabels, repLabels);
    }

    public void setTempReference(Double[] tempReference) {
        this.tempReference = tempReference;
    }

    public Double[] getTempReference() {
        return tempReference;
    }

    public void setAbundances(Double[][] abundances, ObservableList<String> tempLabels, ObservableList<String> repLabels) {
        this.temperatures = new Double[tempLabels.size()];
        for(int i=0; i<tempLabels.size(); i++) {
            this.temperatures[i] = Double.parseDouble(tempLabels.get(i));
        }
        if(this.abundances==null) {

            this.abundances = abundances;
            abundancesTempRatioOL = FXCollections.synchronizedObservableList(FXCollections.observableArrayList());

            for(int i=0; i<abundances[0].length; i++) {
                Double[] ab = new Double[abundances.length];
                for (int j=0; j<abundances.length; j++) {
                    ab[j] = abundances[j][i];
                }
                abundancesTempRatioOL.add(FXCollections.observableArrayList(ab));
            }
        }
        else {
            for(int i=0;i<abundances.length;i++) {
                System.arraycopy(abundances[i], 0, this.abundances[i], 0, abundances[0].length);
            }
        }
    }

    public Double[][] getAbundances() {
        return abundances;
    }


    public void calculateRatios() {

        normalise();

        abundancesTempRatio = new Double[abundances[0].length][abundances.length];
        for(int i=0;i<abundancesTempRatio.length;i++) {
            for(int j=0;j<abundancesTempRatio[0].length;j++) {
                abundancesTempRatio[i][j] = abundances[j][i]/tempReference[i];
            }
        }
        abundancesTempRatioOL = FXCollections.synchronizedObservableList(FXCollections.observableArrayList());
        for(Double[] ar : abundancesTempRatio) {
            abundancesTempRatioOL.add(FXCollections.observableArrayList(ar));
        }
    }

    private void normalise() {
        switch(normalisationMethod) {
            case MEDIAN:
                break;
            default:
                abundancesTempRatioNormalised = abundancesTempRatio;
                abundancesTempRatioNormalisedOL = abundancesTempRatioOL;
                break;
        }
    }

    public void updateScores(ObservableList<String> tempLabels,ObservableList<String> repLabels) {

        Double[] tempLabelArray = new Double[tempLabels.size()];
        for(int i=0;i<tempLabelArray.length;i++) {
            tempLabelArray[i] = Double.parseDouble(tempLabels.get(i));
        }

        double[][] curveFitParams   = new double[repLabels.size()][6];
        double[]   curveFitRMSE     = new double[repLabels.size()];

        long total = 0;

        for(int i = 0; i < getAbundancesTempRatioOL().size(); i++) {
            long t0 = System.currentTimeMillis();
            Double[] abTempRatio = new Double[getAbundancesTempRatioOL().get(0).size()];
            for(int j=0;j<abTempRatio.length;j++) {
                abTempRatio[j] = getAbundancesTempRatioOL().get(i).get(j);
            }

            double[] curveFit = curveFitter(tempLabelArray, abTempRatio, attempts.getValue(), maxIterations.getValue());

            if(curveFit.length==0) {
                curveFitParams[i] = curveFit;
                curveFitRMSE[i] = Double.NaN;
            }
            else {
                curveFitParams[i] = new double[]{curveFit[0], curveFit[1], curveFit[2], curveFit[3], curveFit[4], curveFit[5]};
                curveFitRMSE[i] = curveFit[6];
            }
            long t1 = System.currentTimeMillis()-t0;
            total += t1;
        }

        setCurveFitParams(curveFitParams);

        rmsev1Property.setValue(curveFitRMSE[0]);
        rmsev2Property.setValue(curveFitRMSE[1]);
        rmset1Property.setValue(curveFitRMSE[2]);
        rmset2Property.setValue(curveFitRMSE[3]);

        calculateTM();
    }

    public void setAbundancesTempRatioNormalised(Double[][] abundancesTempRatioNormalised) {

        this.abundancesTempRatioNormalised = abundancesTempRatioNormalised;
        abundancesTempRatioNormalisedOL = FXCollections.synchronizedObservableList(FXCollections.observableArrayList());
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

    @Override
    public Double[][] getAbundancesConcRatioNormalised() {
        throw new UnsupportedOperationException("getAbundancesConcRatioNormalised called for Protein1D");
    }

    @Override
    public void setPValue(Double pVal) {
        throw new UnsupportedOperationException("setPValue called for Protein1D");
    }

    public String getOrganismName() {
        return organismNameProperty.get();
    }

    public ObservableList<ObservableList<Double>> getAbundancesConcRatioNormalisedOL() {
        throw new UnsupportedOperationException("getAbundancesConcNormalisedRatioOL called on Protein1D");
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

    public Double getMeanDifference() {
        return meanDifferenceProperty.get();
    }

    public void setMeanDifference(Double dist) {
        this.meanDifferenceProperty.setValue(dist);
    }

    public DoubleProperty meanDifferenceProperty() {
        return meanDifferenceProperty;
    }

    public Double getTmv1() {
        return tmv1Property.getValue();
    }
    public Double getTmv2() {
        return tmv2Property.getValue();
    }
    public Double getTmt1() {
        return tmt1Property.getValue();
    }
    public Double getTmt2() {
        return tmt2Property.getValue();
    }
    public Double getTmVT1() {
        return tmVT1Property.getValue();
    }
    public Double getTmVT2() {
        return tmVT2Property.getValue();
    }
    public Double getTmVV() {
        return tmVVProperty.getValue();
    }
    public Double getMeanTM() {
        return meanTMProperty.getValue();
    }
    public Boolean getCurveShiftSameDirection() {
        return curveShiftSameDirectionProperty.getValue();
    }
    public Boolean getDeltaVTgtDeltaVV() {
        return deltaVTgtDeltaVVProperty.getValue();
    }

    public double getRmsemean() {
        if(rmsev1Property.getValue().isNaN() || rmsev2Property.getValue().isNaN() || rmset1Property.getValue().isNaN() || rmset2Property.getValue().isNaN()) {
            return Double.NaN;
        }
        else {
            return (rmsev1Property.getValue() + rmsev2Property.getValue() + rmset1Property.getValue() + rmset2Property.getValue()) / 4.0;
        }
    }

    public double getRmsev1() {
        return rmsev1Property.get();
    }

    public double getRmsev2() {
        return rmsev2Property.get();
    }

    public double getRmset1() {
        return rmset1Property.get();
    }

    public double getRmset2() {
        return rmset2Property.get();
    }

    public double getVRep() {
        return vRepProperty.get();
    }

    public double getTRep() {
        return tRepProperty.get();
    }

    public void setScore(double score) {
        this.score.set(score);
    }

    public Double getScore() {
        return score.getValue();
    }

    public Double getMinimum() {

        Double min = 1.0;
        for (Double[] abundancesConcRatioNormalised1 : abundancesTempRatioNormalised) {
            for (int j = 0; j<abundancesTempRatioNormalised[0].length; j++) {
                if (abundancesConcRatioNormalised1[j] < min) {
                    min = abundancesConcRatioNormalised1[j];
                }
            }
        }
        return min;

    }

    public Double getMaximum() {

        Double max = 1.0;
        for (Double[] abundancesConcRatioNormalised1 : abundancesTempRatioNormalised) {
            for (int j = 0; j<abundancesTempRatioNormalised[0].length; j++) {
                if (abundancesConcRatioNormalised1[j] > max) {
                    max = abundancesConcRatioNormalised1[j];
                }
            }
        }
        return max;

    }

    public Double getHcScore() {
        throw new UnsupportedOperationException("getStabilityScore called on Protein1D");
    }

    public Double[][] getAbundancesConcRatio() {
        return new Double[0][];
    }

    public void setTemperatures(Double[] temperatures) {
        this.temperatures = temperatures;
    }

    public Double[] getTemperatures() {
        return temperatures;
    }

    public void setAttempts(Integer maxAttempts) {
        this.attempts.setValue(maxAttempts);
    }

    public void setMaxIterations(Integer maxIterations) {
        this.maxIterations.setValue(maxIterations);
    }

    public int getAttempts() {
        return attempts.getValue();
    }

    public int getMaxIterations() {
        return maxIterations.getValue();
    }

}



