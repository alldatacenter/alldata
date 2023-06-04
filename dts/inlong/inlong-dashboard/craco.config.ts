/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import CracoLess from 'craco-less';
import { CracoAliasPlugin } from 'react-app-alias';
import type { CracoConfig } from 'craco__craco';
import dotenv from 'dotenv';

dotenv.config({ path: '.env.local' });
dotenv.config();

const config: CracoConfig = {
  plugins: [
    {
      plugin: CracoLess,
      options: {
        lessLoaderOptions: {
          lessOptions: {
            javascriptEnabled: true,
          },
        },
      },
    },
    {
      plugin: CracoAliasPlugin,
      options: {
        source: 'tsconfig',
        baseUrl: './',
        tsConfigPath: './tsconfig.json',
      },
    },
  ],
  webpack: {
    configure: webpackConfig => {
      if (webpackConfig.resolve?.extensions && process.env.INLONG_ENV) {
        const tsIndex = webpackConfig.resolve.extensions.findIndex(item => item === '.ts');
        webpackConfig.resolve.extensions.splice(tsIndex, 0, `.${process.env.INLONG_ENV}.ts`);
      }
      return webpackConfig;
    },
  },
};

export default config;
