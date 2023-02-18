package com.datasophon.api.test;

import com.datasophon.api.utils.ProcessUtils;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class ProcessUtilsTest {

    @Test
    public void testGenerateGlobalVariables(){
        Map<String,String> globalVariables =new HashMap<>();
        String variableName = "test";
        String variableValue = "test";
        ProcessUtils.generateClusterVariable(globalVariables,22,variableName,variableValue);
    }
}
