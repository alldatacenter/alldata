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
import { DatePicker, Dropdown, Select } from 'antd';
import { produce } from 'immer';
import { useMount } from 'react-use';
import { Moment } from 'moment';
import { SelectProps } from 'core/common/interface';
import i18n from 'i18n';
import './time-select.scss';
import { useUpdate } from 'common/use-hooks';
import { cloneDeep, map } from 'lodash';
import {
  autoRefreshDuration,
  defaultFormat,
  IRefreshDuration,
  IRelativeTime,
  ITimeRange,
  relativeTimeRange,
  transformRange,
  translateAutoRefreshDuration,
} from './common';
import { ErdaIcon } from 'common';;

type IRefreshStrategy = 'off' | IRefreshDuration;

const { Option } = Select;

export const AutoRefreshStrategy = (props: SelectProps<IRefreshStrategy>) => {
  return (
    <div className="auto-refresh relative border-all hover:border-primary bg-white hover:z-10">
      <Select defaultValue="off" bordered={false} dropdownMatchSelectWidth={false} {...props}>
        <Option key="off" value="off">
          OFF
        </Option>
        {map(autoRefreshDuration, (label, value) => {
          return (
            <Option key={value} value={value}>
              {label}
            </Option>
          );
        })}
      </Select>
    </div>
  );
};

interface ITimeRangeState {
  data: ITimeRange;
  startOpen: boolean;
  endOpen: boolean;
}

interface ITimeRangeProps {
  format?: string;
  value?: ITimeRange;

  onChange: (data: ITimeRange) => void;
}

export const TimeRange = ({ onChange, value, format }: ITimeRangeProps) => {
  const [{ startOpen, endOpen, data }, updater, update] = useUpdate<ITimeRangeState>({
    data: value ?? ({} as ITimeRange),
    startOpen: false,
    endOpen: false,
  });
  const dateRange = React.useRef<ITimeRange['customize']>(cloneDeep(data.customize || {}));
  const triggerChange = (changedValue: ITimeRange) => {
    onChange?.({ ...data, ...value, ...changedValue });
  };

  const handleSelectQuickTimeRange = (str: IRelativeTime) => {
    const v = value ?? data;
    const newData = produce(v, (draft) => {
      draft.mode = 'quick';
      draft.quick = str;
      draft.customize = {
        start: undefined,
        end: undefined,
      };
    });
    dateRange.current.end = undefined;
    dateRange.current.start = undefined;
    if (!('quick' in (value ?? {}))) {
      updater.data(newData);
    }
    triggerChange(newData);
  };

  const handleChangeDate = React.useCallback(
    (flag: string, date: Moment | null, _: string) => {
      const v = value ?? data;
      const newData = produce(v, (draft) => {
        draft.mode = 'customize';
        draft.quick = undefined;
        if (!draft.customize) {
          draft.customize = {};
        }
        draft.customize[flag] = date;
      });
      dateRange.current[flag] = date;
      const { start, end } = dateRange.current;
      const newState: Partial<ITimeRangeState> = {
        startOpen: !start,
        endOpen: !end,
      };
      if (!('customize' in (value ?? {}))) {
        newState.data = newData;
      }
      update(newState);
      if (start && end) {
        triggerChange({
          mode: 'customize',
          customize: { start, end },
          quick: undefined,
        });
      }
    },
    [data, value],
  );
  const disabledStart = (current: Moment) => {
    return current && dateRange.current.end && current > dateRange.current.end;
  };

  const disabledEnd = (current: Moment) => {
    return current && dateRange.current.start && current < dateRange.current.start;
  };

  const mode = value?.mode || data.mode;
  const activeQuick = value?.quick || data.quick;
  const start = mode === 'customize' ? value?.customize.start || data?.customize.start : undefined;
  const end = mode === 'customize' ? value?.customize.end || data?.customize.end : undefined;

  return (
    <div className="flex h-full items-stretch">
      <div className="w-56 h-full px-3">
        <p className="pt-3 font-medium">{i18n.t('absolute time range')}</p>
        <p className="mt-3 mb-1">{i18n.t('common:start at')}</p>
        <DatePicker
          format={format}
          disabledDate={disabledStart}
          disabledTime={disabledStart}
          allowClear={false}
          open={startOpen}
          onOpenChange={updater.startOpen}
          showTime
          className="w-full"
          value={start}
          onChange={(...arg) => {
            handleChangeDate('start', ...arg);
          }}
        />
        <p className="mt-3 mb-1">{i18n.t('common:end at')}</p>
        <DatePicker
          format={format}
          disabledDate={disabledEnd}
          disabledTime={disabledEnd}
          allowClear={false}
          open={endOpen}
          onOpenChange={updater.endOpen}
          showTime
          className="w-full"
          value={end}
          onChange={(...arg) => {
            handleChangeDate('end', ...arg);
          }}
        />
      </div>
      <div className="w-44 h-full border-left flex flex-col">
        <p className="px-3 pt-3 font-medium">{i18n.t('relative time range')}</p>
        <ul className="time-quick-select overflow-y-auto flex-1 mt-3">
          {map(relativeTimeRange, (label, range: IRelativeTime) => {
            return (
              <li
                className={`time-quick-select-item h-9 px-3 flex items-center hover:bg-grey cursor-pointer ${
                  mode === 'quick' && activeQuick === range ? 'text-primary font-medium' : ''
                }`}
                key={range}
                onClick={() => {
                  handleSelectQuickTimeRange(range);
                }}
              >
                {label}
              </li>
            );
          })}
        </ul>
      </div>
    </div>
  );
};

