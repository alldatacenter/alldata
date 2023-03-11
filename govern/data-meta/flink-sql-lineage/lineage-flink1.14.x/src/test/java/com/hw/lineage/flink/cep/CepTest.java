package com.hw.lineage.flink.cep;

import com.hw.lineage.flink.basic.AbstractBasicTest;
import org.junit.Before;
import org.junit.Test;

/**
 * The test case comes from https://www.jianshu.com/p/c0b76abe4224, thanks
 *
 * @description: CepTest
 * @author: HamaWhite
 * @version: 1.0.0
 */
public class CepTest extends AbstractBasicTest {

    @Before
    public void createTable() {
        // create datagen source table temperature_source
        createTableOfTemperatureSource();

        // create print sink table print_sink
        createTableOfPrintSink();
    }

    @Test
    public void testInsertSelectCep() {
        String sql = "INSERT INTO print_sink                                " +
                "SELECT                                                     " +
                "   *                                                       " +
                "FROM                                                       " +
                "   temperature_source MATCH_RECOGNIZE (                    " +
                "       PARTITION BY rack_id                                " +
                "       ORDER BY ts                                         " +
                "       MEASURES                                            " +
                "           A.ts as start_ts,                               " +
                "           LAST(B.ts) as end_ts,                           " +
                "           A.temperature as start_temp,                    " +
                "           LAST(B.temperature) as end_temp,                " +
                "           AVG(B.temperature) as avg_temp                  " +
                "           ONE ROW PER MATCH                               " +
                "           AFTER MATCH SKIP TO NEXT ROW                    " +
                "           PATTERN (A B+ C) WITHIN INTERVAL '90' second    " +
                "           DEFINE                                          " +
                "               A as A.temperature < 50,                    " +
                "               B as B.temperature >=50,                    " +
                "               C as C.temperature < 50                     " +
                "    )";

        /**
         * Variables A and B in transform are not replaced.
         * LAST and AVG functions have also been replaced.
         */
        String[][] expectedArray = {
                {"temperature_source", "rack_id", "print_sink", "rack_id"},
                {"temperature_source", "ts", "print_sink", "start_ts", "A.ts"},
                {"temperature_source", "ts", "print_sink", "end_ts", "LAST(B.ts, 0)"},
                {"temperature_source", "temperature", "print_sink", "start_temp", "A.temperature"},
                {"temperature_source", "temperature", "print_sink", "end_temp", "LAST(B.temperature, 0)"},
                {"temperature_source", "temperature", "print_sink", "avg_temp", "CAST(/(SUM(B.temperature), COUNT(B.temperature))):INTEGER"}
        };

        parseFieldLineage(sql, expectedArray);
    }


    @Test
    public void testInsertSelectViewCep() {
        context.execute("DROP VIEW IF EXISTS temperature_view ");

        context.execute("CREATE VIEW IF NOT EXISTS temperature_view AS " +
                "SELECT                                                     " +
                "   *                                                       " +
                "FROM                                                       " +
                "   temperature_source MATCH_RECOGNIZE (                    " +
                "       PARTITION BY rack_id                                " +
                "       ORDER BY ts                                         " +
                "       MEASURES                                            " +
                "           A.ts as start_ts,                               " +
                "           LAST(B.ts) as end_ts,                           " +
                "           A.temperature as start_temp,                    " +
                "           LAST(B.temperature) as end_temp,                " +
                "           AVG(B.temperature) as avg_temp                  " +
                "           ONE ROW PER MATCH                               " +
                "           AFTER MATCH SKIP TO NEXT ROW                    " +
                "           PATTERN (A B+ C) WITHIN INTERVAL '90' second    " +
                "           DEFINE                                          " +
                "               A as A.temperature < 50,                    " +
                "               B as B.temperature >=50,                    " +
                "               C as C.temperature < 50                     " +
                "   )"
        );

        String sql = "INSERT INTO print_sink (rack_id, start_ts, end_ts, start_temp, end_temp, avg_temp) " +
                "SELECT " +
                "   rack_id     ," +
                "   start_ts    ," +
                "   end_ts      ," +
                "   start_temp  ," +
                "   end_temp    ," +
                "   avg_temp     " +
                "FROM" +
                "   temperature_view";

        /**
         * Variables A and B in transform are not replaced.
         * LAST and AVG functions have also been replaced, and add CAST function.
         */
        String[][] expectedArray = {
                {"temperature_source", "rack_id", "print_sink", "rack_id"},
                {"temperature_source", "ts", "print_sink", "start_ts", "CAST(A.ts):TIMESTAMP(3)"},
                {"temperature_source", "ts", "print_sink", "end_ts", "CAST(LAST(B.ts, 0)):TIMESTAMP(3)"},
                {"temperature_source", "temperature", "print_sink", "start_temp", "A.temperature"},
                {"temperature_source", "temperature", "print_sink", "end_temp", "LAST(B.temperature, 0)"},
                {"temperature_source", "temperature", "print_sink", "avg_temp", "CAST(/(SUM(B.temperature), COUNT(B.temperature))):INTEGER"}
        };

        parseFieldLineage(sql, expectedArray);
    }


    /**
     * Create datagen source table temperature_source
     */
    protected void createTableOfTemperatureSource() {
        context.execute("DROP TABLE IF EXISTS temperature_source ");

        context.execute("CREATE TABLE IF NOT EXISTS temperature_source (" +
                "       rack_id                 INT                         ," +
                "       ts                      TIMESTAMP(3)                ," +
                "       temperature             INT                         ," +
                "       WATERMARK FOR ts AS ts - INTERVAL '1' SECOND         " +
                ") WITH ( " +
                "       'connector' = 'filesystem'                          ," +
                "       'path' = 'data/temperature_record.csv'              ," +
                "       'format' = 'csv'                                     " +
                ")"
        );
    }


    /**
     * Create print sink table print_sink
     */
    protected void createTableOfPrintSink() {
        context.execute("DROP TABLE IF EXISTS print_sink ");

        context.execute("CREATE TABLE IF NOT EXISTS print_sink (" +
                "       rack_id             INT                         ," +
                "       start_ts            TIMESTAMP(3)                ," +
                "       end_ts              TIMESTAMP(3)                ," +
                "       start_temp          INT                         ," +
                "       end_temp            INT                         ," +
                "       avg_temp            INT                         " +
                ") WITH ( " +
                "       'connector' = 'print'                        " +
                ")"
        );
    }
}
