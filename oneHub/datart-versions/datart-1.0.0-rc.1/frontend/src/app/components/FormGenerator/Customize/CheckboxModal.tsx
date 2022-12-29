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

import { InfoCircleOutlined } from '@ant-design/icons';
import { Button, Checkbox, Tooltip } from 'antd';
import { CheckboxChangeEvent } from 'antd/lib/checkbox';
import useStateModal, { StateModalSize } from 'app/hooks/useStateModal';
import { ChartStyleConfig } from 'app/types/ChartConfig';
import { FC, memo } from 'react';
import styled from 'styled-components/macro';
import { SPACE_TIMES } from 'styles/StyleConstants';
import { CollectionLayout } from '../Layout';
import { ItemLayoutProps } from '../types';
import { itemLayoutComparer } from '../utils';

const CheckboxModal: FC<ItemLayoutProps<ChartStyleConfig>> = memo(
  ({
    ancestors,
    translate: t = title => title,
    data,
    dataConfigs,
    context,
    onChange,
  }) => {
    const [openStateModal, contextHolder] = useStateModal({});
    const hasOriginal = !!data?.options?.hasOriginal;
    const enable = !!data.value;

    const handleCheckboxClick = (e: CheckboxChangeEvent) => {
      e.stopPropagation();
      const isCheck = !enable;
      onChange?.(ancestors, isCheck);
    };

    const handleOpenModal = (e: any) => {
      return (openStateModal as Function)({
        modalSize: data?.options?.modalSize || StateModalSize.SMALL,
        onOk: handleConfirmModalDialogOrDataUpdate,
        content: onChangeEvent => {
          return renderCollectionComponents(data, onChangeEvent);
        },
      });
    };

    const handleConfirmModalDialogOrDataUpdate = (
      ancestors,
      data,
      needRefresh,
    ) => {
      onChange?.(ancestors, data, needRefresh);
    };

    const renderCollectionComponents = (data, onChangeEvent) => {
      return (
        <CollectionLayout
          ancestors={ancestors}
          data={data}
          translate={t}
          dataConfigs={dataConfigs}
          context={context}
          onChange={onChangeEvent}
        />
      );
    };

    return (
      <StyledCheckboxModal>
        <Checkbox checked={enable} onChange={handleCheckboxClick}></Checkbox>
        <Button
          block
          disabled={!enable}
          size={'middle'}
          type="link"
          onClick={handleOpenModal}
        >
          {t(data.label)}
        </Button>
        {hasOriginal && (
          <Tooltip title={t('viz.tips.hasChartConfig', true)}>
            <InfoCircleOutlined />
          </Tooltip>
        )}
        {contextHolder}
      </StyledCheckboxModal>
    );
  },
  itemLayoutComparer,
);

export default CheckboxModal;

const StyledCheckboxModal = styled(Button)`
  display: flex;
  align-items: center;
  width: 100%;
  height: ${SPACE_TIMES(20)};

  & > button {
    margin-right: ${SPACE_TIMES(4)};
    color: ${p => p.theme.textColor};
  }
`;
