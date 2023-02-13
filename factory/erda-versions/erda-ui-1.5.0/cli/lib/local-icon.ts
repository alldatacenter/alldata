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

import fs from 'fs';
import agent from 'superagent';
import path from 'path';
import { getPublicDir } from './util/env';
import { logSuccess, logError } from './util/log';

const downloadFile = (url: string, savePath: string) => {
  return agent.get(`https:${url.replace('https:', '')}`).then((res) => {
    let data = '';
    // @ts-ignore api mistake
    if (res.request.url.match(/\.js$/)) {
      // 请求iconfont js 返回的对象中text为undefined，取body;
      data = res.body.toString('utf8');
    } else {
      data = res.text;
    }
    // Check content to avoid network operators hijacking
    if (
      res.status === 200 &&
      (data.startsWith('@font-face') || data.includes('<symbol') || data.startsWith('(function(){window.__iconpark__'))
    ) {
      fs.writeFile(savePath, data, (e) => {
        if (e) {
          logError('write iconfont file failed！', e);
        }
      });
    } else {
      throw new Error(`Got error when downloading ${url}`);
    }
  });
};

const LocalIcon = () => {
  const htmlPath = path.resolve(getPublicDir(), './static/shell/index.html');
  fs.readFile(htmlPath, 'utf8', (err, content) => {
    if (err) logError('read index.html failed');
    const iconfontRegex = /\/\/at.alicdn.com\/t\/(([^.]+)\.(css|js))/g;
    const iconparkRegex = /https:\/\/lf1-cdn-tos\.bytegoofy\.com\/obj\/iconpark\/(([^"']+)\.js)/g;
    let matchedIconfontFile = iconfontRegex.exec(content);
    let replacedContent = content;
    while (matchedIconfontFile) {
      const iconfontFilePath = matchedIconfontFile[0];
      logSuccess('get matched iconfont file', iconfontFilePath);
      downloadFile(iconfontFilePath, path.resolve(getPublicDir(), `./static/${matchedIconfontFile[1]}`));
      replacedContent = replacedContent.replace(iconfontFilePath, `/static/${matchedIconfontFile[1]}`);
      matchedIconfontFile = iconfontRegex.exec(replacedContent);
    }
    let matchedIconparkFile = iconparkRegex.exec(replacedContent);
    while (matchedIconparkFile) {
      const iconparkFilePath = matchedIconparkFile[0];
      logSuccess('get matched iconpark file', iconparkFilePath);
      downloadFile(iconparkFilePath, path.resolve(getPublicDir(), `./static/${matchedIconparkFile[1]}`));
      replacedContent = replacedContent.replace(iconparkFilePath, `/static/${matchedIconparkFile[1]}`);
      matchedIconparkFile = iconparkRegex.exec(replacedContent);
    }

    fs.writeFile(htmlPath, replacedContent, (e) => {
      if (e) {
        logError('rewrite index.html failed！', e);
      } else {
        logSuccess('rewrite index.html completed');
      }
    });
  });
};

export default LocalIcon;
