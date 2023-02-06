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
import moment from 'moment';

Terminal.applyAddon(fit);
Terminal.applyAddon(attach);

export { Terminal };

const SetSize = '4';
const pingTime = 30;
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

function sendData(term: ITerminal, type: string, data?: string) {
  if (term.__socket) {
    term.__socket.send(type + encode(data || ''));
  }
}

function proxyOutput(term: ITerminal, socket: WebSocket) {
  const { __getMessage } = term;
  socket.removeEventListener('message', __getMessage);

  const timeZoneReg = /^\d[\d-]+T[\d:.]+Z/;

  term.__getMessage = (ev) => {
    let { data } = ev;
    data = data && decode(data);
    const timeRegResult = timeZoneReg.exec(data);
    if (timeRegResult) {
      data = `${moment(timeRegResult[0]).format('YYYY-MM-DD HH:mm:ss')} ${data.split(timeRegResult[0])?.[1]}`;
    }
    __getMessage({ data: `\r${data}\n` });
  };
  socket.addEventListener('message', term.__getMessage);
}

function runTerminal(term: ITerminal, socket: WebSocket) {
  term.attach(socket);
  proxyOutput(term, socket);
  term._pingInterval = setInterval(() => {
    sendData(term, '');
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

  const socket = new WebSocket(params.url, 'base64.binary.k8s.io'); // sub protocol for k8s
  socket.onopen = () => runTerminal(term, socket);

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
