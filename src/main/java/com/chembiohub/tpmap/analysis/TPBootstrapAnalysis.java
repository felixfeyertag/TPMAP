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
package com.chembiohub.tpmap.analysis;

import com.chembiohub.tpmap.dstruct.Protein;
import com.chembiohub.tpmap.dstruct.Proteome;
import com.chembiohub.tpmap.scoring.TP2dDestabilisationScorer;
import com.chembiohub.tpmap.scoring.TP2dStabilisationScorer;
import javafx.beans.binding.DoubleExpression;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.stat.descriptive.SynchronizedSummaryStatistics;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * TPBootstrapAnalysis
 *
 * Runs a bootstrap analysis for 2D TP datasets. This method randomly permutes the dataset for a set number of
 * iterations to determine the distribution of scores, from which a p-value can be derived to determine how likely
 * a score is to be obtained by chance. The P-value is calculated by fitting a normal distribution to the obtained
 * scores and determining the cumulative probability.
 *
 * @author felixfeyertag
 */
public class TPBootstrapAnalysis {
    
    private final int iterations;

    private List<DoubleProperty> scores;
    private SynchronizedSummaryStatistics stats;
    private final DoubleProperty min;
    
    public TPBootstrapAnalysis(int iterations) {
        this.iterations = iterations;
        min = new SimpleDoubleProperty(1.0);
    }
    
    public void runBootstrapAnalysis(Proteome<Protein> exp) {

        int x = exp.getProteins().get(0).getAbundances().length;
        int y = exp.getProteins().get(0).getAbundances()[0].length;
        
        int count = exp.getProteins().size();

        ThreadLocalRandom ngr = ThreadLocalRandom.current();

        scores = Collections.synchronizedList(Stream.generate(SimpleDoubleProperty::new).limit(iterations).collect(Collectors.toList()));

        stats = new SynchronizedSummaryStatistics();

        try (Stream<DoubleProperty> nsStream = StreamSupport.stream(() -> scores.spliterator(), Spliterator.CONCURRENT, exp.getMultithreading())) {
            nsStream.forEach((DoubleProperty d) -> {
                Double[][] fcMatrix = new Double[x][y];
                for (int j=0; j<x; j++) {
                    for (int k=0; k<y; k++) {
                        int r = ngr.nextInt(0, count-1);
                        fcMatrix[j][k] = exp.getProteins().get(r).getAbundancesConcRatioNormalised()[j][k];
                    }
                }
                Double hc = TP2dStabilisationScorer.TPP2dStabilisationScorer(fcMatrix, 1);
                Double hd = TP2dDestabilisationScorer.TPP2dDestabilisationScorer(fcMatrix, 1);

                double score = hc - hd;

                stats.addValue(score);

                d.setValue(score);
            });
        }

        boolean bootstrapDistribution = false;

        if(bootstrapDistribution) {
            try {
                File f = new File("/tmp/bsdist");
                f.createNewFile();
                FileOutputStream fos = new FileOutputStream(f);
                scores.forEach((p) -> {
                    try {
                        fos.write(p.getValue().toString().getBytes());
                        fos.write("\n".getBytes());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
                fos.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
            System.exit(0);
        }

        scores.sort(Comparator.comparing(DoubleExpression::getValue));

    }

    public void setBootstrapPVals(Proteome<Protein> exp) {

        min.setValue(1.0);

        try(Stream<Protein> proteinStream = StreamSupport.stream(() -> exp.getProteins().spliterator(), Spliterator.ORDERED, exp.getMultithreading())) {
            proteinStream.forEach((p) -> {
                Double pVal = pVal(p);
                p.setPValue(pVal);
                if(min.getValue().compareTo(pVal) > 0 && pVal.compareTo(0.0) > 0) {
                    min.setValue(pVal);
                }
            });
        }

        exp.setMinPVal(min.getValue());

    }

    private Double pVal(Protein p) {

        NormalDistribution nd = new NormalDistribution(stats.getMean(), stats.getStandardDeviation());

        return p.getScore() < stats.getMean() ?
                2.0 * nd.cumulativeProbability(p.getScore()) :
                2.0 * (1.0 - nd.cumulativeProbability(p.getScore()));

    }

}

