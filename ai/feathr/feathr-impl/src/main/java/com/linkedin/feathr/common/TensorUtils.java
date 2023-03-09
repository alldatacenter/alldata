package com.linkedin.feathr.common;

import com.linkedin.feathr.common.tensor.*;
import com.linkedin.feathr.common.tensorbuilder.TensorBuilder;
import com.linkedin.feathr.common.tensorbuilder.TensorBuilderFactory;
import com.linkedin.feathr.common.tensorbuilder.UniversalTensorBuilderFactory;

import com.google.common.collect.Maps;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;


/**
 * INTERNAL debugging utility functions.
 *
 * In the future, this class may be deprecated.
 */
@InternalApi
public final class TensorUtils {
    public static final int DEFAULT_MAX_STRING_LEN = 10240;
    private static final String SEPARATOR = ",";
    private static final String NEXT_LINE = "\n";
    private static final String VALUE_DIM_NAME = "Value";
    private static final String EXCEED_MAX_LIMIT = "...";

    private TensorUtils() {
    }

    public static String getDebugString(TensorType type, TensorData data, int maxStringLenLimit) {
        StringBuilder debugStringBuilder = new StringBuilder();
        List<String> dimensionNames = type.getDimensionNames();
        for (String dimensionName : dimensionNames) {
            debugStringBuilder.append(dimensionName);
            debugStringBuilder.append(SEPARATOR);
        }
        debugStringBuilder.append(VALUE_DIM_NAME);

        List<Representable> columnTypes = new ArrayList<>(type.getDimensionTypes());
        columnTypes.add(type.getValueType());

        TensorIterator dataIterator = data.iterator();
        dataIterator.start();

        while (dataIterator.isValid()) {
            debugStringBuilder.append(NEXT_LINE);
            if (debugStringBuilder.length() >= maxStringLenLimit) {
                debugStringBuilder.append(EXCEED_MAX_LIMIT);
                break;
            }
            int numCols = columnTypes.size();
            String[] strings = convertToStrings(type, dataIterator, numCols);
            debugStringBuilder.append(String.join(SEPARATOR, strings));
            dataIterator.next();
        }
        return debugStringBuilder.toString();
    }

    /**
     * Convert a nested map into Tensor. A nested map contains keys in string format and value
     * can either be a Primitive or another map.
     * This method converts all the keys to their representation
     * using {@link TensorType#getDimensionTypes()} along with
     * DimensionType#setDimensionValue(WriteableTuple, int, Object)} method and populates the output tensor
     * @param map A nested map to be converted to tensor
     * @param tensorType TensorType of the output tensor
     */
    public static TensorData convertNestedMapToTensor(Map<String, Object> map, TensorType tensorType) {
        return convertNestedMapToTensor(map, tensorType, UniversalTensorBuilderFactory.INSTANCE);
    }

    /**
     * Convert a nested map into Tensor. A nested map contains keys in string format and value
     * can either be a Primitive or another map.
     * This method converts all the keys to their representation
     * using {@link TensorType#getDimensionTypes()} (int, String)} method and populates the output tensor
     *
     * @param map A nested map to be converted to tensor
     * @param tensorType TensorType of the output tensor
     * @param tensorBuilderFactory Factory to generate correct implementation of tensor
     * @return A tensor representation of input nested map
     */
    public static TensorData convertNestedMapToTensor(Map<String, Object> map, TensorType tensorType,
                                                      TensorBuilderFactory tensorBuilderFactory) {
        TensorBuilder tensorBuilder = tensorBuilderFactory.getTensorBuilder(tensorType);
        tensorBuilder.start(map.size()); //This is initial size will grow if needed.
        populateTensorBuilder(map, tensorType, tensorBuilder, 0,
                new SimpleWriteableTuple(tensorType.getColumnTypes()));
        return  tensorBuilder.build();
    }


