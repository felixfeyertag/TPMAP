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
package com.chembiohub.tpmap.scoring;

import com.chembiohub.tpmap.dstruct.Protein1D;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.collections.ObservableList;
import org.apache.commons.math3.util.FastMath;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * TP1dScorer
 *
 * Score for 1D TP datasets based on thermal shift and quality of curve fit.
 *
 * Rank for each protein is calculated for highest TM shift among two replicates, smallest RMSE fit for all
 * conditions, and highest similarity between replicates. The weight between Tm shift and curve fit quality
 * can be adjusted by setting tmPercentage. The resulting score is scaled between 0 and 10.
 *
 */
public class TP1dScorer {

    private final List<Double> tmShiftV1T1List = new ArrayList<>();
    private final List<Double> tmShiftV2T2List = new ArrayList<>();
    private final List<Double> meanTmShiftList = new ArrayList<>();

    private final List<Double> rmseV1List = new ArrayList<>();
    private final List<Double> rmseV2List = new ArrayList<>();
    private final List<Double> rmseT1List = new ArrayList<>();
    private final List<Double> rmseT2List = new ArrayList<>();

    private final List<Double> vRepList = new ArrayList<>();
    private final List<Double> tRepList = new ArrayList<>();

    private final DoubleProperty tmPercentage = new SimpleDoubleProperty(80.0);

    public TP1dScorer(ObservableList<Protein1D> proteins, Double tmPercentage) {

        this.tmPercentage.set(tmPercentage);

        proteins.forEach(p -> {
            if (Double.isFinite(p.getMeanTM())) {
                meanTmShiftList.add(Math.abs(p.getMeanTM()));
            }
            if (Double.isFinite(p.getTmVT1())) {
                tmShiftV1T1List.add(p.getTmVT1());
            }
            if (Double.isFinite(p.getTmVT2())) {
                tmShiftV2T2List.add(p.getTmVT2());
            }
            if (Double.isFinite(p.getRmsev1())) {
                rmseV1List.add(p.getRmsev1());
            }
            if (Double.isFinite(p.getRmsev2())) {
                rmseV2List.add(p.getRmsev2());
            }
            if (Double.isFinite(p.getRmset1())) {
                rmseT1List.add(p.getRmset1());
            }
            if (Double.isFinite(p.getRmset2())) {
                rmseT2List.add(p.getRmset2());
            }
            if (Double.isFinite(p.getVRep())) {
                vRepList.add(p.getVRep());
            }
            if (Double.isFinite(p.getTRep())) {
                tRepList.add(p.getTRep());
            }
        });

        Collections.sort(meanTmShiftList);
        Collections.sort(tmShiftV1T1List);
        Collections.sort(tmShiftV2T2List);
        Collections.sort(rmseV1List);
        Collections.sort(rmseV2List);
        Collections.sort(rmseT1List);
        Collections.sort(rmseT2List);
        Collections.sort(vRepList);
        Collections.sort(tRepList);
    }

