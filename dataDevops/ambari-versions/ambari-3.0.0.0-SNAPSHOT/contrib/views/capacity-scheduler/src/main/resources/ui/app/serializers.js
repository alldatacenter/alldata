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
 * Recursively builds the list of queues.
 *
 */
function _recurseQueues(parentQueue, queueName, depth, props, queues, store) {
  var serializer = store.serializerFor('queue');
  var prefix = serializer.PREFIX;
  var parentPath = '';
  if (parentQueue != null) {
    parentPath = parentQueue.path;
    prefix += ".";
  }

  var queue = serializer.extractQueue({ name: queueName, parentPath: parentPath, depth: depth}, props);
  queues.push(queue);

  var queueProp = prefix + parentPath + "." + queueName + ".queues";
  if (props[queueProp]) {
    var qs = props[queueProp].split(',');
    for (var i=0; i < qs.length; i++) {
      queues = _recurseQueues(queue, qs[i], depth+1, props, queues, store);
    }
  }

  return queues;
}

App.SerializerMixin = Em.Mixin.create({

  PREFIX:"yarn.scheduler.capacity",

  serializeConfig:function (records) {
    var config = {},
        note = this.get('store.configNote');

    Em.EnumerableUtils.forEach(records,function (record) {
      this.serializeIntoHash(config, record.constructor, record);
    },this);

    for (var i in config) {
      if (config[i] === null || config[i] === undefined) {
        delete config[i];
      }
    }

    this.set('store.configNote','');

    return {properties : config, service_config_version_note: note};

  },

  serializeIntoHash: function(hash, type, record, options) {
    Em.merge(hash,this.store.serializerFor(type).serialize(record, options));
  },

  extractUpdateRecord: function(store, type, payload) {
    return this.extractArray(store, App.Queue, payload);
  },

  extractCreateRecord: function(store, type, payload) {
    this._setupLabels({},[payload.queue],payload.label);
    return this.extractSave(store, type, payload);
  },
  extractQueue: function(data, props) {
    var path = (data.parentPath == null || data.parentPath.length == 0)?data.name:[data.parentPath, data.name].join('.'),
        base_path = this.PREFIX + "." + path,
        labelsPath = base_path + ".accessible-node-labels",
        q = {
          id: path.toLowerCase(),
          name: data.name,
          path: path,
          parentPath: data.parentPath,
          depth: data.depth,
          //sort queue list to avoid of parsing different sorting as changed value
          queues:                        (props[base_path + ".queues"] || '').split(',').sort().join(',') || null,
          unfunded_capacity:             props[base_path + ".unfunded.capacity"] || null,
          state:                         props[base_path + ".state"] || null,
          acl_administer_queue:          props[base_path + ".acl_administer_queue"] || null,
          acl_submit_applications:       props[base_path + ".acl_submit_applications"] || null,
          user_limit_factor:             (props[base_path + ".user-limit-factor"])?+props[base_path + ".user-limit-factor"]:null,
          minimum_user_limit_percent:    (props[base_path + ".minimum-user-limit-percent"])?+props[base_path + ".minimum-user-limit-percent"]:null,
          maximum_applications:          (props[base_path + ".maximum-applications"])?+props[base_path + ".maximum-applications"]:null,
          maximum_am_resource_percent:   (props[base_path + ".maximum-am-resource-percent"])?+props[base_path + ".maximum-am-resource-percent"]*100:null, // convert to percent
          ordering_policy:               props[base_path + ".ordering-policy"] || null,
          enable_size_based_weight:      props[base_path + ".ordering-policy.fair.enable-size-based-weight"] || null,
          default_node_label_expression: props[base_path + ".default-node-label-expression"] || null,
          priority:                      (props[base_path + ".priority"])? +props[base_path + ".priority"] : 0,
          labelsEnabled:                 props.hasOwnProperty(labelsPath),
          disable_preemption:            props[base_path + '.disable_preemption'] || '',
          isPreemptionInherited:         (props[base_path + '.disable_preemption'] !== undefined)?false:true,
          maximum_allocation_mb:         props[base_path + '.maximum-allocation-mb'] || null,
          maximum_allocation_vcores:     props[base_path + '.maximum-allocation-vcores'] || null,
          maximum_application_lifetime:     props[base_path + '.maximum-application-lifetime'] || null,
          default_application_lifetime:     props[base_path + '.default-application-lifetime'] || null
        };

    //Converting capacity and max-capacity into two decimal point float numbers
    q.capacity = (props[base_path + ".capacity"])? +parseFloat(props[base_path + ".capacity"]).toFixed(2) : null;
    q.maximum_capacity = (props[base_path + ".maximum-capacity"])? +parseFloat(props[base_path + ".maximum-capacity"]).toFixed(2) : null;

    switch ((props.hasOwnProperty(labelsPath))?props[labelsPath].trim():'') {
      case '*':
        q.labels = this.get('store.nodeLabels.content').map(function(item) {
          return [q.id,item.name].join('.');
        });
        q._accessAllLabels = true;
        break;
      case '':
        q.labels = [];
        q._accessAllLabels = false;
        break;
      default:
        q._accessAllLabels = false;
        q.labels = props[labelsPath].split(',').map(function(labelName) {
          if (!this.get('store.nodeLabels.content').isAny('name',labelName)) {
            this.get('store.nodeLabels.content').pushObject({ name:labelName, notExist:true });
          }
          return [q.id,labelName].join('.');
        }.bind(this)).compact();
        break;
    }

    return q;
  },
  normalizePayload: function (properties) {
    if ((properties && properties.hasOwnProperty('queue')) || !properties) {
      return properties;
    }
    var labels = [], queues = [];

    var scheduler = [{
      id:                          'scheduler',
      maximum_am_resource_percent:    properties[this.PREFIX + ".maximum-am-resource-percent"]*100 || null, // convert to percent
      maximum_applications:           properties[this.PREFIX + ".maximum-applications"] || null,
      node_locality_delay:            properties[this.PREFIX + ".node-locality-delay"] || null,
      resource_calculator:            properties[this.PREFIX + ".resource-calculator"] || null,
      queue_mappings:                 properties[this.PREFIX + ".queue-mappings"] || null,
      queue_mappings_override_enable: properties[this.PREFIX + ".queue-mappings-override.enable"] || null
    }];
    _recurseQueues(null, "root", 0, properties, queues, this.get('store'));
    this._setupLabels(properties,queues,labels,this.PREFIX);

    return {'queue':queues,'scheduler':scheduler,'label':labels};
  },
  _setupLabels :function (properties, queues, labels) {
    var prefix = this.PREFIX;
    var nodeLabels = this.get('store.nodeLabels.content');
    queues.forEach(function(queue) {
      nodeLabels.forEach(function(label) {
        var labelId = [queue.id,label.name].join('.'),
            cp =  [prefix, queue.path, 'accessible-node-labels',label.name,'capacity'].join('.'),
            mcp = [prefix, queue.path, 'accessible-node-labels',label.name,'maximum-capacity'].join('.'),
            labelCapacity = properties.hasOwnProperty(cp)?+properties[cp]:0;
        labels.push({
          id:labelId,
          capacity:labelCapacity,
          maximum_capacity:properties.hasOwnProperty(mcp)?+properties[mcp]:100,
          queue:(queue.labels.contains(labelId))?queue.id:null
        });
      });

      if (queue._accessAllLabels) {
        queue.labels = nodeLabels.map(function(label) {
            return [queue.id,label.name].join('.');
        }.bind(this)).compact();
      }
	  });
	  return labels;
	}
});