    /**
     * Converts a tensor to a map. The map has keys of type ReadableTuple.
     * The ReadableTuple corresponding to a row in the tensor and the value of the map corresponds to the FLOAT value
     * of the given row in the tensor.
     * @param tensor input tensor to convert to a map
     * @return A map that represents the tensor
     */
    public static Map<ReadableTuple, Float> convertTensorToMap(TensorData tensor) {
        if (tensor == null) {
            return null;
        }
        Map<ReadableTuple, Object> outputMap = convertTensorToMapWithGenericValues(tensor);
        Representable[] columnTypes = tensor.getTypes();
        if (columnTypes[columnTypes.length - 1] == Primitive.FLOAT) {
            return (Map) outputMap;
        }
        Map<ReadableTuple, Float> termValues = Maps.newHashMapWithExpectedSize(outputMap.size());
        for (Map.Entry<ReadableTuple, Object> entry: outputMap.entrySet()) {
            if (entry.getValue() instanceof Number) {
                termValues.put(entry.getKey(), ((Number) entry.getValue()).floatValue());
            } else if (entry.getValue() instanceof Boolean) {
                termValues.put(entry.getKey(), ((Boolean) entry.getValue()) ? 1.0F : 0.0F);
            } else if (entry.getValue() instanceof String) {
                try {
                    termValues.put(entry.getKey(), Float.parseFloat((String) entry.getValue()));
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException(String.format("String value %s can not be formatted to a float", entry.getValue()), e);
                }
            } else {
                throw new IllegalArgumentException("Expecting Primitive value but received " + entry.getValue().getClass());
            }
        }
        return termValues;
    }


    /**
     * Converts a tensor to a map. The map has keys of type ReadableTuple.
     * The ReadableTuple corresponding to a row in the tensor and the value of the map corresponds to the Primitive value
     * of the given row in the tensor.
     * @param tensor input tensor to convert to a map
     * @return A map that represents the tensor
     */
    public static Map<ReadableTuple, Object> convertTensorToMapWithGenericValues(TensorData tensor) {
        if (tensor == null) {
            return null;
        }
        Map<ReadableTuple, Object> outputMap = Maps.newHashMapWithExpectedSize(tensor.estimatedCardinality());
        TensorIterator iterator = tensor.iterator();
        Representable[] columnTypes = iterator.getTypes();
        int valueDim = columnTypes.length - 1;
        Primitive valueType = columnTypes[valueDim].getRepresentation();
        iterator.start();
        while (iterator.isValid()) {
            ReadableTuple row = new StandaloneReadableTuple(iterator, true);
            outputMap.put(row, valueType.toObject(iterator, valueDim));
            iterator.next();
        }
        return outputMap;
    }

    private static void populateTensorBuilder(Map<String, Object> map, TensorType tensorType,
                                              TensorBuilder tensorBuilder, int depth, SimpleWriteableTuple writeableTuple) {
        int numColumns = tensorType.getColumnTypes().length;
        map.forEach((k, v) -> {
            //Process key
            tensorType.getDimensionTypes().get(depth).setDimensionValue(writeableTuple, depth, k);

            //Process value
            if (v instanceof Map) {
                if (depth + 2 >= numColumns) {
                    throw new IllegalArgumentException(String.format("Expected only %d columns, but found more", numColumns));
                }
                populateTensorBuilder((Map) v, tensorType, tensorBuilder, depth + 1, writeableTuple);
            } else {
                //Assume value
                if (depth + 2 != numColumns) {
                    throw new IllegalArgumentException(String.format("Value %s is at depth %d but tensorType suggests it should be at %d",
                            v.toString(), depth, numColumns));
                }
                tensorType.getValueType().getRepresentation().from(v, writeableTuple, depth + 1);
                setRow(tensorBuilder, writeableTuple);
            }
        });
    }

    private static void setRow(TensorBuilder tensorBuilder, SimpleWriteableTuple writeableTuple) {
        Representable[] types = tensorBuilder.getTypes();
        for (int i = 0; i < writeableTuple.getTypes().length; i++) {
            types[i].getRepresentation().copy(writeableTuple, i, tensorBuilder, i);
        }
        tensorBuilder.append();
    }

