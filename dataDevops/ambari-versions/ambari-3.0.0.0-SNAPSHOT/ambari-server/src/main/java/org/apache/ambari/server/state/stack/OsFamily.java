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
package org.apache.ambari.server.state.stack;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.ambari.server.configuration.Configuration;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.inject.Singleton;

/**
 * Class that encapsulates OS family logic
 */
@Singleton
public class OsFamily {
    private final static String OS_FAMILY_UBUNTU = "ubuntu";
    private final static String OS_FAMILY_SUSE = "suse";
    private final static String OS_FAMILY_REDHAT = "redhat";

    private final String os_pattern = "([\\D]+|(?:[\\D]+[\\d]+[\\D]+))([\\d]*)";
    private final String OS_DISTRO = "distro";
    private final String OS_VERSION = "versions";
    private final String LOAD_CONFIG_MSG = "Could not load OS family definition from %s file";
    private final String FILE_NAME = "os_family.json";
    private static final Logger LOG = LoggerFactory.getLogger(OsFamily.class);

    private Map<String, JsonOsFamilyEntry> osMap = null;
    private JsonOsFamilyRoot jsonOsFamily = null;

  /**
   * Initialize object
   * @param conf Configuration instance
   */
    public OsFamily(Configuration conf){
      init(conf.getSharedResourcesDirPath());
    }

  /**
   * Initialize object
   * @param properties list of properties
   */
    public OsFamily(Properties properties){
      init(properties.getProperty(Configuration.SHARED_RESOURCES_DIR.getKey()));
    }

    private void init(String SharedResourcesPath){
      FileInputStream inputStream = null;
      try {
        File f = new File(SharedResourcesPath, FILE_NAME);
        if (!f.exists()) {
          throw new Exception();
        }
        inputStream = new FileInputStream(f);

        Type type = new TypeToken<JsonOsFamilyRoot>() {}.getType();
        Gson gson = new Gson();
        jsonOsFamily = gson.fromJson(new InputStreamReader(inputStream), type);
        osMap = jsonOsFamily.getMapping();
      } catch (Exception e) {
        LOG.error(String.format(LOAD_CONFIG_MSG, new File(SharedResourcesPath, FILE_NAME).toString()));
        throw new RuntimeException(e);
      } finally {
        IOUtils.closeQuietly(inputStream);
      }
    }

    /**
     * Separate os name from os major version
     * @param os the os
     * @return separated os name and os version
     */
    private Map<String,String> parse_os(String os){
      Map<String,String> pos = new HashMap<>();

      Pattern r = Pattern.compile(os_pattern);
      Matcher m = r.matcher(os);

      if (m.matches()) {
        pos.put(OS_DISTRO, m.group(1));
        pos.put(OS_VERSION, m.group(2));
      } else {
        pos.put(OS_DISTRO, os);
        pos.put(OS_VERSION, "");
      }
      return pos;
    }

    /**
     * Gets the array of compatible OS types
     * @param os the os
     * @return all types that are compatible with the supplied type
     */
    public Set<String> findTypes(String os) {
      Map<String,String>  pos = parse_os(os);
      for ( String family : osMap.keySet()) {
        JsonOsFamilyEntry fam = osMap.get(family);
        if (fam.getDistro().contains(pos.get(OS_DISTRO)) && fam.getVersions().contains(pos.get(OS_VERSION))){
          Set<String> data= new HashSet<>();
          for (String item: fam.getDistro()) {
            data.add(item + pos.get(OS_VERSION));
          }
            return Collections.unmodifiableSet(data);
        }
      }
      return Collections.emptySet();
    }

    /**
     * Finds the family for the specific OS + it's version number
     * @param os the OS
     * @return the family, or <code>null</code> if not defined
     */
    public String find(String os) {
      Map<String,String>  pos = parse_os(os);
      for ( String family : osMap.keySet()) {
        JsonOsFamilyEntry fam = osMap.get(family);
        if (fam.getDistro().contains(pos.get(OS_DISTRO)) && fam.getVersions().contains(pos.get(OS_VERSION))){
          return family + pos.get(OS_VERSION);
        }
      }

      return null;
    }

    /**
     * Finds the family for the specific OS
     * @param os the OS
     * @return the family, or <code>null</code> if not defined
     */
    public String find_family(String os) {
      Map<String,String>  pos = parse_os(os);
      for ( String family : osMap.keySet()) {
        JsonOsFamilyEntry fam = osMap.get(family);
        if (fam.getDistro().contains(pos.get(OS_DISTRO)) && fam.getVersions().contains(pos.get(OS_VERSION))){
          return family;
        }
      }
      return null;
    }
    /**
     * Form list of all supported os types
     * @return one dimension list with os types
     */
    public Set<String> os_list(){
      Set<String> r= new HashSet<>();
      for ( String family : osMap.keySet()) {
        JsonOsFamilyEntry fam = osMap.get(family);
        for (String version: fam.getVersions()){
          Set<String> data= new HashSet<>();
          for (String item: fam.getDistro()) {
            data.add(item + version);
          }
          r.addAll(data);
        }
      }
      return r;
    }

    public boolean isUbuntuFamily(String osType) {
      return isOsInFamily(osType, OS_FAMILY_UBUNTU);
    }

    public boolean isSuseFamily(String osType) {
      return isOsInFamily(osType, OS_FAMILY_SUSE);
    }

    public boolean isRedhatFamily(String osType) {
      return isOsInFamily(osType, OS_FAMILY_REDHAT);
    }

    public boolean isOsInFamily(String osType, String osFamily) {
      String familyOfOsType = find_family(osType);
      return (familyOfOsType != null && isFamilyExtendedByFamily(familyOfOsType, osFamily));
    }

    private boolean isFamilyExtendedByFamily(String currentFamily, String family) {
      return (currentFamily.equals(family) || getOsFamilyParent(currentFamily)!=null && isFamilyExtendedByFamily(getOsFamilyParent(currentFamily), family));
    }

    public boolean isVersionedOsFamilyExtendedByVersionedFamily(String currentVersionedFamily, String versionedFamily) {
      Map<String,String> pos = this.parse_os(currentVersionedFamily);
      String currentFamily = pos.get(OS_DISTRO);
      String currentFamilyVersion = pos.get(OS_VERSION);

      pos = this.parse_os(versionedFamily);
      String family = pos.get(OS_DISTRO);
      String familyVersion = pos.get(OS_VERSION);

      return currentFamilyVersion.equals(familyVersion) && isFamilyExtendedByFamily(currentFamily, family);
    }

    private String getOsFamilyParent(String osFamily) {
      return osMap.get(osFamily).getExtendsFamily();
    }

    /**
     * @return the map of aliases
     */
    public Map<String, String> getAliases() {
      return (null == jsonOsFamily || null == jsonOsFamily.getAliases()) ?
          Collections.emptyMap() : jsonOsFamily.getAliases();
    }
}
