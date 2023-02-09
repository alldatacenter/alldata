package com.obs.test;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;

public abstract class LogableCase {

    private Logger logger;

    private String caseName;
    
    public LogableCase(Logger logger, String caseName) {
        this.logger = logger;
        this.caseName = caseName;
    }

    abstract void action();
    
    /**
     * 执行测试，并记录日志
     */
    public final void doTest() {
        ThreadContext.put("caseName", this.getCaseName());
        logger.info("=====================  Start TestCase.  =====================");
        String result = "successfully";
        try{
            action();
        } catch(Exception e) {
            logger.error("Failed.");
            e.printStackTrace();
            result = "failure";
            throw e;
        } finally {
            logger.info("End TestCase, Execute " + result);
            ThreadContext.put("caseName", "No Case.");
        }
    }

    public final Logger getLogger() {
        return logger;
    }

    public final String getCaseName() {
        return caseName;
    }
}
