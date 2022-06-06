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

App.Router.map(function() {
  this.resource('queues', { path: '/queues' }, function() {
    this.resource('queue', { path: '/:queue_id' });
    this.resource('trace', { path: '/log' });
  });
  this.resource('capsched', {path: '/capacity-scheduler'}, function() {
    this.route('scheduler', {path: '/scheduler'});
    this.route('advanced', {path: '/advanced'});
    this.route('trace', {path: '/log'});
    this.route('queuesconf', {path: '/queues'}, function() {
      this.route('editqueue', {path: '/:queue_id'});
    });
  });
  this.route('refuse');
});

var RANGER_SITE = 'ranger-yarn-plugin-properties';
var RANGER_YARN_ENABLED = 'ranger-yarn-plugin-enabled';

var YARN_SITE = 'yarn-site';
var PREEMPTION_YARN_ENABLE = 'yarn.resourcemanager.scheduler.monitor.enable';
var NODE_LABELS_YARN_ENABLED = 'yarn.node-labels.enabled';
/**
 * The queues route.
 *
 * /queues
 */
App.QueuesRoute = Ember.Route.extend({
  actions:{
    rollbackProp:function (prop, item) {
      var attributes = item.changedAttributes();
      if (attributes.hasOwnProperty(prop)) {
        item.set(prop,attributes[prop][0]);
      }
    }
  },
  beforeModel:function (transition) {
    var controller = this.container.lookup('controller:loading') || this.generateController('loading');
    controller.set('model', {message:'cluster check'});
    return this.get('store').checkCluster().catch(Em.run.bind(this,'loadingError',transition));
  },
  model: function() {
    var store = this.get('store'),
        controller = this.controllerFor('queues'),
        loadingController = this.container.lookup('controller:loading');
    var _this = this;
    return new Ember.RSVP.Promise(function (resolve,reject) {
      loadingController.set('model', {message:'access check'});
      store.checkOperator().then(function   (isOperator) {
        controller.set('isOperator', isOperator);

        loadingController.set('model', {message:'loading node labels'});
        return store.get('nodeLabels');
      }).then(function(){
        return store.findQuery( 'config', {siteName : RANGER_SITE, configName : RANGER_YARN_ENABLED}).then(function(){
          return store.find( 'config', "siteName_" + RANGER_SITE + "_configName_" + RANGER_YARN_ENABLED)
              .then(function(data){
                _this.controllerFor('configs').set('isRangerEnabledForYarn', data.get('configValue'));
              });
        })
      }).then(function () {
        loadingController.set('model', {message:'loading queues'});
        return store.find('queue');
      }).then(function (queues) {
        resolve(queues);
      }).catch(function (e) {
        reject(e);
      });
    }, 'App: QueuesRoute#model');
  },
  setupController:function (c,model) {
    c.set('model',model);
    this.store.find('scheduler','scheduler').then(function (s) {
      c.set('scheduler',s);
    });
  },
  loadingError: function (transition, error) {
    var refuseController = this.container.lookup('controller:refuse') || this.generateController('refuse'),
        message = error.responseJSON || {'message':'Something went wrong.'};

    transition.abort();

    refuseController.set('model', message);

    this.transitionTo('refuse');
  }
});

/**
 * The queue route.
 *
 * /queues/:id
 */
App.QueueRoute = Ember.Route.extend({

  model: function(params,tr) {
    var queues = this.modelFor('queues') || this.store.find('queue'),
        filterQueues = function (queues) {
          return queues.findBy('id',params.queue_id);
        };

    return (queues instanceof DS.PromiseArray)?queues.then(filterQueues):filterQueues(queues);
  },
  afterModel:function (model) {
    if (!model) {
      this.transitionTo('queues');
    }
  },
  actions: {
    willTransition: function (tr) {
      if (this.get('controller.isRenaming')) {
        tr.abort();
      }
    }
  }
});

/**
 * Routes index to /queues path
 *
 */
App.IndexRoute = Ember.Route.extend({
  redirect: function() {
    this.transitionTo('queues');
  }
});

/**
 * Page for trace output.
 *
 * /queues/log
 */
App.TraceRoute = Ember.Route.extend({
  model: function() {
    return this.controllerFor('queues').get('alertMessage');
  }
});

/**
 * Connection rejection page.
 *
 * /refuse
 */
App.RefuseRoute = Ember.Route.extend({
  setupController:function (controller,model) {
    if (Em.isEmpty(controller.get('model'))) {
      this.transitionTo('queues');
    }
  }
});

