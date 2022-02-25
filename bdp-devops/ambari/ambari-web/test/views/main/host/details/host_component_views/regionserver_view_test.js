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
require('views/main/host/details/host_component_views/regionserver_view');

describe('App.RegionServerComponentView', function () {
  var view;

  beforeEach(function () {
    view = App.RegionServerComponentView.create();
  });

  describe('#setDesiredAdminStateDefault()', function () {
    beforeEach(function () {
      sinon.stub(view, 'setStatusAs', Em.K);
    });
    afterEach(function () {
      view.setStatusAs.restore();
    });
    it('INSERVICE state', function () {
      view.setDesiredAdminStateDefault('INSERVICE');
      expect(view.setStatusAs.calledWith('INSERVICE')).to.be.true;
    });
    it('DECOMMISSIONED state', function () {
      view.setDesiredAdminStateDefault('DECOMMISSIONED');
      expect(view.setStatusAs.calledWith('RS_DECOMMISSIONED')).to.be.true;
    });
  });

  describe('#parseRegionServersHosts()', function () {
    var cases = [
      {
        input: undefined,
        result: [],
        title: 'undefined input'
      },
      {
        input: '',
        result: [],
        title: 'empty string'
      },
      {
        input: 'host,1,2',
        result: ['host'],
        title: 'single host string'
      },
      {
        input: 'host1,1,2;host2,3,4',
        result: ['host1', 'host2'],
        title: 'multiple hosts string'
      }
    ];

    cases.forEach(function (test) {
      it(test.title, function () {
        expect(view.parseRegionServersHosts(test.input)).to.eql(test.result);
      });
    });
  });

  describe('#getRSDecommissionStatusSuccessCallback()', function () {
    var cases = [
      {
        data: null,
        desiredAdminState: 'INSERVICE',
        resultingState: 'INSERVICE',
        title: 'no data, INSERVICE desired state'
      },
      {
        data: null,
        desiredAdminState: 'DECOMMISSIONED',
        resultingState: 'RS_DECOMMISSIONED',
        title: 'no data, DECOMMISSIONED desired state'
      },
      {
        data: {},
        desiredAdminState: 'INSERVICE',
        resultingState: 'INSERVICE',
        title: 'empty data, INSERVICE desired state'
      },
      {
        data: {},
        desiredAdminState: 'DECOMMISSIONED',
        resultingState: 'RS_DECOMMISSIONED',
        title: 'empty data, DECOMMISSIONED desired state'
      },
      {
        data: {
          items: []
        },
        desiredAdminState: 'INSERVICE',
        resultingState: 'INSERVICE',
        title: 'empty items array, INSERVICE desired state'
      },
      {
        data: {
          items: []
        },
        desiredAdminState: 'DECOMMISSIONED',
        resultingState: 'RS_DECOMMISSIONED',
        title: 'empty items array, DECOMMISSIONED desired state'
      },
      {
        data: {
          items: [
            {
              metrics: {
                hbase: {
                  master: {}
                }
              }
            }
          ]
        },
        desiredAdminState: 'INSERVICE',
        resultingState: 'INSERVICE',
        title: 'no live and dead RS hosts provided, INSERVICE desired state'
      },
      {
        data: {
          items: [
            {
              metrics: {
                hbase: {
                  master: {}
                }
              }
            }
          ]
        },
        desiredAdminState: 'DECOMMISSIONED',
        resultingState: 'RS_DECOMMISSIONED',
        title: 'no live and dead RS hosts provided, DECOMMISSIONED desired state'
      },
      {
        data: {
          items: [
            {
              metrics: {
                hbase: {
                  master: {
                    liveRegionServersHosts: 'h0,1,2;h1,3,4'
                  }
                }
              }
            }
          ]
        },
        desiredAdminState: 'INSERVICE',
        resultingState: 'INSERVICE',
        title: 'RS is live, INSERVICE desired state'
      },
      {
        data: {
          items: [
            {
              metrics: {
                hbase: {
                  master: {
                    liveRegionServersHosts: 'h0,1,2;h1,3,4'
                  }
                }
              }
            }
          ]
        },
        desiredAdminState: 'DECOMMISSIONED',
        resultingState: 'INSERVICE',
        title: 'RS is live, DECOMMISSIONED desired state'
      },
      {
        data: {
          items: [
            {
              metrics: {
                hbase: {
                  master: {
                    deadRegionServersHosts: 'h0,1,2;h1,3,4'
                  }
                }
              }
            }
          ]
        },
        desiredAdminState: 'INSERVICE',
        resultingState: 'RS_DECOMMISSIONED',
        title: 'RS is dead, INSERVICE desired state'
      },
      {
        data: {
          items: [
            {
              metrics: {
                hbase: {
                  master: {
                    deadRegionServersHosts: 'h0,1,2;h1,3,4'
                  }
                }
              }
            }
          ]
        },
        desiredAdminState: 'DECOMMISSIONED',
        resultingState: 'RS_DECOMMISSIONED',
        title: 'RS is dead, DECOMMISSIONED desired state'
      }
    ];

    beforeEach(function () {
      sinon.stub(view, 'setStatusAs', Em.K);
      sinon.stub(view, 'startBlinking', Em.K);
      view.set('content', {
        hostName: 'h0'
      });
    });

    afterEach(function () {
      view.setStatusAs.restore();
      view.startBlinking.restore();
    });

    cases.forEach(function (test) {
      it(test.title, function () {
        view.getRSDecommissionStatusSuccessCallback(test.data, null, {
          desiredAdminState: test.desiredAdminState
        });
        expect(view.setStatusAs.calledWith(test.resultingState)).to.be.true;
      });
    });
  });
});
