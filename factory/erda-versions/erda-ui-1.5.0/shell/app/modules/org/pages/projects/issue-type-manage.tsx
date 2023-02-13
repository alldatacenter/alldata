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
import { SectionInfoEdit } from 'project/common/components/section-info-edit';
import i18n from 'i18n';
import issueFieldStore from 'org/stores/issue-field';
import { map, keys } from 'lodash';
import { IssueIcon } from 'project/common/components/issue/issue-icon';
import { ISSUE_LIST_MAP } from 'org/common/config';
import { IssueFieldSettingModal } from 'org/pages/projects/issue-field-setting-modal';
import { useUpdate } from 'common/use-hooks';
import { useMount } from 'react-use';
import orgStore from 'app/org-home/stores/org';

const IssueTypeManage = () => {
  const [issueTimeMap] = issueFieldStore.useStore((s) => [s.issueTimeMap]);
  const { getIssueTime, getFieldsByIssue } = issueFieldStore.effects;
  const { clearFieldList } = issueFieldStore.reducers;
  const { id: orgID } = orgStore.useStore((s) => s.currentOrg);

  useMount(() => {
    getIssueTime({ orgID });
  });

  const [{ modalVisible, issueType }, updater, update] = useUpdate({
    modalVisible: false,
    issueType: 'COMMON' as ISSUE_FIELD.IIssueType,
  });

  const list = React.useMemo(() => {
    return map(keys(ISSUE_LIST_MAP), (t) => {
      return {
        ...ISSUE_LIST_MAP[t],
        updatedTime: issueTimeMap[t],
      };
    });
  }, [issueTimeMap]);

  const formData = {};
  const fieldsList: object[] = [];

  const onClose = () => {
    getIssueTime({ orgID });
    updater.modalVisible(false);
    clearFieldList();
  };

  const readonlyForm = (
    <div>
      {map(list, (item) => {
        return (
          <div
            className="panel hover-active-bg"
            key={item.type}
            onClick={() => {
              getFieldsByIssue({ propertyIssueType: item.type, orgID });
              update({
                modalVisible: true,
                issueType: item.type as ISSUE_FIELD.IIssueType,
              });
            }}
          >
            <div className="common-list-item">
              <div className="list-item-left">
                <div className="flex justify-between items-center">
                  <IssueIcon type={item.type} withName />
                </div>
                <div className="sub">
                  <span>{i18n.t('update time')}：</span>
                  <span>{item.updatedTime || i18n.t('dop:not modified')}</span>
                </div>
              </div>
            </div>
          </div>
        );
      })}
      <IssueFieldSettingModal visible={modalVisible} issueType={issueType} closeModal={onClose} />
    </div>
  );

  return (
    <SectionInfoEdit
      hasAuth={false}
      data={formData}
      readonlyForm={readonlyForm}
      fieldsList={fieldsList}
      updateInfo={getIssueTime}
      name={i18n.t('dop:custom config of issue type')}
      desc={i18n.t(
        'dop:mainly manage the custom configuration of issue field attributes and saved as the issue template configuration of the entire organization-level project.',
      )}
    />
  );
};

export default IssueTypeManage;
