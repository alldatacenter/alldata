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

import { FormInstance } from 'antd';
import { ChartDataSectionFieldActionType } from 'app/constants';
import FieldActions from 'app/pages/ChartWorkbenchPage/components/ChartOperationPanel/components/ChartFieldAction';
import { ChartDataConfig, ChartDataSectionField } from 'app/types/ChartConfig';
import ChartDataSetDTO from 'app/types/ChartDataSet';
import ChartDataView from 'app/types/ChartDataView';
import { ValueOf } from 'types';
import useI18NPrefix, { I18NComponentProps } from './useI18NPrefix';
import useStateModal, { StateModalSize } from './useStateModal';

function useFieldActionModal({ i18nPrefix }: I18NComponentProps) {
  const t = useI18NPrefix(i18nPrefix);
  const [show, contextHolder] = useStateModal({ initState: {} });

  const getContent = (
    actionType,
    config?: ChartDataSectionField,
    dataset?: ChartDataSetDTO,
    dataView?: ChartDataView,
    dataConfig?: ChartDataConfig,
    onChange?,
    aggregation?: boolean,
    form?: FormInstance,
  ) => {
    if (!config) {
      return null;
    }

    const props = {
      config,
      dataset,
      dataView,
      dataConfig,
      onConfigChange: onChange,
      aggregation,
      i18nPrefix,
      form,
    };

    switch (actionType) {
      case ChartDataSectionFieldActionType.Sortable:
        return <FieldActions.SortAction {...props} />;
      case ChartDataSectionFieldActionType.Alias:
        return <FieldActions.AliasAction {...props} />;
      case ChartDataSectionFieldActionType.Format:
        return <FieldActions.NumberFormatAction {...props} />;
      case ChartDataSectionFieldActionType.Aggregate:
        return <FieldActions.AggregationAction {...props} />;
      case ChartDataSectionFieldActionType.AggregateLimit:
        return <FieldActions.AggregationLimitAction {...props} />;
      case ChartDataSectionFieldActionType.Filter:
        return <FieldActions.FilterAction {...props} />;
      case ChartDataSectionFieldActionType.Colorize:
        return <FieldActions.AggregationColorizeAction {...props} />;
      case ChartDataSectionFieldActionType.Size:
        return <FieldActions.SizeOptionsAction {...props} />;
      case ChartDataSectionFieldActionType.ColorRange:
        return <FieldActions.ColorizeRangeAction {...props} />;
      case ChartDataSectionFieldActionType.ColorizeSingle:
        return <FieldActions.ColorizeSingleAction {...props} />;
      default:
        return 'Please use correct action key!';
    }
  };

  const handleOk =
    (onConfigChange, columnUid: string) => (config, needRefresh) => {
      onConfigChange(columnUid, config, needRefresh);
    };

  const showModal = (
    columnUid: string,
    actionType: ValueOf<typeof ChartDataSectionFieldActionType>,
    dataConfig: ChartDataConfig,
    onConfigChange,
    dataset?: ChartDataSetDTO,
    dataView?: ChartDataView,
    modalSize?: string,
    aggregation?: boolean,
  ) => {
    const currentConfig = dataConfig.rows?.find(c => c.uid === columnUid);
    let _modalSize = StateModalSize.MIDDLE;
    if (actionType === ChartDataSectionFieldActionType.Colorize) {
      _modalSize = StateModalSize.XSMALL;
    } else if (actionType === ChartDataSectionFieldActionType.ColorizeSingle) {
      _modalSize = StateModalSize.XSMALL;
    }
    return (show as Function)({
      title: t(actionType),
      modalSize: modalSize || _modalSize,
      content: (onChange, from) =>
        getContent(
          actionType,
          currentConfig,
          dataset,
          dataView,
          dataConfig,
          onChange,
          aggregation,
          from,
        ),
      onOk: handleOk(onConfigChange, columnUid),
      maskClosable: true,
    });
  };

  return [showModal, contextHolder];
}

export default useFieldActionModal;