    public static LOLTensorData convertToLOLTensor(TensorData ut) {
        Representable[] columnTypes = ut.getTypes();
        int numColumns = columnTypes.length;
        List<List<Object>> dimensions = new ArrayList<>(numColumns - 1);
        List<Object> values = new ArrayList<>(ut.estimatedCardinality());
        int valueDim = columnTypes.length - 1;
        Primitive valueType = columnTypes[valueDim].getRepresentation();

        for (int i = 0; i < numColumns - 1; i++) {
            dimensions.add(new ArrayList<>(ut.estimatedCardinality()));
        }
        for (TensorIterator iter = ut.iterator(); iter.isValid(); iter.next()) {
            for (int i = 0; i < numColumns - 1; i++) {
                dimensions.get(i).add(iter.getValue(i));
            }
            values.add(valueType.toObject(iter, valueDim));
        }
        return new LOLTensorData(ut.getTypes(), (List) dimensions, values);
    }


    public static String[] convertToStrings(TensorType tensorType, ReadableTuple readableTuple, int numCols) {
        List<DimensionType> dims = tensorType.getDimensionTypes();
        int numDims = dims.size();
        if (numCols > numDims + 1) {
            throw new IllegalArgumentException("Number of columns in the output is greater than number of dims & value");
        }
        boolean convertValue = false;
        if (numCols > numDims) {
            convertValue = true;
        } else {
            numDims = numCols;
        }
        String[] ret = new String[numCols];
        for (int i = 0; i < numDims; i++) {
            DimensionType dim = dims.get(i);
            ret[i] = dim.getDimensionValue(readableTuple, i).toString();
        }
        if (convertValue) {
            ret[numDims] = tensorType.getValueType().getRepresentation().toString(readableTuple, numDims);
        }
        return ret;
    }

    public static <K> Function<ReadableTuple, K> wrapKeyGen(TensorType inputTensor, Function<String[], K> keyGen) {
        int numDims = inputTensor.getDimensionTypes().size();
        return readableTuple -> keyGen.apply(TensorUtils.convertToStrings(inputTensor, readableTuple, numDims));
    }

    /**
     * Get shape of the tensor as an array
     * @param tensorType Type for which shape needs to be computed
     * @return long array with each element indicating shape of the dimensions
     */
    public static long[] getShape(TensorType tensorType) {
        List<DimensionType> dimensionTypes = tensorType.getDimensionTypes();
        int numDims = dimensionTypes.size();
        long[] shape = new long[numDims];
        for (int i = 0; i < numDims; i++) {
            DimensionType dimensionType = dimensionTypes.get(i);
            shape[i] = dimensionType.getShape();
        }
        return shape;
    }

    /**
     * Creates a Tensor object with the associated columnTypes and data using TensorBuilder
     * @param columnTypes columnTypes of the tensor
     * @param data data within the tensor
     *             data.length corresponds to the number of rows the tensor has
     *             data[i].length corresponds to the number of columns in a tensor row.
     *             Each element in data[i] corresponds to the dimension/value of the tensor at row i
     * @param tensorBuilder tensorbuilder representing the data
     */
    public static TensorData populateTensor(Representable[] columnTypes, Object[][] data, TensorBuilder tensorBuilder) {
        for (int i = 0; i < data.length; i++) {
            for (int j = 0; j < data[i].length; j++) {
                if (data[i].length != columnTypes.length) {
                    throw new IllegalArgumentException(String.format("data[i] length should be equal to columnType length"
                            + "Found data[i].length = %s and columnType.length = %s", data[i].length, columnTypes.length));
                }
                columnTypes[j].getRepresentation().from(data[i][j], tensorBuilder, j);
            }
            tensorBuilder.append();
        }
        return tensorBuilder.build();
    }

    /**
     * @deprecated use {@link TensorTypes#parseTensorType}
     */
    @Deprecated
    public static TensorType parseTensorType(String syntax) {
        return TensorTypes.parseTensorType(syntax);
    }

    // return a ratio of two numbers or 0 if they are both 0.
    public static int safeRatio(int numerator, int denominator) {
        if (denominator == 0) {
            if (numerator == 0) {
                return 0;
            } else {
                throw new IllegalArgumentException("Dividing a non-zero " + numerator + " by zero.");
            }
        }
        int ratio = numerator / denominator;
        if (ratio * denominator != numerator) {
            throw new IllegalArgumentException(
                    "Integer division has a non-zero remainder " + numerator + "/" + denominator + ".");
        }
        return ratio;
    }
}



