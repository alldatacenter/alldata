/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.atlas.tools;

import com.sun.jersey.core.util.MultivaluedMapImpl;
import org.apache.atlas.ApplicationProperties;
import org.apache.atlas.AtlasClientV2;
import org.apache.atlas.AtlasException;
import org.apache.atlas.AtlasServiceException;
import org.apache.atlas.model.SearchFilter;
import org.apache.atlas.model.instance.AtlasClassification;
import org.apache.atlas.model.instance.AtlasEntityHeader;
import org.apache.atlas.model.instance.AtlasEntityHeaders;
import org.apache.atlas.model.typedef.AtlasClassificationDef;
import org.apache.atlas.model.typedef.AtlasEntityDef;
import org.apache.atlas.model.typedef.AtlasStructDef;
import org.apache.atlas.model.typedef.AtlasTypesDef;
import org.apache.atlas.type.AtlasType;
import org.apache.atlas.utils.AtlasJson;
import org.apache.atlas.utils.AuthenticationUtil;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang3.StringUtils;
import org.apache.hadoop.security.UserGroupInformation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.MultivaluedMap;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.function.Consumer;

import static org.apache.atlas.model.instance.AtlasEntity.Status.DELETED;

public class BulkFetchAndUpdate {
    private static final Logger LOG = LoggerFactory.getLogger(BulkFetchAndUpdate.class);

    private static final String DATE_FORMAT_SUPPORTED = "yyyy-MM-dd'T'HH:mm:ss";
    private static final String OPTION_FROM = "f";

    private static final String APPLICATION_PROPERTY_ATLAS_ENDPOINT = "atlas.rest.address";
    private static final String SYSTEM_PROPERTY_USER_DIR = "user.dir";
    private static final String STEP_PREPARE = "prepare";
    private static final String STEP_UPDATE = "update";
    private static final int EXIT_CODE_SUCCESS = 0;
    private static final int EXIT_CODE_FAILED = 1;
    private static final String DEFAULT_ATLAS_URL = "http://localhost:21000/";
    private static final String FILE_CLASSIFICATION_DEFS = "classification-definitions.json";
    private static final String FILE_ENTITY_HEADERS = "entity-headers.json";

    private final static String[] filesToUse = new String[] {
            FILE_CLASSIFICATION_DEFS,
            FILE_ENTITY_HEADERS
    };

    public static void main(String[] args) {
        int exitCode = EXIT_CODE_FAILED;

        try {
            long fromTimestamp = 0L;
            CommandLine cmd = getCommandLine(args);

            String stepToExecute = cmd.getOptionValue("s").trim();
            String uid = cmd.getOptionValue("u");
            String pwd = cmd.getOptionValue("p");
            String directory = cmd.getOptionValue("d");
            String fromTime = cmd.getOptionValue(OPTION_FROM);
            String basePath = getDirectory(directory);

            displayCrLf(basePath);

            String[] atlasEndpoint = getAtlasRESTUrl();
            if (atlasEndpoint == null || atlasEndpoint.length == 0) {
                atlasEndpoint = new String[]{DEFAULT_ATLAS_URL};
            }

            if (StringUtils.equals(stepToExecute, STEP_PREPARE)) {
                if (StringUtils.isEmpty(fromTime)) {
                    displayCrLf("'fromTime' is empty" + fromTime);
                    printUsage();
                    return;
                }

                fromTimestamp = getTimestamp(fromTime);
                displayCrLf("fromTimestamp: " + fromTimestamp);
                if (fromTimestamp == 0L) {
                    printUsage();
                    return;
                }
            }

            process(stepToExecute, basePath, atlasEndpoint, uid, pwd, fromTimestamp);

            exitCode = EXIT_CODE_SUCCESS;
        } catch (ParseException e) {
            LOG.error("Failed to parse arguments. Error: ", e.getMessage());
            printUsage();
        } catch (Exception e) {
            LOG.error("Failed!", e);
            displayCrLf("Failed: " + e.getMessage());
        }

        System.exit(exitCode);
    }

