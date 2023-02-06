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

import { Terminal } from 'xterm';
import * as attach from 'xterm/lib/addons/attach/attach';
import * as fit from 'xterm/lib/addons/fit/fit';

Terminal.applyAddon(fit);
Terminal.applyAddon(attach);

const Ping = '2';
const Pong = '3';
const Input = '4';
const Output = '5';
const SetSize = '8';
const pingTime = 30; // ping间隔，单位秒

// support IE 11 or Edge < 79
function TextEncoder() {}
function TextDecoder() {}
TextEncoder.prototype.encode = function (s) {
  const e = new Uint8Array(s.length);

  for (let i = 0; i < s.length; i += 1) {
    e[i] = s.charCodeAt(i);
  }

  return e;
};

TextDecoder.prototype.decode = function (arr) {
  let d = '';

  for (let i = 0; i < arr.length; i += 1) {
    d += String.fromCharCode(arr[i]);
  }

  return d;
};
if (!window.TextEncoder) {
  window.TextDecoder = TextEncoder;
  window.TextDecoder = TextDecoder;
}
const encoder = new window.TextEncoder();
const decoder = new window.TextDecoder('utf-8');

function sendData(term, type, data) {
  if (term.__socket) {
    term.__socket.send(encoder.encode(type + (data || '')));
  }
}

function proxyInput(term) {
  const { __sendData } = term;
  term.off('data', __sendData);
  term.__sendData = (data) => {
    __sendData(encoder.encode(`${Input}${data}`));
  };
  term.on('data', term.__sendData);
}

function proxyOutput(term, socket) {
  const { __getMessage } = term;
  socket.removeEventListener('message', __getMessage);
  term.__getMessage = (ev) => {
    let { data } = ev;
    data = decoder.decode(data);
    const type = data.substr(0, 1);

    switch (type) {
      case Ping:
        sendData(term, Pong);
        break;
      case Pong:
        break;
      case Output:
        data = data.substr(1);
        __getMessage({ data });
        break;
      default:
        break;
    }
  };
  socket.addEventListener('message', term.__getMessage);
}

function runTerminal(term, socket, initData) {
  term.attach(socket);
  proxyInput(term);
  proxyOutput(term, socket);
  socket.send(JSON.stringify(initData));

  term._pingInterval = setInterval(() => {
    sendData(term, Ping);
  }, pingTime * 1000);
  term.fit();
  const { cols, rows } = term;
  sendData(term, SetSize, JSON.stringify({ cols, rows }));
}

export function createTerm(container, params = {}) {
  const term = new Terminal({
    cursorBlink: true,
    cursorStyle: 'block',
    theme: {
      background: '#48515f',
      foreground: '#bbb',
    },
  });

  term.open(container, true);
  // const { rows, cols } = term.proposeGeometry();
  // const protocol = (window.location.protocol === 'https:') ? 'wss://' : 'ws://';
  // const url = `${protocol}${window.location.hostname}${window.location.port ? `:${window.location.port}` : ''}`;
  // const partParams = Object.keys(params).reduce((all, k) => `${all}&${k}=${params[k]}`, '');
  const socket = new WebSocket(params.url);
  socket.binaryType = 'arraybuffer';
  socket.onopen = () => runTerminal(term, socket, params.initData);
  socket.onclose = () => {
    term.writeln('\x1b[1;35mconnect is close......');
  };
  socket.onerror = () => {
    term.writeln('\x1b[1;35merror......');
  };

  term.on('resize', (size) => {
    const { cols, rows } = size;
    sendData(term, SetSize, JSON.stringify({ cols, rows }));
    term.resize(cols, rows);
    term.fit();
  });

  let timer;
  term._onResize = () => {
    if (timer) clearTimeout(timer);
    timer = setTimeout(() => {
      term.fit();
      term.scrollToBottom();
    }, 500);
  };
  window.addEventListener('resize', term._onResize);
  return term;
}

export function destroyTerm(term) {
  if (term._onResize) {
    window.removeEventListener('resize', term._onResize);
  }
  if (term.socket) {
    term.socket.close();
  }
  if (term._pingInterval) {
    clearInterval(term._pingInterval);
  }
  term.destroy();
}
