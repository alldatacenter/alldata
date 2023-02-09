package com.qcloud.cos;

import com.qcloud.cos.model.*;
import org.junit.Test;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.junit.Assert.assertEquals;
import org.junit.AfterClass;
import org.junit.BeforeClass;

public class SelectObjectTest extends AbstractCOSClientTest{
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        AbstractCOSClientTest.initCosClient();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        AbstractCOSClientTest.destoryCosClient();
    }
    @Test
    public void testSelectCsvObject() throws Exception {
        String key = "test/my_test.csv";
        String csvContent = "HuNan,ChangSha\nSiChuan,ChengDu\nGuiZhou,GuiYang\n";
        cosclient.putObject(bucket, key, csvContent);
        String query = "select * from COSObject s where _1 = 'HuNan'";

        SelectObjectContentRequest request = new SelectObjectContentRequest();
        request.setBucketName(bucket);
        request.setKey(key);
        request.setExpression(query);
        request.setExpressionType(ExpressionType.SQL);

        InputSerialization inputSerialization = new InputSerialization();
        CSVInput csvInput = new CSVInput();
        csvInput.setFieldDelimiter(",");
        csvInput.setRecordDelimiter("\n");
        inputSerialization.setCsv(csvInput);
        inputSerialization.setCompressionType(CompressionType.NONE);
        request.setInputSerialization(inputSerialization);

        OutputSerialization outputSerialization = new OutputSerialization();
        outputSerialization.setCsv(new CSVOutput());
        request.setOutputSerialization(outputSerialization);

        final AtomicBoolean isResultComplete = new AtomicBoolean(false);
        SelectObjectContentResult result = cosclient.selectObjectContent(request);
        InputStream resultInputStream = result.getPayload().getRecordsInputStream(
                new SelectObjectContentEventVisitor() {
                    @Override
                    public void visit(SelectObjectContentEvent.StatsEvent event)
                    {
                        System.out.println(
                                "Received Stats, Bytes Scanned: " + event.getDetails().getBytesScanned()
                                        +  " Bytes Processed: " + event.getDetails().getBytesProcessed());
                    }
                    @Override
                    public void visit(SelectObjectContentEvent.EndEvent event)
                    {
                        isResultComplete.set(true);
                        System.out.println("Received End Event. Result is complete.");
                    }
                }
        );
        BufferedReader reader = new BufferedReader(new InputStreamReader(resultInputStream));
        StringBuffer stringBuffer = new StringBuffer();
        String line;
        while((line = reader.readLine())!= null){
            stringBuffer.append(line).append("\n");
        }
        assertEquals(stringBuffer.toString(), "HuNan,ChangSha\n");
        cosclient.deleteObject(bucket, key);
    }

    @Test
    public void testSelectJsonObject() throws Exception {
        String key = "test/my_test.json";
        String csvContent = "{\"name\":\"xiaoming\",\"mathScore\":89,\"musicScore\":92}\n" +
                "{\"name\":\"xiaowang\",\"mathScore\":93,\"musicScore\":85}\n" +
                "{\"name\":\"xiaoli\",\"mathScore\":82,\"musicScore\":95}\n";
        cosclient.putObject(bucket, key, csvContent);
        String query = "select * from COSObject s where mathScore > 90'";

        SelectObjectContentRequest request = new SelectObjectContentRequest();
        request.setBucketName(bucket);
        request.setKey(key);
        request.setExpression(query);
        request.setExpressionType(ExpressionType.SQL);

        InputSerialization inputSerialization = new InputSerialization();
        JSONInput jsonInput = new JSONInput();
        jsonInput.setType(JSONType.LINES);
        inputSerialization.setJson(jsonInput);
        inputSerialization.setCompressionType(CompressionType.NONE);
        request.setInputSerialization(inputSerialization);

        OutputSerialization outputSerialization = new OutputSerialization();
        outputSerialization.setJson(new JSONOutput());
        request.setOutputSerialization(outputSerialization);

        final AtomicBoolean isResultComplete = new AtomicBoolean(false);
        SelectObjectContentResult result = cosclient.selectObjectContent(request);
        InputStream resultInputStream = result.getPayload().getRecordsInputStream(
                new SelectObjectContentEventVisitor() {
                    @Override
                    public void visit(SelectObjectContentEvent.StatsEvent event)
                    {
                        System.out.println(
                                "Received Stats, Bytes Scanned: " + event.getDetails().getBytesScanned()
                                        +  " Bytes Processed: " + event.getDetails().getBytesProcessed());
                    }
                    @Override
                    public void visit(SelectObjectContentEvent.EndEvent event)
                    {
                        isResultComplete.set(true);
                        System.out.println("Received End Event. Result is complete.");
                    }
                }
        );
        BufferedReader reader = new BufferedReader(new InputStreamReader(resultInputStream));
        StringBuffer stringBuffer = new StringBuffer();
        String line;
        while((line = reader.readLine())!= null){
            stringBuffer.append(line).append("\n");
        }
        assertEquals(stringBuffer.toString(), "{\"name\":\"xiaowang\",\"mathScore\":93,\"musicScore\":85}\n");
        cosclient.deleteObject(bucket, key);
    }
}
