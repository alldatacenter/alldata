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

var stompClientClass = require('utils/stomp_client');

describe('App.StompClient', function () {
  var stomp, mockStomp;

  beforeEach(function() {
    stomp = stompClientClass.create();
    mockStomp = sinon.stub(Stomp, 'over').returns({connect: Em.K});
  });
  afterEach(function() {
    Stomp.over.restore();
  });

  describe('#connect', function() {
    beforeEach(function() {
      sinon.stub(stomp, 'onConnectionSuccess');
      sinon.stub(stomp, 'onConnectionError');
      sinon.stub(stomp, 'getSocket');
    });
    afterEach(function() {
      stomp.onConnectionSuccess.restore();
      stomp.onConnectionError.restore();
      stomp.getSocket.restore();
    });

    it('onConnectionSuccess should be called', function() {
      mockStomp.returns({connect: function(headers, success, error) {
        success();
      }});
      stomp.connect();
      expect(stomp.onConnectionSuccess.calledOnce).to.be.true;
    });
    it('onConnectionError should be called', function() {
      mockStomp.returns({connect: function(headers, success, error) {
        error();
      }});
      stomp.connect();
      expect(stomp.onConnectionError.calledOnce).to.be.true;
    });
    it('should set client', function() {
      stomp.connect();
      expect(stomp.get('client')).to.be.eql({
        connect: Em.K,
        debug: Em.K
      });
    });
  });

  describe('#getSocket', function() {
    beforeEach(function () {
      sinon.stub(stomp, 'getHostName').returns('test');
      sinon.stub(stomp, 'isSecure').returns(false);
    });
    afterEach(function () {
      stomp.getHostName.restore();
      stomp.isSecure.restore();
      stomp.getPort.restore();
    });
    it('should return WebSocket instance', function() {
      sinon.stub(stomp, 'getPort').returns(':8080');
      expect(stomp.getSocket().URL).to.be.equal('ws://test:8080/api/stomp/v1/websocket');
    });
    it('should return SockJS instance', function() {
      sinon.stub(stomp, 'getPort').returns('');
      expect(stomp.getSocket(true).url).to.be.equal('http://test/api/stomp/v1');
    });
  });

  describe('#getSocketUrl', function() {
    beforeEach(function () {
      sinon.stub(stomp, 'getHostName').returns('test');
      sinon.stub(stomp, 'isSecure').returns(false);
    });
    afterEach(function () {
      stomp.getHostName.restore();
      stomp.isSecure.restore();
      stomp.getPort.restore();
    });
    it('should return socket url for websocket', function() {
      sinon.stub(stomp, 'getPort').returns(':8080');
      expect(stomp.getSocketUrl('{protocol}://{hostname}{port}', true)).to.equal('ws://test:8080');
    });
    it('should return socket url for sockjs', function() {
      sinon.stub(stomp, 'getPort').returns('');
      expect(stomp.getSocketUrl('{protocol}://{hostname}{port}', false)).to.equal('http://test');
    });
  });

  describe('#onConnectionSuccess', function() {
    it('isConnected should be true', function() {
      stomp.onConnectionSuccess();
      expect(stomp.get('isConnected')).to.be.true;
    });
  });

  describe('#onConnectionError', function() {
    beforeEach(function() {
      sinon.stub(stomp, 'reconnect');
      sinon.stub(stomp, 'connect');
    });
    afterEach(function() {
      stomp.reconnect.restore();
      stomp.connect.restore();
    });

    it('reconnect should be called when isConnected true', function() {
      stomp.set('isConnected', true);
      stomp.onConnectionError();
      expect(stomp.reconnect.calledOnce).to.be.true;
    });

    it('connect should be called when isConnected false', function() {
      stomp.set('isConnected', false);
      stomp.onConnectionError();
      expect(stomp.connect.calledOnce).to.be.true;
    });

    it('connect should not be called when isConnected false and useSockJS true', function() {
      stomp.set('isConnected', false);
      stomp.onConnectionError(true);
      expect(stomp.connect.called).to.be.false;
    });
  });

  describe('#reconnect', function() {
    beforeEach(function() {
      sinon.stub(stomp, 'connect').returns({done: Em.clb});
      sinon.stub(stomp, 'subscribe');
      this.clock = sinon.useFakeTimers();
    });
    afterEach(function() {
      stomp.connect.restore();
      stomp.subscribe.restore();
      this.clock.restore();
    });

    it('should connect and restore subscriptions', function() {
      var subscriptions = {
        'foo': {
          destination: 'foo',
          handlers: { default: Em.K }
        }
      };
      stomp.set('subscriptions', subscriptions);
      stomp.reconnect();
      this.clock.tick(stomp.RECONNECT_TIMEOUT);
      expect(stomp.subscribe.calledWith('foo', Em.K)).to.be.true;
    });
  });

  describe('#disconnect', function() {
    var client = {
      disconnect: sinon.spy()
    };

    it('disconnect should be called', function() {
      stomp.set('client', client);
      stomp.disconnect();
      expect(client.disconnect.calledOnce).to.be.true;
    });
  });

  describe('#send', function() {
    it('send should not be called', function() {
      var client = {connected: false, send: sinon.spy()};
      stomp.set('client', client);
      expect(stomp.send()).to.be.false;
      expect(client.send.called).to.be.false;
    });
    it('send should be called', function() {
      var client = {connected: true, send: sinon.spy()};
      stomp.set('client', client);
      expect(stomp.send('test', 'body', {})).to.be.true;
      expect(client.send.calledWith('test', {}, 'body')).to.be.true;
    });
  });

  describe('#subscribe', function() {
    it('should not subscribe when client disconnected', function() {
      var client = {connected: false};
      stomp.set('client', client);
      expect(stomp.subscribe('foo')).to.be.null;
      expect(stomp.get('subscriptions')).to.be.empty;
    });
    it('should not subscribe when subscription already exist', function() {
      stomp.set('client', {connected: true});
      stomp.set('subscriptions', {
        'foo': {}
      });
      expect(stomp.subscribe('foo')).to.be.eql({});
    });
    it('should subscribe when client connected', function() {
      var client = {
        connected: true,
        subscribe: sinon.stub().returns({id: 1})
      };
      stomp.set('client', client);
      expect(stomp.subscribe('foo')).to.be.eql({
        handlers: { default: Em.K },
        destination: 'foo',
        id: 1
      });
      expect(stomp.get('subscriptions')['foo']).to.be.eql({
        handlers: { default: Em.K },
        destination: 'foo',
        id: 1
      });
    });
  });

  describe('#addHandler', function() {
    beforeEach(function() {
      sinon.stub(stomp, 'subscribe', function(dest) {
        stomp.get('subscriptions')[dest] = {
          handlers: {}
        };
      });
    });
    afterEach(function() {
      stomp.subscribe.restore();
    });

    it('should add handler and subscribe because there is no subscription', function() {
      stomp.addHandler('dest1', 'handler1', Em.K);
      expect(stomp.subscribe.calledWith('dest1')).to.be.true;
      expect(stomp.get('subscriptions')['dest1'].handlers).to.be.eql({handler1: Em.K});
    });
    it('should add handler', function() {
      stomp.get('subscriptions')['dest2'] = {
        handlers: {}
      };
      stomp.addHandler('dest2', 'handler2', Em.K);
      expect(stomp.get('subscriptions')['dest2'].handlers).to.be.eql({handler2: Em.K});
    });
  });

  describe('#removeHandler', function() {
    beforeEach(function() {
      sinon.stub(stomp, 'unsubscribe');
    });
    afterEach(function() {
      stomp.unsubscribe.restore();
    });

    it('should remove handler', function() {
      stomp.get('subscriptions')['dest1'] = {
        handlers: {
          handler1: Em.K
        }
      };
      stomp.removeHandler('dest1', 'handler1');
      expect(stomp.get('subscriptions')['dest1'].handlers).to.be.empty;
      expect(stomp.unsubscribe.calledOnce).to.be.true;
    });
  });

  describe('#unsubscribe', function() {
    it('should not unsubscribe when no subscription found', function() {
      stomp.set('subscriptions', {});
      expect(stomp.unsubscribe('foo')).to.be.false;
    });
    it('should unsubscribe when subscription found', function() {
      var subscriptions = {
        'foo': {
          unsubscribe: sinon.spy()
        }
      };
      stomp.set('subscriptions', subscriptions);
      expect(stomp.unsubscribe('foo')).to.be.true;
      expect(stomp.get('subscriptions')).to.be.empty;
    });
  });
});
