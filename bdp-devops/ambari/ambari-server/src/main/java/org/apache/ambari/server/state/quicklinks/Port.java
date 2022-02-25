/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.server.state.quicklinks;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.annotate.JsonSerialize;

@JsonSerialize(include= JsonSerialize.Inclusion.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Port{
  @JsonProperty("http_property")
  private String httpProperty;

  @JsonProperty("http_default_port")
  private String httpDefaultPort;

  @JsonProperty("https_property")
  private String httpsProperty;

  @JsonProperty("https_default_port")
  private String httpsDefaultPort;

  @JsonProperty("regex")
  private String regex;

  @JsonProperty("https_regex")
  private String httpsRegex;


  @JsonProperty("site")
  private String site;

  public String getHttpProperty() {
    return httpProperty;
  }

  public void setHttpProperty(String httpProperty) {
    this.httpProperty = httpProperty;
  }

  public String getHttpDefaultPort() {
    return httpDefaultPort;
  }

  public void setHttpDefaultPort(String httpDefaultPort) {
    this.httpDefaultPort = httpDefaultPort;
  }

  public String getHttpsProperty() {
    return httpsProperty;
  }

  public void setHttpsProperty(String httpsProperty) {
    this.httpsProperty = httpsProperty;
  }

  public String getHttpsDefaultPort() {
    return httpsDefaultPort;
  }

  public void setHttpsDefaultPort(String httpsDefaultPort) {
    this.httpsDefaultPort = httpsDefaultPort;
  }

  public String getRegex() {
    return regex;
  }

  public void setRegex(String regex) {
    this.regex = regex;
  }

  public String getHttpsRegex() {
    return httpsRegex;
  }

  public void setHttpsRegex(String httpsRegex) {
    this.httpsRegex = httpsRegex;
  }

  public String getSite() {
    return site;
  }

  public void setSite(String site) {
    this.site = site;
  }

  public void mergetWithParent(Port parentPort){
    if(null == parentPort)
      return;

    if(null == httpProperty && null != parentPort.getHttpProperty())
      httpProperty = parentPort.getHttpProperty();

    if(null == httpDefaultPort && null != parentPort.getHttpDefaultPort())
      httpDefaultPort = parentPort.getHttpDefaultPort();

    if(null == httpsProperty && null != parentPort.getHttpsProperty())
      httpsProperty = parentPort.getHttpsProperty();

    if(null == httpsDefaultPort && null != parentPort.getHttpsDefaultPort())
      httpsDefaultPort = parentPort.getHttpsDefaultPort();

    if(null == regex && null != parentPort.getRegex())
      regex = parentPort.getRegex();

    if(null == httpsRegex && null != parentPort.getHttpsRegex())
      regex = parentPort.getHttpsRegex();

    if(null == site && null != parentPort.getSite())
      site = parentPort.getSite();
  }
}