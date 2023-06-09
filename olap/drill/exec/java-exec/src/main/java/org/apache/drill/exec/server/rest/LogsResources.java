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
package org.apache.drill.exec.server.rest;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;
import org.apache.drill.shaded.guava.com.google.common.collect.Sets;
import org.apache.drill.common.exceptions.DrillRuntimeException;
import org.apache.drill.exec.ExecConstants;
import org.apache.drill.exec.work.WorkManager;
import org.glassfish.jersey.server.mvc.Viewable;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.xml.bind.annotation.XmlRootElement;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static org.apache.drill.exec.server.rest.auth.DrillUserPrincipal.ADMIN_ROLE;

@Path("/")
@RolesAllowed(ADMIN_ROLE)
public class LogsResources {
  private static final Logger logger = LoggerFactory.getLogger(LogsResources.class);

  @Inject DrillRestServer.UserAuthEnabled authEnabled;
  @Inject SecurityContext sc;
  @Inject WorkManager work;

  private static final FileFilter file_filter = new FileFilter() {
    @Override
    public boolean accept(File file) {
      return file.isFile();
    }
  };
  private static final DateTimeFormatter format = DateTimeFormat.forPattern("MM/dd/yyyy HH:mm:ss");


  @GET
  @Path("/logs")
  @Produces(MediaType.TEXT_HTML)
  public Viewable getLogs() {
    Set<Log> logs = getLogsJSON();
    return ViewableWithPermissions.create(authEnabled.get(), "/rest/logs/list.ftl", sc, logs);
  }

  @GET
  @Path("/logs.json")
  @Produces(MediaType.APPLICATION_JSON)
  public Set<Log> getLogsJSON() {
    Set<Log> logs = Sets.newTreeSet();
    File[] files = getLogFolder().listFiles(file_filter);

    for (File file : files) {
      logs.add(new Log(file.getName(), file.length(), file.lastModified()));
    }

    return logs;
  }

  @GET
  @Path("/log/{name}/content")
  @Produces(MediaType.TEXT_HTML)
  public Viewable getLog(@PathParam("name") String name) throws IOException {
    try {
      LogContent content = getLogJSON(name);
      return ViewableWithPermissions.create(authEnabled.get(), "/rest/logs/log.ftl", sc, content);
    } catch (Exception | Error e) {
      logger.error("Exception was thrown when fetching log {} :\n{}", name, e);
      return ViewableWithPermissions.create(authEnabled.get(), "/rest/errorMessage.ftl", sc, e);
    }
  }

  @GET
  @Path("/log/{name}/content.json")
  @Produces(MediaType.APPLICATION_JSON)
  public LogContent getLogJSON(@PathParam("name") final String name) throws IOException {
    File file = getFileByName(getLogFolder(), name);

    final int maxLines = work.getContext().getOptionManager().getOption(ExecConstants.WEB_LOGS_MAX_LINES).num_val.intValue();

    try (BufferedReader br = new BufferedReader(new FileReader(file))) {
      @SuppressWarnings("serial")
      Map<Integer, String> cache = new LinkedHashMap<Integer, String>(maxLines, .75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Integer, String> eldest) {
          return size() > maxLines;
        }
      };

      String line;
      int i = 0;
      while ((line = br.readLine()) != null) {
        cache.put(i++, line);
      }

      return new LogContent(file.getName(), cache.values(), maxLines);
    }
  }

  @GET
  @Path("/log/{name}/download")
  @Produces(MediaType.TEXT_PLAIN)
  public Response getFullLog(@PathParam("name") final String name) {
    File file = getFileByName(getLogFolder(), name);
    return Response.ok(file)
        .header(HttpHeaders.CONTENT_DISPOSITION, String.format("attachment;filename=\"%s\"", name))
        .build();
  }

  private File getLogFolder() {
    return new File(Preconditions.checkNotNull(System.getenv("DRILL_LOG_DIR"), "DRILL_LOG_DIR variable is not set"));
  }

  private File getFileByName(File folder, final String name) {
    File[] files = folder.listFiles((dir, fileName) -> fileName.equals(name));
    if (files.length == 0) {
      throw new DrillRuntimeException(name + " doesn't exist");
    }
    return files[0];
  }

  @XmlRootElement
  public class Log implements Comparable<Log> {

    private final String name;
    private final long size;
    private final DateTime lastModified;

    @JsonCreator
    public Log (@JsonProperty("name") String name,
                @JsonProperty("size") long size,
                @JsonProperty("lastModified") long lastModified) {
      this.name = name;
      this.size = size;
      this.lastModified = new DateTime(lastModified);
    }

    public String getName() {
      return name;
    }

    public String getSize() {
      return Math.ceil(size / 1024d) + " KB";
    }

    public String getLastModified() {
      return lastModified.toString(format);
    }

    @Override
    public int compareTo(Log log) {
      return this.getName().compareTo(log.getName());
    }
  }

  @XmlRootElement
  public class LogContent {
    private final String name;
    private final Collection<String> lines;
    private final int maxLines;

    @JsonCreator
    public LogContent (@JsonProperty("name") String name,
                       @JsonProperty("lines") Collection<String> lines,
                       @JsonProperty("maxLines") int maxLines) {
      this.name = name;
      this.lines = lines;
      this.maxLines = maxLines;
    }

    public String getName() {
      return name;
    }

    public Collection<String> getLines() { return lines; }

    public int getMaxLines() { return maxLines; }
  }
}
