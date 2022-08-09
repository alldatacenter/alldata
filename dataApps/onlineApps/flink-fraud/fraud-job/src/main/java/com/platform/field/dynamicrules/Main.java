package com.platform.field.dynamicrules;
import static com.platform.field.config.Parameters.BOOL_PARAMS;
import static com.platform.field.config.Parameters.INT_PARAMS;
import static com.platform.field.config.Parameters.STRING_PARAMS;
import com.platform.field.config.Config;
import com.platform.field.config.Parameters;
import org.apache.flink.api.java.utils.ParameterTool;

public class Main {

  public static void main(String[] args) throws Exception {
    ParameterTool tool = ParameterTool.fromArgs(args);
    Parameters inputParams = new Parameters(tool);
    Config config = new Config(inputParams, STRING_PARAMS, INT_PARAMS, BOOL_PARAMS);
    RulesEvaluator rulesEvaluator = new RulesEvaluator(config);
    rulesEvaluator.run();
  }
}
