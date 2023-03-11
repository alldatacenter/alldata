package com.hw.lineage.flink.proctime;

import com.hw.lineage.flink.basic.AbstractBasicTest;
import org.junit.Before;
import org.junit.Test;

/**
 * The solution of PROCTIME() is the same as LOCALTIMESTAMP, it is enhanced in the getColumnOrigins(Calc rel...) method.
 * LOCALTIMESTAMP has been processed, so the lineage of PROCTIME() can be directly parsed out
 *
 * @description: ProctimeTest
 * @author: HamaWhite
 * @version: 1.0.0
 */
public class ProctimeTest extends AbstractBasicTest {

    @Before
    public void createTable() {
        // create datagen source table datagen_source
        createTableOfDatagenSource();

        // create print sink table print_sink
        createTableOfPrintSink();
    }

    /**
     * Optimized RelNode:
     * <pre>
     *  FlinkLogicalCalc(select=[id, name, PROCTIME() AS make_time])
     *   FlinkLogicalTableSourceScan(table=[[hive, default, datagen_source]], fields=[id, name])
     * </pre>
     */

    @Test
    public void testInsertSelectProctime() {
        String sql = "INSERT INTO print_sink(id, name, make_time) " +
                "SELECT " +
                "       id                  ," +
                "       name                ," +
                "       make_time            " +
                "FROM" +
                "       datagen_source ";

        String[][] expectedArray = {
                {"datagen_source", "id", "print_sink", "id"},
                {"datagen_source", "name", "print_sink", "name"},
                {"datagen_source", "make_time", "print_sink", "make_time"}
        };

        parseFieldLineage(sql, expectedArray);

    }


    /**
     * Create datagen source table datagen_source
     */
    protected void createTableOfDatagenSource() {
        context.execute("DROP TABLE IF EXISTS datagen_source ");

        context.execute("CREATE TABLE IF NOT EXISTS datagen_source ( " +
                "       id              INT                                  ," +
                "       name            STRING                               ," +
                "       make_time       AS PROCTIME()                         " +
                ") WITH (                                                     " +
                "       'connector' = 'datagen'                               " +
                ")"
        );
    }


    /**
     * Create print sink table print_sink
     */
    protected void createTableOfPrintSink() {

        context.execute("DROP TABLE IF EXISTS print_sink ");

        context.execute("CREATE TABLE IF NOT EXISTS print_sink (    " +
                "       id              INT                                 ," +
                "       name            STRING                              ," +
                "       make_time       TIMESTAMP(3)                         " +
                ") WITH (                                                    " +
                "       'connector' = 'print'                                " +
                ")"
        );
    }
}
