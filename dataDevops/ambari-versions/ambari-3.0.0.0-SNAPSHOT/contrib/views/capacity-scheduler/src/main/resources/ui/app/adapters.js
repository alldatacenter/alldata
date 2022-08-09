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

var App = require('app');

/**
 * Gets the view backend URI
 *
 * @return  view backend URI
 */
function _getCapacitySchedulerViewUri(adapter) {
  if (App.testMode)
    return "/data";

  var parts = window.location.pathname.match(/[^\/]*/g).filterBy('').removeAt(0),
      view = parts[parts.length - 3],
      version = parts[parts.length - 2],
      instance = parts[parts.length - 1];
  if (!/^(\d+\.){2,3}\d+$/.test(parts[parts.length - 2])) { // version is not present
    instance = parts[parts.length - 2];
    version = '';
  }

  return '/' + [adapter.namespace,'views',view,'versions',version,'instances',instance,'resources','scheduler','configuration'].join('/');
}

function _ajaxOptions(url, type, hash) {
  hash = hash || {};
  hash.url = url;
  hash.type = type;
  hash.dataType = 'json';
  hash.context = this;

  hash.beforeSend = function (xhr) {
    xhr.setRequestHeader('X-Requested-By', 'view-capacity-scheduler');
  };
  return hash;
};
function _ajaxError(jqXHR) {
  if (jqXHR && typeof jqXHR === 'object') {
    jqXHR.then = null;
  }

  return jqXHR;
};
function _ajax(url, type, hash) {
  return new Ember.RSVP.Promise(function(resolve, reject) {
    hash = _ajaxOptions(url, type, hash);

    hash.success = function(json) {
      Ember.run(null, resolve, json);
    };

    hash.error = function(jqXHR, textStatus, errorThrown) {
      Ember.run(null, reject, _ajaxError(jqXHR));
    };

    Ember.$.ajax(hash);
  }, "App: Adapters#ajax " + type + " to " + url);
};


