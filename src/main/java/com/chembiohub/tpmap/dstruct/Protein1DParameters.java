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

public class Protein1DParameters implements ProteinParameters {

    private Integer attempts;
    private Integer maxIterations;

    public Protein1DParameters(Integer attempts, Integer maxIterations) {
        this.attempts = attempts;
        this.maxIterations = maxIterations;
    }

    @Override
    public Object[] getParams() {
        return new Object[] { attempts, maxIterations };
    }

    public void setAttempts(Integer attempts) {
        this.attempts = attempts;
    }

    public void setMaxIterations(Integer maxIterations) {
        this.maxIterations = maxIterations;
    }

    public Integer getAttempts() {
        return attempts;
    }

    public Integer getMaxIterations() {
        return maxIterations;
    }

}
