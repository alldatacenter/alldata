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

import { FormModal } from 'common';
import LabelSelector from 'dcos/common/label-selector';
import { CustomLabel, checkCustomLabels, checkTagLabels } from 'dcos/common/custom-label';
import clusterDashboardStore from 'dcos/stores/dashboard';
import i18n from 'i18n';
import { find, uniq } from 'lodash';
import machineStore from 'app/modules/cmp/stores/machine';
import React from 'react';
import orgStore from 'app/org-home/stores/org';

interface IProps {
  visible: boolean;
  machine: ORG_MACHINE.IMachine | null;
  onCancel: () => void;
}

const TagForm = ({ visible, machine, onCancel }: IProps) => {
  const currentOrg = orgStore.useStore((s) => s.currentOrg);
  const nodeLabels = clusterDashboardStore.useStore((s) => s.nodeLabels);
  const { getNodeLabels } = clusterDashboardStore.effects;
  const { updaterMachineLabels } = machineStore.effects;

  React.useEffect(() => {
    visible && getNodeLabels();
  }, [getNodeLabels, visible]);

  const handelSubmit = (values: { labels: string[]; customLabels: string }) => {
    // 组合自定义标签和选项标签，去重
    const { labels, customLabels = [] } = values;
    const savedLabels = uniq(labels.concat(customLabels));

    machine &&
      updaterMachineLabels({
        labels: savedLabels,
        hosts: [machine.ip],
        clusterName: machine.clusterName,
        orgID: currentOrg.id,
      });
    onCancel();
  };

  const chosenLabels = machine ? machine.labels.split(',') : [];
  const normalLabels: string[] = [];
  const customLabels: string[] = [];
  chosenLabels.forEach((item: string) => {
    if (item) {
      if (find(nodeLabels, { label: item })) {
        normalLabels.push(item);
      } else {
        customLabels.push(item);
      }
    }
  });

  const fieldsList = [
    {
      label: i18n.t('label'),
      name: 'labels',
      required: false,
      getComp: () => (
        <LabelSelector
          labelOptions={nodeLabels.filter((l) => !l.isPrefix).map((l) => ({ ...l, value: l.label, name: l.label }))}
        />
      ),
    },
    {
      label: i18n.t('custom labels'),
      name: 'customLabels',
      required: false,
      getComp: () => <CustomLabel />,
      rules: [{ validator: checkTagLabels }],
    },
  ];

  const machineIp = machine?.ip;
  return (
    <>
      <FormModal
        width={620}
        title={i18n.t('cmp:set tags of {ip}', { ip: machineIp })}
        fieldsList={fieldsList}
        visible={visible}
        formData={{ labels: normalLabels, customLabels }}
        onOk={handelSubmit}
        onCancel={onCancel}
      />
    </>
  );
};

export default TagForm;
