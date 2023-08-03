package com.linkedin.feathr.offline;

import com.google.common.io.Resources;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.linkedin.feathr.offline.data.TrainingData;
import org.apache.avro.Schema;
import org.apache.avro.file.DataFileWriter;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.specific.SpecificDatumWriter;


/**
 * MockAvroData contains static functions enabling:
 * - mock Avro records for the purpose of unit testing (mock training data and feature data)
 * - write mock data to local disk
 */
public class MockAvroData {

    private static final String a_1 = "a:1";
    private static final String a_2 = "a:2";
    private static final String a_3 = "a:3";

    /**
     * Create and write to disk, Avro records for training data keyed by a Id
     */
    public static List<GenericRecord> generateMockxTrainingData() {
        TrainingData training1 = TrainingData.newBuilder()
                .setIdInObservation(a_1)
                .setLabel(0).build();
        TrainingData training2 = TrainingData.newBuilder()
                .setIdInObservation(a_2)
                .setLabel(1).build();
        TrainingData training3 = TrainingData.newBuilder()
                .setIdInObservation(a_3)
                .setLabel(0).build();

        List<GenericRecord> mockTrainingData = Arrays.asList(training1, training2, training3);
        return mockTrainingData;
    }

    /**
     * Create and write to disk, Avro records for training data keyed by a Id
     * @param outputFolder path to write Avro records to
     * @throws Exception
     */
    public static List<GenericRecord> createMockxTrainingData(String outputFolder) throws Exception {
        List<GenericRecord> mockTrainingData = generateMockxTrainingData();
        write(mockTrainingData, outputFolder);
        return mockTrainingData;
    }


    /**
     * Create and write to disk, Avro records for feature data keyed by a Id
     *
     * @param outputFolder path to write Avro records to
     */
    public static List<GenericRecord> createMockxFeatureData(String outputFolder) throws Exception {

        List<Float> aEmbedding_1 = Arrays.asList(1.0F, 2.0F, 3.0F);
        List<Float> aEmbedding_2 = Arrays.asList(4.0F, 5.0F, 6.0F);
        Map<CharSequence, Float> aEmbeddingStringToFloat_1 = new HashMap<>();
        aEmbeddingStringToFloat_1.put("A", 7.0f);
        Map<CharSequence, Float> aEmbeddingStringToFloat_2 = new HashMap<>();
        aEmbeddingStringToFloat_2.put("A", 8.0f);
        Map<CharSequence, Long> aEmbeddingStringToLong_1 = new HashMap<>();
        aEmbeddingStringToLong_1.put("A", 9L);
        Map<CharSequence, Long> aEmbeddingStringToLong_2 = new HashMap<>();
        aEmbeddingStringToLong_2.put("A", 10L);
        Map<CharSequence, Integer> aEmbeddingStringToInt_1 = new HashMap<>();
        aEmbeddingStringToInt_1.put("A", 11);
        Map<CharSequence, Integer> aEmbeddingStringToInt_2 = new HashMap<>();
        aEmbeddingStringToInt_2.put("A", 12);
        Map<CharSequence, Double> aEmbeddingStringToDouble_1 = new HashMap<>();
        aEmbeddingStringToDouble_1.put("A", 13.0);
        Map<CharSequence, Double> aEmbeddingStringToDouble_2 = new HashMap<>();
        aEmbeddingStringToDouble_2.put("A", 14.0);
        Map<CharSequence, CharSequence> aEmbeddingStringToString_1 = new HashMap<>();
        aEmbeddingStringToString_1.put("A", "B");
        Map<CharSequence, CharSequence> aEmbeddingStringToString_2 = new HashMap<>();
        aEmbeddingStringToString_2.put("A", "C");
        List<CharSequence> aEmbeddingStringArray_1 = Arrays.asList("A", "B");
        List<CharSequence> aEmbeddingStringArray_2 = Arrays.asList("C", "D");

        // Write the sample data using a 'new' version of this schema (xFeatureData_NewSchema.avsc) that is forward-
        // compatible with the "old" version of the schema (xFeatureData.avsc).
        // A custom-coded AnchorExtractor whose input type is a SpecificRecord that was codegen'd using the old version of
        // the schema, should still be able to process data written with a newer schema without error, as long as the new
        // schema is forward-compatible.
        // Further reading: https://docs.confluent.io/current/avro.html#forward-compatibility
        Schema schema = new Schema.Parser().parse(Resources.getResource("xFeatureData_NewSchema.avsc").openStream());

        GenericRecord a1Features = new GenericData.Record(schema);
        a1Features.put("IdInFeatureData", a_1);
        a1Features.put("a", 1);
        a1Features.put("b", 2.6F);
        a1Features.put("c", aEmbedding_1);
        a1Features.put("d", aEmbeddingStringToString_1);
        a1Features.put("e", aEmbeddingStringToFloat_1);
        a1Features.put("f", aEmbeddingStringArray_1);
        a1Features.put("g", aEmbeddingStringToLong_1);
        a1Features.put("h", aEmbeddingStringToInt_1);
        a1Features.put("i", aEmbeddingStringToDouble_1);
        a1Features.put("newField", "foobar");

        GenericRecord a2Features = new GenericData.Record(schema);
        a2Features.put("IdInFeatureData", a_2);
        a2Features.put("a", 4);
        a2Features.put("b", 8F);
        a2Features.put("c", aEmbedding_2);

        a2Features.put("d", aEmbeddingStringToString_2);
        a2Features.put("e", aEmbeddingStringToFloat_2);
        a2Features.put("f", aEmbeddingStringArray_2);
        a2Features.put("g", aEmbeddingStringToLong_2);
        a2Features.put("h", aEmbeddingStringToInt_2);
        a2Features.put("i", aEmbeddingStringToDouble_2);
        a2Features.put("newField", "foo");

        List<GenericRecord> mockaFeaturesData = Arrays.asList(a1Features, a2Features);
        write(mockaFeaturesData, outputFolder);
        return mockaFeaturesData;
    }

    /**
     * Write a given list of Avro generic records to a given path
     *
     * @param mockData     list of Avro generic records to write to disk
     * @param outputFolder path to write Avro records to
     */
    public static void write(List<GenericRecord> mockData, String outputFolder) throws Exception {

        File outputFolderFile = new File(outputFolder);
        if (!outputFolderFile.exists()) {
            outputFolderFile.mkdirs();
        }

        String outputPath = outputFolder + "/part-00000.avro";
        File outputPathFile = new File(outputPath);
        if (outputPathFile.exists()) {
            outputPathFile.delete();
        }

        Schema schema = mockData.get(0).getSchema();
        OutputStream outputStream = new FileOutputStream(outputPath);

        SpecificDatumWriter<GenericRecord> writer = new SpecificDatumWriter<>(schema);
        DataFileWriter<GenericRecord> dataFileWriter = new DataFileWriter<>(writer);
        dataFileWriter.create(schema, outputStream);

        for (GenericRecord record : mockData) {
            dataFileWriter.append(record);
        }

        dataFileWriter.close();
        outputStream.close();
    }

}
