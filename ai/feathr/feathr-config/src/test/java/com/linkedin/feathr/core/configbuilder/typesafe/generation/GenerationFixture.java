package com.linkedin.feathr.core.configbuilder.typesafe.generation;

import com.linkedin.feathr.core.config.common.DateTimeConfig;
import com.linkedin.feathr.core.config.common.OutputFormat;
import com.linkedin.feathr.core.config.generation.FeatureGenConfig;
import com.linkedin.feathr.core.config.generation.NearlineOperationalConfig;
import com.linkedin.feathr.core.config.generation.OfflineOperationalConfig;
import com.linkedin.feathr.core.config.generation.OperationalConfig;
import com.linkedin.feathr.core.config.generation.OutputProcessorConfig;
import com.typesafe.config.ConfigFactory;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.TimeZone;


public class GenerationFixture {

  static final String generationConfigStr1 =
      String.join("// operational section\n",
          "operational: {\n",
           "  name: XAffinity\n",
           "  endTime: \"2018-05-08\"  // specify a date/time, or ‘NOW’\n",
           "  endTimeFormat: \"yyyy-MM-dd\"\n",
           "  resolution: DAILY      // DAILY/HOURLY\n",
           "  timeDelay: 2 days  // default value is 1, which means generate yesterday’ data\n",
           "  retention: 3 days // only keep one snapshot for frame access and incremental aggregation\n",
           "  offset: 4 days \n",
           "  enableIncremental: true\n",
           "  timeZone: \"America/Los_Angeles\" \n",
           "  output: [            // accept a list of output processors\n",
           "    { name: HDFS           \n",
           "      outputFormat: RAW_DATA // output format can be customized when user changed the feature \n",
           "                      // schema in the processor, or just keep the input format to pass to next\n",
           "                      // processor \n",
           "      params: {     \n",
           "          path: \"/jobs/frame/df\"    // processor can take arbitrary parameters\n",
           "      } \n",
           "    }\n",
           "    {\n",
           "      name: VENICE  \n",
           "      outputFormat: NAME_TERM_VALUE \n",
           "      params: {     \n",
           "        path: \"/jobs/frame/NAME_TERM_VALUE/daily\" // this will be extended according to time set in each\n",
           "                                    // operational section, e.g,  /jobs/frame/daily/2019/02/02”\n",
           "      } \n",
           "    } \n",
           "  ]\n",
           "}\n ",
           "// features section, specify list of features to generate\n",
           "features: [F1, F2]");

  static final FeatureGenConfig expGenerationConfigObj1;
  static {
    Duration offset = Duration.ofDays(4);
    TimeZone timeZone = TimeZone.getTimeZone("America/Los_Angeles");
    DateTimeConfig timeSettings = new DateTimeConfig("2018-05-08", "yyyy-MM-dd",
        ChronoUnit.DAYS, 0, offset, timeZone) ;
    OutputProcessorConfig hdfsProcessor = new OutputProcessorConfig("HDFS", OutputFormat.RAW_DATA,
        ConfigFactory.parseString("{path:/jobs/frame/df}"));
    OutputProcessorConfig veniceProcessor = new OutputProcessorConfig("VENICE",
        OutputFormat.NAME_TERM_VALUE, ConfigFactory.parseString("{path: /jobs/frame/NAME_TERM_VALUE/daily}"));

    List<OutputProcessorConfig> outputProcessorConfigList = Arrays.asList(hdfsProcessor, veniceProcessor);
    Duration retention = Duration.ofDays(3);
    String name = "XAffinity";
    Duration simulateTImeDelay = Duration.ofDays(2);
    Boolean enableIncremental = Boolean.TRUE;
    OperationalConfig operationalConfig =
        new OfflineOperationalConfig(outputProcessorConfigList, name, timeSettings, retention, simulateTImeDelay, enableIncremental);
    List<String> features = Arrays.asList("F1", "F2");
    expGenerationConfigObj1 = new FeatureGenConfig(operationalConfig, features);
  }

  static final String generationConfigStr2 =
      String.join("// operational section\n",
           "operational: {\n",
           "  name: XAffinity\n",
           "  endTime: \"2018-05-08 17:00:00\"  // specify a date/time, or ‘NOW’\n",
           "  endTimeFormat: \"yyyy-MM-dd hh:mm:ss\"\n",
           "  resolution: HOURLY      // DAILY/HOURLY\n",
           "  enableIncremental: true\n",
           "  output: [            // accept a list of output processors\n",
           "    {      \n",
           "      name: HDFS    \n",
           "      outputFormat: NAME_TERM_VALUE   // output format can be customized when user changed the feature \n",
           "                      // schema in the processor, or just keep the input format to pass to next\n",
           "                      // processor \n",
           "      params: {     \n",
           "          path: \"/jobs/frame/df\"    // processor can take arbitrary parameters\n",
           "      } \n",
           "    }\n",
           "  ]\n",
           "}\n ",
           "// features section, specify list of features to generate\n",
           "features: [F1, F2]");

