/**
 * Datart
 *
 * Copyright 2021
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { Button, Collapse } from 'antd';
import useStateModal, { StateModalSize } from 'app/hooks/useStateModal';
import { ChartStyleConfig } from 'app/types/ChartConfig';
import { FC, memo, useState } from 'react';
import styled from 'styled-components/macro';
import { SPACE_MD } from 'styles/StyleConstants';
import { BW } from '../Basic/components/BasicWrapper';
import { FormGroupLayoutMode, FormItemComponentType } from '../constants';
import { FormGeneratorLayoutProps } from '../types';
import { groupLayoutComparer } from '../utils';
import CollapseHeader from './CollapseHeader';
import CollectionLayout from './CollectionLayout';

const { Panel } = Collapse;

const GroupLayout: FC<FormGeneratorLayoutProps<ChartStyleConfig>> = memo(
  ({
    ancestors,
    data,
    mode = FormGroupLayoutMode.OUTER,
    translate: t = title => title,
    dataConfigs,
    flatten,
    onChange,
    context,
  }) => {
    const [openStateModal, contextHolder] = useStateModal({});
    const [type] = useState(data?.options?.type || 'default');
    const [modalSize] = useState(
      data?.options?.modalSize || StateModalSize.SMALL,
    );
    const [expand] = useState(!!data?.options?.expand);

    const handleConfirmModalDialogOrDataUpdate = (
      ancestors,
      data,
      needRefresh,
    ) => {
      onChange?.(ancestors, data, needRefresh);
    };

    const handleOpenStateModal = () => {
      return (openStateModal as Function)({
        modalSize,
        onOk: handleConfirmModalDialogOrDataUpdate,
        content: onChangeEvent => {
          return renderCollectionComponents(data, onChangeEvent);
        },
      });
    };

    const renderGroupByMode = (mode, comType, data) => {
      if (mode === FormGroupLayoutMode.INNER) {
        return renderCollectionComponents(
          data,
          handleConfirmModalDialogOrDataUpdate,
        );
      }
      if (comType === FormItemComponentType.MODAL) {
        return (
          <BW label={data.options?.title ? t(data.options?.title, true) : ''}>
            <Button
              className="datart-modal-button"
              type="ghost"
              block={true}
              title={t(data.label, true)}
              onClick={handleOpenStateModal}
            >
              <CollapseHeader title={t(data.label, true)} />
            </Button>
            {contextHolder}
          </BW>
        );
      }

      return (
        <Collapse
          expandIconPosition="right"
          defaultActiveKey={expand ? '1' : undefined}
        >
          <Panel
            key="1"
            header={<CollapseHeader title={t(data.label, true)} />}
          >
            {renderCollectionComponents(
              data,
              handleConfirmModalDialogOrDataUpdate,
            )}
          </Panel>
        </Collapse>
      );
    };

    const renderCollectionComponents = (data, onChangeEvent) => {
      return (
        <CollectionLayout
          ancestors={ancestors}
          data={data}
          translate={t}
          dataConfigs={dataConfigs}
          flatten={flatten}
          onChange={onChangeEvent}
          context={context}
        />
      );
    };

    return (
      <StyledGroupLayout
        className="chart-config-group-layout"
        flatten={flatten || data?.options?.flatten}
      >
        {renderGroupByMode(mode, type, data)}
      </StyledGroupLayout>
    );
  },
  groupLayoutComparer,
);

export default GroupLayout;

const StyledGroupLayout = styled.div<{ flatten?: boolean }>`
  padding: 0 ${p => (p.flatten ? 0 : SPACE_MD)};
`;
