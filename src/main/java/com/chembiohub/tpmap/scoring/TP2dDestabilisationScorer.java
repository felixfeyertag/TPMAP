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
 * TP2dDestabilisationScorer
 *
 * TPMAP algorithm for identifying destabilized proteins in 2D thermal profiling experiments.
 *
 * From each position with a fold change equal or less than 1 in the abundance-dependent fold-change matrix, a
 * recursive descend method is implemented to reach a trough lower than a specified threshold. The score is a
 * value from 0.0 to 1.0 indicating how often the most commonly reached trough was reached.
 *
 * TP2dStabilisationScorer analogously implements an algorithm for determining the most commonly reached peak. Together,
 * TP2dStabilisationScorer and TP2dDestabilisationScorer are used to determine the 2D TPMAP score of a protein, calculated as the
 * TP2dStabilisationScorer score - TP2dDestabilisationScorer score.
 *
 * @author felixfeyertag
 */
public class TP2dDestabilisationScorer {
    
    
    public static Double TPP2dDestabilisationScorer(Protein2D prot, double minThreshold) {
        return TPP2dDestabilisationScorer(prot.getAbundancesConcRatioNormalised(),minThreshold);
    }
    
    public static Double TPP2dDestabilisationScorer(Double[][] abundances, double minThreshold) {
        int counter=0;

        // count matrix
        int[][] cMatrix = new int[abundances.length][abundances[0].length];

        for(int i=0;i<cMatrix.length;i++) {
            for(int j=0;j<cMatrix[0].length;j++) {
                cMatrix[i][j]=0;
            }
        }
        for(int i=0;i<cMatrix.length;i++) {
            for(int j=0;j<cMatrix[0].length;j++) {
                counter++;
                if(Double.isFinite(abundances[i][j])) {
                    if(abundances[i][j]<=1.0) {
                        int[] bottom = descend(i,j,abundances);
                        if(abundances[bottom[0]][bottom[1]]<minThreshold) {
                            cMatrix[bottom[0]][bottom[1]]++;
                        }
                    }
                }
            }
        }
        int max=cMatrix[0][0];

        for (int[] matrix : cMatrix) {
            for (int j = 0; j < cMatrix[0].length; j++) {
                if (matrix[j] > max) {
                    max = matrix[j];
                }
            }
        }

        // score is 0 if more than 50% of values are missing
        if(counter>=0.5*cMatrix.length*cMatrix[0].length) {
            return ((double)max)/((double)counter);
        }
        else {
            return 0.0;
        }
    }
    
    private static int[] descend(int x, int y, Double[][] matrix) {
        int minx = x;
        int miny = y;
        if(x-1>=0) {
            if(!Double.isNaN(matrix[x - 1][y]) && matrix[x-1][y]<matrix[minx][miny]) {
                minx=x-1;
            }
        }
        if(x+1<matrix.length) {
            if(!Double.isNaN(matrix[x + 1][y]) && matrix[x+1][y]<matrix[minx][miny]) {
                minx=x+1;
            }
        }
        if(y-1>=0) {
            if(!Double.isNaN(matrix[x][y - 1]) && matrix[x][y-1]<matrix[minx][miny]) {
                minx=x;
                miny=y-1;
            }
        }
        if(y+1<matrix[0].length) {
            if(!Double.isNaN(matrix[x][y + 1]) && matrix[x][y+1]<matrix[minx][miny]) {
                minx=x;
                miny=y+1;
            }
        }
        if(x==minx && y==miny) {
            return new int[] {x,y};
        }
        else {
            return descend(minx,miny,matrix);
        }
    }
    
}