/**
 * Loading spinner page.
 *
 */
App.LoadingRoute = Ember.Route.extend({
  setupController:function(controller) {
    if (Em.isEmpty(controller.get('model'))) {
      this._super();
    }
  }
});

/**
 * Error page.
 *
 */
App.ErrorRoute = Ember.Route.extend({
  setupController:function (controller,model) {
    //TODO Handle Ember Error!
    var response;
    try {
      response = JSON.parse(model.responseText);
    } catch (e) {
      throw model;
      response = model;
    }
    model.trace = model.stack;
    controller.set('model',response);
  }
});

App.CapschedRoute = Ember.Route.extend({
  actions: {
    saveCapSchedConfigs: function(saveMode, forceRefresh) {
      var store = this.get('store'),
      capschedCtrl = this.controllerFor("capsched"),
      eventBus = this.get('eventBus'),
      that = this;

      eventBus.publish('beforeSavingConfigs');

      if (forceRefresh) {
        return this.forceRefreshOrRestartCapSched(saveMode);
      }

      var collectedLabels = capschedCtrl.get('queues').reduce(function (prev,q) {
        return prev.pushObjects(q.get('labels.content'));
      },[]);

      var scheduler = capschedCtrl.get('content').save(),
          queues = capschedCtrl.get('queues').save(),
          labels = DS.ManyArray.create({content: collectedLabels}).save(),
          opt = '';

      if (saveMode == 'restart') {
        opt = 'saveAndRestart';
      } else if (saveMode == 'refresh') {
        opt = 'saveAndRefresh';
      }

      capschedCtrl.startSpinner(saveMode);
      Em.RSVP.Promise.all([labels, queues, scheduler]).then(
        Em.run.bind(that,'saveConfigsSuccess'),
        Em.run.bind(that,'saveConfigsError', 'save')
      ).then(function () {
        eventBus.publish('afterConfigsSaved');
        if (opt) {
          return store.relaunchCapSched(opt);
        }
      }).then(function(){
        if (opt) {
          eventBus.publish('afterConfigsSaved', false);
        }
        return store.getRmSchedulerConfigInfo();
      }).catch(
        Em.run.bind(this,'saveConfigsError', opt)
      ).finally(function(){
        capschedCtrl.stopSpinner();
        store.setLastSavedConfigXML();
      });
    },
    viewConfigXmlDiff: function() {
      var store = this.get('store'),
        controller = this.controllerFor("capsched"),
        lastSavedXML = store.get('lastSavedConfigXML'),
        currentXmlConfigs = store.buildConfig('xml'),
        diffConfigs = {baseXML: lastSavedXML, newXML: currentXmlConfigs};

      controller.viewConfigXmlDiff(diffConfigs);
    },
    viewCapSchedConfigXml: function() {
      var store = this.get('store'),
        controller = this.controllerFor("capsched"),
        viewXmlConfigs = store.buildConfig('xml');

      controller.viewCapSchedXml({xmlConfig: viewXmlConfigs});
    }
  },
  beforeModel: function(transition) {
    var controller = this.container.lookup('controller:loading') || this.generateController('loading');
    controller.set('model', {
      message: 'cluster check'
    });
    return this.get('store').checkCluster().catch(Em.run.bind(this, 'loadingError', transition));
  },
  model: function() {
    var store = this.get('store'),
      _this = this,
      controller = this.controllerFor("capsched"),
      loadingController = this.container.lookup('controller:loading');

    return new Ember.RSVP.Promise(function(resolve, reject) {
      loadingController.set('model', {
        message: 'access check'
      });
      store.checkOperator().then(function(isOperator) {
        controller.set('isOperator', isOperator);
        loadingController.set('model', {
          message: 'loading node labels'
        });
        return store.get('nodeLabels');
      }).then(function() {
        var rangerEnabled = store.findQuery('config', {
          siteName: RANGER_SITE,
          configName: RANGER_YARN_ENABLED
        });
        var nodeLabelsEnabled = store.findQuery('config', {
          siteName: YARN_SITE,
          configName: NODE_LABELS_YARN_ENABLED
        });
        var preemptionEnabled = store.findQuery('config', {
          siteName: YARN_SITE,
          configName: PREEMPTION_YARN_ENABLE
        });
        Ember.RSVP.Promise.all([rangerEnabled, nodeLabelsEnabled, preemptionEnabled]).then(function() {
          var rangerConfig = store.getById('config', "siteName_" + RANGER_SITE + "_configName_" + RANGER_YARN_ENABLED),
            nodeLabelConfig = store.getById('config', "siteName_" + YARN_SITE + "_configName_" + NODE_LABELS_YARN_ENABLED),
            preemptionConfig = store.getById('config', "siteName_" + YARN_SITE + "_configName_" + PREEMPTION_YARN_ENABLE),
            rangerValue = rangerConfig.get('configValue'),
            nodeLabelValue = nodeLabelConfig.get('configValue'),
            preemptionValue = preemptionConfig.get('configValue');

          controller.set('isRangerEnabledForYarn', rangerValue);
          store.set('isNodeLabelsEnabledByRM', nodeLabelValue === "true");
          controller.set('isPreemptionEnabledByYarn', preemptionValue);
        });
      }).then(function() {
        loadingController.set('model', {
          message: 'loading queues'
        });
        return store.find('queue');
      }).then(function(queues) {
        controller.set('queues', queues);
        var allQLabels = store.all('label');
        controller.set('allQueueLabels', allQLabels);
        loadingController.set('model', {
          message: 'loading rm info'
        });
        return store.getRmSchedulerConfigInfo();
      }).then(function() {
        return store.find('scheduler', 'scheduler');
      }).then(function(scheduler){
        store.setLastSavedConfigXML();
        resolve(scheduler);
      }).catch(function(e) {
        reject(e);
      });
    }, 'App: CapschedRoute#model');
  },
  loadingError: function (transition, error) {
    var refuseController = this.container.lookup('controller:refuse') || this.generateController('refuse'),
        message = error.responseJSON || {'message': 'Something went wrong.'};

    transition.abort();
    refuseController.set('model', message);
    this.transitionTo('refuse');
  },
  saveConfigsSuccess: function() {
    this.set('store.deletedQueues', []);
  },
  saveConfigsError: function(operation, err) {
    this.controllerFor("capsched").stopSpinner();
    var response = {};
    if (err && err.responseJSON) {
      response = err.responseJSON;
    }
    response.simpleMessage = operation.capitalize() + ' failed!';
    this.controllerFor("capsched").set('alertMessage', response);
    throw Error("Configs Error: ", err);
  },
  forceRefreshOrRestartCapSched: function(saveMode) {
    var opt = '',
      that = this,
      store = this.get('store'),
      capschedCtrl = this.controllerFor("capsched");

    if (saveMode == 'restart') {
      opt = 'saveAndRestart';
    } else {
      opt = 'saveAndRefresh';
    }

    capschedCtrl.startSpinner(saveMode);
    store.relaunchCapSched(opt).then(function() {
      that.get('eventBus').publish('afterConfigsSaved', false);
    }).catch(
      Em.run.bind(that, 'saveConfigsError', opt)
    ).finally(function() {
      capschedCtrl.stopSpinner();
      store.setLastSavedConfigXML();
    });
  }
});