  static final FeatureGenConfig expGenerationConfigObj2;
  static {
    Duration offset = Duration.ofHours(0);
    TimeZone timeZone = TimeZone.getTimeZone("America/Los_Angeles");
    DateTimeConfig timeSettings = new DateTimeConfig("2018-05-08 17:00:00", "yyyy-MM-dd hh:mm:ss",
        ChronoUnit.HOURS, 0, offset, timeZone);
    OutputProcessorConfig hdfsProcessor = new OutputProcessorConfig("HDFS", OutputFormat.NAME_TERM_VALUE,
        ConfigFactory.parseString("{path:/jobs/frame/df}"));
    List<OutputProcessorConfig>
        outputProcessorConfigList = Arrays.asList(hdfsProcessor);
    Duration retention = Duration.ofHours(1);
    String name = "XAffinity";
    Duration simulateTImeDelay = Duration.ofHours(0);
    Boolean enableIncremental = Boolean.TRUE;
    OperationalConfig operationalConfig =
        new OfflineOperationalConfig(outputProcessorConfigList, name, timeSettings, retention, simulateTImeDelay, enableIncremental);
    List<String> features = Arrays.asList("F1", "F2");
    expGenerationConfigObj2 = new FeatureGenConfig(operationalConfig, features);
  }

  static final String nearlineGenerationConfigStr =
      String.join("// operational section\n",
          "operational: {\n",
          "  name: XAffinity\n",
          "  output: [            // accept a list of output processors\n",
          "    {      \n",
          "      name: KAFKA    \n",
          "      outputFormat: NAME_TERM_VALUE   // output format can be customized when user changed the feature \n",
          "                      // schema in the processor, or just keep the input format to pass to next\n",
          "                      // processor \n",
          "      params: {     \n",
          "          type: KAFKA",
          "          topic: kafkaTopic",
          "          path: \"/jobs/frame/df\"    // processor can take arbitrary parameters\n",
          "      } \n",
          "    }\n",
          "    {      \n",
          "      name: VENICE    \n",
          "      outputFormat: NAME_TERM_VALUE   // output format can be customized when user changed the feature \n",
          "                      // schema in the processor, or just keep the input format to pass to next\n",
          "                      // processor \n",
          "      params: {     \n",
          "          type: VENICE",
          "          store: veniceStore",
          "      } \n",
          "    }\n",
          "    {      \n",
          "      name: ESPRESSO    \n",
          "      outputFormat: NAME_TERM_VALUE   // output format can be customized when user changed the feature \n",
          "                      // schema in the processor, or just keep the input format to pass to next\n",
          "                      // processor \n",
          "      params: {     \n",
          "          type: ESPRESSO",
          "          store: espressoStore",
          "          table: tableName",
          "          d2uri: d2uri",
          "      } \n",
          "    }\n",
          "    {      \n",
          "      name: LOG    \n",
          "      outputFormat: NAME_TERM_VALUE   // output format can be customized when user changed the feature \n",
          "                      // schema in the processor, or just keep the input format to pass to next\n",
          "                      // processor \n",
          "      params: {     \n",
          "          type: CONSOLE",
          "      } \n",
          "    }\n",
          "  ]\n",
          "  env: NEARLINE\n",
          "}\n ",
          "// features section, specify list of features to generate\n",
          "features: [F1, F2]");

  static final FeatureGenConfig nearlineGenerationConfigObj;
  static {
    OutputProcessorConfig kafkaProcessor = new OutputProcessorConfig("KAFKA", OutputFormat.NAME_TERM_VALUE,
        ConfigFactory.parseString("{type: KAFKA\n topic: kafkaTopic\n path:/jobs/frame/df}"));
    OutputProcessorConfig veniceProcessor = new OutputProcessorConfig("VENICE", OutputFormat.NAME_TERM_VALUE,
        ConfigFactory.parseString("{type: VENICE\n store: veniceStore\n}"));
    OutputProcessorConfig espressoProcessor = new OutputProcessorConfig("ESPRESSO", OutputFormat.NAME_TERM_VALUE,
        ConfigFactory.parseString("{type: ESPRESSO\n store: espressoStore\n table: tableName\n d2uri: d2uri\n}"));
    OutputProcessorConfig logProcessor = new OutputProcessorConfig("LOG", OutputFormat.NAME_TERM_VALUE,
        ConfigFactory.parseString("{type: CONSOLE\n}"));
    List<OutputProcessorConfig>
        outputProcessorConfigList = Arrays.asList(kafkaProcessor, veniceProcessor, espressoProcessor, logProcessor);
    String name = "XAffinity";
    OperationalConfig operationalConfig =
        new NearlineOperationalConfig(outputProcessorConfigList, name);
    List<String> features = Arrays.asList("F1", "F2");
    nearlineGenerationConfigObj = new FeatureGenConfig(operationalConfig, features);
  }
}