App.SchedulerSerializer = DS.RESTSerializer.extend(App.SerializerMixin,{
  serialize:function (record, options) {
    var json = {};

    json[this.PREFIX + ".maximum-am-resource-percent"] = record.get('maximum_am_resource_percent')/100; // convert back to decimal
    json[this.PREFIX + ".maximum-applications"] = record.get('maximum_applications');
    json[this.PREFIX + ".node-locality-delay"] = record.get('node_locality_delay');
    json[this.PREFIX + ".resource-calculator"] = record.get('resource_calculator');
    json[this.PREFIX + ".queue-mappings"] = record.get('queue_mappings') || null;
    json[this.PREFIX + ".queue-mappings-override.enable"] = record.get('queue_mappings_override_enable');

    return json;
  }
});

App.QueueSerializer = DS.RESTSerializer.extend(App.SerializerMixin,{
  serialize:function (record, options) {
    var json = {};

    if (options && options.clone) {
      Em.merge(json,record.toJSON({ includeId:true }));

      record.eachRelationship(function(key, relationship) {
        if (relationship.kind === 'belongsTo') {
          //TODO will implement if need
        } else if (relationship.kind === 'hasMany') {
          json[key] = record.get(key).mapBy('id');
        }
      }, this);

      return json;
    }

    json[this.PREFIX + "." + record.get('path') + ".unfunded.capacity"] = record.get('unfunded_capacity');
    json[this.PREFIX + "." + record.get('path') + ".acl_administer_queue"] = record.get('acl_administer_queue');
    json[this.PREFIX + "." + record.get('path') + ".acl_submit_applications"] = record.get('acl_submit_applications');
    json[this.PREFIX + "." + record.get('path') + ".minimum-user-limit-percent"] = record.get('minimum_user_limit_percent');
    json[this.PREFIX + "." + record.get('path') + ".maximum-capacity"] = record.get('maximum_capacity');
    json[this.PREFIX + "." + record.get('path') + ".user-limit-factor"] = record.get('user_limit_factor');
    json[this.PREFIX + "." + record.get('path') + ".state"] = record.get('state');
    json[this.PREFIX + "." + record.get('path') + ".capacity"] = record.get('capacity');
    json[this.PREFIX + "." + record.get('path') + ".queues"] = record.get('queues')||null;
    json[this.PREFIX + "." + record.get('path') + ".default-node-label-expression"] = record.get('default_node_label_expression')||null;
    json[this.PREFIX + "." + record.get('path') + ".ordering-policy"] = record.get('ordering_policy')||null;
    json[this.PREFIX + "." + record.get('path') + ".maximum-allocation-mb"] = record.get('maximum_allocation_mb') || null;
    json[this.PREFIX + "." + record.get('path') + ".maximum-allocation-vcores"] = record.get('maximum_allocation_vcores') || null;
    json[this.PREFIX + "." + record.get('path') + ".maximum-application-lifetime"] = record.get('maximum_application_lifetime') || null;
    json[this.PREFIX + "." + record.get('path') + ".default-application-lifetime"] = record.get('default_application_lifetime') || null;

    if (record.get('ordering_policy') == 'fair') {
      json[this.PREFIX + "." + record.get('path') + ".ordering-policy.fair.enable-size-based-weight"] = record.get('enable_size_based_weight');
    }

    if (this.get('store.isPriorityUtilizationSupported')) {
      json[this.PREFIX + "." + record.get('path') + ".priority"] = record.get('priority') || 0;
    }

    // do not set property if not set
    var ma = record.get('maximum_applications')||'';
    if (ma) {
      json[this.PREFIX + "." + record.get('path') + ".maximum-applications"] = ma;
    }

    // do not set property if not set
    var marp = record.get('maximum_am_resource_percent')||'';
    if (marp) {
      marp = marp/100; // convert back to decimal
      json[this.PREFIX + "." + record.get('path') + ".maximum-am-resource-percent"] = marp;
    }

    record.eachRelationship(function(key, relationship) {
      if (relationship.kind === 'belongsTo') {
        this.serializeBelongsTo(record, json, relationship);
      } else if (relationship.kind === 'hasMany') {
        this.serializeHasMany(record, json, relationship);
      }
    }, this);

    var isPreemptionSupported = record.get('store.isPreemptionSupported');
    if (isPreemptionSupported && !record.get('isPreemptionInherited')) {
      json[this.PREFIX + "." + record.get('path') + ".disable_preemption"] = (record.get('disable_preemption')==='true')? true:false;
    }

    return json;
  },
  serializeHasMany:function (record, json, relationship) {
    var key = relationship.key,
        recordLabels = record.get(key),
        accessible_node_labels_key = [this.PREFIX, record.get('path'), 'accessible-node-labels'].join('.');

    switch (true) {
      case (record.get('accessAllLabels')):
        json[accessible_node_labels_key] = '*';
        break;
      case (!Em.isEmpty(recordLabels)):
        json[accessible_node_labels_key] = recordLabels.mapBy('name').join(',');
        break;
      case (record.get('labelsEnabled')):
        json[accessible_node_labels_key] = '';
        break;
      default:
        json[accessible_node_labels_key] = null;
    }

    recordLabels.forEach(function (l) {
        json[[accessible_node_labels_key, l.get('name'), 'capacity'].join('.')] = l.get('capacity');
        json[[accessible_node_labels_key, l.get('name'), 'maximum-capacity'].join('.')] = l.get('maximum_capacity');
    });
  }
});

App.LabelSerializer = DS.RESTSerializer.extend({
  serialize:function () {
    return {};
  }
});

App.TagSerializer = DS.RESTSerializer.extend({
  extractFindAll: function(store, type, payload){
    return this.extractArray(store, type, {'tag':payload.items});
  },
  normalizeHash: {
    tag: function(hash) {
      hash.id = hash.version;
      delete hash.version;
      delete hash.href;
      delete hash.Config;
      delete hash.type;
      return hash;
    }
  }
});


App.ConfigSerializer = DS.RESTSerializer.extend({
  normalize : function(modelclass, resourceHash, prop){
    resourceHash.id = 'siteName_'+ resourceHash.siteName + "_configName_" + resourceHash.configName;
    return resourceHash;
  }
});
