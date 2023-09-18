import { ChartDataSectionType, ControllerFacadeTypes } from 'app/constants';
import { ChartConfig } from 'app/types/ChartConfig';
import { PendingChartDataRequestFilter } from 'app/types/ChartDataRequest';
import { RUNTIME_FILTER_KEY } from 'globalConstants';
import { isEmptyArray } from 'utils/object';
import { FilterSearchParams } from './types';

const valueTransder = {
  list: (value?: string | string[]) => {
    if (Array.isArray(value)) {
      return value.map(v => ({ key: v, label: v, isSelected: true }));
    } else {
      return [];
    }
  },
  value: (value?: string | string[]) => {
    if (Array.isArray(value)) {
      return value[0];
    } else {
      return value;
    }
  },
  dropdownList: (valueOptions, value) => {
    const _value = value?.[0];
    if (valueOptions?.length > 0 && _value) {
      return valueOptions.map(v => {
        if (_value === v.label && v) {
          v.isSelected = true;
        } else if (v) {
          v.isSelected = false;
        }
        return v;
      });
    } else {
      return valueOptions;
    }
  },
};

export const transferChartConfig = (
  chartConfig?: ChartConfig,
  params?: FilterSearchParams,
  matchByName?: boolean,
  jumpFilterParams?: PendingChartDataRequestFilter[],
): ChartConfig => {
  if (!chartConfig) {
    return {};
  }
  const datas = chartConfig.datas || [];
  if (datas?.length > 0 && params) {
    const idKeys = Object.keys(params);
    chartConfig.datas = datas.map(item => {
      if (
        item.type === ChartDataSectionType.Filter &&
        item.rows &&
        item.rows?.length > 0
      ) {
        item.rows = item.rows.map(v => {
          const isMatch = matchByName
            ? v.colName && idKeys.includes(v.colName)
            : v.uid && idKeys.includes(v.uid);
          const value = matchByName
            ? v.colName && params[v.colName]
            : v.uid && params[v.uid];
          if (isMatch && v.filter && v.filter.condition) {
            const facade =
              typeof v?.filter?.facade === 'object'
                ? v?.filter?.facade?.facade
                : v?.filter?.facade;
            switch (facade) {
              case ControllerFacadeTypes.DropdownList:
                const _value0 = valueTransder.dropdownList(
                  v.filter.condition.value,
                  value,
                ) as any[];
                v.filter.condition.value = _value0;
                break;
              case ControllerFacadeTypes.MultiDropdownList:
                const _value = valueTransder.list(value) as any[];
                v.filter.condition.value = _value;
                break;
              case ControllerFacadeTypes.RadioGroup:
                const _value2 = valueTransder.value(value);
                v.filter.condition.value = _value2;
                break;
              case ControllerFacadeTypes.Slider:
                break;
              case ControllerFacadeTypes.Text:
                const _value7 = valueTransder.value(value);
                v.filter.condition.value = _value7;
                break;
              case ControllerFacadeTypes.Time:
                break;
              case ControllerFacadeTypes.Tree:
                break;
              case ControllerFacadeTypes.RangeTime:
                break;
              case ControllerFacadeTypes.RangeValue:
                break;
              case ControllerFacadeTypes.Value:
                break;
            }
          }
          return { ...v };
        });
      }
      if (
        item.type === ChartDataSectionType.Filter &&
        !isEmptyArray(jumpFilterParams)
      ) {
        item[RUNTIME_FILTER_KEY] = jumpFilterParams;
      }
      return item;
    });
    return { ...chartConfig };
  } else {
    return chartConfig;
  }
};
