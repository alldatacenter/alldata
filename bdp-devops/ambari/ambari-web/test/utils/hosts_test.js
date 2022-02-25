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
var hostsUtils = require('utils/hosts');
var validator = require('utils/validator');
var testHelpers = require('test/helpers');

describe('hosts utils', function () {

  describe('#launchHostsSelectionDialog', function () {

    var popup,
      mock = {
        callback: Em.K
      };

    describe('popup header and message', function () {

      var cases = [
        {
          popupDescription: {},
          header: Em.I18n.t('hosts.selectHostsDialog.title'),
          dialogMessage: Em.I18n.t('hosts.selectHostsDialog.message'),
          title: 'default header and message'
        },
        {
          popupDescription: {
            dialogMessage: 'Message 0'
          },
          header: Em.I18n.t('hosts.selectHostsDialog.title'),
          dialogMessage: 'Message 0',
          title: 'default header, custom message'
        },
        {
          popupDescription: {
            header: 'Header 0'
          },
          header: 'Header 0',
          dialogMessage: Em.I18n.t('hosts.selectHostsDialog.message'),
          title: 'custom header, default message'
        },
        {
          popupDescription: {
            header: 'Header 1',
            dialogMessage: 'Message 1'
          },
          header: 'Header 1',
          dialogMessage: 'Message 1',
          title: 'custom header and message'
        }
      ];

      cases.forEach(function (item) {

        describe(item.title, function () {

          beforeEach(function () {
            hostsUtils.launchHostsSelectionDialog(null, null, null, null, null, item.popupDescription);
            popup = Em.Object.create(App.ModalPopup.show.firstCall.args[0]);
          });

          it('header', function () {
            expect(popup.get('header')).to.equal(item.header);
          });

          it('message', function () {
            expect(popup.get('dialogMessage')).to.equal(item.dialogMessage);
          });

        });

      });

    });

    describe('#onPrimary', function () {

      var cases = [
        {
          selectAtleastOneHost: true,
          availableHosts: [],
          warningMessage: Em.I18n.t('hosts.selectHostsDialog.message.warning'),
          callbackCallCount: 0,
          hideCallCount: 0,
          title: 'no hosts available'
        },
        {
          selectAtleastOneHost: false,
          availableHosts: [],
          warningMessage: null,
          callbackCallCount: 1,
          arrayOfSelectedHosts: [],
          hideCallCount: 1,
          title: 'no hosts available, no selection requirement'
        },
        {
          selectAtleastOneHost: true,
          availableHosts: [
            {},
            {
              selected: 1
            },
            {
              selected: 'true'
            }
          ],
          warningMessage: Em.I18n.t('hosts.selectHostsDialog.message.warning'),
          callbackCallCount: 0,
          hideCallCount: 0,
          title: 'no selection state info'
        },
        {
          selectAtleastOneHost: false,
          availableHosts: [
            {},
            {
              selected: 1
            },
            {
              selected: 'true'
            }
          ],
          warningMessage: null,
          callbackCallCount: 1,
          arrayOfSelectedHosts: [],
          hideCallCount: 1,
          title: 'no selection state info, no selection requirement'
        },
        {
          selectAtleastOneHost: true,
          availableHosts: [
            {
              selected: false
            },
            {
              selected: false
            },
            {},
            {
              selected: 1
            },
            {
              selected: 'true'
            }
          ],
          warningMessage: Em.I18n.t('hosts.selectHostsDialog.message.warning'),
          callbackCallCount: 0,
          hideCallCount: 0,
          title: 'no hosts selected'
        },
        {
          selectAtleastOneHost: false,
          availableHosts: [
            {
              selected: false
            },
            {
              selected: false
            },
            {},
            {
              selected: 1
            },
            {
              selected: 'true'
            }
          ],
          warningMessage: null,
          callbackCallCount: 1,
          arrayOfSelectedHosts: [],
          hideCallCount: 1,
          title: 'no hosts selected, no selection requirement'
        },
        {
          selectAtleastOneHost: true,
          availableHosts: [
            {
              selected: true
            },
            {
              selected: false
            },
            {},
            {
              selected: 1
            },
            {
              selected: 'true'
            }
          ],
          warningMessage: null,
          callbackCallCount: 1,
          arrayOfSelectedHosts: [undefined],
          hideCallCount: 1,
          title: '1 host selected, no host info'
        },
        {
          selectAtleastOneHost: false,
          availableHosts: [
            {
              selected: true
            },
            {
              selected: false
            },
            {},
            {
              selected: 1
            },
            {
              selected: 'true'
            }
          ],
          warningMessage: null,
          callbackCallCount: 1,
          arrayOfSelectedHosts: [undefined],
          hideCallCount: 1,
          title: '1 host selected, no host info, no selection requirement'
        },
        {
          selectAtleastOneHost: true,
          availableHosts: [
            {
              selected: true,
              host: {}
            },
            {
              selected: false
            },
            {},
            {
              selected: 1
            },
            {
              selected: 'true'
            }
          ],
          warningMessage: null,
          callbackCallCount: 1,
          arrayOfSelectedHosts: [undefined],
          hideCallCount: 1,
          title: '1 host selected, no host id'
        },
        {
          selectAtleastOneHost: false,
          availableHosts: [
            {
              selected: true,
              host: {}
            },
            {
              selected: false
            },
            {},
            {
              selected: 1
            },
            {
              selected: 'true'
            }
          ],
          warningMessage: null,
          callbackCallCount: 1,
          arrayOfSelectedHosts: [undefined],
          hideCallCount: 1,
          title: '1 host selected, no host id, no selection requirement'
        },
        {
          selectAtleastOneHost: true,
          availableHosts: [
            {
              selected: true,
              host: {
                id: 'h0'
              }
            },
            {
              selected: false,
              host: {
                id: 'h1'
              }
            },
            {
              host: {
                id: 'h2'
              }
            },
            {
              selected: 1,
              host: {
                id: 'h3'
              }
            },
            {
              selected: 'true',
              host: {
                id: 'h4'
              }
            }
          ],
          warningMessage: null,
          callbackCallCount: 1,
          arrayOfSelectedHosts: ['h0'],
          hideCallCount: 1,
          title: '1 host selected, host id available'
        },
        {
          selectAtleastOneHost: false,
          availableHosts: [
            {
              selected: true,
              host: {
                id: 'h5'
              }
            },
            {
              selected: false,
              host: {
                id: 'h6'
              }
            },
            {
              host: {
                id: 'h7'
              }
            },
            {
              selected: 1,
              host: {
                id: 'h8'
              }
            },
            {
              selected: 'true',
              host: {
                id: 'h9'
              }
            }
          ],
          warningMessage: null,
          callbackCallCount: 1,
          arrayOfSelectedHosts: ['h5'],
          hideCallCount: 1,
          title: '1 host selected, host id available, no selection requirement'
        },
        {
          selectAtleastOneHost: true,
          availableHosts: [
            {
              selected: true
            },
            {
              selected: true
            },
            {
              selected: false
            },
            {},
            {
              selected: 1
            },
            {
              selected: 'true'
            }
          ],
          warningMessage: null,
          callbackCallCount: 1,
          arrayOfSelectedHosts: [undefined, undefined],
          hideCallCount: 1,
          title: 'more than 1 host selected, no host info'
        },
        {
          selectAtleastOneHost: false,
          availableHosts: [
            {
              selected: true
            },
            {
              selected: true
            },
            {
              selected: false
            },
            {},
            {
              selected: 1
            },
            {
              selected: 'true'
            }
          ],
          warningMessage: null,
          callbackCallCount: 1,
          arrayOfSelectedHosts: [undefined, undefined],
          hideCallCount: 1,
          title: 'more than 1 host selected, no host info, no selection requirement'
        },
        {
          selectAtleastOneHost: true,
          availableHosts: [
            {
              selected: true,
              host: {}
            },
            {
              selected: true,
              host: {}
            },
            {
              selected: false
            },
            {},
            {
              selected: 1
            },
            {
              selected: 'true'
            }
          ],
          warningMessage: null,
          callbackCallCount: 1,
          arrayOfSelectedHosts: [undefined, undefined],
          hideCallCount: 1,
          title: 'more than 1 host selected, no host id'
        },
        {
          selectAtleastOneHost: false,
          availableHosts: [
            {
              selected: true,
              host: {}
            },
            {
              selected: true,
              host: {}
            },
            {
              selected: false
            },
            {},
            {
              selected: 1
            },
            {
              selected: 'true'
            }
          ],
          warningMessage: null,
          callbackCallCount: 1,
          arrayOfSelectedHosts: [undefined, undefined],
          hideCallCount: 1,
          title: 'more than 1 host selected, no host id, no selection requirement'
        },
        {
          selectAtleastOneHost: true,
          availableHosts: [
            {
              selected: true,
              host: {
                id: 'h10'
              }
            },
            {
              selected: true,
              host: {
                id: 'h11'
              }
            },
            {
              selected: false,
              host: {
                id: 'h12'
              }
            },
            {
              host: {
                id: 'h13'
              }
            },
            {
              selected: 1,
              host: {
                id: 'h14'
              }
            },
            {
              selected: 'true',
              host: {
                id: 'h15'
              }
            }
          ],
          warningMessage: null,
          callbackCallCount: 1,
          arrayOfSelectedHosts: ['h10', 'h11'],
          hideCallCount: 1,
          title: 'more than 1 host selected, host ids available'
        },
        {
          selectAtleastOneHost: false,
          availableHosts: [
            {
              selected: true,
              host: {
                id: 'h16'
              }
            },
            {
              selected: true,
              host: {
                id: 'h17'
              }
            },
            {
              selected: false,
              host: {
                id: 'h18'
              }
            },
            {
              host: {
                id: 'h19'
              }
            },
            {
              selected: 1,
              host: {
                id: 'h20'
              }
            },
            {
              selected: 'true',
              host: {
                id: 'h21'
              }
            }
          ],
          warningMessage: null,
          callbackCallCount: 1,
          arrayOfSelectedHosts: ['h16', 'h17'],
          hideCallCount: 1,
          title: 'more than 1 host selected, host ids available, no selection requirement'
        }
      ];

      cases.forEach(function (item) {

        describe(item.title, function () {

          beforeEach(function () {
            sinon.spy(mock, 'callback');
            hostsUtils.launchHostsSelectionDialog(null, null, item.selectAtleastOneHost, null, mock.callback, {});
            popup = Em.Object.create(App.ModalPopup.show.firstCall.args[0], {
              warningMessage: '',
              availableHosts: item.availableHosts,
              hide: Em.K
            });
            sinon.spy(popup, 'hide');
            popup.onPrimary();
          });

          afterEach(function () {
            mock.callback.restore();
            popup.hide.restore();
          });

          it('warning message', function () {
            expect(popup.get('warningMessage')).to.equal(item.warningMessage);
          });

          it('callback', function () {
            expect(mock.callback.callCount).to.equal(item.callbackCallCount);
          });

          if (item.callbackCallCount) {
            it('callback argument', function () {
              expect(mock.callback.firstCall.args).to.eql([item.arrayOfSelectedHosts]);
            });
          }

          it('hide popup', function () {
            expect(popup.hide.callCount).to.equal(item.hideCallCount);
          });

        });

      });

    });

    describe('#disablePrimary', function () {

      var cases = [
        {
          isLoaded: true,
          disablePrimary: false,
          title: 'enable button'
        },
        {
          isLoaded: false,
          disablePrimary: true,
          title: 'disable button'
        }
      ];

      cases.forEach(function (item) {

        it(item.title, function () {
          hostsUtils.launchHostsSelectionDialog(null, null, null, null, null, item.popupDescription);
          popup = Em.Object.create(App.ModalPopup.show.firstCall.args[0], {
            isLoaded: item.isLoaded
          });
          expect(popup.get('disablePrimary')).to.equal(item.disablePrimary);
        });

      });

    });

    describe('#onSecondary', function () {

      beforeEach(function () {
        sinon.spy(mock, 'callback');
        hostsUtils.launchHostsSelectionDialog(null, null, null, null, mock.callback, {});
        popup = Em.Object.create(App.ModalPopup.show.firstCall.args[0], {
          hide: Em.K
        });
        sinon.spy(popup, 'hide');
        popup.onSecondary();
      });

      afterEach(function () {
        mock.callback.restore();
        popup.hide.restore();
      });

      it('callback', function () {
        expect(mock.callback.calledOnce).to.be.true;
      });

      it('callback argument', function () {
        expect(mock.callback.firstCall.args).to.eql([null]);
      });

      it('hide popup', function () {
        expect(popup.hide.calledOnce).to.be.true;
      });

    });

    describe('#bodyClass', function () {

      var view,
        validComponents = [
          {
            componentName: 'c0'
          },
          {
            componentName: 'c1'
          }
        ];

      beforeEach(function () {
        hostsUtils.launchHostsSelectionDialog([
          {
            host: {
              id: 'h0'
            },
            filtered: false
          },
          {
            host: {
              id: 'h1'
            }
          }
        ], null, null, validComponents, null, {});
        popup = Em.Object.create(App.ModalPopup.show.firstCall.args[0]);
        view = popup.get('bodyClass').create({
          parentView: popup
        });
      });

      describe('#filteredContentObs', function () {

        beforeEach(function () {
          sinon.stub(view, 'filteredContentObsOnce', Em.K);
          sinon.stub(view, 'filterHosts', Em.K);
          sinon.stub(Em.run, 'once', function (context, callback) {
            callback.call(context);
          });
        });

        afterEach(function () {
          view.filteredContentObsOnce.restore();
          view.filterHosts.restore();
          Em.run.once.restore();
        });

        it('should run filteredContentObsOnce', function () {
          popup.set('availableHosts', [
            {
              filtered: true
            }
          ]);
          expect(view.filteredContentObsOnce.calledOnce).to.be.true;
        });

      });

      describe('#filteredContentObsOnce', function () {

        var cases = [
          {
            availableHosts: [],
            filteredContent: [],
            title: 'no hosts available'
          },
          {
            availableHosts: [
              {},
              {
                filtered: false
              },
              {
                filtered: null
              },
              {
                filtered: 0
              }
            ],
            filteredContent: [],
            title: 'no hosts filtered'
          },
          {
            availableHosts: [
              {},
              {
                filtered: false
              },
              {
                filtered: null
              },
              {
                filtered: 0
              },
              {
                filtered: true
              },
              {
                filtered: 'true'
              },
              {
                filtered: 1
              }
            ],
            filteredContent: [
              {
                filtered: true
              },
              {
                filtered: 'true'
              },
              {
                filtered: 1
              }
            ],
            title: 'filtered hosts present'
          }
        ];

        beforeEach(function () {
          sinon.stub(view, 'filteredContentObs', Em.K);
          sinon.stub(view, 'filterHosts', Em.K);
        });

        afterEach(function () {
          view.filteredContentObs.restore();
          view.filterHosts.restore();
        });

        cases.forEach(function (item) {

          it(item.title, function () {
            popup.set('availableHosts', item.availableHosts);
            view.filteredContentObsOnce();
            expect(view.get('filteredContent')).to.eql(item.filteredContent);
          });

        });

      });

      describe('#filterComponents', function () {

        it('should be set from validComponents', function () {
          expect(view.get('filterComponents')).to.eql(validComponents);
        });

      });

      describe('#isDisabled', function () {

        var cases = [
          {
            isLoaded: true,
            isDisabled: false,
            title: 'enabled'
          },
          {
            isLoaded: false,
            isDisabled: true,
            title: 'disabled'
          }
        ];

        cases.forEach(function (item) {

          it(item.title, function () {
            popup.set('isLoaded', item.isLoaded);
            expect(view.get('isDisabled')).to.equal(item.isDisabled);
          });

        });

      });

      describe('#didInsertElement', function () {

        beforeEach(function () {
          sinon.stub(view, 'filterHosts', Em.K);
          sinon.stub(view, 'filteredContentObs', Em.K);
          sinon.stub(view, 'filteredContentObsOnce', Em.K);
          view.get('filterColumns').setEach('selected', false);
          view.get('filterColumns').findProperty('id', 'cpu').set('selected', true);
          view.didInsertElement();
        });

        afterEach(function () {
          view.filterHosts.restore();
          view.filteredContentObs.restore();
          view.filteredContentObsOnce.restore();
        });

        it('column for filter', function () {
          expect(view.get('filterColumn')).to.eql(Em.Object.create({
            id: 'cpu',
            name: 'CPU',
            selected: true
          }));
        });

        it('available hosts', function () {
          expect(view.get('parentView.availableHosts').toArray()).to.eql([
            {
              host: {
                id: 'h0'
              },
              filtered: true
            },
            {
              host: {
                id: 'h1'
              },
              filtered: true
            }
          ]);
        });

        it('isLoaded', function () {
          expect(view.get('parentView.isLoaded')).to.be.true;
        });

        it('observer called', function () {
          expect(view.filteredContentObsOnce.calledOnce).to.be.true;
        });

      });

      describe('#filterHosts', function () {

        var cases = [
          {
            filterText: '',
            filterComponent: null,
            showOnlySelectedHosts: true,
            availableHosts: [
              Em.Object.create({
                host: Em.Object.create({
                  osType: 'centos6'
                }),
                selected: true
              }),
              Em.Object.create({
                host: Em.Object.create({
                  osType: 'centos7'
                }),
                selected: false
              })
            ],
            result: [
              Em.Object.create({
                host: Em.Object.create({
                  osType: 'centos6'
                }),
                selected: true,
                filterColumnValue: 'centos6',
                filtered: true
              }),
              Em.Object.create({
                host: Em.Object.create({
                  osType: 'centos7'
                }),
                selected: false,
                filterColumnValue: 'centos7',
                filtered: false
              })
            ],
            title: 'no filter text, no component for filter, show selected hosts only'
          },
          {
            filterText: '',
            filterComponent: null,
            showOnlySelectedHosts: false,
            availableHosts: [
              Em.Object.create({
                host: Em.Object.create({
                  osType: 'centos6'
                }),
                selected: true
              }),
              Em.Object.create({
                host: Em.Object.create({
                  osType: 'centos7'
                }),
                selected: false
              })
            ],
            result: [
              Em.Object.create({
                host: Em.Object.create({
                  osType: 'centos6'
                }),
                selected: true,
                filterColumnValue: 'centos6',
                filtered: true
              }),
              Em.Object.create({
                host: Em.Object.create({
                  osType: 'centos7'
                }),
                selected: false,
                filterColumnValue: 'centos7',
                filtered: true
              })
            ],
            title: 'no filter text, no component for filter, show all hosts'
          },
          {
            filterText: 'n',
            filterComponent: null,
            showOnlySelectedHosts: true,
            availableHosts: [
              Em.Object.create({
                host: Em.Object.create({
                  publicHostName: 'h0'
                }),
                selected: true
              }),
              Em.Object.create({
                host: Em.Object.create({
                  publicHostName: 'h1'
                }),
                selected: false
              }),
              Em.Object.create({
                host: Em.Object.create({
                  publicHostName: 'hn2'
                }),
                selected: true
              }),
              Em.Object.create({
                host: Em.Object.create({
                  publicHostName: 'hn3'
                }),
                selected: false
              }),
              Em.Object.create({
                host: Em.Object.create({
                  publicHostName: 'h4',
                  osType: 'centos6'
                }),
                selected: true
              }),
              Em.Object.create({
                host: Em.Object.create({
                  publicHostName: 'h5',
                  osType: 'centos6'
                }),
                selected: false
              }),
              Em.Object.create({
                host: Em.Object.create({
                  publicHostName: 'hn6',
                  osType: 'centos6'
                }),
                selected: true
              }),
              Em.Object.create({
                host: Em.Object.create({
                  publicHostName: 'hn7',
                  osType: 'centos6'
                }),
                selected: false
              }),
              Em.Object.create({
                host: Em.Object.create({
                  publicHostName: 'h8',
                  osType: 'suse11'
                }),
                selected: true
              }),
              Em.Object.create({
                host: Em.Object.create({
                  publicHostName: 'h9',
                  osType: 'suse11'
                }),
                selected: false
              }),
              Em.Object.create({
                host: Em.Object.create({
                  publicHostName: 'hn10',
                  osType: 'suse11'
                }),
                selected: true
              }),
              Em.Object.create({
                host: Em.Object.create({
                  publicHostName: 'hn11',
                  osType: 'suse11'
                }),
                selected: false
              })
            ],
            result: [
              Em.Object.create({
                host: Em.Object.create({
                  publicHostName: 'h0'
                }),
                selected: true,
                filtered: false
              }),
              Em.Object.create({
                host: Em.Object.create({
                  publicHostName: 'h1'
                }),
                selected: false,
                filtered: false
              }),
              Em.Object.create({
                host: Em.Object.create({
                  publicHostName: 'hn2'
                }),
                selected: true,
                filtered: true
              }),
              Em.Object.create({
                host: Em.Object.create({
                  publicHostName: 'hn3'
                }),
                selected: false,
                filtered: false
              }),
              Em.Object.create({
                host: Em.Object.create({
                  publicHostName: 'h4',
                  osType: 'centos6'
                }),
                selected: true,
                filterColumnValue: 'centos6',
                filtered: true
              }),
              Em.Object.create({
                host: Em.Object.create({
                  publicHostName: 'h5',
                  osType: 'centos6'
                }),
                selected: false,
                filterColumnValue: 'centos6',
                filtered: false
              }),
              Em.Object.create({
                host: Em.Object.create({
                  publicHostName: 'hn6',
                  osType: 'centos6'
                }),
                selected: true,
                filterColumnValue: 'centos6',
                filtered: true
              }),
              Em.Object.create({
                host: Em.Object.create({
                  publicHostName: 'hn7',
                  osType: 'centos6'
                }),
                selected: false,
                filterColumnValue: 'centos6',
                filtered: false
              }),
              Em.Object.create({
                host: Em.Object.create({
                  publicHostName: 'h8',
                  osType: 'suse11'
                }),
                selected: true,
                filterColumnValue: 'suse11',
                filtered: false
              }),
              Em.Object.create({
                host: Em.Object.create({
                  publicHostName: 'h9',
                  osType: 'suse11'
                }),
                selected: false,
                filterColumnValue: 'suse11',
                filtered: false
              }),
              Em.Object.create({
                host: Em.Object.create({
                  publicHostName: 'hn10',
                  osType: 'suse11'
                }),
                selected: true,
                filterColumnValue: 'suse11',
                filtered: true
              }),
              Em.Object.create({
                host: Em.Object.create({
                  publicHostName: 'hn11',
                  osType: 'suse11'
                }),
                selected: false,
                filterColumnValue: 'suse11',
                filtered: false
              })
            ],
            title: 'filter text set, no component for filter, show selected hosts only'
          },
          {
            filterText: 'n',
            filterComponent: null,
            showOnlySelectedHosts: false,
            availableHosts: [
              Em.Object.create({
                host: Em.Object.create({
                  publicHostName: 'h0'
                }),
                selected: true
              }),
              Em.Object.create({
                host: Em.Object.create({
                  publicHostName: 'h1'
                }),
                selected: false
              }),
              Em.Object.create({
                host: Em.Object.create({
                  publicHostName: 'hn2'
                }),
                selected: true
              }),
              Em.Object.create({
                host: Em.Object.create({
                  publicHostName: 'hn3'
                }),
                selected: false
              }),
              Em.Object.create({
                host: Em.Object.create({
                  publicHostName: 'h4',
                  osType: 'centos6'
                }),
                selected: true
              }),
              Em.Object.create({
                host: Em.Object.create({
                  publicHostName: 'h5',
                  osType: 'centos6'
                }),
                selected: false
              }),
              Em.Object.create({
                host: Em.Object.create({
                  publicHostName: 'hn6',
                  osType: 'centos6'
                }),
                selected: true
              }),
              Em.Object.create({
                host: Em.Object.create({
                  publicHostName: 'hn7',
                  osType: 'centos6'
                }),
                selected: false
              }),
              Em.Object.create({
                host: Em.Object.create({
                  publicHostName: 'h8',
                  osType: 'suse11'
                }),
                selected: true
              }),
              Em.Object.create({
                host: Em.Object.create({
                  publicHostName: 'h9',
                  osType: 'suse11'
                }),
                selected: false
              }),
              Em.Object.create({
                host: Em.Object.create({
                  publicHostName: 'hn10',
                  osType: 'suse11'
                }),
                selected: true
              }),
              Em.Object.create({
                host: Em.Object.create({
                  publicHostName: 'hn11',
                  osType: 'suse11'
                }),
                selected: false
              })
            ],
            result: [
              Em.Object.create({
                host: Em.Object.create({
                  publicHostName: 'h0'
                }),
                selected: true,
                filtered: false
              }),
              Em.Object.create({
                host: Em.Object.create({
                  publicHostName: 'h1'
                }),
                selected: false,
                filtered: false
              }),
              Em.Object.create({
                host: Em.Object.create({
                  publicHostName: 'hn2'
                }),
                selected: true,
                filtered: true
              }),
              Em.Object.create({
                host: Em.Object.create({
                  publicHostName: 'hn3'
                }),
                selected: false,
                filtered: true
              }),
              Em.Object.create({
                host: Em.Object.create({
                  publicHostName: 'h4',
                  osType: 'centos6'
                }),
                selected: true,
                filterColumnValue: 'centos6',
                filtered: true
              }),
              Em.Object.create({
                host: Em.Object.create({
                  publicHostName: 'h5',
                  osType: 'centos6'
                }),
                selected: false,
                filterColumnValue: 'centos6',
                filtered: true
              }),
              Em.Object.create({
                host: Em.Object.create({
                  publicHostName: 'hn6',
                  osType: 'centos6'
                }),
                selected: true,
                filterColumnValue: 'centos6',
                filtered: true
              }),
              Em.Object.create({
                host: Em.Object.create({
                  publicHostName: 'hn7',
                  osType: 'centos6'
                }),
                selected: false,
                filterColumnValue: 'centos6',
                filtered: true
              }),
              Em.Object.create({
                host: Em.Object.create({
                  publicHostName: 'h8',
                  osType: 'suse11'
                }),
                selected: true,
                filterColumnValue: 'suse11',
                filtered: false
              }),
              Em.Object.create({
                host: Em.Object.create({
                  publicHostName: 'h9',
                  osType: 'suse11'
                }),
                selected: false,
                filterColumnValue: 'suse11',
                filtered: false
              }),
              Em.Object.create({
                host: Em.Object.create({
                  publicHostName: 'hn10',
                  osType: 'suse11'
                }),
                selected: true,
                filterColumnValue: 'suse11',
                filtered: true
              }),
              Em.Object.create({
                host: Em.Object.create({
                  publicHostName: 'hn11',
                  osType: 'suse11'
                }),
                selected: false,
                filterColumnValue: 'suse11',
                filtered: true
              })
            ],
            title: 'filter text set, no component for filter, show all hosts'
          },
          {
            filterText: '',
            filterComponent: Em.Object.create({
              componentName: 'c0'
            }),
            showOnlySelectedHosts: true,
            availableHosts: [
              Em.Object.create({
                host: Em.Object.create({
                  osType: 'centos6'
                }),
                hostComponentNames: [],
                selected: true
              }),
              Em.Object.create({
                host: Em.Object.create({
                  osType: 'centos7'
                }),
                hostComponentNames: [],
                selected: false
              }),
              Em.Object.create({
                host: Em.Object.create({
                  osType: 'centos6'
                }),
                hostComponentNames: ['c1', 'c2'],
                selected: true
              }),
              Em.Object.create({
                host: Em.Object.create({
                  osType: 'centos7'
                }),
                hostComponentNames: ['c1', 'c2'],
                selected: false
              }),
              Em.Object.create({
                host: Em.Object.create({
                  osType: 'centos6'
                }),
                hostComponentNames: ['c0', 'c2'],
                selected: true
              }),
              Em.Object.create({
                host: Em.Object.create({
                  osType: 'centos7'
                }),
                hostComponentNames: ['c0', 'c2'],
                selected: false
              })
            ],
            result: [
              Em.Object.create({
                host: Em.Object.create({
                  osType: 'centos6'
                }),
                hostComponentNames: [],
                selected: true,
                filterColumnValue: 'centos6',
                filtered: true
              }),
              Em.Object.create({
                host: Em.Object.create({
                  osType: 'centos7'
                }),
                hostComponentNames: [],
                selected: false,
                filterColumnValue: 'centos7',
                filtered: false
              }),
              Em.Object.create({
                host: Em.Object.create({
                  osType: 'centos6'
                }),
                hostComponentNames: ['c1', 'c2'],
                selected: true,
                filterColumnValue: 'centos6',
                filtered: false
              }),
              Em.Object.create({
                host: Em.Object.create({
                  osType: 'centos7'
                }),
                hostComponentNames: ['c1', 'c2'],
                selected: false,
                filterColumnValue: 'centos7',
                filtered: false
              }),
              Em.Object.create({
                host: Em.Object.create({
                  osType: 'centos6'
                }),
                hostComponentNames: ['c0', 'c2'],
                selected: true,
                filterColumnValue: 'centos6',
                filtered: true
              }),
              Em.Object.create({
                host: Em.Object.create({
                  osType: 'centos7'
                }),
                hostComponentNames: ['c0', 'c2'],
                selected: false,
                filterColumnValue: 'centos7',
                filtered: false
              })
            ],
            title: 'no filter text, component filter set, show selected hosts only'
          },
          {
            filterText: '',
            filterComponent: Em.Object.create({
              componentName: 'c1'
            }),
            showOnlySelectedHosts: false,
            availableHosts: [
              Em.Object.create({
                host: Em.Object.create({
                  osType: 'centos6'
                }),
                hostComponentNames: [],
                selected: true
              }),
              Em.Object.create({
                host: Em.Object.create({
                  osType: 'centos7'
                }),
                hostComponentNames: [],
                selected: false
              }),
              Em.Object.create({
                host: Em.Object.create({
                  osType: 'centos6'
                }),
                hostComponentNames: ['c0', 'c2'],
                selected: true
              }),
              Em.Object.create({
                host: Em.Object.create({
                  osType: 'centos7'
                }),
                hostComponentNames: ['c0', 'c2'],
                selected: false
              }),
              Em.Object.create({
                host: Em.Object.create({
                  osType: 'centos6'
                }),
                hostComponentNames: ['c1', 'c2'],
                selected: true
              }),
              Em.Object.create({
                host: Em.Object.create({
                  osType: 'centos7'
                }),
                hostComponentNames: ['c1', 'c2'],
                selected: false
              })
            ],
            result: [
              Em.Object.create({
                host: Em.Object.create({
                  osType: 'centos6'
                }),
                hostComponentNames: [],
                selected: true,
                filterColumnValue: 'centos6',
                filtered: true
              }),
              Em.Object.create({
                host: Em.Object.create({
                  osType: 'centos7'
                }),
                hostComponentNames: [],
                selected: false,
                filterColumnValue: 'centos7',
                filtered: true
              }),
              Em.Object.create({
                host: Em.Object.create({
                  osType: 'centos6'
                }),
                hostComponentNames: ['c0', 'c2'],
                selected: true,
                filterColumnValue: 'centos6',
                filtered: false
              }),
              Em.Object.create({
                host: Em.Object.create({
                  osType: 'centos7'
                }),
                hostComponentNames: ['c0', 'c2'],
                selected: false,
                filterColumnValue: 'centos7',
                filtered: false
              }),
              Em.Object.create({
                host: Em.Object.create({
                  osType: 'centos6'
                }),
                hostComponentNames: ['c1', 'c2'],
                selected: true,
                filterColumnValue: 'centos6',
                filtered: true
              }),
              Em.Object.create({
                host: Em.Object.create({
                  osType: 'centos7'
                }),
                hostComponentNames: ['c1', 'c2'],
                selected: false,
                filterColumnValue: 'centos7',
                filtered: true
              })
            ],
            title: 'no filter text, component filter set, show all hosts'
          },
          {
            filterText: 'n',
            filterComponent: Em.Object.create({
              componentName: 'c2'
            }),
            showOnlySelectedHosts: true,
            availableHosts: [
              Em.Object.create({
                host: Em.Object.create({
                  publicHostName: 'h0'
                }),
                hostComponentNames: [],
                selected: true
              }),
              Em.Object.create({
                host: Em.Object.create({
                  publicHostName: 'h1'
                }),
                hostComponentNames: [],
                selected: false
              }),
              Em.Object.create({
                host: Em.Object.create({
                  publicHostName: 'hn2'
                }),
                hostComponentNames: [],
                selected: true
              }),
              Em.Object.create({
                host: Em.Object.create({
                  publicHostName: 'hn3'
                }),
                hostComponentNames: [],
                selected: false
              }),
              Em.Object.create({
                host: Em.Object.create({
                  publicHostName: 'h4',
                  osType: 'centos6'
                }),
                hostComponentNames: [],
                selected: true
              }),
              Em.Object.create({
                host: Em.Object.create({
                  publicHostName: 'h5',
                  osType: 'centos6'
                }),
                hostComponentNames: [],
                selected: false
              }),
              Em.Object.create({
                host: Em.Object.create({
                  publicHostName: 'hn6',
                  osType: 'centos6'
                }),
                hostComponentNames: [],
                selected: true
              }),
              Em.Object.create({
                host: Em.Object.create({
                  publicHostName: 'hn7',
                  osType: 'centos6'
                }),
                hostComponentNames: [],
                selected: false
              }),
              Em.Object.create({
                host: Em.Object.create({
                  publicHostName: 'h8',
                  osType: 'suse11'
                }),
                hostComponentNames: [],
                selected: true
              }),
              Em.Object.create({
                host: Em.Object.create({
                  publicHostName: 'h9',
                  osType: 'suse11'
                }),
                hostComponentNames: [],
                selected: false
              }),
              Em.Object.create({
                host: Em.Object.create({
                  publicHostName: 'hn10',
                  osType: 'suse11'
                }),
                hostComponentNames: [],
                selected: true
              }),
              Em.Object.create({
                host: Em.Object.create({
                  publicHostName: 'hn11',
                  osType: 'suse11'
                }),
                hostComponentNames: [],
                selected: false
              }),
              Em.Object.create({
                host: Em.Object.create({
                  publicHostName: 'h12'
                }),
                hostComponentNames: ['c0', 'c1'],
                selected: true
              }),
              Em.Object.create({
                host: Em.Object.create({
                  publicHostName: 'h13'
                }),
                hostComponentNames: ['c0', 'c1'],
                selected: false
              }),
              Em.Object.create({
                host: Em.Object.create({
                  publicHostName: 'hn14'
                }),
                hostComponentNames: ['c0', 'c1'],
                selected: true
              }),
              Em.Object.create({
                host: Em.Object.create({
                  publicHostName: 'hn15'
                }),
                hostComponentNames: ['c0', 'c1'],
                selected: false
              }),
              Em.Object.create({
                host: Em.Object.create({
                  publicHostName: 'h16',
                  osType: 'centos6'
                }),
                hostComponentNames: ['c0', 'c1'],
                selected: true
              }),
              Em.Object.create({
                host: Em.Object.create({
                  publicHostName: 'h17',
                  osType: 'centos6'
                }),
                hostComponentNames: ['c0', 'c1'],
                selected: false
              }),
              Em.Object.create({
                host: Em.Object.create({
                  publicHostName: 'hn18',
                  osType: 'centos6'
                }),
                hostComponentNames: ['c0', 'c1'],
                selected: true
              }),
              Em.Object.create({
                host: Em.Object.create({
                  publicHostName: 'hn19',
                  osType: 'centos6'
                }),
                hostComponentNames: ['c0', 'c1'],
                selected: false
              }),
              Em.Object.create({
                host: Em.Object.create({
                  publicHostName: 'h20',
                  osType: 'suse11'
                }),
                hostComponentNames: ['c0', 'c1'],
                selected: true
              }),
              Em.Object.create({
                host: Em.Object.create({
                  publicHostName: 'h21',
                  osType: 'suse11'
                }),
                hostComponentNames: ['c0', 'c1'],
                selected: false
              }),
              Em.Object.create({
                host: Em.Object.create({
                  publicHostName: 'hn22',
                  osType: 'suse11'
                }),
                hostComponentNames: ['c0', 'c1'],
                selected: true
              }),
              Em.Object.create({
                host: Em.Object.create({
                  publicHostName: 'hn23',
                  osType: 'suse11'
                }),
                hostComponentNames: ['c0', 'c1'],
                selected: false
              }),
              Em.Object.create({
                host: Em.Object.create({
                  publicHostName: 'h24'
                }),
                hostComponentNames: ['c0', 'c2'],
                selected: true
              }),
              Em.Object.create({
                host: Em.Object.create({
                  publicHostName: 'h25'
                }),
                hostComponentNames: ['c0', 'c2'],
                selected: false
              }),
              Em.Object.create({
                host: Em.Object.create({
                  publicHostName: 'hn26'
                }),
                hostComponentNames: ['c0', 'c2'],
                selected: true
              }),
              Em.Object.create({
                host: Em.Object.create({
                  publicHostName: 'hn27'
                }),
                hostComponentNames: ['c0', 'c2'],
                selected: false
              }),
              Em.Object.create({
                host: Em.Object.create({
                  publicHostName: 'h28',
                  osType: 'centos6'
                }),
                hostComponentNames: ['c0', 'c2'],
                selected: true
              }),
              Em.Object.create({
                host: Em.Object.create({
                  publicHostName: 'h29',
                  osType: 'centos6'
                }),
                hostComponentNames: ['c0', 'c2'],
                selected: false
              }),
              Em.Object.create({
                host: Em.Object.create({
                  publicHostName: 'hn30',
                  osType: 'centos6'
                }),
                hostComponentNames: ['c0', 'c2'],
                selected: true
              }),
              Em.Object.create({
                host: Em.Object.create({
                  publicHostName: 'hn31',
                  osType: 'centos6'
                }),
                hostComponentNames: ['c0', 'c2'],
                selected: false
              }),
              Em.Object.create({
                host: Em.Object.create({
                  publicHostName: 'h32',
                  osType: 'suse11'
                }),
                hostComponentNames: ['c0', 'c2'],
                selected: true
              }),
              Em.Object.create({
                host: Em.Object.create({
                  publicHostName: 'h33',
                  osType: 'suse11'
                }),
                hostComponentNames: ['c0', 'c2'],
                selected: false
              }),
              Em.Object.create({
                host: Em.Object.create({
                  publicHostName: 'hn34',
                  osType: 'suse11'
                }),
                hostComponentNames: ['c0', 'c2'],
                selected: true
              }),
              Em.Object.create({
                host: Em.Object.create({
                  publicHostName: 'hn35',
                  osType: 'suse11'
                }),
                hostComponentNames: ['c0', 'c2'],
                selected: false
              })
            ],
            result: [
              Em.Object.create({
                host: Em.Object.create({
                  publicHostName: 'h0'
                }),
                hostComponentNames: [],
                selected: true,
                filtered: false
              }),
              Em.Object.create({
                host: Em.Object.create({
                  publicHostName: 'h1'
                }),
                hostComponentNames: [],
                selected: false,
                filtered: false
              }),
              Em.Object.create({
                host: Em.Object.create({
                  publicHostName: 'hn2'
                }),
                hostComponentNames: [],
                selected: true,
                filtered: true
              }),
              Em.Object.create({
                host: Em.Object.create({
                  publicHostName: 'hn3'
                }),
                hostComponentNames: [],
                selected: false,
                filtered: false
              }),
              Em.Object.create({
                host: Em.Object.create({
                  publicHostName: 'h4',
                  osType: 'centos6'
                }),
                hostComponentNames: [],
                selected: true,
                filterColumnValue: 'centos6',
                filtered: true
              }),
              Em.Object.create({
                host: Em.Object.create({
                  publicHostName: 'h5',
                  osType: 'centos6'
                }),
                hostComponentNames: [],
                selected: false,
                filterColumnValue: 'centos6',
                filtered: false
              }),
              Em.Object.create({
                host: Em.Object.create({
                  publicHostName: 'hn6',
                  osType: 'centos6'
                }),
                hostComponentNames: [],
                selected: true,
                filterColumnValue: 'centos6',
                filtered: true
              }),
              Em.Object.create({
                host: Em.Object.create({
                  publicHostName: 'hn7',
                  osType: 'centos6'
                }),
                hostComponentNames: [],
                selected: false,
                filterColumnValue: 'centos6',
                filtered: false
              }),
              Em.Object.create({
                host: Em.Object.create({
                  publicHostName: 'h8',
                  osType: 'suse11'
                }),
                hostComponentNames: [],
                selected: true,
                filterColumnValue: 'suse11',
                filtered: false
              }),
              Em.Object.create({
                host: Em.Object.create({
                  publicHostName: 'h9',
                  osType: 'suse11'
                }),
                hostComponentNames: [],
                selected: false,
                filterColumnValue: 'suse11',
                filtered: false
              }),
              Em.Object.create({
                host: Em.Object.create({
                  publicHostName: 'hn10',
                  osType: 'suse11'
                }),
                hostComponentNames: [],
                selected: true,
                filterColumnValue: 'suse11',
                filtered: true
              }),
              Em.Object.create({
                host: Em.Object.create({
                  publicHostName: 'hn11',
                  osType: 'suse11'
                }),
                hostComponentNames: [],
                selected: false,
                filterColumnValue: 'suse11',
                filtered: false
              }),
              Em.Object.create({
                host: Em.Object.create({
                  publicHostName: 'h12'
                }),
                hostComponentNames: ['c0', 'c1'],
                selected: true,
                filtered: false
              }),
              Em.Object.create({
                host: Em.Object.create({
                  publicHostName: 'h13'
                }),
                hostComponentNames: ['c0', 'c1'],
                selected: false,
                filtered: false
              }),
              Em.Object.create({
                host: Em.Object.create({
                  publicHostName: 'hn14'
                }),
                hostComponentNames: ['c0', 'c1'],
                selected: true,
                filtered: false
              }),
              Em.Object.create({
                host: Em.Object.create({
                  publicHostName: 'hn15'
                }),
                hostComponentNames: ['c0', 'c1'],
                selected: false,
                filtered: false
              }),
              Em.Object.create({
                host: Em.Object.create({
                  publicHostName: 'h16',
                  osType: 'centos6'
                }),
                hostComponentNames: ['c0', 'c1'],
                selected: true,
                filterColumnValue: 'centos6',
                filtered: false
              }),
              Em.Object.create({
                host: Em.Object.create({
                  publicHostName: 'h17',
                  osType: 'centos6'
                }),
                hostComponentNames: ['c0', 'c1'],
                selected: false,
                filterColumnValue: 'centos6',
                filtered: false
              }),
              Em.Object.create({
                host: Em.Object.create({
                  publicHostName: 'hn18',
                  osType: 'centos6'
                }),
                hostComponentNames: ['c0', 'c1'],
                selected: true,
                filterColumnValue: 'centos6',
                filtered: false
              }),
              Em.Object.create({
                host: Em.Object.create({
                  publicHostName: 'hn19',
                  osType: 'centos6'
                }),
                hostComponentNames: ['c0', 'c1'],
                selected: false,
                filterColumnValue: 'centos6',
                filtered: false
              }),
              Em.Object.create({
                host: Em.Object.create({
                  publicHostName: 'h20',
                  osType: 'suse11'
                }),
                hostComponentNames: ['c0', 'c1'],
                selected: true,
                filterColumnValue: 'suse11',
                filtered: false
              }),
              Em.Object.create({
                host: Em.Object.create({
                  publicHostName: 'h21',
                  osType: 'suse11'
                }),
                hostComponentNames: ['c0', 'c1'],
                selected: false,
                filterColumnValue: 'suse11',
                filtered: false
              }),
              Em.Object.create({
                host: Em.Object.create({
                  publicHostName: 'hn22',
                  osType: 'suse11'
                }),
                hostComponentNames: ['c0', 'c1'],
                selected: true,
                filterColumnValue: 'suse11',
                filtered: false
              }),
              Em.Object.create({
                host: Em.Object.create({
                  publicHostName: 'hn23',
                  osType: 'suse11'
                }),
                hostComponentNames: ['c0', 'c1'],
                selected: false,
                filterColumnValue: 'suse11',
                filtered: false
              }),
              Em.Object.create({
                host: Em.Object.create({
                  publicHostName: 'h24'
                }),
                hostComponentNames: ['c0', 'c2'],
                selected: true,
                filtered: false
              }),
              Em.Object.create({
                host: Em.Object.create({
                  publicHostName: 'h25'
                }),
                hostComponentNames: ['c0', 'c2'],
                selected: false,
                filtered: false
              }),
              Em.Object.create({
                host: Em.Object.create({
                  publicHostName: 'hn26'
                }),
                hostComponentNames: ['c0', 'c2'],
                selected: true,
                filtered: true
              }),
              Em.Object.create({
                host: Em.Object.create({
                  publicHostName: 'hn27'
                }),
                hostComponentNames: ['c0', 'c2'],
                selected: false,
                filtered: false
              }),
              Em.Object.create({
                host: Em.Object.create({
                  publicHostName: 'h28',
                  osType: 'centos6'
                }),
                hostComponentNames: ['c0', 'c2'],
                selected: true,
                filterColumnValue: 'centos6',
                filtered: true
              }),
              Em.Object.create({
                host: Em.Object.create({
                  publicHostName: 'h29',
                  osType: 'centos6'
                }),
                hostComponentNames: ['c0', 'c2'],
                selected: false,
                filterColumnValue: 'centos6',
                filtered: false
              }),
              Em.Object.create({
                host: Em.Object.create({
                  publicHostName: 'hn30',
                  osType: 'centos6'
                }),
                hostComponentNames: ['c0', 'c2'],
                selected: true,
                filterColumnValue: 'centos6',
                filtered: true
              }),
              Em.Object.create({
                host: Em.Object.create({
                  publicHostName: 'hn31',
                  osType: 'centos6'
                }),
                hostComponentNames: ['c0', 'c2'],
                selected: false,
                filterColumnValue: 'centos6',
                filtered: false
              }),
              Em.Object.create({
                host: Em.Object.create({
                  publicHostName: 'h32',
                  osType: 'suse11'
                }),
                hostComponentNames: ['c0', 'c2'],
                selected: true,
                filterColumnValue: 'suse11',
                filtered: false
              }),
              Em.Object.create({
                host: Em.Object.create({
                  publicHostName: 'h33',
                  osType: 'suse11'
                }),
                hostComponentNames: ['c0', 'c2'],
                selected: false,
                filterColumnValue: 'suse11',
                filtered: false
              }),
              Em.Object.create({
                host: Em.Object.create({
                  publicHostName: 'hn34',
                  osType: 'suse11'
                }),
                hostComponentNames: ['c0', 'c2'],
                selected: true,
                filterColumnValue: 'suse11',
                filtered: true
              }),
              Em.Object.create({
                host: Em.Object.create({
                  publicHostName: 'hn35',
                  osType: 'suse11'
                }),
                hostComponentNames: ['c0', 'c2'],
                selected: false,
                filterColumnValue: 'suse11',
                filtered: false
              })
            ],
            title: 'filter text set, component filter set, show selected hosts only'
          },
          {
            filterText: 'n',
            filterComponent: Em.Object.create({
              componentName: 'c3'
            }),
            showOnlySelectedHosts: false,
            availableHosts: [
              Em.Object.create({
                host: Em.Object.create({
                  publicHostName: 'h0'
                }),
                hostComponentNames: [],
                selected: true
              }),
              Em.Object.create({
                host: Em.Object.create({
                  publicHostName: 'h1'
                }),
                hostComponentNames: [],
                selected: false
              }),
              Em.Object.create({
                host: Em.Object.create({
                  publicHostName: 'hn2'
                }),
                hostComponentNames: [],
                selected: true
              }),
              Em.Object.create({
                host: Em.Object.create({
                  publicHostName: 'hn3'
                }),
                hostComponentNames: [],
                selected: false
              }),
              Em.Object.create({
                host: Em.Object.create({
                  publicHostName: 'h4',
                  osType: 'centos6'
                }),
                hostComponentNames: [],
                selected: true
              }),
              Em.Object.create({
                host: Em.Object.create({
                  publicHostName: 'h5',
                  osType: 'centos6'
                }),
                hostComponentNames: [],
                selected: false
              }),
              Em.Object.create({
                host: Em.Object.create({
                  publicHostName: 'hn6',
                  osType: 'centos6'
                }),
                hostComponentNames: [],
                selected: true
              }),
              Em.Object.create({
                host: Em.Object.create({
                  publicHostName: 'hn7',
                  osType: 'centos6'
                }),
                hostComponentNames: [],
                selected: false
              }),
              Em.Object.create({
                host: Em.Object.create({
                  publicHostName: 'h8',
                  osType: 'suse11'
                }),
                hostComponentNames: [],
                selected: true
              }),
              Em.Object.create({
                host: Em.Object.create({
                  publicHostName: 'h9',
                  osType: 'suse11'
                }),
                hostComponentNames: [],
                selected: false
              }),
              Em.Object.create({
                host: Em.Object.create({
                  publicHostName: 'hn10',
                  osType: 'suse11'
                }),
                hostComponentNames: [],
                selected: true
              }),
              Em.Object.create({
                host: Em.Object.create({
                  publicHostName: 'hn11',
                  osType: 'suse11'
                }),
                hostComponentNames: [],
                selected: false
              }),
              Em.Object.create({
                host: Em.Object.create({
                  publicHostName: 'h12'
                }),
                hostComponentNames: ['c0', 'c1'],
                selected: true
              }),
              Em.Object.create({
                host: Em.Object.create({
                  publicHostName: 'h13'
                }),
                hostComponentNames: ['c0', 'c1'],
                selected: false
              }),
              Em.Object.create({
                host: Em.Object.create({
                  publicHostName: 'hn14'
                }),
                hostComponentNames: ['c0', 'c1'],
                selected: true
              }),
              Em.Object.create({
                host: Em.Object.create({
                  publicHostName: 'hn15'
                }),
                hostComponentNames: ['c0', 'c1'],
                selected: false
              }),
              Em.Object.create({
                host: Em.Object.create({
                  publicHostName: 'h16',
                  osType: 'centos6'
                }),
                hostComponentNames: ['c0', 'c1'],
                selected: true
              }),
              Em.Object.create({
                host: Em.Object.create({
                  publicHostName: 'h17',
                  osType: 'centos6'
                }),
                hostComponentNames: ['c0', 'c1'],
                selected: false
              }),
              Em.Object.create({
                host: Em.Object.create({
                  publicHostName: 'hn18',
                  osType: 'centos6'
                }),
                hostComponentNames: ['c0', 'c1'],
                selected: true
              }),
              Em.Object.create({
                host: Em.Object.create({
                  publicHostName: 'hn19',
                  osType: 'centos6'
                }),
                hostComponentNames: ['c0', 'c1'],
                selected: false
              }),
              Em.Object.create({
                host: Em.Object.create({
                  publicHostName: 'h20',
                  osType: 'suse11'
                }),
                hostComponentNames: ['c0', 'c1'],
                selected: true
              }),
              Em.Object.create({
                host: Em.Object.create({
                  publicHostName: 'h21',
                  osType: 'suse11'
                }),
                hostComponentNames: ['c0', 'c1'],
                selected: false
              }),
              Em.Object.create({
                host: Em.Object.create({
                  publicHostName: 'hn22',
                  osType: 'suse11'
                }),
                hostComponentNames: ['c0', 'c1'],
                selected: true
              }),
              Em.Object.create({
                host: Em.Object.create({
                  publicHostName: 'hn23',
                  osType: 'suse11'
                }),
                hostComponentNames: ['c0', 'c1'],
                selected: false
              }),
              Em.Object.create({
                host: Em.Object.create({
                  publicHostName: 'h24'
                }),
                hostComponentNames: ['c0', 'c3'],
                selected: true
              }),
              Em.Object.create({
                host: Em.Object.create({
                  publicHostName: 'h25'
                }),
                hostComponentNames: ['c0', 'c3'],
                selected: false
              }),
              Em.Object.create({
                host: Em.Object.create({
                  publicHostName: 'hn26'
                }),
                hostComponentNames: ['c0', 'c3'],
                selected: true
              }),
              Em.Object.create({
                host: Em.Object.create({
                  publicHostName: 'hn27'
                }),
                hostComponentNames: ['c0', 'c3'],
                selected: false
              }),
              Em.Object.create({
                host: Em.Object.create({
                  publicHostName: 'h28',
                  osType: 'centos6'
                }),
                hostComponentNames: ['c0', 'c3'],
                selected: true
              }),
              Em.Object.create({
                host: Em.Object.create({
                  publicHostName: 'h29',
                  osType: 'centos6'
                }),
                hostComponentNames: ['c0', 'c3'],
                selected: false
              }),
              Em.Object.create({
                host: Em.Object.create({
                  publicHostName: 'hn30',
                  osType: 'centos6'
                }),
                hostComponentNames: ['c0', 'c3'],
                selected: true
              }),
              Em.Object.create({
                host: Em.Object.create({
                  publicHostName: 'hn31',
                  osType: 'centos6'
                }),
                hostComponentNames: ['c0', 'c3'],
                selected: false
              }),
              Em.Object.create({
                host: Em.Object.create({
                  publicHostName: 'h32',
                  osType: 'suse11'
                }),
                hostComponentNames: ['c0', 'c3'],
                selected: true
              }),
              Em.Object.create({
                host: Em.Object.create({
                  publicHostName: 'h33',
                  osType: 'suse11'
                }),
                hostComponentNames: ['c0', 'c3'],
                selected: false
              }),
              Em.Object.create({
                host: Em.Object.create({
                  publicHostName: 'hn34',
                  osType: 'suse11'
                }),
                hostComponentNames: ['c0', 'c3'],
                selected: true
              }),
              Em.Object.create({
                host: Em.Object.create({
                  publicHostName: 'hn35',
                  osType: 'suse11'
                }),
                hostComponentNames: ['c0', 'c3'],
                selected: false
              })
            ],
            result: [
              Em.Object.create({
                host: Em.Object.create({
                  publicHostName: 'h0'
                }),
                hostComponentNames: [],
                selected: true,
                filtered: false
              }),
              Em.Object.create({
                host: Em.Object.create({
                  publicHostName: 'h1'
                }),
                hostComponentNames: [],
                selected: false,
                filtered: false
              }),
              Em.Object.create({
                host: Em.Object.create({
                  publicHostName: 'hn2'
                }),
                hostComponentNames: [],
                selected: true,
                filtered: true
              }),
              Em.Object.create({
                host: Em.Object.create({
                  publicHostName: 'hn3'
                }),
                hostComponentNames: [],
                selected: false,
                filtered: true
              }),
              Em.Object.create({
                host: Em.Object.create({
                  publicHostName: 'h4',
                  osType: 'centos6'
                }),
                hostComponentNames: [],
                selected: true,
                filterColumnValue: 'centos6',
                filtered: true
              }),
              Em.Object.create({
                host: Em.Object.create({
                  publicHostName: 'h5',
                  osType: 'centos6'
                }),
                hostComponentNames: [],
                selected: false,
                filterColumnValue: 'centos6',
                filtered: true
              }),
              Em.Object.create({
                host: Em.Object.create({
                  publicHostName: 'hn6',
                  osType: 'centos6'
                }),
                hostComponentNames: [],
                selected: true,
                filterColumnValue: 'centos6',
                filtered: true
              }),
              Em.Object.create({
                host: Em.Object.create({
                  publicHostName: 'hn7',
                  osType: 'centos6'
                }),
                hostComponentNames: [],
                selected: false,
                filterColumnValue: 'centos6',
                filtered: true
              }),
              Em.Object.create({
                host: Em.Object.create({
                  publicHostName: 'h8',
                  osType: 'suse11'
                }),
                hostComponentNames: [],
                selected: true,
                filterColumnValue: 'suse11',
                filtered: false
              }),
              Em.Object.create({
                host: Em.Object.create({
                  publicHostName: 'h9',
                  osType: 'suse11'
                }),
                hostComponentNames: [],
                selected: false,
                filterColumnValue: 'suse11',
                filtered: false
              }),
              Em.Object.create({
                host: Em.Object.create({
                  publicHostName: 'hn10',
                  osType: 'suse11'
                }),
                hostComponentNames: [],
                selected: true,
                filterColumnValue: 'suse11',
                filtered: true
              }),
              Em.Object.create({
                host: Em.Object.create({
                  publicHostName: 'hn11',
                  osType: 'suse11'
                }),
                hostComponentNames: [],
                selected: false,
                filterColumnValue: 'suse11',
                filtered: true
              }),
              Em.Object.create({
                host: Em.Object.create({
                  publicHostName: 'h12'
                }),
                hostComponentNames: ['c0', 'c1'],
                selected: true,
                filtered: false
              }),
              Em.Object.create({
                host: Em.Object.create({
                  publicHostName: 'h13'
                }),
                hostComponentNames: ['c0', 'c1'],
                selected: false,
                filtered: false
              }),
              Em.Object.create({
                host: Em.Object.create({
                  publicHostName: 'hn14'
                }),
                hostComponentNames: ['c0', 'c1'],
                selected: true,
                filtered: false
              }),
              Em.Object.create({
                host: Em.Object.create({
                  publicHostName: 'hn15'
                }),
                hostComponentNames: ['c0', 'c1'],
                selected: false,
                filtered: false
              }),
              Em.Object.create({
                host: Em.Object.create({
                  publicHostName: 'h16',
                  osType: 'centos6'
                }),
                hostComponentNames: ['c0', 'c1'],
                selected: true,
                filterColumnValue: 'centos6',
                filtered: false
              }),
              Em.Object.create({
                host: Em.Object.create({
                  publicHostName: 'h17',
                  osType: 'centos6'
                }),
                hostComponentNames: ['c0', 'c1'],
                selected: false,
                filterColumnValue: 'centos6',
                filtered: false
              }),
              Em.Object.create({
                host: Em.Object.create({
                  publicHostName: 'hn18',
                  osType: 'centos6'
                }),
                hostComponentNames: ['c0', 'c1'],
                selected: true,
                filterColumnValue: 'centos6',
                filtered: false
              }),
              Em.Object.create({
                host: Em.Object.create({
                  publicHostName: 'hn19',
                  osType: 'centos6'
                }),
                hostComponentNames: ['c0', 'c1'],
                selected: false,
                filterColumnValue: 'centos6',
                filtered: false
              }),
              Em.Object.create({
                host: Em.Object.create({
                  publicHostName: 'h20',
                  osType: 'suse11'
                }),
                hostComponentNames: ['c0', 'c1'],
                selected: true,
                filterColumnValue: 'suse11',
                filtered: false
              }),
              Em.Object.create({
                host: Em.Object.create({
                  publicHostName: 'h21',
                  osType: 'suse11'
                }),
                hostComponentNames: ['c0', 'c1'],
                selected: false,
                filterColumnValue: 'suse11',
                filtered: false
              }),
              Em.Object.create({
                host: Em.Object.create({
                  publicHostName: 'hn22',
                  osType: 'suse11'
                }),
                hostComponentNames: ['c0', 'c1'],
                selected: true,
                filterColumnValue: 'suse11',
                filtered: false
              }),
              Em.Object.create({
                host: Em.Object.create({
                  publicHostName: 'hn23',
                  osType: 'suse11'
                }),
                hostComponentNames: ['c0', 'c1'],
                selected: false,
                filterColumnValue: 'suse11',
                filtered: false
              }),
              Em.Object.create({
                host: Em.Object.create({
                  publicHostName: 'h24'
                }),
                hostComponentNames: ['c0', 'c3'],
                selected: true,
                filtered: false
              }),
              Em.Object.create({
                host: Em.Object.create({
                  publicHostName: 'h25'
                }),
                hostComponentNames: ['c0', 'c3'],
                selected: false,
                filtered: false
              }),
              Em.Object.create({
                host: Em.Object.create({
                  publicHostName: 'hn26'
                }),
                hostComponentNames: ['c0', 'c3'],
                selected: true,
                filtered: true
              }),
              Em.Object.create({
                host: Em.Object.create({
                  publicHostName: 'hn27'
                }),
                hostComponentNames: ['c0', 'c3'],
                selected: false,
                filtered: true
              }),
              Em.Object.create({
                host: Em.Object.create({
                  publicHostName: 'h28',
                  osType: 'centos6'
                }),
                hostComponentNames: ['c0', 'c3'],
                selected: true,
                filterColumnValue: 'centos6',
                filtered: true
              }),
              Em.Object.create({
                host: Em.Object.create({
                  publicHostName: 'h29',
                  osType: 'centos6'
                }),
                hostComponentNames: ['c0', 'c3'],
                selected: false,
                filterColumnValue: 'centos6',
                filtered: true
              }),
              Em.Object.create({
                host: Em.Object.create({
                  publicHostName: 'hn30',
                  osType: 'centos6'
                }),
                hostComponentNames: ['c0', 'c3'],
                selected: true,
                filterColumnValue: 'centos6',
                filtered: true
              }),
              Em.Object.create({
                host: Em.Object.create({
                  publicHostName: 'hn31',
                  osType: 'centos6'
                }),
                hostComponentNames: ['c0', 'c3'],
                selected: false,
                filterColumnValue: 'centos6',
                filtered: true
              }),
              Em.Object.create({
                host: Em.Object.create({
                  publicHostName: 'h32',
                  osType: 'suse11'
                }),
                hostComponentNames: ['c0', 'c3'],
                selected: true,
                filterColumnValue: 'suse11',
                filtered: false
              }),
              Em.Object.create({
                host: Em.Object.create({
                  publicHostName: 'h33',
                  osType: 'suse11'
                }),
                hostComponentNames: ['c0', 'c3'],
                selected: false,
                filterColumnValue: 'suse11',
                filtered: false
              }),
              Em.Object.create({
                host: Em.Object.create({
                  publicHostName: 'hn34',
                  osType: 'suse11'
                }),
                hostComponentNames: ['c0', 'c3'],
                selected: true,
                filterColumnValue: 'suse11',
                filtered: true
              }),
              Em.Object.create({
                host: Em.Object.create({
                  publicHostName: 'hn35',
                  osType: 'suse11'
                }),
                hostComponentNames: ['c0', 'c3'],
                selected: false,
                filterColumnValue: 'suse11',
                filtered: true
              })
            ],
            title: 'filter text set, component filter set, show all hosts'
          }
        ];

        cases.forEach(function (item) {

          var arrayForCheck;

          it(item.title, function () {
            view.setProperties({
              'showOnlySelectedHosts': item.showOnlySelectedHosts,
              'filterText': item.filterText,
              'filterComponent': item.filterComponent,
              'filterColumn': {
                id: 'osType'
              },
              'parentView.availableHosts': item.availableHosts
            });
            item.result.forEach(function (host) {
              if (!host.hasOwnProperty('filterColumnValue')) {
                host.set('filterColumnValue', undefined);
              }
            });
            arrayForCheck = view.get('parentView.availableHosts').toArray();
            arrayForCheck.forEach(function (host) {
              var hostComponentNames = host.get('hostComponentNames');
              if (host.get('hostComponentNames')) {
                host.set('hostComponentNames', hostComponentNames.toArray());
              }
            });
            expect(arrayForCheck).to.eql(item.result);
          });

        });

        it('start index', function () {
          view.set('startIndex', 0);
          view.set('parentView.availableHosts', []);
          view.propertyDidChange('filterColumn');
          expect(view.get('startIndex')).to.equal(0);
        });

      });

      describe('#hostSelectMessage', function () {

        beforeEach(function () {
          sinon.stub(view, 'filterHosts', Em.K);
          sinon.stub(view, 'filteredContentObs', Em.K);
          sinon.stub(view, 'filteredContentObsOnce', Em.K);
        });

        afterEach(function () {
          view.filterHosts.restore();
          view.filteredContentObs.restore();
          view.filteredContentObsOnce.restore();
        });

        it('correct message formatting', function () {
          view.set('parentView.availableHosts', [
            {
              selected: true
            },
            {
              selected: false
            },
            {
              selected: true
            }
          ]);
          expect(view.get('hostSelectMessage')).to.equal(Em.I18n.t('hosts.selectHostsDialog.selectedHostsLink').format(2, 3));
        });

      });

      describe('#selectFilterColumn', function () {

        var columnObject = {
            selected: true
          },
          cases = [
            {
              event: null,
              filterColumn: Em.Object.create(columnObject),
              isPreviousColumnSelected: true,
              title: 'no event data'
            },
            {
              event: {},
              filterColumn: Em.Object.create(columnObject),
              isPreviousColumnSelected: true,
              title: 'no context'
            },
            {
              event: {
                context: null
              },
              filterColumn: Em.Object.create(columnObject),
              isPreviousColumnSelected: true,
              title: 'empty context'
            },
            {
              event: {
                context: Em.Object.create()
              },
              filterColumn: Em.Object.create(columnObject),
              isPreviousColumnSelected: true,
              title: 'no id'
            },
            {
              event: {
                context: Em.Object.create({
                  id: null
                })
              },
              filterColumn: Em.Object.create(columnObject),
              isPreviousColumnSelected: true,
              title: 'invalid id'
            },
            {
              event: {
                context: Em.Object.create({
                  id: 0
                })
              },
              filterColumn: Em.Object.create({
                id: 0,
                selected: true
              }),
              isPreviousColumnSelected: false,
              title: 'valid id'
            }
          ];

        cases.forEach(function (item) {

          describe(item.title, function () {

            var column = Em.Object.create(columnObject);

            beforeEach(function () {
              view.set('filterColumn', column);
              view.selectFilterColumn(item.event);
            });

            it('new column', function () {
              expect(view.get('filterColumn')).to.eql(item.filterColumn);
            });

            it('should deselect previous column', function () {
              expect(column.get('selected')).to.equal(item.isPreviousColumnSelected);
            });

          });

        });

      });

      describe('#selectFilterComponent', function () {

        var componentObject = {
            selected: true
          },
          cases = [
            {
              event: null,
              filterComponent: Em.Object.create(componentObject),
              title: 'no event data'
            },
            {
              event: {},
              filterComponent: Em.Object.create(componentObject),
              title: 'no context'
            },
            {
              event: {
                context: null
              },
              filterComponent: Em.Object.create(componentObject),
              title: 'empty context'
            },
            {
              event: {
                context: Em.Object.create()
              },
              filterComponent: Em.Object.create(componentObject),
              title: 'no component name'
            },
            {
              event: {
                context: Em.Object.create({
                  componentName: null
                })
              },
              filterComponent: Em.Object.create(componentObject),
              title: 'invalid component name'
            },
            {
              event: {
                context: Em.Object.create({
                  componentName: 'c0'
                })
              },
              filterComponent: Em.Object.create({
                componentName: 'c0',
                selected: true
              }),
              title: 'valid component name'
            },
            {
              event: {
                context: Em.Object.create({
                  componentName: 'c0'
                })
              },
              previousFilterComponent: Em.Object.create({
                componentName: 'c0',
                selected: true
              }),
              filterComponent: null,
              title: 'manual deselection'
            },
            {
              event: {
                context: Em.Object.create({
                  componentName: 'c0'
                })
              },
              previousFilterComponent: Em.Object.create({
                componentName: 'c1',
                selected: true
              }),
              filterComponent: Em.Object.create({
                componentName: 'c0',
                selected: true
              }),
              title: 'no manual deselection'
            }
          ];

        cases.forEach(function (item) {

          it(item.title, function () {
            var component = item.previousFilterComponent || Em.Object.create(componentObject);
            view.set('filterComponent', component);
            view.selectFilterComponent(item.event);
            expect(view.get('filterComponent')).to.eql(item.filterComponent);
          });

        });

      });

      describe('#toggleSelectAllHosts', function () {

        var cases = [
          {
            allHostsSelected: true,
            availableHosts: [
              {
                host: {
                  id: 'h0'
                },
                filtered: true,
                selected: true
              },
              {
                host: {
                  id: 'h1'
                },
                filtered: false
              },
              {
                host: {
                  id: 'h2'
                },
                filtered: 'true',
                selected: true
              },
              {
                host: {
                  id: 'h3'
                },
                filtered: 1,
                selected: true
              },
              {
                host: {
                  id: 'h4'
                }
              }
            ],
            title: 'select all hosts'
          },
          {
            allHostsSelected: false,
            availableHosts: [
              {
                host: {
                  id: 'h0'
                },
                filtered: true,
                selected: false
              },
              {
                host: {
                  id: 'h1'
                },
                filtered: false
              },
              {
                host: {
                  id: 'h2'
                },
                filtered: 'true',
                selected: false
              },
              {
                host: {
                  id: 'h3'
                },
                filtered: 1,
                selected: false
              },
              {
                host: {
                  id: 'h4'
                }
              }
            ],
            title: 'deselect all hosts'
          }
        ];

        beforeEach(function () {
          sinon.stub(view, 'filterHosts', Em.K);
          sinon.stub(view, 'filteredContentObs', Em.K);
          sinon.stub(view, 'filteredContentObsOnce', Em.K);
        });

        afterEach(function () {
          view.filterHosts.restore();
          view.filteredContentObs.restore();
          view.filteredContentObsOnce.restore();
        });

        cases.forEach(function (item) {

          it(item.title, function () {
            view.setProperties({
              'allHostsSelected': !item.allHostsSelected,
              'parentView.availableHosts': [
                {
                  host: {
                    id: 'h0'
                  },
                  filtered: true
                },
                {
                  host: {
                    id: 'h1'
                  },
                  filtered: false
                },
                {
                  host: {
                    id: 'h2'
                  },
                  filtered: 'true'
                },
                {
                  host: {
                    id: 'h3'
                  },
                  filtered: 1
                },
                {
                  host: {
                    id: 'h4'
                  }
                }
              ]
            });
            view.toggleProperty('allHostsSelected');
            expect(view.get('parentView.availableHosts').toArray()).to.eql(item.availableHosts);
          });

        });

      });

      describe('#toggleShowSelectedHosts', function () {

        var cases = [
          {
            showOnlySelectedHosts: true,
            title: 'no component for filter'
          },
          {
            showOnlySelectedHosts: false,
            filterComponent: null,
            title: 'empty component for filter'
          },
          {
            showOnlySelectedHosts: true,
            filterComponent: Em.Object.create(),
            title: 'component for filter set'
          }
        ];

        cases.forEach(function (item) {

          describe(item.title, function () {

            beforeEach(function () {
              view.setProperties({
                showOnlySelectedHosts: item.showOnlySelectedHosts,
                filterComponent: item.filterComponent,
                filterText: ''
              });
              view.toggleShowSelectedHosts();
            });

            it('clear component filter', function () {
              expect(view.get('filterComponent')).to.be.null;
            });

            it('clear text filter', function () {
              expect(view.get('filterText')).to.be.null;
            });

            it('toggle hosts display mode', function () {
              expect(view.get('showOnlySelectedHosts')).to.equal(!item.showOnlySelectedHosts);
            });

          });

        });

      });

    });

  });

  describe('#setRackInfo', function () {

    beforeEach(function () {
      App.ModalPopup.show.restore();
      sinon.stub(App.ModalPopup, 'show', function (obj) {
        return Em.Object.create(obj);
      });
    });

    describe('#bodyClass', function () {

      describe('#validation', function () {

        var popup,
          view,
          cases = [
            {
              isValidRackId: false,
              errorMessage: Em.I18n.t('hostPopup.setRackId.invalid'),
              disablePrimary: true,
              title: 'invalid rack id'
            },
            {
              isValidRackId: true,
              errorMessage: '',
              disablePrimary: false,
              title: 'valid rack id'
            }
          ];

        cases.forEach(function (item) {

          describe(item.title, function () {

            beforeEach(function () {
              sinon.stub(validator, 'isValidRackId').returns(item.isValidRackId);
              popup = hostsUtils.setRackInfo(null, [], '');
              view = popup.get('bodyClass').create({
                parentView: popup
              });
              view.propertyDidChange('parentView.rackId');
            });

            afterEach(function () {
              validator.isValidRackId.restore();
            });

            it('isValid', function () {
              expect(view.get('isValid')).to.equal(item.isValidRackId);
            });

            it('errorMessage', function () {
              expect(view.get('errorMessage')).to.equal(item.errorMessage);
            });

            it('disablePrimary', function () {
              expect(view.get('parentView.disablePrimary')).to.equal(item.disablePrimary);
            });

          });

        });

      });

    });

    describe('#onPrimary', function () {

      var popup,
        cases = [
          {
            hosts: [],
            ajaxCallArguments: undefined,
            title: 'no hosts'
          },
          {
            hosts: [
              {
                hostName: 'h0'
              },
              {
                hostName: 'h1'
              }
            ],
            ajaxCallArguments: [{
              name: 'bulk_request.hosts.update_rack_id',
              sender: hostsUtils,
              data: {
                hostNames: 'h0,h1',
                requestInfo: 'msg',
                rackId: '/default-rack',
                hostNamesArray: ['h0', 'h1']
              },
              success: 'successRackId',
              error: 'errorRackId'
            }],
            title: 'hosts passed'
          }
        ];

      cases.forEach(function (item) {

        describe(item.title, function () {

          beforeEach(function () {
            popup = hostsUtils.setRackInfo({
              message: 'msg'
            }, item.hosts, '/default-rack');
            popup.set('hide', Em.K);
            sinon.spy(popup, 'hide');
            popup.onPrimary();
          });

          afterEach(function () {
            popup.hide.restore();
          });

          it('AJAX call', function () {
            expect(testHelpers.findAjaxRequest('name', 'bulk_request.hosts.update_rack_id')).to.eql(item.ajaxCallArguments);
          });

          it('hide popup', function () {
            expect(popup.hide.calledOnce).to.be.true;
          });

        });

      });

    });

  });

  describe('#successRackId', function () {

    var hosts = [
      Em.Object.create({
        hostName: 'h0'
      }),
      Em.Object.create({
        hostName: 'h1'
      }),
      Em.Object.create({
        hostName: 'h2'
      })
    ];

    beforeEach(function () {
      sinon.stub(App.Host, 'find').returns(hosts);
    });

    afterEach(function () {
      App.Host.find.restore();
    });

    it('should set rack ids for the proper hosts', function () {
      hostsUtils.successRackId(null, null, {
        hostNamesArray: ['h0', 'h2'],
        rackId: '/default-rack'
      });
      expect(hosts).to.eql([
        Em.Object.create({
          hostName: 'h0',
          rack: '/default-rack'
        }),
        Em.Object.create({
          hostName: 'h1'
        }),
        Em.Object.create({
          hostName: 'h2',
          rack: '/default-rack'
        })
      ]);
    });

  });

  describe('#errorRackId', function () {

    beforeEach(function () {
      sinon.stub(App, 'showAlertPopup', Em.K);
    });

    afterEach(function () {
      App.showAlertPopup.restore();
    });

    it('should show alert popup', function () {
      hostsUtils.errorRackId();
      expect(App.showAlertPopup.calledOnce).to.be.true;
    });

  });

});
