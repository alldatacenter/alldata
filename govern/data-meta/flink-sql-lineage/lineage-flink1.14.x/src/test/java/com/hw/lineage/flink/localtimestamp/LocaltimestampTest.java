package com.hw.lineage.flink.localtimestamp;

import com.hw.lineage.flink.basic.AbstractBasicTest;
import org.junit.Before;
import org.junit.Test;

/**
 * @description: LocaltimestampTest
 * @author: HamaWhite
 * @version: 1.0.0
 */
public class LocaltimestampTest extends AbstractBasicTest {

    @Before
    public void createTable() {
        // create datagen source table datagen_source
        createTableOfDatagenSource();

        // create print sink table print_sink
        createTableOfPrintSink();
    }


    /**
     * Optimized RelNode: FlinkLogicalWatermarkAssigner(rowtime=[ts], watermark=[$3])
     * FlinkLogicalCalc(select=[id, name, LOCALTIMESTAMP() AS birthday, LOCALTIMESTAMP() AS ts])
     * FlinkLogicalTableSourceScan(table=[[hive, flink_demo, datagen_source]], fields=[id, name])
     * <p>
     * Since FlinkLogicalTableSourceScan in Optimized RelNode only has id and name fields, there are
     * no birthday and ts fields. The birthday and ts fields have been optimized to LOCALTIMESTAMP()
     * AS birthday, LOCALTIMESTAMP() AS ts. Therefore, in the getColumnOrigins(Calc rel, final
     * RelMetadataQuery mq, int iOutputColumn) method, under special treatment for LOCALTIMESTAMP()
     * AS birthday, LOCALTIMESTAMP() AS ts
     */
    @Test
    public void testInsertSelectLocaltimestamp() {
        String sql = "INSERT INTO print_sink " +
                "SELECT " +
                "       id          ," +
                "       name        ," +
                "       birthday    ," +
                "       ts           " +
                "FROM" +
                "       datagen_source ";

        String[][] expectedArray = {
                {"datagen_source", "id", "print_sink", "id"},
                {"datagen_source", "name", "print_sink", "name"},
                {"datagen_source", "birthday", "print_sink", "birthday"},
                {"datagen_source", "ts", "print_sink", "ts"}
        };

        parseFieldLineage(sql, expectedArray);

    }


    /**
     * Create datagen source table datagen_source
     */
    protected void createTableOfDatagenSource() {
        context.execute("DROP TABLE IF EXISTS datagen_source ");

        context.execute("CREATE TABLE IF NOT EXISTS datagen_source (" +
                "       id              INT                        ," +
                "       name            STRING                     ," +
                "       birthday        AS LOCALTIMESTAMP          ," +
                "       ts              AS LOCALTIMESTAMP          ," +
                "       WATERMARK FOR ts AS ts                      " +
                ") WITH ( " +
                "       'connector' = 'datagen'                     " +
                ")"
        );
    }


    /**
     * Create print sink table print_sink
     */
    protected void createTableOfPrintSink() {

        context.execute("DROP TABLE IF EXISTS print_sink ");

        context.execute("CREATE TABLE IF NOT EXISTS print_sink (" +
                "       id              INT                         ," +
                "       name            STRING                      ," +
                "       birthday        TIMESTAMP(3)                ," +
                "       ts              TIMESTAMP(3)                 " +
                ") WITH ( " +
                "       'connector' = 'print'                        " +
                ")"
        );
    }
}