    private static long getTimestamp(String str) {
        try {
            if (StringUtils.isEmpty(str)) {
                return 0;
            }

            TimeZone utc = TimeZone.getDefault();
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat(DATE_FORMAT_SUPPORTED);
            simpleDateFormat.setTimeZone(utc);

            return simpleDateFormat.parse(str).getTime();
        } catch (java.text.ParseException e) {
            displayCrLf("Unsupported date format: " + str);
            return 0;
        }
    }

    private static void process(String stepToExecute, String basePath, String[] atlasEndpoint, String uid, String pwd, long fromTimestamp) throws Exception {
        AtlasClientV2 atlasClientV2 = getAtlasClientV2(atlasEndpoint, new String[]{uid, pwd});

        switch (stepToExecute) {
            case STEP_PREPARE: {
                Preparer p = new Preparer(atlasClientV2);
                p.run(basePath, fromTimestamp);
            }
            break;

            case STEP_UPDATE: {
                Updater u = new Updater(atlasClientV2);
                u.run(basePath);
            }
            break;

            default:
                printUsage();
                break;
        }
    }

    private static String getDirectory(String directory) {
        String basePath = System.getProperty(SYSTEM_PROPERTY_USER_DIR) + File.separatorChar;
        if (StringUtils.isNotEmpty(directory) && checkDirectoryExists(directory)) {
            basePath = directory + File.separatorChar;
        } else {
            display("Using directory: ");
        }

        return basePath;
    }

    private static CommandLine getCommandLine(String[] args) throws ParseException {
        Options options = new Options();
        options.addRequiredOption("s", "step", true, "Step to run.");
        options.addOption("u", "user", true, "User name.");
        options.addOption("p", "password", true, "Password name.");
        options.addOption("d", "dir", true, "Directory for reading/writing data.");
        options.addOption(OPTION_FROM, "fromDate", true, "Date, in YYYY-MM-DD format, from where to start reading.");

        return new DefaultParser().parse(options, args);
    }

    private static void printUsage() {
        System.out.println();
        displayCrLf("Usage: classification-updater.sh [-s <step>] [-f <from time>] [-t <optional: to time>] [-d <dir>]");
        displayCrLf("    step: Specify which step to execute:");
        displayCrLf("           prepare: prepare classifications and associated entities.");
        displayCrLf("           update: update classifications and entities.");
        displayCrLf("    dir: [optional] Directory where read/write will happen.");
        displayCrLf("           If not specified, current directory will be used.");
        displayCrLf("    from: [mandatory for 'prepare' step, optional for 'update' step] Date, in YYYY-MM-DD format, from where audits need to be read.");
        displayCrLf("           If not specified, current directory will be used.");
        System.out.println();
    }

    private static String[] getAtlasRESTUrl() {
        Configuration atlasConf = null;
        try {
            atlasConf = ApplicationProperties.get();
            return atlasConf.getStringArray(APPLICATION_PROPERTY_ATLAS_ENDPOINT);
        } catch (AtlasException e) {
            return new String[]{DEFAULT_ATLAS_URL};
        }
    }

    private static AtlasClientV2 getAtlasClientV2(String[] atlasEndpoint, String[] uidPwdFromCommandLine) throws IOException {
        AtlasClientV2 atlasClientV2;
        if (!AuthenticationUtil.isKerberosAuthenticationEnabled()) {
            String[] uidPwd = (uidPwdFromCommandLine[0] == null || uidPwdFromCommandLine[1] == null)
                    ? AuthenticationUtil.getBasicAuthenticationInput()
                    : uidPwdFromCommandLine;

            atlasClientV2 = new AtlasClientV2(atlasEndpoint, uidPwd);

        } else {
            UserGroupInformation ugi = UserGroupInformation.getCurrentUser();
            atlasClientV2 = new AtlasClientV2(ugi, ugi.getShortUserName(), atlasEndpoint);
        }
        return atlasClientV2;
    }

