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

/**
 * TP2dMeanFC
 *
 * Calculates the mean abundance-dependent fold change of a protein in a 2d thermal profiling experiment.
 *
 * @author felixfeyertag
 */
public class TP2dMeanFC {
        
    public static Double TPP2dMeanFC(Protein2D prot) {
        
        Double[][] abundances = prot.getAbundancesConcRatioNormalised();
        Double fc = 0.0;
        
        int counter = 0;
        
        for (Double[] abundance : abundances) {
            for (int j = 0; j<abundances[0].length; j++) {
                if(!abundance[j].isNaN()) { 
                    fc += abundance[j];
                    counter ++;
                }
                else {
                    fc += 1.0;
                    counter ++;
                }
            }
        }
        
        if (counter<30) {
            return 0.0;
        }
        
        return fc/counter;
        
    }
    
}
