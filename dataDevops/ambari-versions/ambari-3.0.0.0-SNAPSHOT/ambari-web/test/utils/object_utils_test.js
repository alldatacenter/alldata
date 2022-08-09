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

var objectUtils = require('utils/object_utils');

describe('utils/object_utils', function() {
  describe('#recursiveTree()', function() {
    var testObj = {
      a1: {
        a2: 'v1',
        a3: {
          a4: {
            a5: {
              a6: 'v2',
              a7: 'v3'
            }
          }
        }
      }
    };
    it('should return correct tree of childs', function(){
      var result = objectUtils.recursiveTree(testObj);
      expect(result).to.be.equal('a2 (/a1)<br/>a5 (/a1/a3/a4)<br/>');
    });

    it('should return `null` if type missed', function() {
      var result = objectUtils.recursiveTree('{ a1: "v1"}');
      expect(result).to.be.null;
    });
  });
  describe('#recursiveKeysCount()', function() {
    var tests = [
      {
        m: 'should return 1 child',
        e: 3,
        obj: {
          a1: {
            a2: 'v1',
            a3: 'v2',
            a4: {
              a5: 'v3'
            }
          }
        }
      },
      {
        m: 'should return 1 childs',
        e: 1,
        obj: {
          a1: 'c1'
        }
      },
      {
        m: 'should return `null`',
        e: null,
        obj: 'a1'
      }
    ];
    tests.forEach(function(test){
      it(test.m, function() {
        expect(objectUtils.recursiveKeysCount(test.obj)).to.be.eql(test.e);
      });
    });
  });

  describe('#deepEqual', function() {
    it('simple values', function() {
      expect(objectUtils.deepEqual(true, true)).to.true;
    });
    it('simple values strict', function() {
      expect(objectUtils.deepEqual(true, 1)).to.false;
    });
    it('simple with complex', function() {
      expect(objectUtils.deepEqual(true, {})).to.false;
    });
    it('complex with simple', function() {
      expect(objectUtils.deepEqual({}, 2)).to.false;
    });
    it('simple objects', function() {
      var a = {
        value: 1
      };
      var b = {
        value: 1
      };
      expect(objectUtils.deepEqual(a, b)).to.true;
    });
    it('simple objects failed', function() {
      var a = {
        value: 1,
        c: 1
      };
      var b = {
        value: 1
      };
      expect(objectUtils.deepEqual(a, b)).to.false;
    });
    it('complex objects', function() {
      var a = {
        value: 1,
        c: {
          d: {
            x: {
              val: 1
            }
          }
        }
      };
      var b = {
        value: 1,
        c: {
          d: {
            x: {
              val: 1
            }
          }
        }
      };
      expect(objectUtils.deepEqual(a, b)).to.true;
    });
    it('complex objects failed', function() {
      var a = {
        value: 1,
        c: {
          d: {
            x: {
              val: 1
            }
          }
        }
      };
      var b = {
        value: 1,
        c: {
          d: {
            x: {
              val: 2
            }
          }
        }
      };
      expect(objectUtils.deepEqual(a, b)).to.false;
    });
    it('complex array', function() {
      var a = [1,2,{a: 2}, 4, {b:{}}];
      var b = [1,2,{a: 2}, 4, {b:{}}];
      expect(objectUtils.deepEqual(a, b)).to.true;
    });
    it('complex array failed', function() {
      var a = [1,3,{a: 2}, 4, {b:{}}];
      var b = [1,2,{a: 2}, 4, {b:{}}];
      expect(objectUtils.deepEqual(a, b)).to.false;
    });
    it('simple array', function() {
      var a = [1,3];
      var b = [1,3];
      expect(objectUtils.deepEqual(a, b)).to.true;
    });
    it('simple array failed', function() {
      var a = [3,1];
      var b = [1,3];
      expect(objectUtils.deepEqual(a, b)).to.false;
    });
  });

  describe('#deepMerge', function() {
    var tests = [
      {
        target: {
          a: [
            {
              c: 3
            }
          ]
        },
        source: {
          a: [
            {
              b: 2
            }
          ]
        },
        e: {
          a: [
            {
              c: 3
            },
            {
              b: 2
            }
          ]
        }
      },
      {
        target: {
          a: {}
        },
        source: {
          a: {
            b: 2,
            c: [1,2,3]
          }
        },
        e: {
          a: {
            b: 2,
            c: [1,2,3]
          }
        }
      },
      {
        target: {
          artifact_data: {
            services: [
              {
                name: "HIVE",
                configurations: [
                  {
                    "hive-site": {
                      hive_prop1: "hive_val1"
                    }
                  }
                ]
              }
            ]
          }
        },
        source: {
          artifact_data: {
            services: [
              {
                name: "HDFS",
                configurations: [
                  {
                    "hdfs-site": {
                      hdfs_prop1: "hdfs_val1"
                    }
                  }
                ]
              }
            ]
          }
        },
        e: {
          artifact_data: {
            services: [
              {
                name: "HIVE",
                configurations: [
                  {
                    "hive-site": {
                      hive_prop1: "hive_val1"
                    }
                  }
                ]
              },
              {
                name: "HDFS",
                configurations: [
                  {
                    "hdfs-site": {
                      hdfs_prop1: "hdfs_val1"
                    }
                  }
                ]
              }
            ]
          }
        }
      },
      {
        source: {
          "artifact_data" : {
            "identities" : [
              {
                "principal" : {
                  "value" : "HTTP/_HOST@${realm}",
                  "type" : "service"
                },
                "name" : "spnego",
                "keytab" : {
                  "file" : "${keytab_dir}/spnego.service.keytab",
                  "owner" : {
                    "name" : "root",
                    "access" : "r"
                  },
                  "group" : {
                    "name" : "${cluster-env/user_group}",
                    "access" : "r"
                  }
                }
              },
              {
                "principal" : {
                  "value" : "${cluster-env/smokeuser}-----@${realm}",
                  "local_username" : "${cluster-env/smokeuser}",
                  "configuration" : "cluster-env/smokeuser_principal_name",
                  "type" : "user"
                },
                "name" : "smokeuser",
                "keytab" : {
                  "file" : "${keytab_dir}/smokeuser.headless.keytab",
                  "owner" : {
                    "name" : "${cluster-env/smokeuser}",
                    "access" : "r"
                  },
                  "configuration" : "cluster-env/smokeuser_keytab",
                  "group" : {
                    "name" : "${cluster-env/user_group}",
                    "access" : "r"
                  }
                }
              }
            ]
          }
        },
        target: {
          "artifact_data" : {
            "identities" : [
              {
                "principal" : {
                  "value" : "${cluster-env/smokeuser}@${realm}",
                  "local_username" : "${cluster-env/smokeuser}",
                  "configuration" : "cluster-env/smokeuser_principal_name",
                  "type" : "user"
                },
                "name" : "smokeuser",
                "keytab" : {
                  "file" : "${keytab_dir}/smokeuser.headless.keytab",
                  "owner" : {
                    "name" : "${cluster-env/smokeuser}",
                    "access" : "r"
                  },
                  "configuration" : "cluster-env/smokeuser_keytab",
                  "group" : {
                    "name" : "${cluster-env/user_group}",
                    "access" : "r"
                  }
                }
              },
              {
                "principal" : {
                  "value" : "HTTP/_HOST@${realm}",
                  "local_username" : null,
                  "configuration" : null,
                  "type" : "service"
                },
                "name" : "spnego",
                "keytab" : {
                  "file" : "${keytab_dir}/spnego.service.keytab",
                  "owner" : {
                    "name" : "root",
                    "access" : "r"
                  },
                  "configuration" : null,
                  "group" : {
                    "name" : "${cluster-env/user_group}",
                    "access" : "d"
                  }
                }
              },
              {
                "name": "anotherOne"
              }
            ]
          }
        },
        e: {
          "artifact_data" : {
            "identities" : [
              {
                "principal" : {
                  "value" : "${cluster-env/smokeuser}-----@${realm}",
                  "local_username" : "${cluster-env/smokeuser}",
                  "configuration" : "cluster-env/smokeuser_principal_name",
                  "type" : "user"
                },
                "name" : "smokeuser",
                "keytab" : {
                  "file" : "${keytab_dir}/smokeuser.headless.keytab",
                  "owner" : {
                    "name" : "${cluster-env/smokeuser}",
                    "access" : "r"
                  },
                  "configuration" : "cluster-env/smokeuser_keytab",
                  "group" : {
                    "name" : "${cluster-env/user_group}",
                    "access" : "r"
                  }
                }
              },
              {
                "principal" : {
                  "value" : "HTTP/_HOST@${realm}",
                  "local_username" : null,
                  "configuration" : null,
                  "type" : "service"
                },
                "name" : "spnego",
                "keytab" : {
                  "file" : "${keytab_dir}/spnego.service.keytab",
                  "owner" : {
                    "name" : "root",
                    "access" : "r"
                  },
                  "configuration" : null,
                  "group" : {
                    "name" : "${cluster-env/user_group}",
                    "access" : "r"
                  }
                }
              },
              {
                "name": "anotherOne"
              }
            ]
          }
        }
      }
    ];

    tests.forEach(function(test) {
      it("Should merge objects `{0}`, `{1}`".format(JSON.stringify(test.target), JSON.stringify(test.source)), function() {
        expect(objectUtils.deepMerge(test.target, test.source, test.handler)).to.be.eql(test.e);
      });
    });
  });

  describe('#detectIndexedKey', function() {
    var tests = [
      {
        target: [
          {
            a: 1,
            b: []
          },
          {
            a: 3,
            b: 2
          },
          {
            a: 2,
            b: {}
          }
        ],
        e: 'a',
        m: 'should detect uniq key as `a`'
      },
      {
        target: [
          {
            "principal" : {
              "value" : "HTTP/_HOST@${realm}",
              "local_username" : null,
              "configuration" : null,
              "type" : "service"
            },
            "name" : "spnego",
            "keytab" : {
              "file" : "${keytab_dir}/spnego.service.keytab",
              "owner" : {
                "name" : "root",
                "access" : "r"
              },
              "configuration" : null,
              "group" : {
                "name" : "${cluster-env/user_group}",
                "access" : "r"
              }
            }
          },
          {
            "principal" : {
              "value" : "${cluster-env/smokeuser}${principal_suffix}@${realm}",
              "local_username" : "${cluster-env/smokeuser}",
              "configuration" : "cluster-env/smokeuser_principal_name",
              "type" : "user"
            },
            "name" : "smokeuser",
            "keytab" : {
              "file" : "${keytab_dir}/smokeuser.headless.keytab",
              "owner" : {
                "name" : "${cluster-env/smokeuser}",
                "access" : "r"
              },
              "configuration" : "cluster-env/smokeuser_keytab",
              "group" : {
                "name" : "${cluster-env/user_group}",
                "access" : "r"
              }
            }
          }
        ],
        e: 'name',
        m: 'should detect uniq key as `name`'
      },
    ];

    tests.forEach(function(test) {
      it(test.m, function() {
        expect(objectUtils.detectIndexedKey(test.target)).to.eql(test.e);
      });
    });
  });

  describe('#smartArrayObjectMerge', function() {
    var tests = [
      {
        target: [
          {
            a: 2,
            B: 2
          }
        ],
        source: [
          {
            a: 3,
            c: 4
          },
        ],
        m: 'should merge {0} {1}, into {2}',
        e: [
          {
            a: 2,
            B: 2
          },
          {
            a: 3,
            c: 4
          }
        ]
      },
      {
        target: [
          {
            a: 2,
            B: 2
          }
        ],
        source: [
          {
            a: 2,
            B: 3,
            b: 4
          },
          {
            a: 3,
            c: 4
          }
        ],
        m: 'should merge {0} {1}, into {2}',
        e: [
          {
            a: 2,
            B: 3,
            b: 4
          },
          {
            a: 3,
            c: 4
          }
        ]
      },
      {
        target: [
          {
            "spark-defaults" : {
              "spark.history.kerberos.enabled" : "true",
              "spark.history.enabled" : "no"
            }
          }
        ],
        source: [
          {
            "spark-defaults" : {
              "spark.history.kerberos.enabled" : "false"
            }
          },
          {
            "spark-site" : {
              "spark.property" : "false"
            }
          }
        ],
        m: 'should merge {0} {1}, into {2}',
        e: [
          {
            "spark-defaults" : {
              "spark.history.kerberos.enabled" : "true",
              "spark.history.enabled" : "no"
            }
          },
          {
            "spark-site" : {
              "spark.property" : "false"
            }
          }
        ]
      }
    ];

    tests.forEach(function(test) {
      it(test.m.format(JSON.stringify(test.target), JSON.stringify(test.source), JSON.stringify(test.e)), function() {
        expect(objectUtils.smartArrayObjectMerge(test.target, test.source).toArray()).to.be.eql(test.e);
      });
    });
  });
});
