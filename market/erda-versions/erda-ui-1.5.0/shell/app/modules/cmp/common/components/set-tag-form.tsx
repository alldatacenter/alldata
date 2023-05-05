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

import { insertWhen } from 'common/utils';
import i18n from 'i18n';
import React from 'react';
import { useEffectOnce } from 'react-use';
import { FormModal } from 'common';
import { useUpdate } from 'common/use-hooks';
import clusterStore from 'cmp/stores/cluster';
import { map } from 'lodash';
import cloudCommonStore from 'cmp/stores/cloud-common';
import { getProjectList } from 'project/services/project';
import orgStore from 'app/org-home/stores/org';

interface ISetTagFromProps {
  visible: boolean;
  formData?: {
    tags: string[];
    projects?: string[];
  } | null;
  items: CLOUD.TagItem[];
  resourceType: CLOUD.SetTagType;
  instanceID?: string;
  showClustertLabel?: boolean;
  showProjectLabel?: boolean;
  onCancel: () => void;
  afterSubmit?: (res?: any) => void;
}
export const SetTagForm = ({
  visible,
  onCancel,
  items,
  formData,
  showClustertLabel = true,
  showProjectLabel = false,
  resourceType,
  instanceID,
  afterSubmit,
}: ISetTagFromProps) => {
  const [{ projectList }, updater] = useUpdate({
    projectList: [],
  });
  const clusterList = clusterStore.useStore((s) => s.list);
  const orgId = orgStore.getState((s) => s.currentOrg.id);

  useEffectOnce(() => {
    !clusterList.length && clusterStore.effects.getClusterList();
    (getProjectList({ orgId, pageNo: 1, pageSize: 100 }) as any).then((res: any) => {
      updater.projectList(res.data.list);
    });
  });

  const onOk = ({ tags = [], projects = [] }: any) => {
    cloudCommonStore
      .setCloudResourceTags({
        tags: tags.concat(projects),
        items,
        resourceType,
        instanceID,
      })
      .then((res) => {
        afterSubmit && afterSubmit(res);
      });
    onCancel();
  };

  const tagFields = [
    ...insertWhen(showClustertLabel, [
      {
        label: i18n.t('label'),
        name: 'tags',
        required: false,
        type: 'select',
        options: map(clusterList, (item) => ({
          name: `dice-cluster/${item.name}`,
          value: `dice-cluster/${item.name}`,
        })),
        itemProps: {
          mode: 'multiple',
        },
      },
    ]),
    ...insertWhen(showProjectLabel, [
      {
        label: i18n.t('cmp:project label'),
        name: 'projects',
        required: false,
        type: 'select',
        options: map(projectList, (item) => ({
          name: `dice-project/${item.name}`,
          value: `dice-project/${item.name}`,
        })),
        itemProps: {
          mode: 'multiple',
        },
      },
    ]),
  ];

  return (
    <FormModal
      title={i18n.t('set tags')}
      visible={visible}
      fieldsList={tagFields}
      formData={formData}
      onOk={onOk}
      onCancel={onCancel}
    />
  );
};
