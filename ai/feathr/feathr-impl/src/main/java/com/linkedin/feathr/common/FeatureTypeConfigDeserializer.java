package com.linkedin.feathr.common;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.linkedin.feathr.common.tensor.*;
import com.linkedin.feathr.common.types.PrimitiveType;
import com.linkedin.feathr.common.types.ValueType;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


/**
 * Config deserializer for FeatureType config. It mainly supports parsing two type configs:
 * 1. Simple type config: type with a enum. Example: type: BOOLEAN
 * 2. Type config with more specifications. For example:
 *    DENSE_VECTOR with size:
 *        type: {
 *          type: DENSE_VECTOR
 *          shape: [10]
 *          doc: "documentation"
 *        }
 *    TENSOR type with more details:
 *        type: {
 *          type: DENSE_TENSOR
 *          shape: [10, 10]
 *          dimensionType: [INT, INT]
 *          valType: FLOAT
 *          doc: "documentation"
 *        }
 */
public class FeatureTypeConfigDeserializer extends JsonDeserializer<FeatureTypeConfig> {
    private final static String TYPE_FIELD = "type";
    private final static String SHAPE_FIELD = "shape";
    private final static String DIMENSION_TYPE_FIELD = "dimensionType";
    private final static String VAL_TYPE_FIELD = "valType";
    private final static String DOC_FIELD = "doc";
    private static final Set<FeatureTypes> ALL_FEATURE_TYPES = new HashSet<>(Arrays.asList(FeatureTypes.values()));
    private static final Set<String> ALL_FEATURE_TYPES_STRING =
            ALL_FEATURE_TYPES.stream().map(Enum::toString).collect(Collectors.toSet());
    private static final Set<Primitive> DENSE_TENSOR_ALLOWED_PRIMITIVE = new HashSet<>(Arrays.asList(Primitive.INT));
    private static final Set<Primitive> SPARSE_TENSOR_ALLOWED_PRIMITIVE =
            new HashSet<>(Arrays.asList(Primitive.INT, Primitive.LONG, Primitive.STRING));
    private static final String DENSE_TENSOR_TYPE_STRING = "DENSE_TENSOR";
    private static final String SPARSE_TENSOR_TYPE_STRING = "SPARSE_TENSOR";
    private static final String RAGGED_TENSOR_TYPE_STRING = "RAGGED_TENSOR";
    private static final Set<String> SUPPORTED_TENSOR_TYPES =
            new HashSet<>(Arrays.asList(DENSE_TENSOR_TYPE_STRING, SPARSE_TENSOR_TYPE_STRING, RAGGED_TENSOR_TYPE_STRING));

    @Override
    public FeatureTypeConfig deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
        TreeNode node = jp.getCodec().readTree(jp);
        // If it's TextNode, then it's simple enum.
        if (node instanceof TextNode) {
            FeatureTypes featureTypes = FeatureTypes.valueOf(((TextNode) node).asText());
            return new FeatureTypeConfig(featureTypes);
        }
        ObjectNode objectNode = (ObjectNode) node;
        JsonNode typeNode = objectNode.get(TYPE_FIELD);
        if (typeNode == null) {
            throw new RuntimeException("Type should be specified but it's not. The config is: " + node);
        }
        String typeString = typeNode.asText();
        FeatureTypes featureType;
        TensorType tensorType = null;
        TensorCategory tensorCategory = null;
        if (typeString.equals(FeatureTypes.TENSOR.toString())) {
            switch (objectNode.get("tensorCategory").asText()) {
                case "DENSE":
                    tensorCategory = TensorCategory.DENSE;
                    break;
                case "SPARSE":
                    tensorCategory = TensorCategory.SPARSE;
                    break;
                case "RAGGED":
                    tensorCategory = TensorCategory.RAGGED;
                    break;
                default:
                    throw new RuntimeException(
                        "The specified feature type is not supported: " + typeString + ". Supported types are: "
                            + ALL_FEATURE_TYPES + " and " + SUPPORTED_TENSOR_TYPES);
            }
            featureType = FeatureTypes.TENSOR;
        } else {
            featureType = FeatureTypes.valueOf(typeString);
        }

        JsonNode docNode = objectNode.get(DOC_FIELD);
        String doc = docNode != null ? docNode.asText() : null;

        ArrayNode shapeNode = (ArrayNode) objectNode.get(SHAPE_FIELD);
        List<Integer> shapes = new ArrayList<>();
        if (shapeNode != null) {
            for (JsonNode jsonNode : shapeNode) {
                Integer shape = Integer.parseInt(jsonNode.asText());
                // -1 means unknown size. Numbers smaller than -1 is invalid.
                if (shape != -1 && shape < 1) {
                    throw new RuntimeException("Shape should either be -1 or positive numbers.");
                }
                shapes.add(shape);
            }
        }

        // For DENSE_VECTOR, shape size should be 0 or 1.
        if (featureType != FeatureTypes.TENSOR && !shapes.isEmpty()) {
            throw new RuntimeException("Only TENSOR type can have shape defined.");
        }

        // Handle tensor-related type information
        if (featureType == FeatureTypes.TENSOR) {
            ArrayNode dimensionTypeNode = (ArrayNode) objectNode.get(DIMENSION_TYPE_FIELD);
            List<Primitive> dimensionTypes = new ArrayList<>();
            if (dimensionTypeNode != null) {
                for (JsonNode jsonNode : dimensionTypeNode) {
                    Primitive primitive = Primitive.valueOf(jsonNode.asText());
                    if (tensorCategory == TensorCategory.DENSE && !DENSE_TENSOR_ALLOWED_PRIMITIVE.contains(primitive)) {
                        throw new RuntimeException("DENSE_TENSOR can only have INT dimension but got: " + primitive);
                    }
                    if (tensorCategory == TensorCategory.SPARSE && !SPARSE_TENSOR_ALLOWED_PRIMITIVE.contains(primitive)) {
                        throw new RuntimeException(
                                "SPARSE_TENSOR can only have " + SPARSE_TENSOR_ALLOWED_PRIMITIVE + " dimensions but got: " + primitive);
                    }
                    dimensionTypes.add(primitive);
                }
            }

            List<DimensionType> primitiveDimensionTypes = new ArrayList<>();
            if (shapes.isEmpty()) {
                for (Primitive dimensionType : dimensionTypes) {
                    PrimitiveDimensionType primitiveDimensionType = new PrimitiveDimensionType(dimensionType);
                    primitiveDimensionTypes.add(primitiveDimensionType);
                }
            } else {
                if (dimensionTypes.size() != shapes.size()) {
                    throw new RuntimeException(
                            "Sizes of dimensionType and shape should match but got: " + dimensionTypes + " and " + shapes);
                }
                for (int i = 0; i < dimensionTypes.size(); i++) {
                    Primitive dimensionType = dimensionTypes.get(i);
                    Integer shape = shapes.get(i);
                    PrimitiveDimensionType primitiveDimensionType = new PrimitiveDimensionType(dimensionType, shape);
                    primitiveDimensionTypes.add(primitiveDimensionType);
                }
            }

            JsonNode valType = objectNode.get(VAL_TYPE_FIELD);
            ValueType valueType = new PrimitiveType(Primitive.valueOf(valType.textValue()));
            // dimensionNames is not supported so it's always null
            tensorType = new TensorType(tensorCategory, valueType, primitiveDimensionTypes, null);
            return new FeatureTypeConfig(featureType, tensorType, doc);
        } else {
            return new FeatureTypeConfig(featureType);
        }
    }
}