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

/**
 * TP1dAUC
 *
 * The TP1dAUC class calculates area under curve of temperature dependent abundance ratios using the trapezoidal rule
 */
class TP1dAUC {

    public static Double[] TP1dAUC(Double[][] abundanceRatios, Double[] temps) {

        assert(abundanceRatios.length==temps.length);

        Double[] auc = new Double[abundanceRatios.length];

        for(int i=0; i<abundanceRatios.length-1; i++) {
            for(int j=0; j<abundanceRatios[0].length; j++) {
                auc[i] += (temps[j+1]-temps[j]) * ((abundanceRatios[i][j] + abundanceRatios[i][j+1]) / 2.0);
            }
        }

        return auc;
    }
}
