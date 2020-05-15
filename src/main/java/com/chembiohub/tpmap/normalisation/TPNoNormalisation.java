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

/**
 * TPNoNormalisation
 *
 * Class that sets abundance ratios to the base fold change values without any normalisation
 *
 * @author felixfeyertag
 */
public class TPNoNormalisation extends TPNormalisation {
        
    public TPNoNormalisation() {
    }
    
    @Override
    public Protein normalise(Protein protein) {

        if (protein instanceof Protein1D) {
            Protein1D protein1d = (Protein1D) protein;
            protein1d.setAbundancesTempRatioNormalised(protein1d.getAbundancesTempRatio());
            return protein1d;
        }
        if (protein instanceof Protein2D) {
            Protein2D protein2d = (Protein2D) protein;
            protein2d.setAbundancesConcRatioNormalised(protein2d.getAbundancesConcRatio());
            return protein2d;
        }

        return null;

    }
    
}
