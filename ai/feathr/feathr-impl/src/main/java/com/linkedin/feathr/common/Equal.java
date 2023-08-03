package com.linkedin.feathr.common;


import com.linkedin.feathr.common.tensor.ReadableTuple;
import com.linkedin.feathr.common.tensor.Representable;
import com.linkedin.feathr.common.tensor.TensorData;
import com.linkedin.feathr.common.tensor.TensorIterator;

/**
 * Compare whether two tensor data are equal.
 *
 * Note: order does not matter here, and we assume that there is no duplicate rows in left & right tensor data
 *
 * Return true if the tensors are equal.
 */
public class Equal {
    public static final Equal INSTANCE = new Equal();

    public boolean apply(TensorData leftData, TensorData rightData) {
        return apply(leftData, rightData, false);
    }

    public boolean apply(TensorData leftData, TensorData rightData, boolean dimOnly) {
        if (leftData == null && rightData == null) {
            return true;
        }

        if (leftData == null || rightData == null) {
            return false;
        }

        if (leftData.isEmpty() && rightData.isEmpty()) {
            return true;
        }

        if (leftData.isEmpty() || rightData.isEmpty()) {
            return false;
        }
        /* compare the type */
        Representable[] leftTypes = leftData.getTypes();
        Representable[] rightTypes = rightData.getTypes();

        if (leftTypes.length != rightTypes.length) {
            return false;
        }

        int len = leftTypes.length;
        for (int i = 0; i < len; i++) {
            if (leftTypes[i].getRepresentation() != rightTypes[i].getRepresentation()) {
                return false;
            }
        }

        int numColumnsToCompare = dimOnly ? ColumnUtils.getDimArity(leftTypes) : leftTypes.length;

        TensorIterator left = leftData.iterator();
        TensorIterator right = rightData.iterator();
        while (left.isValid() && right.isValid()) {
            if (!tupleEquals(left, right, leftTypes, numColumnsToCompare)) {
                return false;
            }
            left.next();
            right.next();
        }
        return !left.isValid() && !right.isValid();
    }

    public static boolean tupleEquals(ReadableTuple left, ReadableTuple right, Representable[] featureTypes, int numColumnsToCompare) {
        for (int i = 0;  i < numColumnsToCompare; i++) {
            if (!featureTypes[i].getRepresentation().equals(left, i, right, i)) {
                return false;
            }
        }
        return true;
    }


}