interface IState {
  data: ITimeRange;
  strategy: IRefreshStrategy;
  refreshDuration: number;
  visible: boolean;
  text?: string;
}

export interface IProps {
  className?: string;
  triggerChangeOnMounted?: boolean;
  defaultValue?: ITimeRange;
  strategy?: IRefreshStrategy;
  defaultStrategy?: IRefreshStrategy;
  value?: ITimeRange;
  defaultRefreshDuration?: string;
  refreshDuration?: string;
  format?: string;

  onRefreshStrategyChange?: (strategy: string) => void;

  onChange?: (data: ITimeRange, range: Moment[]) => void;
}

const TimeSelect = (props: IProps) => {
  const format = props.format ?? defaultFormat;
  const [{ visible, text, strategy, refreshDuration, data }, updater, update] = useUpdate<IState>({
    refreshDuration: -1,
    strategy: props.defaultStrategy || 'off',
    visible: false,
    data: props.defaultValue ?? {
      mode: 'quick',
      customize: {},
      quick: undefined,
    },
    text: props.defaultValue ? transformRange(props.defaultValue, format).dateStr : '',
  });
  const timer = React.useRef<number>();
  const payload = React.useRef<ITimeRange>(props.value || props.defaultValue || ({} as ITimeRange));
  useMount(() => {
    if (props.triggerChangeOnMounted) {
      handleManualRefresh();
    }
  });

  const refreshStrategy = props.strategy ?? strategy;
  React.useEffect(() => {
    const isAutoRefresh = refreshStrategy !== 'off';
    let duration = -1;
    if (isAutoRefresh) {
      const [unit, count] = refreshStrategy.split(':') || [];
      duration = translateAutoRefreshDuration(parseInt(count, 10), unit);
    }
    updater.refreshDuration(duration);
  }, [refreshStrategy]);

  React.useEffect(() => {
    openAutoRefresh();
    return () => {
      closeAutoRefresh();
    };
  }, [refreshDuration, props.onChange]);

  React.useEffect(() => {
    if (props?.value) {
      payload.current = props.value;
      updater.text(transformRange(props.value, format).dateStr);
    }
  }, [props.value]);

  /**
   * @description open auto refresh
   */
  const openAutoRefresh = () => {
    closeAutoRefresh();
    if (refreshDuration === -1) {
      return;
    }
    timer.current = setInterval(() => {
      const { date } = transformRange(payload.current, format);
      props?.onChange?.(payload.current, date);
    }, refreshDuration);
  };

  /**
   * @description close auto refresh
   */
  const closeAutoRefresh = () => {
    timer.current && clearInterval(timer.current);
    timer.current = undefined;
  };

  /**
   * @description select date
   * @param range
   */
  const handleSelectDate = (range: ITimeRange) => {
    const { date, dateStr } = transformRange(range, format);
    const newState: Partial<IState> = {
      text: dateStr,
      visible: false,
    };
    payload.current = range;
    if (!('value' in props)) {
      newState.data = range;
    }
    update(newState);
    props.onChange?.(range, date);
  };

  /**
   * @description switch refresh strategy
   * @param key
   */
  const handleChangeRefreshStrategy = (key: IRefreshStrategy) => {
    if (!('strategy' in props)) {
      updater.strategy(key);
    }
    props?.onRefreshStrategyChange?.(key);
  };

  /**
   * @description manual refresh
   */
  const handleManualRefresh = () => {
    const { date } = transformRange(payload.current, format);
    props?.onChange?.(payload.current, date);
  };

  return (
    <div className={`time-select h-8 flex rounded ${props.className ?? ''}`}>
      <Dropdown
        visible={visible}
        trigger={['click']}
        overlayClassName="time-range-dropdown bg-white"
        overlay={
          <TimeRange format={format} key={`${visible}`} value={props?.value ?? data} onChange={handleSelectDate} />
        }
        onVisibleChange={updater.visible}
      >
        <div
          className="time-range cursor-pointer border-all rounded-l flex items-center px-2 hover:border-primary bg-white hover:z-10"
          onClick={() => {
            updater.visible(true);
          }}
        >
          <ErdaIcon className="mr-1" size="16" type="time" />
          {text}
        </div>
      </Dropdown>
      <AutoRefreshStrategy
        suffixIcon={<ErdaIcon type="caret-down" className="ml-1 -mt-0.5" size="16" />}
        style={{ width: 70 }}
        defaultValue={refreshStrategy}
        onChange={handleChangeRefreshStrategy}
      />
      <div className="manual-refresh flex justify-center items-center w-8 relative border-all rounded-r hover:border-primary bg-white">
        <ErdaIcon
          size="14"
          type="refresh1"
          className="m-0 cursor-pointer"
          fill="#070A1A"
          onClick={handleManualRefresh}
        />
      </div>
    </div>
  );
};

export default TimeSelect;
