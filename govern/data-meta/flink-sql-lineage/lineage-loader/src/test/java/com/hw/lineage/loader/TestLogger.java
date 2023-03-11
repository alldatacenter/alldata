package com.hw.lineage.loader;

import org.junit.Rule;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Adds automatic test name logging. Every test which wants to log which test is currently executed
 * and why it failed, simply has to extend this class.
 *
 * @description: TestLogger
 * @author: HamaWhite
 * @version: 1.0.0
 */
public class TestLogger {
    protected final Logger log = LoggerFactory.getLogger(getClass());

    @Rule
    public TestRule watchman =
            new TestWatcher() {

                @Override
                public void starting(Description description) {
                    log.info(
                            "\n================================================================================"
                                    + "\nTest {} is running."
                                    + "\n--------------------------------------------------------------------------------",
                            description);
                }

                @Override
                public void succeeded(Description description) {
                    log.info(
                            "\n--------------------------------------------------------------------------------"
                                    + "\nTest {} successfully run."
                                    + "\n================================================================================",
                            description);
                }

                @Override
                public void failed(Throwable e, Description description) {
                    log.error(
                            "\n--------------------------------------------------------------------------------"
                                    + "\nTest {} failed with:\n{}"
                                    + "\n================================================================================",
                            description,
                            exceptionToString(e));
                }
            };

    private static String exceptionToString(Throwable t) {
        if (t == null) {
            return "(null)";
        }

        try {
            StringWriter stm = new StringWriter();
            PrintWriter wrt = new PrintWriter(stm);
            t.printStackTrace(wrt);
            wrt.close();
            return stm.toString();
        } catch (Throwable ignored) {
            return t.getClass().getName() + " (error while printing stack trace)";
        }
    }

}
