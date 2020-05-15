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
package com.chembiohub.tpmap.normalisation;

import com.chembiohub.tpmap.dstruct.Protein;
import com.chembiohub.tpmap.dstruct.Protein1D;
import com.chembiohub.tpmap.dstruct.Protein2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * TP2DMedianNormalisation
 *
 * Normalise proteins by the median value among all proteins
 *
 * @author felixfeyertag
 */
public class TP2DMedianNormalisation<T extends Protein> extends TPNormalisation {
    
    private Double[][] medians;
    private boolean initialised;
    
    public TP2DMedianNormalisation() {
        initialised = false;
    }
    
    public void initMedianNormalisation(List<Protein> proteins) {

        // 1D experiments are normalised by the abundance/temperature ratio
        if(proteins.get(0) instanceof Protein1D) {

            Protein1D protein1d = (Protein1D)proteins.get(0);

            int x = protein1d.getAbundancesTempRatio().length;
            int y = protein1d.getAbundancesTempRatio()[0].length;
            List<Double>[][] abundances = new List[x][y];
            medians = new Double[x][y];

            for(int i=0;i<x;i++) {
                for(int j=0;j<y;j++) {
                    abundances[i][j] = new ArrayList<>();
                }
            }

            proteins.forEach((prot) -> {
                Protein1D prot1d = (Protein1D) prot;
                for(int i=0;i<x;i++) {
                    for(int j=0;j<y;j++) {
                        if(Double.isFinite(prot1d.getAbundancesTempRatio()[i][j]))
                            abundances[i][j].add(prot1d.getAbundancesTempRatio()[i][j]);
                    }
                }
            });

            for (int i=0;i<x;i++) {
                for (int j=0;j<y;j++) {
                    Collections.sort(abundances[i][j]);
                    try {
                        medians[i][j] = abundances[i][j].get((int)Math.floor(abundances[i][j].size()/2));
                    } catch (IndexOutOfBoundsException e) {
                        medians[i][j] = 0.0;
                    }
                }
            }

            initialised = true;
        }

        // 2D experiments are normalised by the abundance/concentration ratio
        if(proteins.get(0) instanceof  Protein2D) {
            Protein2D protein2d = (Protein2D) proteins.get(0);

            int x = protein2d.getAbundancesConcRatio().length;
            int y = protein2d.getAbundancesConcRatio()[0].length;
            List<Double>[][] abundances = new List[x][y];
            medians = new Double[x][y];

            for (int i = 0; i < x; i++) {
                for (int j = 0; j < y; j++) {
                    abundances[i][j] = new ArrayList<>();
                }
            }

            proteins.forEach((prot) -> {
                Protein2D prot2d = (Protein2D) prot;
                for (int i = 0; i < x; i++) {
                    for (int j = 0; j < y; j++) {
                        if (Double.isFinite(prot2d.getAbundancesConcRatio()[i][j]))
                            abundances[i][j].add(prot2d.getAbundancesConcRatio()[i][j]);
                    }
                }
            });

            for (int i = 0; i < x; i++) {
                for (int j = 0; j < y; j++) {
                    Collections.sort(abundances[i][j]);
                    try {
                        medians[i][j] = abundances[i][j].get((int) Math.floor(abundances[i][j].size() / 2));
                    } catch (IndexOutOfBoundsException e) {
                        medians[i][j] = 1.0;
                    }
                }
            }

            initialised = true;
        }
    }
    
    @Override
    public Protein normalise(Protein protein) {

        if(!initialised) {
            return null;
        }


        if(protein instanceof Protein2D) {
            Protein2D protein2d = (Protein2D) protein;
            Double[][] normalised = new Double[medians.length][medians[0].length];
            for (int i = 0; i < normalised.length; i++) {
                for (int j = 0; j < normalised[0].length; j++) {
                    normalised[i][j] = protein2d.getAbundancesConcRatio()[i][j] / medians[i][j];
                }
            }
            protein2d.setAbundancesConcRatioNormalised(normalised);
            return protein2d;
        }
        else if(protein instanceof Protein1D) {
            Protein1D protein1d = (Protein1D) protein;
            Double[][] normalised = new Double[medians.length][medians[0].length];
            for (int i = 0; i < normalised.length; i++) {
                for (int j = 0; j < normalised[0].length; j++) {
                    normalised[i][j] = protein1d.getAbundancesTempRatio()[i][j] / medians[i][j];
                }
            }
            protein1d.setAbundancesTempRatioNormalised(normalised);
        }


        return protein;
    }
    
}
