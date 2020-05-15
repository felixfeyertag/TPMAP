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

/**
 * TPNormalisation
 *
 * Normalisation methods supported in TPMAP
 *
 * @author felixfeyertag
 */
public abstract class TPNormalisation<T extends Protein> {
    
    public enum Normalisation {
        NONE,
        MEDIAN
    }
    
    public abstract T normalise(T protein);

}
