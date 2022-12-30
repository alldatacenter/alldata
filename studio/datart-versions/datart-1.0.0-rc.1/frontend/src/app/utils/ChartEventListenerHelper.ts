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

import { ChartInteractionEvent } from 'app/constants';
import { ChartConfigReducerActionType } from 'app/pages/ChartWorkbenchPage/slice/constant';
import { ChartMouseEventParams } from 'app/types/Chart';
import { IChartDrillOption } from 'app/types/ChartDrillOption';

export const tablePagingAndSortEventListener = (
  param?: ChartMouseEventParams,
  callback?: (newParams) => void,
) => {
  if (
    param?.chartType === 'table' &&
    param?.interactionType === ChartInteractionEvent.PagingOrSort
  ) {
    callback?.({
      sorter: {
        column: param?.seriesName,
        operator: param?.value?.direction,
        aggOperator: param?.value?.aggOperator,
      },
      pageInfo: {
        pageNo: param?.value?.pageNo,
      },
    });
  }
};

export const drillDownEventListener = (
  drillOption?: IChartDrillOption,
  param?: ChartMouseEventParams,
  callback?: (newParams) => void,
) => {
  if (drillOption?.isSelectedDrill && !drillOption?.isBottomLevel) {
    drillOption?.drillDown(param?.data?.rowData);
    callback?.(drillOption);
  }
};

export const pivotTableDrillEventListener = (
  param?: ChartMouseEventParams,
  callback?: (newParams) => void,
) => {
  if (
    param?.chartType === 'pivotSheet' &&
    param?.interactionType === ChartInteractionEvent.Drilled
  ) {
    callback?.(param.drillOption);
  }
};

export const richTextContextEventListener = (
  row: any,
  param?: ChartMouseEventParams,
  callback?: (newParams) => void,
) => {
  if (
    param?.chartType === 'rich-text' &&
    param?.interactionType === ChartInteractionEvent.ChangeContext
  ) {
    callback?.({
      type: ChartConfigReducerActionType.STYLE,
      payload: {
        ancestors: [1, 0],
        value: {
          ...row,
          value: param.value,
        },
      },
      needRefresh: false,
      updateDrillOption: config => {
        return undefined;
      },
    });
  }
};

export const chartSelectionEventListener = (
  param?: ChartMouseEventParams,
  callback?: (newParams) => void,
) => {
  if (
    param?.interactionType === ChartInteractionEvent.Select ||
    param?.interactionType === ChartInteractionEvent.UnSelect
  ) {
    callback?.(param?.selectedItems);
  }
};
