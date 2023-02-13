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
import { decode, encode } from 'js-base64';
import { isEmpty } from 'lodash';

Terminal.applyAddon(fit);
Terminal.applyAddon(attach);

export { Terminal };

export interface ITerminal extends Terminal {
  __socket: WebSocket;
  socket: WebSocket;
  __sendData: (val: string) => void;
  __getMessage: (o: Obj) => void;
  _pingInterval: NodeJS.Timeout;
  attach: (ws: WebSocket) => void;
  _onResize: () => void;
  fit: () => void;
}

const Ping = '6';
const Pong = '7';
const Input = '0'; // input code
const Output = '1'; // output code
const Error = '2'; // standard error code
const ServiceError = '3'; // service error code
const SetSize = '4';
const pingTime = 30;

function sendData(term: ITerminal, type: string, data?: string) {
  if (term.__socket) {
    term.__socket.send(type + encode(data || ''));
  }
}

function proxyInput(term: ITerminal) {
  const { __sendData } = term;
  term.off('data', __sendData);
  term.__sendData = (data) => {
    __sendData(`${Input}${encode(data || '')}`);
  };
  term.on('data', term.__sendData);
}

function proxyOutput(term: ITerminal, socket: WebSocket) {
  const { __getMessage } = term;
  socket.removeEventListener('message', __getMessage);
  term.__getMessage = (ev) => {
    let { data } = ev;

    const type = data.substr(0, 1);
    data = data.substr(1);
    data && (data = decode(data));
    switch (type) {
      case Ping:
        sendData(term, Pong);
        break;
      case Pong:
        break;
      case Output:
      case Error:
      case ServiceError:
        __getMessage({ data });
        break;
      default:
        break;
    }
  };
  socket.addEventListener('message', term.__getMessage);
}

function runTerminal(term: ITerminal, socket: WebSocket, initData?: Obj) {
  term.attach(socket);
  proxyInput(term);
  proxyOutput(term, socket);
  !isEmpty(initData) && socket.send(JSON.stringify(initData));
  term._pingInterval = setInterval(() => {
    sendData(term, Ping);
  }, pingTime * 1000);
  term.fit();
  const { cols, rows } = term;
  sendData(term, SetSize, JSON.stringify({ Width: cols * 9, Height: rows * 17 }));
}

interface IWSParams {
  url: string;
  initData?: Obj;
}
export function createTerm(container: HTMLDivElement, params: IWSParams) {
  const term = new Terminal({
    cursorBlink: true,
    cursorStyle: 'block',
    theme: {
      background: '#48515f',
      foreground: '#bbb',
    },
  }) as unknown as ITerminal;

  term.open(container, true);

  const socket = new WebSocket(params.url, 'base64.channel.k8s.io'); // sub protocol for k8s

  socket.onopen = () => runTerminal(term, socket, params.initData);

  socket.onclose = () => {
    term.writeln('\x1b[1;35mconnect is close......');
  };
  socket.onerror = () => {
    term.writeln('\x1b[1;35merror......');
  };

  term.on('resize', (size) => {
    const { cols, rows } = size;
    sendData(term, SetSize, JSON.stringify({ Width: cols * 9, Height: rows * 17 }));
    term.resize(cols, rows);
    term.fit();
  });

  let timer: NodeJS.Timeout | null = null;
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

export function destroyTerm(term: ITerminal) {
  if (term._onResize) {
    window.removeEventListener('resize', term._onResize);
  }

  if (term.__socket) {
    term.__socket.close();
  }

  if (term._pingInterval) {
    clearInterval(term._pingInterval);
  }
  term.destroy();
}
