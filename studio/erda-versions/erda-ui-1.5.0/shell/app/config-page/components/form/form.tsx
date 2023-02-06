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
import { Button } from 'antd';
import { isEmpty, map, get } from 'lodash';
import i18n from 'i18n';
import { Form as PureForm } from 'dop/pages/form-editor/index';
import { Title } from 'common';
import { OperationAction } from 'config-page/utils';
import { useUpdate } from 'common/use-hooks';

export const Form = (props: CP_FORM.Props) => {
  const { state, props: configProps, operations, execOperation, updateState } = props;

  const { formData: fD } = state || ({} as any);
  const [{ formData }, updater, update] = useUpdate({
    formData: fD,
  });

  React.useEffect(() => {
    if (state) {
      update({
        formData: state.formData,
      });
    }
  }, [state, update]);

  const onCancel = () => {
    if (operations?.cancel) {
      execOperation(operations?.cancel);
    } else {
      updateState({ visible: false, formData: undefined });
    }
  };

  const { fields: _fields, title, visible = true, readOnly = false, ...rest } = configProps || {};
  const formRef = React.useRef(null as any);

  const onFinish = (val: any) => {
    operations?.submit && execOperation(operations?.submit, { formData: { ...(fD || {}), ...val } });
  };

  const onOk = () => {
    operations?.submit && execOperation(operations?.submit);
  };

  const fields = React.useMemo(() => {
    const useableFields = map(_fields, (item) => {
      const curItem = { ...item };

      const curKeyOperation = get(curItem, 'operations.change');

      if (curKeyOperation) {
        curItem.componentProps = {
          ...curItem.componentProps,
          onChange: () => {
            const validFormData = formRef.current.getData();
            execOperation({ ...curKeyOperation }, { formData: validFormData });
          },
        };
      }
      if (readOnly) curItem.disabled = true;
      return curItem;
    });

    formRef.current && formRef.current.setFields(useableFields);
    return useableFields;
  }, [_fields, execOperation, readOnly]);

  if (!visible) return null;
  return (
    <>
      {title ? <Title title={title} level={2} showDivider={false} /> : null}
      {!isEmpty(fields) ? (
        <PureForm {...rest} fields={fields || []} formRef={formRef} onFinish={onFinish} value={formData}>
          {!readOnly ? (
            <div>
              {operations?.submit && (
                <OperationAction onClick={() => formRef.current.onSubmit()} operation={operations.submit}>
                  <Button type="primary">{i18n.t('commit')}</Button>
                </OperationAction>
              )}
              {operations?.cancel && (
                <Button className="ml-2" onClick={onCancel}>
                  {i18n.t('cancel')}
                </Button>
              )}
            </div>
          ) : undefined}
        </PureForm>
      ) : (
        <div>
          {operations?.submit && (
            <OperationAction onClick={onOk} operation={operations.submit}>
              <Button type="primary">{i18n.t('commit')}</Button>
            </OperationAction>
          )}
          {operations?.cancel && (
            <OperationAction onClick={onCancel} operation={operations.cancel}>
              <Button className="ml-2">{i18n.t('cancel')}</Button>
            </OperationAction>
          )}
        </div>
      )}
    </>
  );
};
