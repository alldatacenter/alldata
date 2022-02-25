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

import Ember from 'ember';
import FileOperationMixin from '../mixins/file-operation';

export default Ember.Component.extend(FileOperationMixin, {
  fileOperationService: Ember.inject.service('file-operation'),
  classNames: ['directory-viewer'],
  startPath: '/',
  treeData: Ember.A(),
  currentPath: Ember.computed.oneWay('startPath'),
  currentQueryParam: Ember.computed('currentPath', function() {
    return Ember.$.param({path: this.get('currentPath')});
  }),

  startFetch: Ember.on('didInitAttrs', function() {
    this.fetchData();
  }),

  fetchData: function() {
    this.get('fileOperationService').listPath(this.get('currentQueryParam')).then(
      (response) => {
        this.modifyTreeViewData(response);
      }, (error) => {
        this.sendAction('errorAction', error);
      }
    )
  },

  modifyTreeViewData: function(response) {

    let paths = response.map((entry) => {
      return {
        path: entry.path,
        pathSegment: this.getNameForPath(entry.path),
        text: this.getNameForPath(entry.path),
        nodes: Ember.A()
      };
    });

    var currentPath = this.get('currentPath');
    var newTreeData = Ember.copy(this.get('treeData'), true);
    if(currentPath === '/') {
      newTreeData = paths;
    } else {
      this.insertPathToTreeData(newTreeData, paths, currentPath.substring(1));
    }

    this.set('treeData', newTreeData);
    this.send('refreshTreeView');
  },

  insertPathToTreeData(treeData, paths, pathSegment) {
    let firstPathSegment;
    if (pathSegment.indexOf('/') !== -1) {
      firstPathSegment = pathSegment.substring(0, pathSegment.indexOf('/'));
    } else {
      firstPathSegment = pathSegment;
    }

    if(treeData.length === 0) {
      treeData.pushObjects(paths);
    } else {
      treeData.forEach((entry) => {
        entry.state = {};
        if (entry.pathSegment === firstPathSegment) {
          entry.state.expanded = true;
          if(entry.nodes.length === 0) {
            entry.nodes.pushObjects(paths);
          } else {
            this.insertPathToTreeData(entry.nodes, paths, pathSegment.substring(pathSegment.indexOf('/') + 1));
          }
        } else {
          this.collapseAll(entry);
        }
      });
    }
  },

  collapseAll: function(node) {
    if (Ember.isNone(node.state)) {
      node.state = {};
    }
    node.state.expanded = false;
    node.nodes.forEach((entry) => {
      this.collapseAll(entry);
    });

  },

  getNameForPath: function(path) {
    return path.substring(path.lastIndexOf("/") + 1);
  },

  collapseAllExceptPath: function(pathSegment) {
    let collapseAll = function(nodes, pathSegment) {
      var firstPathSegment;
      if (pathSegment.indexOf('/') !== -1) {
        firstPathSegment = pathSegment.substring(0, pathSegment.indexOf('/'));
      } else {
        firstPathSegment = pathSegment;
      }

      nodes.forEach((entry) => {
        if (Ember.isNone(entry.state)) {
          entry.state = {};
        }
        if(firstPathSegment !== entry.pathSegment) {
          entry.state.expanded = false;
        } else {
          entry.state.expanded = true;
          collapseAll(entry.nodes, pathSegment.substring(pathSegment.indexOf('/') + 1));
        }
      });
    };
    var newTreeData = this.get('treeData');
    collapseAll(newTreeData, pathSegment);
    this.set('treeData', newTreeData);
    this.send('refreshTreeView');
  },

  actions: {
    refreshTreeView() {
      Ember.run.later(() => {
        this.$().treeview({
          data: this.get('treeData'),
          expandIcon: "fa fa-folder",
          collapseIcon: "fa fa-folder-open",
          emptyIcon: "fa fa-folder-open-o",
          showBorder: false,
          onNodeSelected: (event, data) => {
            this.set('currentPath', data.path);
            this.sendAction('pathSelectAction', data.path);
          },
          onNodeExpanded: (event, data) => {
            this.set('currentPath', data.path);
            if (!Ember.isNone(data.nodes) && data.nodes.length === 0) {
              var node = this.$().treeview('getNode', data.nodeId);
              node.icon = "fa fa-refresh fa-spin";
              this.fetchData();
            } else {
              this.collapseAllExceptPath(data.path.substring(1));
            }
          }
        });
      });
    }
  }
});
