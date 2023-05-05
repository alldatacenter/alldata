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
import { FormModal as PureFormModal } from 'app/configForm/nusi-form/form-modal';
import { useUpdate } from 'common/use-hooks';

export const FormModal = (props: CP_FORM_MODAL.Props) => {
  const { state, execOperation, updateState, operations, props: configProps } = props;

  const { fields, ...rest } = configProps || {};
  const { visible: sVisible, formData: fD } = state || ({} as any);
  const [{ visible, formData, title }, updater, update] = useUpdate({
    visible: sVisible || false,
    formData: fD,
    title: '',
  });
  React.useEffect(() => {
    if (state) {
      update({
        visible: state.visible,
        formData: state.formData,
        title: state.title,
      });
    }
  }, [state, update]);

  React.useEffect(() => {
    // reload pages
    if (operations?.submit?.refresh) {
      window.location.reload();
    }
  }, [operations?.submit]);

  const onCancel = () => updateState({ visible: false, formData: undefined });
  const onFinish = (arg: any) => {
    if (operations?.submit) {
      execOperation(operations.submit, { formData: { ...fD, ...arg } });
    }
  };
  const curTitle = title || rest?.title;
  return (
    <PureFormModal
      {...rest}
      onCancel={onCancel}
      onOk={onFinish}
      formData={formData}
      title={curTitle}
      fieldList={fields}
      visible={visible}
    />
  );
};