    private static void displayCrLf(String... formatMessage) {
        displayFn(System.out::println, formatMessage);
    }

    private static void display(String... formatMessage) {
        displayFn(System.out::print, formatMessage);
    }

    private static void displayFn(Consumer<String> fn, String... formatMessage) {
        if (formatMessage.length == 1) {
            fn.accept(formatMessage[0]);
        } else {
            fn.accept(String.format(formatMessage[0], formatMessage[1]));
        }
    }
    private static void closeReader(BufferedReader bufferedReader) {
        try {
            if (bufferedReader == null) {
                return;
            }

            bufferedReader.close();
        } catch (IOException ex) {
            LOG.error("closeReader", ex);
        }
    }

    private static BufferedReader getBufferedReader(String basePath, String fileName) throws FileNotFoundException {
        return new BufferedReader(new FileReader(basePath + fileName));
    }

    private static boolean fileCheck(String basePath, String[] files, boolean existCheck) {
        boolean ret = true;
        for (String f : files) {
            ret = ret && fileCheck(basePath, f, existCheck);
        }

        return ret;
    }

    private static boolean fileCheck(String basePath, String file, boolean existCheck) {
        String errorMessage = existCheck ? "does not exist" : "exists" ;
        if (checkFileExists(basePath + file) != existCheck) {
            displayCrLf(String.format("File '%s' %s!", basePath + file, errorMessage));
            return false;
        }

        return true;
    }

    private static boolean checkFileExists(String fileName) {
        File f = new File(fileName);
        return f.exists() && !f.isDirectory();
    }

    private static boolean checkDirectoryExists(String fileName) {
        File f = new File(fileName);
        return f.exists() && f.isDirectory();
    }

    private static FileWriter getFileWriter(String basePath, String fileName) throws IOException {
        String filePath = basePath + fileName;
        displayCrLf("Creating %s", filePath);
        return new FileWriter(filePath, true);
    }

    private static class Preparer {
        private static final String  ATTR_NAME_QUALIFIED_NAME = "qualifiedName";

        private AtlasClientV2 atlasClientV2;
        private Map<String, String> typeNameUniqueAttributeNameMap = new HashMap<>();

        public Preparer(AtlasClientV2 atlasClientV2) {
            this.atlasClientV2 = atlasClientV2;
        }

        public void run(String basePath, long fromTimestamp) throws Exception {
            if (!fileCheck(basePath, filesToUse, false)) return;

            displayCrLf("Starting: from: " + fromTimestamp + " to: " + "current time (" + System.currentTimeMillis() + ")...");
            writeClassificationDefs(basePath, FILE_CLASSIFICATION_DEFS, getAllClassificationsDefs());
            writeEntityHeaders(basePath, FILE_ENTITY_HEADERS, fromTimestamp);
            displayCrLf("Done!");
        }

        private void writeClassificationDefs(String basePath, String fileName, List<AtlasClassificationDef> classificationDefs) throws IOException {
            FileWriter fileWriter = null;
            try {
                fileWriter = getFileWriter(basePath, fileName);
                for (AtlasClassificationDef classificationDef : classificationDefs) {
                    try {
                        classificationDef.setGuid(null);
                        String json = AtlasType.toJson(classificationDef);
                        fileWriter.write(json + "\n");
                    } catch (Exception e) {
                        LOG.error("Error writing classifications: {}", e);
                        displayCrLf("Error writing classifications.");
                    }
                }
            }
            finally {
                if (fileWriter != null) {
                    fileWriter.close();
                }
            }
        }

