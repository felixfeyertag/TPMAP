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

import org.apache.commons.math3.analysis.ParametricUnivariateFunction;
import org.apache.commons.math3.analysis.differentiation.DerivativeStructure;
import org.apache.commons.math3.analysis.differentiation.UnivariateDifferentiableFunction;
import org.apache.commons.math3.exception.DimensionMismatchException;
import org.apache.commons.math3.exception.NullArgumentException;
import org.apache.commons.math3.util.FastMath;

public class TP1dDenaturationFunction implements UnivariateDifferentiableFunction {

    private final double a;
    private final double b;
    private final double plateau;

    public TP1dDenaturationFunction(double a, double b, double plateau) {

        this.a = a;
        this.b = b;
        this.plateau = plateau;

    }

    public double value(double x) {
        return value(x, a, b, plateau);
    }

    private static double value(double x, double a, double b, double plateau) {
        return (1.0 - plateau) / (1.0 + FastMath.exp(-(a/x - b))) + plateau;
    }

    public DerivativeStructure value(final DerivativeStructure t) {
        return t.reciprocal().multiply(a).subtract(b).negate().exp().add(1.0).reciprocal().multiply(1.0 - plateau).add(plateau);
    }

    public static class Parametric implements ParametricUnivariateFunction {

        @Override
        public double value(double x, double... params) {

            validateParameters(params);

            double a = params[0];
            double b = params[1];
            double p = params[2];

            return TP1dDenaturationFunction.value(x, a, b, p);
        }

        @Override
        public double[] gradient(double x, double... params) {

            validateParameters(params);

            double a = params[0];
            double b = params[1];
            double p = params[2];

            double exp = FastMath.exp(b-a/x);

            double dda = ((1.0-p) * exp) / (x * FastMath.pow(exp + 1.0,2));
            double ddb = - ((1.0-p) * exp) / FastMath.pow(exp + 1.0,2);
            double ddp = 1.0 - 1.0 / (exp + 1.0);

            return new double[] { dda, ddb, ddp };

        }

        private void validateParameters(double[] param) {

            if(param == null) {
                throw new NullArgumentException();
            }
            if(param.length != 3) {
                throw new DimensionMismatchException(param.length, 3);
            }

        }

    }

}


