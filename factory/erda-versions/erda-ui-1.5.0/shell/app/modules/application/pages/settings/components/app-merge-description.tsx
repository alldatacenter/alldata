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
import { Button, Input } from 'antd';
import i18n from 'i18n';

import './app-merge-description.scss';

const { TextArea } = Input;

export const MergeDes = () => {
  const placeholder = `### MR ${i18n.t('type')}
  - Bugfix (non-breaking change)
  - New feature (non-breaking change, ${i18n.t('dop:just add new features')})
  - Breaking change (${i18n.t('dop:Modified or added features affect the original function.')})
  - Refactor (non-breaking change)、

  ### ${i18n.t('dop:what did this MR do?')}？
  > ${i18n.t('dop:describe this mr here')}

  ### ${i18n.t('dop:self-test situation')}
  > ${i18n.t('dop:describe the situation of manual testing here')}
  > ${i18n.t('dop:For example, what other components are depended on, and what circumstances are not covered, etc.')}

  ### ${i18n.t('dop:mr-tpl-unit-test')}
  > 1. ${i18n.t('dop:If it is a bugfix, please add a test case proving that the bug is fixed.')}
  > 2. ${i18n.t('dop:local execution results of dev ut')}

  ### ${i18n.t('dop:integration testing')}

  ### ${i18n.t('dop:modification of dice.yml')}
  > ${i18n.t('dop:update-dice-yml-env')}`;

  return (
    <div className="merge-des">
      <Button disabled>{i18n.t('edit')}</Button>
      <div className="des-template">
        <div className="title">{i18n.t('dop:description template')}</div>
        <TextArea disabled placeholder={placeholder} autoSize />
      </div>
    </div>
  );
};
