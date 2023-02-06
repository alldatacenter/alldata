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

import { ExceptionFilter, Catch, NotFoundException, HttpException, ArgumentsHost } from '@nestjs/common';
import { Request, Response } from 'express';
import path from 'path';
import fs from 'fs';
import { getEnv } from '../util';

const { staticDir } = getEnv();
const indexHtmlPath = path.join(staticDir, 'shell', 'index.html');
if (!fs.existsSync(indexHtmlPath)) {
  throw Error('You should build shell first before start scheduler');
}
const indexHtmlContent = fs.readFileSync(indexHtmlPath, { encoding: 'utf8' });
const newIndexHtmlPath = path.join(staticDir, 'shell', 'index-new.html');

const {
  UC_PUBLIC_URL = '',
  ENABLE_BIGDATA = '',
  ENABLE_EDGE = '',
  TERMINUS_KEY = '',
  TERMINUS_TA_ENABLE = false,
  TERMINUS_TA_URL = '',
  TERMINUS_TA_COLLECTOR_URL = '',
} = process.env;

let newContent = indexHtmlContent.replace(
  '<!-- $ -->',
  `<script>window.erdaEnv={UC_PUBLIC_URL:"${UC_PUBLIC_URL}",ENABLE_BIGDATA:"${ENABLE_BIGDATA}",ENABLE_EDGE:"${ENABLE_EDGE}"}</script>`,
);
if (TERMINUS_TA_ENABLE) {
  const taContent = `
<script>
!function(e,n,r,t,a,o,c){e[a]=e[a]||function(){(e[a].q=e[a].q||[]).push(arguments)},e.onerror=function(n,r,t,o,c){e[a]("sendExecError",n,r,t,o,c)},n.addEventListener("error",function(n){e[a]("sendError",n)},!0),o=n.createElement(r),c=n.getElementsByTagName(r)[0],o.async=1,o.src=t,c.parentNode.insertBefore(o,c)}(window,document,"script","${TERMINUS_TA_URL}","$ta");
$ta('start', { udata: { uid: 0 }, ak: "${TERMINUS_KEY}", url: "${TERMINUS_TA_COLLECTOR_URL}", ck: true });
</script>
`;
  newContent = newContent.replace('<!-- $ta -->', taContent);
}
fs.writeFileSync(newIndexHtmlPath, newContent, { encoding: 'utf8' });

@Catch(NotFoundException)
export class NotFoundExceptionFilter implements ExceptionFilter {
  // same action as nginx try_files
  catch(_exception: HttpException, host: ArgumentsHost) {
    const ctx = host.switchToHttp();
    const request = ctx.getRequest<Request>();
    const response = ctx.getResponse<Response>();
    const extension = path.extname(request.path);
    if (!extension || request.path.match(/^\/\w+\/dop\/projects\/\d+\/apps\/\d+\/repo/)) {
      response.setHeader('cache-control', 'no-store');
      if (process.env.DEV) {
        if (!fs.existsSync(newIndexHtmlPath)) {
          fs.writeFileSync(newIndexHtmlPath, newContent, { encoding: 'utf8' });
        }
      }
      response.sendFile(newIndexHtmlPath);
    } else {
      response.statusCode = 404;
      response.end('Not Found');
    }
  }
}
