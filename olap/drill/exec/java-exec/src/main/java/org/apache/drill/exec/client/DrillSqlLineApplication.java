/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.drill.exec.client;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.apache.drill.common.scanner.ClassPathScanner;
import org.apache.drill.common.util.DrillVersionInfo;
import org.apache.drill.shaded.guava.com.google.common.annotations.VisibleForTesting;
import org.jline.reader.impl.completer.StringsCompleter;
import sqlline.Application;
import sqlline.CommandHandler;
import sqlline.ConnectionMetadata;
import sqlline.OutputFormat;
import sqlline.PromptHandler;
import sqlline.ReflectiveCommandHandler;
import sqlline.SqlLine;
import sqlline.SqlLineOpts;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>Drill SqlLine application configuration.</p>
 *
 * <p>Customizes SqlLine for Drill, i.e. overrides application info message,
 * known drivers, connection url examples, removes non applicable commands, sets SqlLine properties.</p>
 *
 * <p>Uses {@link #DRILL_SQLLINE_CONF} as base configuration, allows to override it using {@link #DRILL_SQLLINE_OVERRIDE_CONF}.
 * If configuration files are missing in the classpath, issues warning and proceeds with default SqlLine configuration.</p>
 */
public class DrillSqlLineApplication extends Application {

  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(DrillSqlLineApplication.class);

  private static final String DRILL_SQLLINE_CONF = "drill-sqlline.conf";
  private static final String DRILL_SQLLINE_OVERRIDE_CONF = "drill-sqlline-override.conf";

  private static final String INFO_MESSAGE_TEMPLATE_CONF = "drill.sqlline.info_message_template";
  private static final String QUOTES_CONF = "drill.sqlline.quotes";
  private static final String DRIVERS_CONF = "drill.sqlline.drivers";
  private static final String CONNECTION_URL_EXAMPLES_CONF = "drill.sqlline.connection_url_examples";
  private static final String COMMANDS_TO_EXCLUDE_CONF = "drill.sqlline.commands.exclude";
  private static final String OPTS_CONF = "drill.sqlline.opts";
  private static final String PROMPT_WITH_SCHEMA = "drill.sqlline.prompt.with_schema";

  private final Config config;

  public DrillSqlLineApplication() {
    this(DRILL_SQLLINE_CONF, DRILL_SQLLINE_OVERRIDE_CONF);
  }

  @VisibleForTesting
  public DrillSqlLineApplication(String configName, String overrideConfigName) {
    this.config = overrideConfig(overrideConfigName, loadConfig(configName));
    if (config.isEmpty()) {
      logger.warn("Was unable to find / load [{}]. Will use default SqlLine configuration.", configName);
    }
  }

  public Config getConfig() {
    return config;
  }

  @Override
  public String getInfoMessage() {
    if (config.hasPath(INFO_MESSAGE_TEMPLATE_CONF)) {
      String quote = "";
      if (config.hasPath(QUOTES_CONF)) {
        List<String> quotes = config.getStringList(QUOTES_CONF);
        quote = quotes.get(new Random().nextInt(quotes.size()));
      }
      return String.format(config.getString(INFO_MESSAGE_TEMPLATE_CONF), getVersion(), quote);
    }

    return super.getInfoMessage();
  }

  @Override
  public String getVersion() {
    return DrillVersionInfo.getVersion();
  }

  @Override
  public List<String> allowedDrivers() {
    if (config.hasPath(DRIVERS_CONF)) {
      return config.getStringList(DRIVERS_CONF);
    }
    return super.allowedDrivers();
  }

  @Override
  public Map<String, OutputFormat> getOutputFormats(SqlLine sqlLine) {
    return sqlLine.getOutputFormats();
  }

  @Override
  public Collection<String> getConnectionUrlExamples() {
    if (config.hasPath(CONNECTION_URL_EXAMPLES_CONF)) {
      return config.getStringList(CONNECTION_URL_EXAMPLES_CONF);
    }
    return super.getConnectionUrlExamples();
  }

  @Override
  public Collection<CommandHandler> getCommandHandlers(SqlLine sqlLine) {
    List<String> commandsToExclude = new ArrayList<>();

    // exclude connect command and then add it back to ensure connection url examples are updated
    boolean reloadConnect = config.hasPath(CONNECTION_URL_EXAMPLES_CONF);
    if (reloadConnect) {
      commandsToExclude.add("connect");
    }

    if (config.hasPath(COMMANDS_TO_EXCLUDE_CONF)) {
      commandsToExclude.addAll(config.getStringList(COMMANDS_TO_EXCLUDE_CONF));
    }

    if (commandsToExclude.isEmpty()) {
      return sqlLine.getCommandHandlers();
    }

    List<CommandHandler> commandHandlers = sqlLine.getCommandHandlers().stream()
        .filter(c -> c.getNames().stream()
            .noneMatch(commandsToExclude::contains))
        .collect(Collectors.toList());

    if (reloadConnect) {
      commandHandlers.add(new ReflectiveCommandHandler(sqlLine,
          new StringsCompleter(getConnectionUrlExamples()), "connect", "open"));
    }

    return commandHandlers;
  }

  @Override
  public SqlLineOpts getOpts(SqlLine sqlLine) {
    SqlLineOpts opts = sqlLine.getOpts();
    if (config.hasPath(OPTS_CONF)) {
      Config optsConfig = config.getConfig(OPTS_CONF);
      optsConfig.entrySet().forEach(
          e -> {
            String key = e.getKey();
            String value = String.valueOf(e.getValue().unwrapped());
            if (!opts.set(key, value, true)) {
              logger.warn("Unable to set SqlLine property [{}] to [{}].", key, value);
            }
          }
      );
    }
    return opts;
  }

  @Override
  public PromptHandler getPromptHandler(SqlLine sqlLine) {
    if (config.hasPath(PROMPT_WITH_SCHEMA) && config.getBoolean(PROMPT_WITH_SCHEMA)) {
      return new PromptHandler(sqlLine) {
        @Override
        protected String getDefaultPrompt(int connectionIndex, String url, String defaultPrompt) {
          StringBuilder builder = new StringBuilder();
          builder.append("apache drill");

          ConnectionMetadata meta = sqlLine.getConnectionMetadata();

          String currentSchema = meta.getCurrentSchema();
          if (currentSchema != null) {
            builder.append(" (").append(currentSchema).append(")");
          }
          return builder.append("> ").toString();
        }
      };
    }
    return super.getPromptHandler(sqlLine);
  }

  private Config loadConfig(String configName) {
    Set<URL> urls = ClassPathScanner.forResource(configName, false);
    if (urls.size() != 1) {
      if (logger.isDebugEnabled()) {
        urls.forEach(
            u -> logger.debug("Found duplicating [{}]: [{}].", configName, u.getPath())
        );
      }
      return ConfigFactory.empty();
    }

    URL url = urls.iterator().next();
    try {
      logger.debug("Parsing [{}] for the url: [{}].", configName, url.getPath());
      return ConfigFactory.parseURL(url);
    } catch (Exception e) {
      logger.warn("Was unable to parse [{}].", url.getPath(), e);
      return ConfigFactory.empty();
    }
  }

  private Config overrideConfig(String configName, Config config) {
    Config overrideConfig = loadConfig(configName);
    return overrideConfig.withFallback(config).resolve();
  }

}
