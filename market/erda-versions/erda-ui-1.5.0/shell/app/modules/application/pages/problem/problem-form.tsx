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
import { FormModal } from 'common';
import MarkdownEditor from 'common/components/markdown-editor';
import i18n from 'i18n';

export const getProblemType = (): PROBLEM.TicketType[] => {
  const typeArr = [
    {
      value: 'bug',
      name: i18n.t('dop:code defect'),
    },
    {
      value: 'vulnerability',
      name: i18n.t('dop:code vulnerability'),
    },
    {
      value: 'codeSmell',
      name: i18n.t('dop:code smell'),
    },
  ];
  return typeArr;
};

export const ProblemPriority = [
  {
    value: 'low',
    name: i18n.t('dop:low'),
    color: 'tag-info',
  },
  {
    value: 'medium',
    name: i18n.t('dop:medium'),
    color: 'tag-warning',
  },
  {
    value: 'high',
    name: i18n.t('dop:high'),
    color: 'tag-danger',
  },
];

export const ProblemForm = ({
  visible,
  onOk,
  onCancel,
}: {
  visible: boolean;
  onOk: (values: any) => void;
  onCancel: () => void;
}) => {
  const fieldsList = [
    {
      label: i18n.t('dop:ticket title'),
      name: 'title',
    },
    {
      label: i18n.t('dop:ticket content'),
      name: 'content',
      getComp: () => <MarkdownEditor />,
    },
    {
      label: i18n.t('dop:ticket type'),
      name: 'type',
      type: 'select',
      options: getProblemType(),
    },
    {
      label: i18n.t('dop:priority'),
      name: 'priority',
      type: 'radioGroup',
      options: ProblemPriority,
    },
  ];
  return (
    <FormModal
      width={700}
      name={i18n.t('task ticket')}
      fieldsList={fieldsList}
      visible={visible}
      onOk={onOk}
      onCancel={onCancel}
    />
  );
};
