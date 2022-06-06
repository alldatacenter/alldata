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
var lazyloading = require('utils/lazy_loading');
require('views/wizard/step3/hostWarningPopupBody_view');
var view;

function getView() {
  return App.WizardStep3HostWarningPopupBody.create({
    didInsertElement: Em.K,
    $: function() {
      return Em.Object.create({
        toggle: Em.K
      })
    }
  });
}

describe('App.WizardStep3HostWarningPopupBody', function() {

  beforeEach(function() {
    view = getView();
  });

  App.TestAliases.testAsComputedAlias(getView(), 'warningsByHost', 'bodyController.warningsByHost', 'array');

  App.TestAliases.testAsComputedAlias(getView(), 'warnings', 'bodyController.warnings', 'array');

  describe('#onToggleBlock', function() {
    it('should toggle', function() {
      var context = Em.Object.create({isCollapsed: false});
      view.onToggleBlock({context: context});
      expect(context.get('isCollapsed')).to.equal(true);
      view.onToggleBlock({context: context});
      expect(context.get('isCollapsed')).to.equal(false);
    });
  });

  describe('#showHostsPopup', function() {
    it('should call App.ModalPopup.show', function() {
      view.showHostsPopup({context: []});
      expect(App.ModalPopup.show.calledOnce).to.equal(true);
    });
  });

  describe('#categoryWarnings', function() {
    it('should return empty array', function() {
      var warningsByHost = null;
      view.reopen({warningsByHost: warningsByHost});
      expect(view.get('categoryWarnings')).to.eql([]);
    });
    it('should return filtered warnings', function() {
      var warningsByHost = [
        {name: 'c', warnings: [{}, {}, {}]},
        {name: 'd', warnings: [{}]}
      ];
      view.reopen({warningsByHost: warningsByHost, category: 'c'});
      expect(view.get('categoryWarnings.length')).to.equal(3);
    });
  });

  describe('#warningHostsNamesCount', function() {
    it('should parse warnings', function() {
      view.set('bodyController', Em.Object.create({
        repoCategoryWarnings: [
          {hostsNames: ['h1', 'h4']}
        ],
        thpCategoryWarnings: [
          {hostsNames: ['h2', 'h3']}
        ],
        jdkCategoryWarnings: [
          {hostsNames: ['h3', 'h5']}
        ],
        hostCheckWarnings: [
          {hostsNames: ['h1', 'h2']}
        ],
        diskCategoryWarnings: [
          {hostsNames: ['h2', 'h5']}
        ],
        warningsByHost: [
          {},
          { name: 'h1', warnings: [{}, {}, {}] },
          { name: 'h2', warnings: [{}, {}, {}] },
          { name: 'h3', warnings: [] }
        ]
      }));
      expect(view.warningHostsNamesCount()).to.equal(5);
    });
  });

  describe('#hostSelectView', function() {

    var v;

    beforeEach(function() {
      v = view.get('hostSelectView').create();
    });

    describe('#click', function() {

      beforeEach(function () {
        sinon.spy(lazyloading, 'run');
      });

      afterEach(function () {
        lazyloading.run.restore();
      });

      Em.A([
          {
            isLoaded: false,
            isLazyLoading: true,
            e: true
          },
          {
            isLoaded: true,
            isLazyLoading: true,
            e: false
          },
          {
            isLoaded: false,
            isLazyLoading: false,
            e: false
          },
          {
            isLoaded: true,
            isLazyLoading: false,
            e: false
          }
        ]).forEach(function (test) {
          it('isLoaded: ' + test.isLoaded.toString() + ', isLazyLoading: ' + test.isLazyLoading.toString(), function () {
            v.reopen({
              isLoaded: test.isLoaded,
              isLazyLoading: test.isLazyLoading
            });
            v.click();
            if (test.e) {
              expect(lazyloading.run.calledOnce).to.equal(true);
            }
            else {
              expect(lazyloading.run.called).to.equal(false);
            }
          });
        });
    });

  });

  describe('#contentInDetails', function() {
    var content = [
      {category: 'firewall', warnings: [{name: 'n1'}, {name: 'n2'}, {name: 'n3'}]},
      {category: 'fileFolders', warnings: [{name: 'n4'}, {name: 'n5'}, {name: 'n6'}]},
      {category: 'reverseLookup', warnings: [{name: 'n19', hosts: ["h1"], hostsLong: ["h1"]}]},
      {
        category: 'process',
        warnings: [
          {name: 'n7', hosts:['h1', 'h2'], hostsLong:['h1', 'h2'], user: 'u1', pid: 'pid1'},
          {name: 'n8', hosts:['h2'], hostsLong:['h2'], user: 'u2', pid: 'pid2'},
          {name: 'n9', hosts:['h3'], hostsLong:['h3'], user: 'u1', pid: 'pid3'}
        ]
      },
      {category: 'package', warnings: [{name: 'n10'}, {name: 'n11'}, {name: 'n12'}]},
      {category: 'service', warnings: [{name: 'n13'}, {name: 'n14'}, {name: 'n15'}]},
      {category: 'user', warnings: [{name: 'n16'}, {name: 'n17'}, {name: 'n18'}]},
      {category: 'jdk', warnings: []},
      {category: 'disk', warnings: []},
      {category: 'repositories', warnings: []},
      {category: 'hostNameResolution', warnings: []},
      {category: 'thp', warnings: []}
    ];
    beforeEach(function() {
      view.reopen({content: content, warningsByHost: [], hostNamesWithWarnings: ['c', 'd']});
      this.newContent = view.get('contentInDetails');
    });
    it('should map hosts', function() {
      expect(this.newContent.contains('c d')).to.equal(true);
    });
    it('should map firewall warnings', function() {
      expect(this.newContent.contains('n1<br>n2<br>n3')).to.equal(true);
    });
    it('should map fileFolders warnings', function() {
      expect(this.newContent.contains('n4 n5 n6')).to.equal(true);
    });
    it('should map process warnings', function() {
      expect(this.newContent.contains('(h1,u1,pid1)')).to.equal(true);
      expect(this.newContent.contains('(h2,u1,pid1)')).to.equal(true);
      expect(this.newContent.contains('(h2,u2,pid2)')).to.equal(true);
      expect(this.newContent.contains('(h3,u1,pid3)')).to.equal(true);
    });
    it('should map package warnings', function() {
      expect(this.newContent.contains('n10 n11 n12')).to.equal(true);
    });
    it('should map service warnings', function() {
      expect(this.newContent.contains('n13 n14 n15')).to.equal(true);
    });
    it('should map user warnings', function() {
      expect(this.newContent.contains('n16 n17 n18')).to.equal(true);
    });
    it('should map reverse lookup warnings', function() {
      expect(this.newContent.contains('h1')).to.equal(true);
    });
  });

  describe('#content', function () {

    beforeEach(function () {
      view.set('bodyController', Em.Object.create({
        hostCheckWarnings: [
          {
            hosts: ['h0', 'h1', 'h2', 'h3', 'h4', 'h5', 'h5', 'h7', 'h8', 'h9', 'h10']
          }
        ],
        repoCategoryWarnings: [
          {
            hosts: ['h11', 'h12']
          }
        ],
        diskCategoryWarnings: [
          {
            hosts: ['h13']
          }
        ],
        jdkCategoryWarnings: [
          {
            hosts: ['h14']
          }
        ],
        thpCategoryWarnings: [
          {
            hosts: ['h15']
          }
        ]
      }));
      view.reopen({
        categoryWarnings: [
          {
            category: 'firewall',
            hosts: ['h16']
          },
          {
            category: 'firewall',
            hosts: ['h17']
          },
          {
            category: 'processes',
            hosts: ['h18']
          },
          {
            category: 'packages',
            hosts: ['h19']
          },
          {
            category: 'fileFolders',
            hosts: ['h20']
          },
          {
            category: 'services',
            hosts: ['h21']
          },
          {
            category: 'users',
            hosts: ['h22']
          },
          {
            category: 'misc',
            hosts: ['h23']
          },
          {
            category: 'alternatives',
            hosts: ['h24']
          },
          {
            category: 'reverseLookup',
            hosts: ['h25']
          },
          {
            category: 'reverseLookup',
            hosts: ['h26']
          },
          {
            category: 'reverseLookup',
            hosts: ['h27']
          },
          {
            category: 'reverseLookup',
            hosts: ['h28']
          },
          {
            category: 'reverseLookup',
            hosts: ['h29']
          },
          {
            category: 'reverseLookup',
            hosts: ['h30']
          },
          {
            category: 'reverseLookup',
            hosts: ['h31']
          },
          {
            category: 'reverseLookup',
            hosts: ['h32']
          },
          {
            category: 'reverseLookup',
            hosts: ['h33']
          },
          {
            category: 'reverseLookup',
            hosts: ['h34']
          },
          {
            category: 'reverseLookup',
            hosts: ['h35', 'h36']
          }
        ]
      });
      this.content = view.get('content');
    });

    it('isCollapsed', function () {
      expect(this.content.mapProperty('isCollapsed').uniq()).to.eql([true]);
    });

    it('hostNameResolution', function () {
      expect(this.content.findProperty('category', 'hostNameResolution').get('warnings')[0].hostsList).
        to.equal('h0<br>h1<br>h2<br>h3<br>h4<br>h5<br>h5<br>h7<br>h8<br>h9<br> ' + Em.I18n.t('installer.step3.hostWarningsPopup.moreHosts').format(1));
    });

    it('repositories', function () {
      expect(this.content.findProperty('category', 'repositories').get('warnings')[0].hostsList).to.equal('h11<br>h12');
    });

    it('disk', function () {
      expect(this.content.findProperty('category', 'disk').get('warnings')[0].hostsList).to.equal('h13');
    });

    it('jdk', function () {
      expect(this.content.findProperty('category', 'jdk').get('warnings')[0].hostsList).to.equal('h14');
    });

    it('thp', function () {
      expect(this.content.findProperty('category', 'thp').get('warnings')[0].hostsList).to.equal('h15');
    });

    it('firewall', function () {
      expect(this.content.findProperty('category', 'firewall').get('warnings').mapProperty('hostsList')).to.eql(['h16', 'h17']);
    });

    it('process', function () {
      expect(this.content.findProperty('category', 'process').get('warnings')[0].hostsList).to.equal('h18');});

    it('package', function () {
      expect(this.content.findProperty('category', 'package').get('warnings')[0].hostsList).to.equal('h19');
    });

    it('fileFolders', function () {
      expect(this.content.findProperty('category', 'fileFolders').get('warnings')[0].hostsList).to.equal('h20');
    });

    it('service', function () {
      expect(this.content.findProperty('category', 'service').get('warnings')[0].hostsList).to.equal('h21');
    });

    it('user', function () {
      expect(this.content.findProperty('category', 'user').get('warnings')[0].hostsList).to.equal('h22');
    });

    it('misc', function () {
      expect(this.content.findProperty('category', 'misc').get('warnings')[0].hostsList).to.equal('h23');
    });

    it('alternatives', function () {
      expect(this.content.findProperty('category', 'alternatives').get('warnings')[0].hostsList).to.equal('h24');
    });

    it('reverseLookup', function () {
      expect(this.content.findProperty('category', 'reverseLookup').get('warnings').mapProperty('hostsList')).to.eql([
        'h25', 'h26', 'h27', 'h28', 'h29', 'h30', 'h31', 'h32', 'h33', 'h34', 'h35<br>h36'
      ]);
    });

  });

});