        private void writeEntityHeaders(String basePath, String fileName, long fromTimestamp) throws AtlasServiceException, IOException {
            FileWriter fileWriter = null;

            try {
                fileWriter = getFileWriter(basePath, fileName);
            } catch (IOException e) {
                LOG.error("Error opening {}/{}", basePath, fileName, e);
                displayCrLf("Error opening: %", basePath + File.separatorChar + fileName);
                return;
            }

            try {
                AtlasEntityHeaders entityHeaders = atlasClientV2.getEntityHeaders(fromTimestamp);
                int guidHeaderMapSize = entityHeaders.getGuidHeaderMap().size();
                try {
                    displayCrLf("Read entities: " + guidHeaderMapSize);
                    AtlasEntityHeaders updatedHeaders = removeEntityGuids(entityHeaders);
                    fileWriter.write(AtlasType.toJson(updatedHeaders));

                    displayCrLf("Writing entities: " + updatedHeaders.getGuidHeaderMap().size());
                } catch (Exception e) {
                    LOG.error("Error writing: {}", guidHeaderMapSize, e);
                    displayCrLf("Error writing: " + e.toString());
                }
            } finally {
                if (fileWriter != null) {
                    fileWriter.close();
                }
            }
        }

        private AtlasEntityHeaders removeEntityGuids(AtlasEntityHeaders headers) {
            Map<String, AtlasEntityHeader> uniqueNameEntityHeaderMap = new HashMap<>();

            for (AtlasEntityHeader header : headers.getGuidHeaderMap().values()) {
                String uniqueName = getUniqueName(header);
                if (StringUtils.isEmpty(uniqueName)) {
                    displayCrLf("UniqueName is empty.  Ignoring: " + header.getGuid());
                    LOG.warn("UniqueName is empty. Ignoring: {}", AtlasJson.toJson(header));
                    continue;
                }

                displayCrLf("Processing: " + uniqueName);
                if (header.getStatus() == DELETED) {
                    continue;
                }

                String key = String.format("%s:%s", header.getTypeName(), uniqueName);
                header.setGuid(null);
                boolean keyFound = uniqueNameEntityHeaderMap.containsKey(key);
                if (!keyFound) {
                    uniqueNameEntityHeaderMap.put(key, header);
                }

                updateClassificationsForHeader(header, uniqueNameEntityHeaderMap.get(key), keyFound);
                displayCrLf("Processing: " + uniqueName);
            }

            displayCrLf("Processed: " + uniqueNameEntityHeaderMap.size());
            headers.setGuidHeaderMap(uniqueNameEntityHeaderMap);
            return headers;
        }

        private void updateClassificationsForHeader(AtlasEntityHeader header, AtlasEntityHeader currentHeader, boolean keyFound) {
            for (AtlasClassification c : header.getClassifications()) {
                c.setEntityGuid(null);

                if (keyFound) {
                    boolean found =
                            currentHeader.getClassifications().stream().anyMatch(ox -> ox.getTypeName().equals(c.getTypeName()));
                    if (!found) {
                        currentHeader.getClassifications().add(c);
                    } else {
                        displayCrLf("Ignoring: " + c.toString());
                        LOG.warn("Ignoring: {}", AtlasJson.toJson(c));
                    }
                }
            }
        }

        private String getUniqueName(AtlasEntityHeader header) {
            String uniqueAttributeName = ATTR_NAME_QUALIFIED_NAME;
            if (!header.getAttributes().containsKey(ATTR_NAME_QUALIFIED_NAME)) {
                uniqueAttributeName = getUniqueAttribute(header.getTypeName());
            }

            Object attrValue = header.getAttribute(uniqueAttributeName);
            if (attrValue == null) {
                LOG.warn("Unique Attribute Value: empty: {}", AtlasJson.toJson(header));
                return StringUtils.EMPTY;
            }

            return attrValue.toString();
        }

        private String getUniqueAttribute(String typeName) {
            try {
                if (typeNameUniqueAttributeNameMap.containsKey(typeName)) {
                    return typeNameUniqueAttributeNameMap.get(typeName);
                }

                AtlasEntityDef entityDef = atlasClientV2.getEntityDefByName(typeName);
                for (AtlasStructDef.AtlasAttributeDef ad : entityDef.getAttributeDefs()) {
                    if (ad.getIsUnique()) {
                        typeNameUniqueAttributeNameMap.put(typeName, ad.getName());
                        return ad.getName();
                    }
                }
            } catch (AtlasServiceException e) {
                LOG.error("Error fetching type: {}", typeName, e);
                return null;
            }

            return null;
        }