App.CapschedIndexRoute = Ember.Route.extend({
  redirect: function() {
    this.transitionTo('capsched.scheduler');
  }
});

App.CapschedSchedulerRoute = Ember.Route.extend({
  actions: {
    didTransition: function() {
      this.controllerFor('capsched').set('selectedQueue', null);
    }
  }
});

App.CapschedQueuesconfIndexRoute = Ember.Route.extend({
  beforeModel: function(transition) {
    var rootQ = this.store.getById('queue', 'root');
    this.transitionTo('capsched.queuesconf.editqueue', rootQ);
  }
});

App.CapschedQueuesconfEditqueueRoute = Ember.Route.extend({
  actions: {
    willTransition: function(transition) {
      if (this.controllerFor('capsched.queuesconf').get('isCreaingOrRenamingQueue')) {
        transition.abort();
      }
    },
    didTransition: function() {
      this.controllerFor('capsched').set('selectedQueue', this.controller.get('model'));
    }
  },
  model: function(params, transition) {
    var queue = this.store.getById('queue', params.queue_id);
    if (queue) {
      return queue;
    }
    this.transitionTo('capsched.queuesconf.editqueue', this.store.getById('queue', 'root'));
  },
  setupController: function(controller, model) {
    controller.set('model', model);
    this.controllerFor('capsched.queuesconf').set('selectedQueue', model);
  }
});

App.CapschedAdvancedRoute = Ember.Route.extend({
  actions: {
    didTransition: function() {
      this.controllerFor('capsched').set('selectedQueue', null);
    }
  }
});

App.CapschedTraceRoute = Ember.Route.extend({
  model: function() {
    return this.controllerFor('capsched').get('alertMessage');
  }
});
