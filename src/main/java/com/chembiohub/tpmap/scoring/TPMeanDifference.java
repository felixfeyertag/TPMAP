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

import com.chembiohub.tpmap.dstruct.Protein2D;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 * TPMeanDifference
 *
 * Calculates the mean difference between a selected protein and all other proteins in a list.
 *
 * @author felixfeyertag
 */
public class TPMeanDifference {
    
    public static void TPPMeanDifference(ObservableList<Protein2D> proteins, Protein2D selectedProtein)
            throws TPPNoneSelectedException {
        ObservableList<Double> distances = FXCollections.observableArrayList();

        if(null==selectedProtein) {
            throw new TPPNoneSelectedException("No selected proteins");
        }
        
        proteins.forEach((p) -> {
            double dist = 0.0;
            int counter = 0;
            for(int i=0;i<p.getAbundancesConcRatioNormalised().length;i++) {
                for(int j=0;j<p.getAbundancesConcRatioNormalised()[0].length;j++) {
                    if(!selectedProtein.getAbundancesConcRatioNormalised()[i][j].isNaN() && !p.getAbundancesConcRatioNormalised()[i][j].isNaN()) {
                        dist += Math.abs(selectedProtein.getAbundancesConcRatioNormalised()[i][j]-p.getAbundancesConcRatioNormalised()[i][j]);
                        counter ++;
                    }
                    else if(!p.getAbundancesConcRatioNormalised()[i][j].isNaN()) {
                        dist += p.getAbundancesConcRatioNormalised()[i][j];
                        counter ++;
                    }
                    else if(!selectedProtein.getAbundancesConcRatioNormalised()[i][j].isNaN()) {
                        dist += selectedProtein.getAbundancesConcRatioNormalised()[i][j];
                        counter ++;
                    }
                }
            }
            p.addMeanDifference(dist/counter);
        });
    }

    /*
    public static void stabilisationGradient(ObservableList<Protein> proteins,Double max) {
        Double[][] template = new Double[proteins.get(0).getAbundances().length][proteins.get(0).getAbundances()[0].length];
        for(int i=0;i<template[0].length;i++) {
            for(int j=0;j<template.length;j++) {
                Double value = 1.0 + i * ((max-1) / (template[0].length-1));
            }
        }
    }
    
    public static void destabilisationGradient(ObservableList<Protein> proteins,Double min) {
        Double[][] template = new Double[proteins.get(0).getAbundances().length][proteins.get(0).getAbundances()[0].length];
        for(int i=0;i<template[0].length;i++) {
            for(int j=0;j<template.length;j++) {
                Double value = min + i * ((1-min)/(template[0].length-1));
            }
        }
    }
    */

    public static class TPPNoneSelectedException extends Exception {

        TPPNoneSelectedException(String ex) {
            super(ex);
        }
    }

}
