package com.hw.lineage.flink;

import com.hw.lineage.flink.cep.CepTest;
import com.hw.lineage.flink.common.CommonTest;
import com.hw.lineage.flink.localtimestamp.LocaltimestampTest;
import com.hw.lineage.flink.lookup.join.LookupJoinTest;
import com.hw.lineage.flink.tablefuncion.TableFunctionTest;
import com.hw.lineage.flink.watermark.WatermarkTest;
import com.hw.lineage.flink.window.WindowTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * @description: SuiteTest
 * @author: HamaWhite
 * @version: 1.0.0
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({CommonTest.class
        , LookupJoinTest.class
        , TableFunctionTest.class
        , WatermarkTest.class
        , LocaltimestampTest.class
        , CepTest.class
        , WindowTest.class})
public class SuiteTest {

    /**
     * The entry class of the test suite is just to organize the test classes together for testing,
     * without any test methods.
     */
}
