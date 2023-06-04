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
import { PageLoading } from '@ant-design/pro-layout';
import i18n from '@/i18n';
import Provider from '@/ui/components/Provider';
import Layout from '@/ui/components/Layout';
import { message } from 'antd';
import type { SuccessResponse } from '@/core/utils/request';

const conf = {
  title: '',
  logo: '/logo.svg',
  useLogin: true,
  redirectRoutes: {
    '/': '/group',
  } as Record<string, string>,
  loginUrl: `${window.location.origin}/#/${i18n?.language || ''}/login`,
  AppProvider: Provider,
  AppLoading: PageLoading,
  AppLayout: Layout,
  requestPrefix: '/inlong/manager/api',
  requestErrorAlert: (msg: string) => message.error(msg),
  responseParse: (res: any): SuccessResponse => res,
};

export default conf;
