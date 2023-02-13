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

import { NestFactory } from '@nestjs/core';
import { AppModule } from './app.module';
import helmet from 'helmet';
import { NotFoundExceptionFilter } from './filters/not-found.filter';
import { AllExceptionsFilter } from './filters/error-catch.filter';
import compression from 'compression';
import { logger, getDirectories, getEnv, getHttpsOptions } from './util';
import { createProxyService } from './proxy';
import { guardMiddleware } from './middlewares/guard.middleware';

const isProd = process.env.NODE_ENV === 'production';

const { staticDir, envConfig } = getEnv();
const { MODULES, SCHEDULER_URL, SCHEDULER_PORT } = envConfig;

if (!isProd) {
  const modules: { [k: string]: boolean } = {};
  getDirectories(staticDir).forEach((m) => {
    modules[m] = true;
  });

  MODULES.split(',').forEach((m) => {
    if (!modules[m]) {
      logger.warn(`module:【${m}】have not build to public`);
    }
  });

  logger.info(`Exist static modules: ${Object.keys(modules)}`);
}

async function bootstrap() {
  const app = await NestFactory.create(AppModule, {
    httpsOptions: isProd ? undefined : getHttpsOptions(),
  });
  app.use(helmet({ contentSecurityPolicy: false, referrerPolicy: false }));
  app.useGlobalFilters(new AllExceptionsFilter());
  app.useGlobalFilters(new NotFoundExceptionFilter());
  if (isProd) {
    app.use(compression()); // gzip
  }
  app.use(guardMiddleware); // must create global guard middleware here, otherwise the proxy middleware will ignore the guard
  const wsProxy = createProxyService(app);

  const server = await app.listen(isProd ? 80 : SCHEDULER_PORT);
  server.on('upgrade', wsProxy.upgrade);

  if (isProd) {
    logger.info('erda ui server started at port 80');
  } else {
    logger.info(`server started at ${SCHEDULER_URL}:${SCHEDULER_PORT || 3000}`);
  }
}

bootstrap();
