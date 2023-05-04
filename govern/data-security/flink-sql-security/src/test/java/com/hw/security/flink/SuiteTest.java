package com.hw.security.flink;

import com.hw.security.flink.common.CommonTest;
import com.hw.security.flink.execute.ExecuteDataMaskTest;
import com.hw.security.flink.execute.ExecuteRowFilterTest;
import com.hw.security.flink.execute.MixedExecuteTest;
import com.hw.security.flink.rewrite.MixedRewriteTest;
import com.hw.security.flink.rewrite.RewriteDataMaskTest;
import com.hw.security.flink.rewrite.RewriteRowFilterTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * @description: SuiteTest
 * @author: HamaWhite
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({CommonTest.class
        , PolicyManagerTest.class
        , RewriteRowFilterTest.class
        , RewriteDataMaskTest.class
        , MixedRewriteTest.class
        , ExecuteRowFilterTest.class
        , ExecuteDataMaskTest.class
        , MixedExecuteTest.class})
public class SuiteTest {
    /*
      The entry class of the test suite is just to organize the test classes together for testing,
      without any test methods.
     */
}
