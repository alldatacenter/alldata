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

import { MarkdownEditor } from 'common';
import { useUpdate } from 'common/use-hooks';
import { isEmpty } from 'lodash';
import { Button, message } from 'antd';
import React from 'react';
import { WithAuth } from 'user/common';
import i18n from 'i18n';

interface IProps {
  editAuth?: boolean;
  onSave?: (v: string) => void;
}

export const IssueCommentBox = (props: IProps) => {
  const { onSave = () => {}, editAuth } = props;

  const [stateMap, updater] = useUpdate({
    visible: false,
    content: '',
  });

  return stateMap.visible ? (
    <div>
      <div className="comment-box-markdown">
        <MarkdownEditor
          onChange={(val: any) => {
            updater.content(val);
          }}
          style={{ height: '200px' }}
          maxLength={3000}
        />
      </div>
      <div className="mt-3 btn-line-rtl">
        <Button
          type="primary"
          className="ml-3"
          onClick={() => {
            if (isEmpty(stateMap.content.trim())) {
              message.warning(i18n.t('dop:this item cannot be empty'));
              return;
            }
            onSave(stateMap.content);
            updater.content('');
            updater.visible(false);
          }}
        >
          {i18n.t('ok')}
        </Button>
        <Button type="link" onClick={() => updater.visible(false)}>
          {i18n.t('cancel')}
        </Button>
      </div>
    </div>
  ) : (
    <WithAuth pass={editAuth}>
      <Button onClick={() => updater.visible(true)}>{i18n.t('dop:add remark')}</Button>
    </WithAuth>
  );
};
