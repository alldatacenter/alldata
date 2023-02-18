package com.datasophon.worker.handler;

import com.datasophon.common.model.Generators;
import com.datasophon.common.model.ServiceConfig;
import com.datasophon.common.utils.ExecResult;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConfigureServiceHandlerTest {

    private ConfigureServiceHandler configureServiceHandlerUnderTest;

    @Before
    public void setUp() {
        configureServiceHandlerUnderTest = new ConfigureServiceHandler();
    }

    @Test
    public void testConfigure() {
        // Setup
        final Map<Generators, List<ServiceConfig>> cofigFileMap = new HashMap<>();

        // Run the test
//        final ExecResult result = configureServiceHandlerUnderTest.configure(cofigFileMap, "decompressPackageName", 0,
//                "serviceRoleName");

        // Verify the results
    }
}
