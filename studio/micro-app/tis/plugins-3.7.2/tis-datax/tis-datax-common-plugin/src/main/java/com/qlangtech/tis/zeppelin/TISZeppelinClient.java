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

package com.qlangtech.tis.zeppelin;

import com.qlangtech.tis.extension.INotebookable;
import com.qlangtech.tis.manage.common.TisUTF8;
import com.qlangtech.tis.plugin.AuthToken;
import com.qlangtech.tis.plugin.HttpEndpoint;
import com.qlangtech.tis.plugin.IdentityName;
import com.qlangtech.tis.plugin.aliyun.AccessKey;
import com.qlangtech.tis.plugin.aliyun.NoneToken;
import com.qlangtech.tis.plugin.aliyun.UsernamePassword;
import com.qlangtech.tis.web.start.TisAppLaunch;
import com.qlangtech.tis.web.start.TisSubModule;
import org.apache.commons.io.FileUtils;
import org.apache.zeppelin.client.ClientConfig;
import org.apache.zeppelin.client.ZeppelinClient;

import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.function.Consumer;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2022-12-28 12:18
 **/
public class TISZeppelinClient {

    private static final ZeppelinClient zeppelinClient;

    static {
        try {
            ClientConfig clientConfig = new ClientConfig(
                    "http://127.0.0.1:" + (TisSubModule.ZEPPELIN.getLaunchPort())+ TisSubModule.ZEPPELIN.servletContext);
            zeppelinClient = new ZeppelinClient(clientConfig);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    public static String createJdbcNotebook(IdentityName pluginId) {
        return createOrGetNotebook(pluginId, INotebookable.InterpreterGroup.JDBC_GROUP, (props) -> {
            props.add("default.url", "tis_url");
            props.add("default.tisDbName", pluginId.identityValue());
            props.add("default.driver", pluginId.identityValue() + "driver");
        });
    }

    public static final String ELASTICSEARCH_HOST = "elasticsearch.host";
    public static final String ELASTICSEARCH_PORT = "elasticsearch.port";
    public static final String ELASTICSEARCH_CLIENT_TYPE = "elasticsearch.client.type";
    public static final String ELASTICSEARCH_CLUSTER_NAME = "elasticsearch.cluster.name";
    public static final String ELASTICSEARCH_RESULT_SIZE = "elasticsearch.result.size";
    public static final String ELASTICSEARCH_BASIC_AUTH_USERNAME = "elasticsearch.basicauth.username";
    public static final String ELASTICSEARCH_BASIC_AUTH_PASSWORD = "elasticsearch.basicauth.password";

    public static String createESNotebook(HttpEndpoint endpoint) throws Exception {
        URL endpointURL = new URL(endpoint.getEndpoint());
        return createOrGetNotebook(endpoint, INotebookable.InterpreterGroup.ELASTIC_GROUP,
                (props) -> {
                    props.add(ELASTICSEARCH_HOST, endpointURL.getHost());
                    props.add(ELASTICSEARCH_PORT, String.valueOf(endpointURL.getPort()));
                    props.add(ELASTICSEARCH_CLIENT_TYPE, "http");
                    endpoint.accept(new AuthToken.Visitor<Void>() {
                        @Override
                        public Void visit(NoneToken noneToken) {
                            return null;
                        }

                        @Override
                        public Void visit(AccessKey accessKey) {
                            return null;
                        }

                        @Override
                        public Void visit(UsernamePassword token) {
                            props.add(ELASTICSEARCH_BASIC_AUTH_USERNAME, token.userName);
                            props.add(ELASTICSEARCH_BASIC_AUTH_PASSWORD, token.password);
                            return null;
                        }
                    });
                }
        );
    }

    /**
     * @param pluginId
     * @return notebookId
     */
    private static String createOrGetNotebook(IdentityName pluginId, INotebookable.InterpreterGroup group
            , Consumer<ZeppelinClient.InterpreterProps> propsSetter) {
        try {
            String idVal = pluginId.identityValue();
            String notebookId = null;
            File zeppelinDir = new File(com.qlangtech.tis.TIS.pluginCfgRoot, "zeppelin");
            File notebookDir = new File(zeppelinDir, "notebook");
            File notebookToken = new File(notebookDir, "notebook_" + idVal);
            if (notebookToken.exists()) {
                notebookId = FileUtils.readFileToString(notebookToken, TisUTF8.get());
                return notebookId;
            }


            File interpreterCfg = new File(zeppelinDir, "interpreter/" + idVal);
            if (!interpreterCfg.exists()) {
                zeppelinClient.deleteInterpreter(idVal);
// List<String> jdbcUrls = dsFactory.getJdbcUrls();
//                if (CollectionUtils.isEmpty(jdbcUrls)) {
//                    throw new IllegalStateException("dataSource:" + idVal + " relevant jdbcUrl can not be empty");
//                }
                // FileUtils.write(interpreterCfg, zeppelinClient.createJDBCInterpreter(idVal), TisUTF8.get(), false);
                FileUtils.write(interpreterCfg, zeppelinClient.createInterpreter(idVal, group, propsSetter), TisUTF8.get(), false);
            }

            final String targetNotePath = "/tis/" + idVal;

            List<org.apache.zeppelin.common.NotebookInfo> notebookInfos = zeppelinClient.listNotes();
            for (org.apache.zeppelin.common.NotebookInfo enote : notebookInfos) {
                // 将已经存在的相同路径的note删除掉
                if (targetNotePath.equalsIgnoreCase(enote.getPath())) {
                    zeppelinClient.deleteNote(enote.getId());
                }
            }

            notebookId = zeppelinClient.createNoteWithParagraph(targetNotePath, ZeppelinClient.getInterpreterName(idVal));
            FileUtils.write(notebookToken, notebookId, TisUTF8.get(), false);
            return notebookId;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
