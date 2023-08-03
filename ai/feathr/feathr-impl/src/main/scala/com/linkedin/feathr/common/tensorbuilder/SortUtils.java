package com.linkedin.feathr.common.tensorbuilder;

public final class SortUtils {
    private SortUtils() {
    }

    /**
     * Swaps the value blocks in v[] based on a, b and floatArity.
     *
     * @throws IndexOutOfBoundsException if {@code floatArity} is negative
     */
    public static void swapFloat(int floatArity, float[] v, int a, int b, float[] arityBuffer) {
        if (floatArity == 1) {
            float tmp = v[a];
            v[a] = v[b];
            v[b] = tmp;
        } else if (floatArity != 0) {
            System.arraycopy(v, a * floatArity, arityBuffer, 0, floatArity);
            System.arraycopy(v, b * floatArity, v, a * floatArity, floatArity);
            System.arraycopy(arityBuffer, 0, v, b * floatArity, floatArity);
        }
    }

    /**
     * Swaps the value blocks in v[] based on a, b and doubleArity.
     *
     * @throws IndexOutOfBoundsException if {@code doubleArity} is negative
     */
    public static void swapDouble(int doubleArity, double[] v, int a, int b, double[] arityBuffer) {
        if (doubleArity == 1) {
            double tmp = v[a];
            v[a] = v[b];
            v[b] = tmp;
        } else if (doubleArity != 0) {
            System.arraycopy(v, a * doubleArity, arityBuffer, 0, doubleArity);
            System.arraycopy(v, b * doubleArity, v, a * doubleArity, doubleArity);
            System.arraycopy(arityBuffer, 0, v, b * doubleArity, doubleArity);
        }
    }
}
