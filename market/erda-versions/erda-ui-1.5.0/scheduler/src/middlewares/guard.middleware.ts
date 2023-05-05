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

import { Request, Response, NextFunction } from 'express';
import { logger } from '../util';

const METHODS = ['GET', 'POST', 'PUT', 'DELETE', 'PATCH', 'HEAD', 'OPTIONS'];

export function guardMiddleware(req: Request, res: Response, next: NextFunction) {
  // guard unexpected http method
  if (!METHODS.includes(req.method.toUpperCase())) {
    res.type('application/json');
    // eslint-disable-next-line no-param-reassign
    res.statusCode = 403;
    res.json({ error: 'Forbidden request method' });
  }
  next();
}
