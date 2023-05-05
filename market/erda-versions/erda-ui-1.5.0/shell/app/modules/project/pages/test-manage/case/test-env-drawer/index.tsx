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

import React from 'react';
import i18n from 'i18n';
import { Drawer } from 'antd';
import { AutoTestEnv, ManualTestEnv } from '../../../test-env/test-env';
import testEnvStore from 'project/stores/test-env';

interface IProps {
  testType: 'manual' | 'auto';
}
const TestEnvDrawer = (props: IProps) => {
  const { testType = 'manual' } = props;
  const { envID, envType } = testEnvStore.useStore((s) => s.envInfo);
  return (
    <Drawer
      destroyOnClose
      title={`${i18n.t('runtime:environment variable configs')}（#${envID}）`}
      width="50%"
      visible={!!envID}
      onClose={testEnvStore.closeEnvVariable}
    >
      {testType === 'manual' ? (
        <ManualTestEnv envID={+envID} envType={envType} isSingle />
      ) : (
        <AutoTestEnv envID={+envID} envType={envType} isSingle />
      )}
    </Drawer>
  );
};

export default TestEnvDrawer;
