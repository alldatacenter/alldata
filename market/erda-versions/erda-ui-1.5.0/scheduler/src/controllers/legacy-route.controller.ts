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

import { Controller, Get, Redirect, Req } from '@nestjs/common';
import { Request } from 'express';

@Controller(':orgName')
export class LegacyRouteController {
  @Get('workBench/*')
  @Redirect('', 301)
  redirectWorkBench(@Req() req: Request) {
    return { url: req.path.replace('workBench', 'dop') };
  }

  @Get('microService/*')
  @Redirect('', 301)
  redirectMicroService(@Req() req: Request) {
    return { url: req.path.replace('microService', 'msp') };
  }

  @Get('dataCenter/*')
  @Redirect('', 301)
  redirectDataCenter(@Req() req: Request) {
    return { url: req.path.replace('dataCenter', 'cmp') };
  }

  @Get('edge/*')
  @Redirect('', 301)
  redirectEdge(@Req() req: Request) {
    return { url: req.path.replace('edge', 'ecp') };
  }
}
