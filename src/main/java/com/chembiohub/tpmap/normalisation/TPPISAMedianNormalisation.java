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
import com.chembiohub.tpmap.dstruct.ProteinPISA;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * TPPISAMedianNormalisation
 *
 * TODO: Implement normalisation for PISA experiments
 *
 * @author felixfeyertag
 */
public class TPPISAMedianNormalisation extends TPNormalisation {

    private Double[][] medians;
    private boolean initialised;

    public TPPISAMedianNormalisation() {
        initialised = false;
    }

    public TPPISAMedianNormalisation(List<Protein> proteins) {

        assert null != proteins;
        assert proteins.get(0) instanceof ProteinPISA;

        ProteinPISA protein0 = (ProteinPISA) proteins.get(0);

        int x = 1;
        int y = 0;
        //int y = protein0.getAbundancesTempRatio()[0].length;
        List<Double>[][] abundances = new List[1][y];
        medians = new Double[1][y];

        for(int i=0; i<y; i++) {
            abundances[0][i] = new ArrayList<>();
        }

        proteins.forEach((prot) -> {
            assert prot instanceof ProteinPISA;
            ProteinPISA protPISA = (ProteinPISA) prot;
            for(int i=0;i<y;i++) {
                /*if(Double.isFinite(protPISA.getAbundancesTempRatio()[0][i])) {
                    abundances[0][i].add(protPISA.getAbundancesTempRatio()[0][i]);
                }*/
            }
        });

        for(int i=0;i<y;i++) {
            Collections.sort(abundances[0][i]);
            try {
                medians[0][i] = abundances[0][i].get((int)Math.floor(abundances[0][i].size()/2));
            } catch (IndexOutOfBoundsException e) {
                medians[0][i] = 0.0;
            }
        }

        initialised = true;
    }

    @Override
    public Protein normalise(Protein protein) {
        assert protein instanceof ProteinPISA;

        ProteinPISA proteinPISA = (ProteinPISA) protein;

        if(!initialised) {
            return null;
        }

        Double[][] normalised = new Double[1][medians[0].length];
        for(int i=0; i<normalised[0].length; i++) {
            //normalised[0][i] = proteinPISA.getAbundancesTempRatio()[0][i] / medians[0][i];
        }

        //proteinPISA.setAbundancesTempRatioNormalised(normalised);

        return protein;
    }

}
