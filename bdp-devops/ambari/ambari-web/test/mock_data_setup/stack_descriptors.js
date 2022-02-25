/**
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

module.exports = {
  "Artifacts": {
    "artifact_name": "kerberos_descriptor",
    "stack_name": "HDP",
    "stack_version": "2.2"
  },
  "KerberosDescriptor": {
    "kerberos_descriptor": {
      "properties": {
        "realm": "${cluster-env/kerberos_domain}",
        "keytab_dir": "/etc/security/keytabs"
      },
      "identities": [
        {
          "principal": {
            "value": "HTTP/_HOST@${realm}",
            "configuration": null
          },
          "name": "spnego",
          "keytab": {
            "file": "${keytab_dir}/spnego.service.keytab",
            "owner": {
              "name": "root",
              "access": "r"
            },
            "configuration": null,
            "group": {
              "name": "${hadoop-env/user_group}",
              "access": "r"
            }
          }
        },
        {
          "principal": {
            "value": "hdfs@${realm}",
            "configuration": "cluster-env/hdfs_principal_name"
          },
          "name": "hdfs",
          "keytab": {
            "file": "${keytab_dir}/hdfs.headless.keytab",
            "owner": {
              "name": "root",
              "access": "r"
            },
            "configuration": "hadoop-env/hdfs_user_keytab",
            "group": {
              "name": "${cluster-env/user_group}",
              "access": "r"
            }
          }
        },
        {
          "principal": {
            "value": "hbase@${realm}",
            "configuration": "hbase-env/hbase_principal_name"
          },
          "name": "hbase",
          "keytab": {
            "file": "${keytab_dir}/hbase.headless.keytab",
            "owner": {
              "name": "root",
              "access": "r"
            },
            "configuration": "hbase-env/hbase_user_keytab",
            "group": {
              "name": "${cluster-env/user_group}",
              "access": "r"
            }
          }
        },
        {
          "principal": {
            "value": "ambari-qa@${realm}",
            "configuration": "cluster-env/smokeuser_principal_name"
          },
          "name": "smokeuser",
          "keytab": {
            "file": "${keytab_dir}/smokeuser.headless.keytab",
            "owner": {
              "name": "root",
              "access": "r"
            },
            "configuration": "cluster-env/smokeuser_keytab",
            "group": {
              "name": "${cluster-env/user_group}",
              "access": "r"
            }
          }
        }
      ],
      "configurations": [
        {
          "core-site": {
            "hadoop.security.authentication": "kerberos",
            "hadoop.rpc.protection": "authentication; integrity; privacy",
            "hadoop.security.authorization": "true"
          }
        }
      ],
      "services": [
        {
          "name": "HDFS",
          "components": [
            {
              "name": "NAMENODE",
              "identities": [
                {
                  "name": "namenode_nn",
                  "principal": {
                    "value": "nn/_HOST@${realm}",
                    "configuration": "hdfs-site/dfs.namenode.kerberos.principal"
                  },
                  "keytab": {
                    "file": "${keytab_dir}/nn.service.keytab",
                    "owner": {
                      "name": "${hadoop-env/hdfs_user}",
                      "access": "r"
                    },
                    "group": {
                      "name": "${cluster-env/user_group}",
                      "access": ""
                    },
                    "configuration": "hdfs-site/dfs.namenode.keytab.file"
                  }
                },
                {
                  "name": "namenode_host",
                  "principal": {
                    "value": "host/_HOST@${realm}",
                    "configuration": "hdfs-site/dfs.namenode.kerberos.https.principal"
                  },
                  "keytab": {
                    "file": "${keytab_dir}/host.keytab",
                    "owner": {
                      "name": "${hadoop-env/hdfs_user}",
                      "access": "r"
                    },
                    "group": {
                      "name": "${cluster-env/user_group}",
                      "access": ""
                    },
                    "configuration": "hdfs-site/dfs.namenode.keytab.file"
                  }
                },
                {
                  "name": "/spnego",
                  "principal": {
                    "configuration": "hdfs-site/dfs.web.authentication.kerberos.principal"
                  },
                  "keytab": {
                    "configuration": "hdfs/dfs.web.authentication.kerberos.keytab"
                  }
                }
              ]
            },
            {
              "name": "DATANODE",
              "identities": [
                {
                  "name": "datanode_dn",
                  "principal": {
                    "value": "dn/_HOST@${realm}",
                    "configuration": "hdfs-site/dfs.namenode.kerberos.principal"
                  },
                  "keytab": {
                    "file": "${keytab_dir}/dn.service.keytab",
                    "owner": {
                      "name": "${hadoop-env/hdfs_user}",
                      "access": "r"
                    },
                    "group": {
                      "name": "${cluster-env/user_group}",
                      "access": ""
                    },
                    "configuration": "hdfs-site/dfs.namenode.keytab.file"
                  }
                },
                {
                  "name": "datanode_host",
                  "principal": {
                    "value": "host/_HOST@${realm}",
                    "configuration": "hdfs-site/dfs.datanode.kerberos.https.principal"
                  },
                  "keytab": {
                    "file": "${keytab_dir}/host.keytab.file",
                    "owner": {
                      "name": "${hadoop-env/hdfs_user}",
                      "access": "r"
                    },
                    "group": {
                      "name": "${cluster-env/user_group}",
                      "access": ""
                    },
                    "configuration": "hdfs-site/dfs.namenode.secondary.keytab.file"
                  }
                }
              ]
            },
            {
              "name": "SECONDARY_NAMENODE",
              "identities": [
                {
                  "name": "secondary_namenode_nn",
                  "principal": {
                    "value": "nn/_HOST@${realm}",
                    "configuration": "hdfs-site/dfs.namenode.secondary.kerberos.principal"
                  },
                  "keytab": {
                    "file": "${keytab_dir}/snn.service.keytab",
                    "owner": {
                      "name": "${hadoop-env/hdfs_user}",
                      "access": "r"
                    },
                    "group": {
                      "name": "${cluster-env/user_group}",
                      "access": ""
                    },
                    "configuration": "hdfs-site/dfs.namenode.secondary.keytab.file"
                  }
                },
                {
                  "name": "secondary_namenode_host",
                  "principal": {
                    "value": "host/_HOST@${realm}",
                    "configuration": "hdfs-site/dfs.namenode.secondary.kerberos.https.principal"
                  },
                  "keytab": {
                    "file": "${keytab_dir}/host.keytab.file",
                    "owner": {
                      "name": "${hadoop-env/hdfs_user}",
                      "access": "r"
                    },
                    "group": {
                      "name": "${cluster-env/user_group}",
                      "access": ""
                    },
                    "configuration": "hdfs-site/dfs.namenode.secondary.keytab.file"
                  }
                },
                {
                  "name": "/spnego",
                  "principal": {
                    "configuration": "hdfs-site/dfs.web.authentication.kerberos.principal"
                  },
                  "keytab": {
                    "configuration": "hdfs/dfs.web.authentication.kerberos.keytab"
                  }
                }
              ]
            }
          ]
        },
        {
          "name": "FALCON",
          "identities": [
            {
              "name": "/spnego"
            },
            {
              "name": "/smokeuser"
            },
            {
              "name": "/hdfs"
            }
          ],
          "configurations": [
            {
              "falcon-startup.properties": {
                "*.falcon.http.authentication.type": "kerberos",
                "*.falcon.authentication.type": "kerberos",
                "*.dfs.namenode.kerberos.principal": "nn/_HOST@${realm}"
              }
            }
          ],
          "components": [
            {
              "name": "FALCON_SERVER",
              "identities": [
                {
                  "principal": {
                    "value": "falcon/${host}@${realm}",
                    "configuration": "falcon-startup.properties/*.falcon.service.authentication.kerberos.principal"
                  },
                  "name": "falcon_server",
                  "keytab": {
                    "file": "${keytab_dir}/falcon.service.keytab",
                    "owner": {
                      "name": "${falcon-env/falcon_user}",
                      "access": "r"
                    },
                    "configuration": "falcon-startup.properties/*.falcon.service.authentication.kerberos.keytab",
                    "group": {
                      "name": "${cluster-env/user_group}",
                      "access": ""
                    }
                  }
                },
                {
                  "principal": {
                    "value": "HTTP/${host}@${realm}",
                    "configuration": "falcon-startup.properties/oozie.authentication.kerberos.principal"
                  },
                  "name": "/spnego",
                  "keytab": {
                    "file": null,
                    "owner": {
                      "name": null,
                      "access": null
                    },
                    "configuration": "falcon-startup.properties/oozie.authentication.kerberos.keytab",
                    "group": {
                      "name": null,
                      "access": null
                    }
                  }
                }
              ]
            }
          ]
        }
      ]
    }
  }
};