        private List<AtlasClassificationDef> getAllClassificationsDefs() throws Exception {
            MultivaluedMap<String, String> searchParams = new MultivaluedMapImpl();
            searchParams.add(SearchFilter.PARAM_TYPE, "CLASSIFICATION");
            SearchFilter searchFilter = new SearchFilter(searchParams);

            AtlasTypesDef typesDef = atlasClientV2.getAllTypeDefs(searchFilter);
            displayCrLf("Found classifications: " + typesDef.getClassificationDefs().size());
            return typesDef.getClassificationDefs();
        }
    }

    private static class Updater {
        private AtlasClientV2 atlasClientV2;

        public Updater(AtlasClientV2 atlasClientV2) {

            this.atlasClientV2 = atlasClientV2;
        }

        public void run(String basePath) throws Exception {
            if (!fileCheck(basePath, filesToUse, true)) return;

            displayCrLf("Starting...");
            readAndCreateOrUpdateClassificationDefs(basePath, FILE_CLASSIFICATION_DEFS);
            readEntityUpdates(basePath, FILE_ENTITY_HEADERS);
            displayCrLf("Done!");
        }

        private void readEntityUpdates(String basePath, String fileName) throws IOException {
            BufferedReader bufferedReader = null;
            try {
                bufferedReader = getBufferedReader(basePath, fileName);
                String json = bufferedReader.readLine();
                if (StringUtils.isEmpty(json)) {
                    displayCrLf("Empty file encountered: %s", fileName);
                    return;
                }

                AtlasEntityHeaders response = AtlasType.fromJson(json, AtlasEntityHeaders.class);
                displayCrLf("Found :" + response.getGuidHeaderMap().size());
                String output = atlasClientV2.setClassifications(response);
                displayCrLf(output);
            } catch (AtlasServiceException e) {
                displayCrLf("Error updating. Please see log for details.");
                LOG.error("Error updating. {}", e);
            } finally {
                closeReader(bufferedReader);
            }
        }

        private void readAndCreateOrUpdateClassificationDefs(String basePath, String fileName) throws Exception {
            BufferedReader bufferedReader = null;
            try {
                bufferedReader = getBufferedReader(basePath, fileName);

                for (String cd; (cd = bufferedReader.readLine()) != null; ) {
                    AtlasClassificationDef classificationDef = AtlasType.fromJson(cd, AtlasClassificationDef.class);
                    createOrUpdateClassification(classificationDef);
                }
            } finally {
                closeReader(bufferedReader);
            }
        }

        private void createOrUpdateClassification(AtlasClassificationDef classificationDef) {
            String name = classificationDef.getName();
            AtlasTypesDef typesDef = new AtlasTypesDef(null, null, Collections.singletonList(classificationDef), null, null);
            try {
                display("%s -> ", name);
                atlasClientV2.createAtlasTypeDefs(typesDef);
                displayCrLf(" [Done]");
            } catch (AtlasServiceException e) {
                LOG.error("{} skipped!", name, e);
                displayCrLf(" [Skipped]", name);
                updateClassification(classificationDef);
                displayCrLf(" [Done!]");
            } catch (Exception ex) {
                LOG.error("{} skipped!", name, ex);
                displayCrLf(" [Skipped]", name);
            }
        }

        private void updateClassification(AtlasClassificationDef classificationDef) {
            String name = classificationDef.getName();
            AtlasTypesDef typesDef = new AtlasTypesDef(null, null, Collections.singletonList(classificationDef), null, null);
            try {
                display("Update: %s -> ", name);
                atlasClientV2.updateAtlasTypeDefs(typesDef);
                displayCrLf(" [Done]");
            } catch (AtlasServiceException e) {
                LOG.error("{} skipped!", name, e);
                displayCrLf(" [Skipped]", name);
            } catch (Exception ex) {
                LOG.error("{} skipped!", name, ex);
                displayCrLf(" [Skipped]", name);
            }
        }
    }
}
