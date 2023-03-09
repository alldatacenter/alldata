package com.linkedin.feathr.core.configbuilder.typesafe.consumer;

import com.linkedin.feathr.core.config.consumer.AbsoluteTimeRangeConfig;
import com.linkedin.feathr.core.config.consumer.DateTimeRange;
import com.linkedin.feathr.core.config.consumer.FeatureBagConfig;
import com.linkedin.feathr.core.config.consumer.JoinConfig;
import com.linkedin.feathr.core.config.consumer.JoinTimeSettingsConfig;
import com.linkedin.feathr.core.config.consumer.KeyedFeatures;
import com.linkedin.feathr.core.config.consumer.ObservationDataTimeSettingsConfig;
import com.linkedin.feathr.core.config.consumer.RelativeTimeRangeConfig;
import com.linkedin.feathr.core.config.consumer.SettingsConfig;
import com.linkedin.feathr.core.config.consumer.TimestampColumnConfig;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class JoinFixture {
  static final String emptySettingsConfigStr = "settings: {\n}";

  static final SettingsConfig expEmptySettingsConfigObj = new SettingsConfig(null, null);

  public static final String settingsWithAbsoluteTimeRange = String.join("\n",
      "settings: {",
      "    observationDataTimeSettings: {",
      "       absoluteTimeRange: {",
      "         startTime: \"2018/05/01/00/00/00\"",
      "         endTime:\"2018/05/05/23/59/59\"",
      "         timeFormat: \"yyyy/MM/dd/HH/mm/ss\"",
      "       }",
      "     }",
      "     joinTimeSettings: {",
      "        timestampColumn: {",
      "          def: timestamp",
      "          format: \"yyyy/MM/dd/HH/mm/ss\"",
      "        }",
      "        simulateTimeDelay: 1d",
      "      }",
      "}");

  static final SettingsConfig expSettingsWithAbsoluteTimeRange;
  static {
    String timestampField = "timestamp";
    String timestampFormat = "yyyy/MM/dd/HH/mm/ss";

    String startTime = "2018/05/01/00/00/00";
    String endTime = "2018/05/05/23/59/59";
    Duration simulateTimeDelay = Duration.ofDays(1);
    AbsoluteTimeRangeConfig absoluteTimeRangeConfig = new AbsoluteTimeRangeConfig(startTime, endTime, timestampFormat);
    ObservationDataTimeSettingsConfig observationDataTimeSettingsConfig = new ObservationDataTimeSettingsConfig(
        absoluteTimeRangeConfig, null);
    TimestampColumnConfig timestampColumnConfig = new TimestampColumnConfig(timestampField, timestampFormat);
    JoinTimeSettingsConfig joinTimeSettingsConfig = new JoinTimeSettingsConfig(timestampColumnConfig, simulateTimeDelay, null);

    expSettingsWithAbsoluteTimeRange = new SettingsConfig(observationDataTimeSettingsConfig, joinTimeSettingsConfig);
  }

  public static final String settingsWithLatestFeatureData = String.join("\n",
      "settings: {",
      "    joinTimeSettings: {",
      "       useLatestFeatureData: true",
      "    }",
      "}");

  static final SettingsConfig expSettingsWithLatestFeatureData;
  static {
    JoinTimeSettingsConfig joinTimeSettingsConfig = new JoinTimeSettingsConfig( null, null,true);

    expSettingsWithLatestFeatureData = new SettingsConfig(null, joinTimeSettingsConfig);
  }

  public static final String settingsWithRelativeTimeRange = String.join("\n",
      "settings: {",
      "    observationDataTimeSettings: {",
      "       relativeTimeRange: {",
      "         window: 1d",
      "         offset: 1d",
      "       }",
      "     }",
      "     joinTimeSettings: {",
      "         useLatestFeatureData: true",
      "     }",
      "}");

  static final SettingsConfig expSettingsWithRelativeTimeRange;
  static {
    Duration window = Duration.ofDays(1);
    Duration offset = Duration.ofDays(1);
    Duration simulateTimeDelay = Duration.ofDays(1);
    RelativeTimeRangeConfig relativeTimeRangeConfig = new RelativeTimeRangeConfig(window, offset);
    ObservationDataTimeSettingsConfig observationDataTimeSettingsConfig = new ObservationDataTimeSettingsConfig(
 null, relativeTimeRangeConfig);
    JoinTimeSettingsConfig joinTimeSettingsConfig = new JoinTimeSettingsConfig(null, null, true);

    expSettingsWithRelativeTimeRange = new SettingsConfig(observationDataTimeSettingsConfig, joinTimeSettingsConfig);
  }

  public static final String settingsWithOnlyWindow = String.join("\n",
      "settings: {",
      "    observationDataTimeSettings: {",
      "       relativeTimeRange: {",
      "         window: 1d",
      "       }",
      "     }",
      "     joinTimeSettings: {",
      "        timestampColumn: {",
      "           def: timestamp",
      "           format: yyyy/MM/dd",
      "        }",
      "        simulateTimeDelay: 1d",
      "     }",
      "}");

  static final SettingsConfig expSettingsWithOnlyWindow;
  static {
    Duration window = Duration.ofDays(1);
    Duration simulateTimeDelay = Duration.ofDays(1);
    String timestampField = "timestamp";
    String timestampFormat = "yyyy/MM/dd";
    TimestampColumnConfig timestampColumnConfig = new TimestampColumnConfig(timestampField, timestampFormat);
    RelativeTimeRangeConfig relativeTimeRangeConfig = new RelativeTimeRangeConfig(window, null);
    ObservationDataTimeSettingsConfig observationDataTimeSettingsConfig = new ObservationDataTimeSettingsConfig(
        null, relativeTimeRangeConfig);
    JoinTimeSettingsConfig joinTimeSettingsConfig = new JoinTimeSettingsConfig(timestampColumnConfig, simulateTimeDelay, null);

    expSettingsWithOnlyWindow = new SettingsConfig(observationDataTimeSettingsConfig, joinTimeSettingsConfig);
  }
  public static final String invalidWithOnlyStartTime = String.join("\n",
      "settings: {",
      "     observationDataTimeSettings: {",
      "       absoluteTimeRange: {",
      "         startTime: 2020/09/20",
      "       }",
      "     }",
      "}");

  public static final String invalidWithNoTimestampFormat = String.join("\n",
      "settings: {",
      "     joinTimeSettings: {",
      "       timestampColumn: {",
      "         def: timestamp",
      "       }",
      "     }",
      "}");

  public static final String invalidWithBothAbsoluteTimeRangeAndRelativeTimeRange = String.join("\n",
      "settings: {",
      "    observationDataTimeSettings: {",
      "       absoluteTimeRange: {",
      "         startTime: 2020/09/20",
      "         endTime: 2020/09/25",
      "         timeFormat: yyyy/MM/dd",
      "       }",
      "      relativeTimeRange: {",
      "         window: 1d",
      "         offset: 1d",
      "        }",
      "      }",
      "}");

  public static final String invalidWithUseLatestFeatureDataAndTimestampCol = String.join("\n",
      "settings: {",
      "      joinTimeSettings: {",
      "         timestampColumn: {",
      "           def: timestamp",
      "           format: \"yyyy/MM/dd/HH/mm/ss\"",
      "         }",
      "         useLatestFeatureData: true",
      "       }",
      "}");

  public static final String invalidWithUseLatestFeatureDataAndTimeDelay = String.join("\n",
      "settings: {",
      "      joinTimeSettings: {",
      "         simulateTimeDelay: 1d",
      "         useLatestFeatureData: true",
      "       }",
      "}");

  public static final String settingsWithTimeWindowConfigAndNegativeTimeDelay = String.join("\n",
      "settings: {",
      "      joinTimeSettings: {",
      "         timestampColumn: {",
      "           def: timestamp",
      "           format: yyyy/MM/dd",
      "         }",
      "         simulateTimeDelay: -1d",
      "       }",
      "}");

  public static final String invalidSettingsWithTimeWindowConfigNegativeTimeDelay = String.join("\n",
      "settings: {",
      "      joinTimeSettings: {",
      "         timestampColumn: {",
      "           def: timestamp",
      "           format: yyyy/MM/dd",
      "         }",
      "         simulateTimeDelay: ---1d",
      "       }",
      "}");


  static final String featureBagConfigStr = String.join("\n",
      "features: [",
      "    {",
      "      key: \"targetId\"",
      "      featureList: [\"waterloo_job_location\", ",
      "\"waterloo_job_jobTitle\", \"waterloo_job_jobSeniority\"]",
      "    },",
      "    {",
      "       key: \"sourceId\"",
      "       featureList: [\"TimeBasedFeatureA\"]",
      "       startDate: \"20170522\"",
      "       endDate: \"20170522\"",
      "    },",
      "    {",
      "      key: \"sourceId\"",
      "      featureList: [\"jfu_resolvedPreference_seniority\", ",
      "\"jfu_resolvedPreference_country\", \"waterloo_member_currentTitle\"]",
      "    },",
      "    {",
      "      key: [\"sourceId\",\"targetId\"]",
      "      featureList: [\"memberJobFeature1\",\"memberJobFeature2\"]",
      "    },",
      "    {",
      "       key: [x],",
      "       featureList: [\"sumPageView1d\", \"waterloo-member-title\"]",
      "    }",
      "    {",
      "       key: [x],",
      "       featureList: [\"pageId\", \"memberJobFeature6\"]",
      "       overrideTimeDelay: 3d",
      "     }",
      "]");

  static final String featureBagConfigStrWithSpecialChars = String.join("\n",
      "\"features.dot:colon\": [",
      "    {",
      "      key: \"targetId\"",
      "      featureList: [\"waterloo:job.location\", ",
      "\"waterloo_job_jobTitle\", \"waterloo_job_jobSeniority\"]",
      "    },",
      "    {",
      "       key: \"sourceId\"",
      "       featureList: [\"TimeBased.Feature:A\"]",
      "       startDate: \"20170522\"",
      "       endDate: \"20170522\"",
      "    },",
      "]");


  static FeatureBagConfig expFeatureBagConfigObj;
  static final Map<String, FeatureBagConfig> expFeatureBagConfigs;
  static {
    List<String> key1 = Collections.singletonList("targetId");
    List<String> features1 =
        Arrays.asList("waterloo_job_location", "waterloo_job_jobTitle", "waterloo_job_jobSeniority");
    KeyedFeatures keyedFeature1 = new KeyedFeatures(key1, features1, null, null);

    List<String> key2 = Collections.singletonList("sourceId");
    List<String> features2 = Collections.singletonList("TimeBasedFeatureA");
    LocalDateTime start = LocalDateTime.of(2017, 5, 22, 0, 0);
    LocalDateTime end = LocalDateTime.of(2017, 5, 22, 0, 0);
    DateTimeRange dates = new DateTimeRange(start, end);
    KeyedFeatures keyedFeature2 = new KeyedFeatures(key2, features2, dates, null);

    List<String> key3 = Collections.singletonList("sourceId");
    List<String> features3 = Arrays.asList("jfu_resolvedPreference_seniority",
        "jfu_resolvedPreference_country", "waterloo_member_currentTitle");
    KeyedFeatures keyedFeature3 = new KeyedFeatures(key3, features3, null, null);

    List<String> key4 = Arrays.asList("sourceId","targetId");
    List<String> features4 = Arrays.asList("memberJobFeature1","memberJobFeature2");
    KeyedFeatures keyedFeature4 = new KeyedFeatures(key4, features4, null, null);

    List<String> key = Collections.singletonList("x");
    List<String> features = Arrays.asList("sumPageView1d", "waterloo-member-title");
    KeyedFeatures keyedFeatures5 = new KeyedFeatures(key, features, null, null);

    List<String> key5 = Collections.singletonList("x");
    List<String> features5 = Arrays.asList("pageId", "memberJobFeature6");
    Duration overrideTimeDelay = Duration.ofDays(3);
    KeyedFeatures keyedFeatures6 = new KeyedFeatures(key5, features5, null, overrideTimeDelay);

    expFeatureBagConfigObj =
        new FeatureBagConfig(Arrays.asList(keyedFeature1, keyedFeature2, keyedFeature3, keyedFeature4, keyedFeatures5, keyedFeatures6));

    expFeatureBagConfigs = new HashMap<>();
    expFeatureBagConfigs.put("features", expFeatureBagConfigObj);
  }

  static FeatureBagConfig expFeatureBagConfigObjWithSpecialChars;
  static final Map<String, FeatureBagConfig> expFeatureBagConfigsWithSpecialChars;
  static {
    List<String> key1 = Collections.singletonList("targetId");
    List<String> features1 =
        Arrays.asList("waterloo:job.location", "waterloo_job_jobTitle", "waterloo_job_jobSeniority");
    KeyedFeatures keyedFeature1 = new KeyedFeatures(key1, features1, null, null);

    List<String> key2 = Collections.singletonList("sourceId");
    List<String> features2 = Collections.singletonList("TimeBased.Feature:A");
    LocalDateTime start = LocalDateTime.of(2017, 5, 22, 0, 0);
    LocalDateTime end = LocalDateTime.of(2017, 5, 22, 0, 0);
    DateTimeRange dates = new DateTimeRange(start, end);
    KeyedFeatures keyedFeature2 = new KeyedFeatures(key2, features2, dates, null);

    expFeatureBagConfigObjWithSpecialChars =
        new FeatureBagConfig(Arrays.asList(keyedFeature1, keyedFeature2));

    expFeatureBagConfigsWithSpecialChars = new HashMap<>();
    expFeatureBagConfigsWithSpecialChars.put("features.dot:colon", expFeatureBagConfigObjWithSpecialChars);
  }

  static final String joinConfigStr1 = featureBagConfigStr;

  static final String joinConfigStr1WithSpecialChars = featureBagConfigStrWithSpecialChars;

  public static final JoinConfig expJoinConfigObj1 = new JoinConfig(null, expFeatureBagConfigs);

  public static final JoinConfig expJoinConfigObj1WithSpecialChars = new JoinConfig(null, expFeatureBagConfigsWithSpecialChars);

  static final String joinConfigStr2 = String.join("\n", emptySettingsConfigStr, featureBagConfigStr);

  static final JoinConfig expJoinConfigObj2 =
      new JoinConfig(expEmptySettingsConfigObj, expFeatureBagConfigs);

  static final String joinConfigStr3 = String.join("\n", settingsWithAbsoluteTimeRange, featureBagConfigStr);

  static final JoinConfig expJoinConfigObj3 =
      new JoinConfig(expSettingsWithAbsoluteTimeRange, expFeatureBagConfigs);

  static final String multiFeatureBagsStr = String.join("\n",
      "featuresGroupA: [",
      "    {",
      "      key: \"viewerId\"",
      "      featureList: [",
      "        waterloo_member_currentCompany,",
      "        waterloo_job_jobTitle,",
      "      ]",
      "    }",
      "]",
      "featuresGroupB: [",
      "    {",
      "      key: \"viewerId\"",
      "      featureList: [",
      "        waterloo_member_location,",
      "        waterloo_job_jobSeniority",
      "      ]",
      "    }",
      "]");

  static final Map<String, FeatureBagConfig> expMultiFeatureBagConfigs;
  static {
    String featureBag1Name = "featuresGroupA";
    List<String> key1 = Collections.singletonList("viewerId");
    List<String> featuresList1 = Arrays.asList("waterloo_member_currentCompany", "waterloo_job_jobTitle");
    KeyedFeatures keyedFeatures1 = new KeyedFeatures(key1, featuresList1, null, null);
    FeatureBagConfig featureBag1Config = new FeatureBagConfig(Collections.singletonList(keyedFeatures1));

    String featureBag2Name = "featuresGroupB";
    List<String> key2 = Collections.singletonList("viewerId");
    List<String> featuresList2 = Arrays.asList("waterloo_member_location", "waterloo_job_jobSeniority");
    KeyedFeatures keyedFeatures2 = new KeyedFeatures(key2, featuresList2, null, null);
    FeatureBagConfig featureBag2Config = new FeatureBagConfig(Collections.singletonList(keyedFeatures2));

    expMultiFeatureBagConfigs = new HashMap<>();
    expMultiFeatureBagConfigs.put(featureBag1Name, featureBag1Config);
    expMultiFeatureBagConfigs.put(featureBag2Name, featureBag2Config);
  }

  static final String joinConfigStr4 = multiFeatureBagsStr;

  static final JoinConfig expJoinConfigObj4 =
      new JoinConfig(null, expMultiFeatureBagConfigs);
}