    public double calculateScore(Protein1D p) {

        // Determine ranks for TM shifts and RMSE fits
        Integer rankTmVT1 = 1;
        Integer rankTmVT2 = 1;
        Integer rankMeanTm = 1;
        int rankRMSEV1 = 1;
        int rankRMSEV2 = 1;
        int rankRMSET1 = 1;
        int rankRMSET2 = 1;
        int rankVRep = 1;
        int rankTRep = 1;

        if(Double.isFinite(p.getMeanTM())) {
            int i=meanTmShiftList.size();
            while(meanTmShiftList.get(--i) > Math.abs(p.getMeanTM())) rankMeanTm++;
        } else {
            rankMeanTm = meanTmShiftList.size();
        }

        // Calculate Tm ranking
        if(Double.isFinite(p.getTmVT1()) && Double.isFinite(p.getTmVT2())) {
            // If both Tm shifts are positive then the rank is counted from the top of the list
            if(p.getTmVT1()>=0.0 && p.getTmVT2()>=0.0) {
                int i = tmShiftV1T1List.size();
                while (tmShiftV1T1List.get(--i)>p.getTmVT1()) rankTmVT1++;
                i = tmShiftV2T2List.size();
                while (tmShiftV2T2List.get(--i)>p.getTmVT2()) rankTmVT2++;
            }
            // If both Tm shifts are negative then the rank is counted from the bottom of the list
            else if(p.getTmVT1()<=0.0 && p.getTmVT2()<=0.0) {
                int i = 0;
                while (tmShiftV1T1List.get(i++)<p.getTmVT1()) rankTmVT1++;
                i = 0;
                while (tmShiftV2T2List.get(i++)<p.getTmVT2()) rankTmVT2++;
            }
            // If Tm shift directions contradict then the ranks are calculated for the positive and negative shifts
            // and the higher rank is used to determine the direction (this ensures that a penalty is applied to
            // large discrepancies)
            else if(p.getTmVT1()>=0.0 && p.getTmVT2()<=0.0) {
                int i = tmShiftV1T1List.size();
                while (tmShiftV1T1List.get(--i)>p.getTmVT1()) rankTmVT1++;
                i = 0;
                while (tmShiftV2T2List.get(i++)<p.getTmVT2()) rankTmVT2++;
                if(rankTmVT1>rankTmVT2) {
                    rankTmVT1 = 1;
                    i = 0;
                    while (tmShiftV1T1List.get(i++)<p.getTmVT1()) rankTmVT1++;
                }
                else {
                    rankTmVT2 = 1;
                    i = tmShiftV2T2List.size();
                    while (tmShiftV2T2List.get(--i)>p.getTmVT2()) rankTmVT2++;
                }
            }
            else if(p.getTmVT1()<=0.0 && p.getTmVT2()>=0.0) {
                int i = 0;
                while (tmShiftV1T1List.get(i++)<p.getTmVT1()) rankTmVT1++;
                i = tmShiftV2T2List.size();
                while (tmShiftV2T2List.get(--i)>p.getTmVT2()) rankTmVT2++;

                if(rankTmVT1>rankTmVT2) {
                    rankTmVT1 = 1;
                    i = tmShiftV1T1List.size();
                    while (tmShiftV1T1List.get(--i)>p.getTmVT1()) rankTmVT1++;
                }
                else {
                    rankTmVT2 = 1;
                    i = 0;
                    while (tmShiftV2T2List.get(i++)<p.getTmVT2()) rankTmVT2++;
                }
            }
        } else if (Double.isFinite(p.getTmVT1())) {
            if(p.getTmVT1()>0.0) {
                int i = tmShiftV1T1List.size();
                while (tmShiftV1T1List.get(--i)>p.getTmVT1()) rankTmVT1++;
            }
            else {
                int i = 0;
                while (tmShiftV1T1List.get(i++)<p.getTmVT1()) rankTmVT1++;
            }
            rankTmVT2 = tmShiftV2T2List.size();
        } else if (Double.isFinite(p.getTmVT2())) {
            if(p.getTmVT2()>0.0) {
                int i = tmShiftV2T2List.size();
                while (tmShiftV2T2List.get(--i)>p.getTmVT2()) rankTmVT2++;
            }
            else {
                int i = 0;
                while (tmShiftV2T2List.get(i++)<p.getTmVT2()) rankTmVT2++;
            }
            rankTmVT1 = tmShiftV1T1List.size();
        }
        // Tm shift set to lowest rank if NaN
        else {
            rankTmVT1 = tmShiftV1T1List.size();
            rankTmVT2 = tmShiftV2T2List.size();
        }

        // Calculate RMSE ranking
        if(Double.isFinite(p.getRmsev1())) {
            int i=0;
            while(rmseV1List.get(i++)<p.getRmsev1()) rankRMSEV1++;
        }
        else {
            rankRMSEV1 = rmseV1List.size();
        }
        if(Double.isFinite(p.getRmsev2())) {
            int i=0;
            while(rmseV2List.get(i++)<p.getRmsev2()) rankRMSEV2++;
        }
        else {
            rankRMSEV2 = rmseV2List.size();
        }
        if(Double.isFinite(p.getRmset1())) {
            int i=0;
            while(rmseT1List.get(i++)<p.getRmset1()) rankRMSET1++;
        }
        else {
            rankRMSET1 = rmseT1List.size();
        }
        if(Double.isFinite(p.getRmset2())) {
            int i=0;
            while(rmseT2List.get(i++)<p.getRmset2()) rankRMSET2++;
        }
        else {
            rankRMSET2 = rmseT2List.size();
        }

        //calculate replicate ranking
        if(Double.isFinite(p.getVRep())) {
            int i=0;
            while(vRepList.get(i++)<p.getVRep()) rankVRep++;
        }
        else {
            rankVRep = vRepList.size();
        }
        if(Double.isFinite(p.getTRep())) {
            int i=0;
            while(tRepList.get(i++)<p.getTRep()) rankTRep++;
        }
        else {
            rankTRep = tRepList.size();
        }

        double scoreMeanTm = FastMath.abs((double)rankMeanTm-(double)meanTmShiftList.size()) / (double)meanTmShiftList.size();
        double scoreTmVT1  = FastMath.abs((double)rankTmVT1-(double)tmShiftV1T1List.size())  / (double)tmShiftV1T1List.size();
        double scoreTmVT2  = FastMath.abs((double)rankTmVT2-(double)tmShiftV2T2List.size())  / (double)tmShiftV2T2List.size();
        double scoreRMSEV1 = FastMath.abs((double)rankRMSEV1-(double)rmseV1List.size())      / (double)rmseV1List.size();
        double scoreRMSEV2 = FastMath.abs((double)rankRMSEV2-(double)rmseV2List.size())      / (double)rmseV2List.size();
        double scoreRMSET1 = FastMath.abs((double)rankRMSET1-(double)rmseT1List.size())      / (double)rmseT1List.size();
        double scoreRMSET2 = FastMath.abs((double)rankRMSET2-(double)rmseT2List.size())      / (double)rmseT2List.size();
        double scoreVRep   = FastMath.abs((double)rankVRep-(double)vRepList.size())          / (double)vRepList.size();
        double scoreTRep   = FastMath.abs((double)rankTRep-(double)tRepList.size())          / (double)tRepList.size();

        //return (2.0 * scoreTmVT1 + 2.0 * scoreTmVT2 + scoreRMSEV1 + scoreRMSEV2 + scoreRMSET1 + scoreRMSET2 + scoreVRep + scoreTRep);
        return ( (tmPercentage.getValue()/100.0) * (3.0 * scoreTmVT1 + 3.0 * scoreTmVT2) + (1.0-tmPercentage.getValue()/100.0) * (scoreRMSEV1 + scoreRMSEV2 + scoreRMSET1 + scoreRMSET2 + scoreVRep + scoreTRep) ) / 6.0 * 10.0;
        //return ( (tmPercentage.getValue()/100.0) * (6.0 * scoreMeanTm) + (1.0-tmPercentage.getValue()/100.0) * (scoreRMSEV1 + scoreRMSEV2 + scoreRMSET1 + scoreRMSET2 + scoreVRep + scoreTRep) ) / 6.0 * 10.0;
    }

}

