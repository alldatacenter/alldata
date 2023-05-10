// Copyright (c) 2021 Terminus, Inc.
//
// This program is free software: you can use, redistribute, and/or modify
// it under the terms of the GNU Affero General Public License, version 3
// or later ("AGPL"), as published by the Free Software Foundation.
//
// This program is distributed in the hope that it will be useful, but WITHOUT
// ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
// FITNESS FOR A PARTICULAR PURPOSE.
//
// You should have received a copy of the GNU Affero General Public License
// along with this program. If not, see <http://www.gnu.org/licenses/>.

/* eslint-disable no-console */

interface IHandlerMap {
  [k: string]: Array<(d: any) => void>;
}

const handlerMap: IHandlerMap = {};
let tryTimes = 1;
const tryLimit = 6;

// from https://github.com/sockjs/sockjs-client/blob/e563829c546de5a3acf5cbb97c46b25e01259060/lib/utils/escape.js#L6
// eslint-disable-next-line no-misleading-character-class
const extraEscapable =
  /[\x00-\x1f\ud800-\udfff\ufffe\uffff\u0300-\u0333\u033d-\u0346\u034a-\u034c\u0350-\u0352\u0357-\u0358\u035c-\u0362\u0374\u037e\u0387\u0591-\u05af\u05c4\u0610-\u0617\u0653-\u0654\u0657-\u065b\u065d-\u065e\u06df-\u06e2\u06eb-\u06ec\u0730\u0732-\u0733\u0735-\u0736\u073a\u073d\u073f-\u0741\u0743\u0745\u0747\u07eb-\u07f1\u0951\u0958-\u095f\u09dc-\u09dd\u09df\u0a33\u0a36\u0a59-\u0a5b\u0a5e\u0b5c-\u0b5d\u0e38-\u0e39\u0f43\u0f4d\u0f52\u0f57\u0f5c\u0f69\u0f72-\u0f76\u0f78\u0f80-\u0f83\u0f93\u0f9d\u0fa2\u0fa7\u0fac\u0fb9\u1939-\u193a\u1a17\u1b6b\u1cda-\u1cdb\u1dc0-\u1dcf\u1dfc\u1dfe\u1f71\u1f73\u1f75\u1f77\u1f79\u1f7b\u1f7d\u1fbb\u1fbe\u1fc9\u1fcb\u1fd3\u1fdb\u1fe3\u1feb\u1fee-\u1fef\u1ff9\u1ffb\u1ffd\u2000-\u2001\u20d0-\u20d1\u20d4-\u20d7\u20e7-\u20e9\u2126\u212a-\u212b\u2329-\u232a\u2adc\u302b-\u302c\uaab2-\uaab3\uf900-\ufa0d\ufa10\ufa12\ufa15-\ufa1e\ufa20\ufa22\ufa25-\ufa26\ufa2a-\ufa2d\ufa30-\ufa6d\ufa70-\ufad9\ufb1d\ufb1f\ufb2a-\ufb36\ufb38-\ufb3c\ufb3e\ufb40-\ufb41\ufb43-\ufb44\ufb46-\ufb4e\ufff0-\uffff]/g;
let extraLookup: any;

// This may be quite slow, so let's delay until user actually uses bad characters.
function unrollLookup(escapable: RegExp) {
  let i;
  const unrolled = {};
  const c = [];
  for (i = 0; i < 65536; i++) {
    c.push(String.fromCharCode(i));
  }
  // eslint-disable-next-line no-param-reassign
  escapable.lastIndex = 0;
  c.join('').replace(escapable, (a) => {
    unrolled[a] = `\\u${`0000${a.charCodeAt(0).toString(16)}`.slice(-4)}`;
    return '';
  });
  // eslint-disable-next-line no-param-reassign
  escapable.lastIndex = 0;
  return unrolled;
}

function quote(str: any) {
  const quoted = JSON.stringify(str);

  // In most cases this should be very fast and good enough.
  extraEscapable.lastIndex = 0;
  if (!extraEscapable.test(quoted)) {
    return quoted;
  }

  if (!extraLookup) {
    extraLookup = unrollLookup(extraEscapable);
  }

  return quoted.replace(extraEscapable, (a) => extraLookup[a]);
}

const _randomStringChars = 'abcdefghijklmnopqrstuvwxyz012345';
function randomString(length: number) {
  const max = _randomStringChars.length;
  const bytes = new Array(length);
  for (let i = 0; i < length; i++) {
    bytes[i] = Math.floor(Math.random() * 256);
  }
  const ret = [];
  for (let i = 0; i < length; i++) {
    ret.push(_randomStringChars.substr(bytes[i] % max, 1));
  }
  return ret.join('');
}

export function connect(api: string) {
  if (!api) {
    throw new Error('ws api can not be empty');
  }
  const prefix = `${window.location.protocol.replace('http', 'ws')}//${window.location.host}`;
  const suffix = `/${Math.floor(Math.random() * 1000)}/${randomString(8)}/websocket`;
  let socket = new WebSocket(`${prefix}${api}${suffix}`); // sockjs-client style

  const originalSend = socket.send;
  socket.send = function (data) {
    originalSend.call(socket, `[${quote(data)}]`);
  };

  socket.onmessage = (e: MessageEvent) => {
    if (e) {
      const msg = e.data;
      const type = msg.slice(0, 1);
      const content = msg.slice(1);
      let payload;

      // first check for messages that don't need a payload
      switch (type) {
        case 'o':
          // mark as sockjs client is ready
          // @ts-ignore
          socket.isReady = true;
          return;
        case 'h':
          // heartbeat
          return;
        default:
      }

      if (content) {
        try {
          payload = JSON.parse(content);
        } catch (e) {
          // do nothing
        }
      }

      if (typeof payload === 'undefined') {
        return;
      }
      const callListeners = (d: string) => {
        const r = JSON.parse(d);
        (handlerMap[r.type] || []).forEach((cb) => cb(r));
      };

      switch (type) {
        case 'a':
          if (Array.isArray(payload)) {
            payload.forEach((p) => {
              callListeners(p);
            });
          }
          break;
        case 'm':
          callListeners(payload);
          break;
        case 'c':
          if (Array.isArray(payload) && payload.length === 2) {
            socket.close();
            // @ts-ignore
            socket.isReady = false;
          }
          break;
        default:
      }
    }
  };

  let timer: NodeJS.Timeout | null = null;

  const getTimer = () => {
    return setTimeout(() => {
      tryTimes += 1;
      if (tryTimes < tryLimit) {
        console.log(`try reconnecting ${tryTimes - 1} times`);
        socket = connect(api);
      }
    }, 10000 * tryTimes);
  };

  function reconnect() {
    if (this.connected) {
      console.log('disconnecting...');
      this.close();
    }
    timer = getTimer();
  }

  socket.onclose = reconnect;
  socket.onerror = reconnect;

  return socket;
}

export type WSHandler = (data: any) => void;
export const registerWSHandler = (type: string, handler: WSHandler) => {
  if (!handlerMap[type]) {
    handlerMap[type] = [];
  }
  handlerMap[type].push(handler);
};
