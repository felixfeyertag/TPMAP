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
package com.chembiohub.tpmap.dstruct;

public class Protein2DParameters implements ProteinParameters {

    private Integer iterations;

    public Protein2DParameters(Integer iterations) {
        this.iterations = iterations;
    }

    @Override
    public Object[] getParams() {
        return new Object[] { iterations };
    }

    public void setIterations(Integer maxIterations) {
        this.iterations = maxIterations;
    }

    public Integer getIterations() {
        return iterations;
    }

}
