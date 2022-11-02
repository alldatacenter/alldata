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

var FileSaver = Ember.Object.extend({
  save: function(fileContents, mimeType, filename) {
    window.saveAs(new Blob([fileContents], {type: mimeType}), filename);
  }
});

Ember.Application.initializer({
  name: 'file-saver',

  initialize: function(container, application) {
    container.register('file-saver:main', FileSaver);
    container.injection('controller', 'fileSaver', 'file-saver:main');
  }
});

var EventBus = Ember.Object.extend(Ember.Evented, {
  publish: function() {
    return this.trigger.apply(this, arguments);
  },
  subscribe: function() {
    return this.on.apply(this, arguments);
  },
  unsubscribe: function() {
    return this.off.apply(this, arguments);
  }
});

Ember.Application.initializer({
  name: 'eventBus',
  initialize: function(container, application) {
    container.register('eventBus:main', EventBus);
    container.injection('route', 'eventBus', 'eventBus:main');
    container.injection('controller', 'eventBus', 'eventBus:main');
    container.injection('component', 'eventBus', 'eventBus:main');
  }
});

module.exports = Em.Application.create({
  Resolver: Ember.DefaultResolver.extend({
    resolveTemplate: function(parsedName) {
      var resolvedTemplate = this._super(parsedName);
      var templateName = 'templates/' + parsedName.fullNameWithoutType.replace(/\./g, '/');
      if (resolvedTemplate) {
        return resolvedTemplate;
      } else {
        return Ember.TEMPLATES[templateName];
      }
    }
  })
});