App.ConfigAdapter = DS.Adapter.extend({
  defaultSerializer:'config',
  namespace: 'api/v1'.replace(/^\//, ''),
  findQuery : function(store, type, query){
    var adapter = this;
    var uri = [_getCapacitySchedulerViewUri(this),'getConfig'].join('/') + "?siteName=" + query.siteName + "&configName="+ query.configName;

    if (App.testMode)
      uri = uri + ".json";

    return new Ember.RSVP.Promise(function(resolve, reject) {
      _ajax(uri ,'GET').then(function(data) {
        Ember.run(null, resolve, data);
      }, function(jqXHR) {
        jqXHR.then = null;
        Ember.run(null, resolve, false);
      });
    }, "App: ConfigAdapter#findQuery siteName : " + query.siteName + ", configName : " + query.configName);
  }
});

App.QueueAdapter = DS.Adapter.extend({
  defaultSerializer:'queue',
  PREFIX: "yarn.scheduler.capacity",
  namespace: 'api/v1'.replace(/^\//, ''),
  queues: [],

  createRecord: function(store, type, record) {
    var data = record.serialize({clone:true});
    return new Ember.RSVP.Promise(function(resolve, reject) {
      if (type.typeKey === 'label') {
        Ember.run(record, resolve, {'label':data});
        return;
      }
      return store.filter('queue',function (q) {
        return q.id === record.id;
      }).then(function (queues) {
        var message;
        if (record.get('name.length')==0) {
          message = "Field can not be empty";
        } else if (queues.get('length') > 1) {
          message = "Queue already exists";
        }
        if (message) {
          var error = new DS.InvalidError({path:[message]});
          store.recordWasInvalid(record, error.errors);
          return;
        }
        data.labelsEnabled = record.get('labelsEnabled');
        Ember.run(record, resolve, {'queue':data,'label':[]});

      });
    },'App: QueueAdapter#createRecord ' + type + ' ' + record.id);
  },

  deleteRecord:function (store, type, record) {
    return new Ember.RSVP.Promise(function(resolve, reject) {
      Ember.run(null, resolve, {'queue':record.serialize({ includeId: true , clone: true })});
    },'App: QueueAdapter#deleteRecord ' + type + ' ' + record.id);
  },

  saveMark:'',

  updateRecord:function (store,type,record) {
    var adapter = this,
        uri = _getCapacitySchedulerViewUri(this),
        serializer = store.serializerFor('queue'),
        props = serializer.serializeConfig(record),
        new_tag = 'version' + Math.floor(+moment()),
        data;

    data = JSON.stringify({'Clusters':
      {'desired_config':
        [{
          'type': 'capacity-scheduler',
          'tag': new_tag,
          'service_config_version_note': props.service_config_version_note,
          'properties': props.properties
        }]
      }
    });

    return new Ember.RSVP.Promise(function(resolve, reject) {
      _ajax(uri,'PUT',{contentType:'text/plain; charset=utf-8',data:data}).then(function(data) {
        store.setProperties({'current_tag':new_tag,'tag':new_tag});
        Ember.run(null, resolve, data.resources.objectAt(0).configurations.objectAt(0).configs);
      }, function(jqXHR) {
        jqXHR.then = null;
        Ember.run(null, reject, jqXHR);
      });
    },'App: QueueAdapter#updateRecord save config woth ' + new_tag + ' tag');
  },

  relaunchCapSched: function (opt) {
    if (!opt) return;
    var uri = [_getCapacitySchedulerViewUri(this),opt].join('/');
    return new Ember.RSVP.Promise(function(resolve, reject) {
      _ajax(uri,'PUT',{contentType:'application/json; charset=utf-8',data:JSON.stringify({save:true})})
      .then(function (data) {
        resolve(data);
      },function (error) {
        reject(error);
      });
    }.bind(this),'App: QueueAdapter#relaunchCapSched ' + opt + ' capacity-scheduler');
  },

  /**
   * Finds queue by id in store.
   *
   */
  find: function(store, type, id) {
    id = id.toLowerCase();
    var record = store.getById(type,id);
    var key = type.typeKey;
    var json = {};
    if (record) {
      return new Ember.RSVP.Promise(function(resolve, reject) {
        json[key] = record.toJSON({includeId:true});
        resolve(json);
      },'App: QueueAdapter#find ' + type + ' ' + id);
    } else {
      return store.findAll('queue').then(function (queues) {
        json[key] = store.getById(type,id).toJSON({includeId:true});
        resolve(json);
      });
    }
  },

  /**
   * Finds all queues.
   *
   */
  findAll: function(store, type) {
    var adapter = this;
    var uri = _getCapacitySchedulerViewUri(this);
    if (App.testMode)
      uri = uri + "/scheduler-configuration.json";

    return new Ember.RSVP.Promise(function(resolve, reject) {
      _ajax(uri,'GET').then(function(data) {
        var config = data.items.objectAt(0);
        if (!store.get('isInitialized')) {
          store.set('current_tag',config.tag);
        }
        store.setProperties({'clusterName':config.Config.cluster_name,'tag':config.tag});
        Ember.run(null, resolve, data.items.objectAt(0).properties);
      }, function(jqXHR) {
        jqXHR.then = null;
        Ember.run(null, reject, jqXHR);
      });
    },'App: QueueAdapter#findAll ' + type);
  },

  findAllTagged: function(store, type) {
    var adapter = this,
        tag = store.get('tag'),
        uri = [_getCapacitySchedulerViewUri(this),'byTag',tag].join('/');

    return new Ember.RSVP.Promise(function(resolve, reject) {
      _ajax(uri,'GET').then(function(data) {
        Ember.run(null, resolve, data);
      }, function(jqXHR) {
        jqXHR.then = null;
        Ember.run(null, reject, jqXHR);
      });
    },'App: QueueAdapter#findAllTagged ' + tag);
  },

  getNodeLabels:function (store) {
    var uri = [_getCapacitySchedulerViewUri(this),'nodeLabels'].join('/');
    var stackId = store.get('stackId'),
    stackVersion = stackId.substr(stackId.indexOf('-') + 1);

    if (App.testMode)
      uri = uri + ".json";

    return new Ember.RSVP.Promise(function(resolve, reject) {
      _ajax(uri,'GET').then(function(data) {
        var parsedData = JSON.parse(data), labels;

        if (parsedData !== null) {
          store.set('isNodeLabelsConfiguredByRM', true);
        } else {
          store.set('isNodeLabelsConfiguredByRM', false);
        }

        if (stackVersion >= 2.5) {
          if (parsedData && Em.isArray(parsedData.nodeLabelInfo)) {
            labels = parsedData.nodeLabelInfo;
          } else {
            labels = (parsedData && parsedData.nodeLabelInfo)?[parsedData.nodeLabelInfo]:[];
          }
          Ember.run(null, resolve, labels.map(function (label) {
            return {name:label.name,exclusivity:label.exclusivity};
          }));
        } else {
          if (parsedData && Em.isArray(parsedData.nodeLabels)) {
            labels = parsedData.nodeLabels;
          } else {
            labels = (parsedData && parsedData.nodeLabels)?[parsedData.nodeLabels]:[];
          }
          Ember.run(null, resolve, labels.map(function (label) {
            return {name:label};
          }));
        }
      }, function(jqXHR) {
        jqXHR.then = null;
        Ember.run(null, reject, jqXHR);
      });
    }.bind(this),'App: QueueAdapter#getNodeLabels');
  },

  getPrivilege:function () {
    var uri = [_getCapacitySchedulerViewUri(this),'privilege'].join('/');
    if (App.testMode)
      uri = uri + ".json";
    return new Ember.RSVP.Promise(function(resolve, reject) {
      _ajax(uri,'GET').then(function(data) {
        Ember.run(null, resolve, data);
      }, function(jqXHR) {
        jqXHR.then = null;
        Ember.run(null, resolve, false);
      });
    }.bind(this),'App: QueueAdapter#getPrivilege');
  },

  checkCluster:function (store) {
    var uri = [_getCapacitySchedulerViewUri(this),'cluster'].join('/');
    if (App.testMode)
      uri = uri + ".json";
    return new Ember.RSVP.Promise(function(resolve, reject) {
      _ajax(uri,'GET').then(function(data) {
        if (data && data.Clusters && data.Clusters.version) {
          store.set("stackId", data.Clusters.version);
        }
        Ember.run(null, resolve, data);
      }, function(jqXHR) {
        if (jqXHR.status === 404) {
          Ember.run(null, resolve, []);
        } else {
          jqXHR.then = null;
          Ember.run(null, reject, jqXHR);
        }
      });
    }.bind(this),'App: QueueAdapter#checkCluster');
  },

  getRmSchedulerConfigInfo: function() {
    var uri = [_getCapacitySchedulerViewUri(this), 'rmCurrentConfig'].join('/');
    if (App.testMode) {
      uri = uri + ".json";
    }
    return new Ember.RSVP.Promise(function(resolve, reject) {
      _ajax(uri, 'GET').then(function(data) {
        Ember.run(null, resolve, data);
      }, function(jqXHR) {
        jqXHR.then = null;
        Ember.run(null, reject, jqXHR);
      });
    }.bind(this),'App: QueueAdapter#getRmSchedulerConfigInfo');
  }
});

App.SchedulerAdapter = App.QueueAdapter.extend({
  find: function(store, type, id) {
    return store.findAll('scheduler').then(function (scheduler) {
      return {"scheduler":scheduler.findBy('id',id).toJSON({includeId:true})};
    });
  }
});

App.TagAdapter = App.QueueAdapter.extend({
  /**
   * Finds all tags.
   *
   */
  findAll: function(store, type) {
    var adapter = this;
    var uri = [_getCapacitySchedulerViewUri(this),'all'].join('/');

    if (App.testMode)
      uri = uri + ".json";

    return new Ember.RSVP.Promise(function(resolve, reject) {
      _ajax(uri ,'GET').then(function(data) {
        Ember.run(null, resolve, data);
      }, function(jqXHR) {
        jqXHR.then = null;
        Ember.run(null, resolve, false);
      });
    }, "App: TagAdapter#findAll " + type);
  }
});
