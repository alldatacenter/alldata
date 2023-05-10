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
import { Menu, Dropdown, Input, DatePicker, Checkbox, Tooltip } from 'antd';
import { MemberSelector, ErdaIcon, Icon as CustomIcon } from 'common';
import moment, { Moment } from 'moment';
import { useUpdateEffect } from 'react-use';
import { debounce, isEmpty, isArray, map, max, sortBy, isString, has, isNumber } from 'lodash';
import i18n from 'i18n';
import './index.scss';

interface Option {
  label: string;
  value: string | number;
  icon: string;
  children?: Option[];
}

type ConditionType = 'select' | 'input' | 'dateRange';
const { RangePicker } = DatePicker;

export interface ICondition {
  key: string;
  label: string;
  type: ConditionType;
  disabled?: boolean;
  emptyText?: string;
  required?: boolean;
  firstShowLength?: number;
  split?: boolean;
  value?: string | number | string[] | number[] | Obj;
  fixed?: boolean;
  showIndex?: number; // 0： 隐藏、其他显示
  haveFilter?: boolean;
  placeholder?: string;
  quickAdd?: {
    operationKey: string;
    show: boolean;
    placeholder?: string;
  };
  quickDelete?: {
    operationKey: string;
  };
  quickSelect?: {
    label: string;
    operationKey: string;
  };
  getComp?: (props: Obj) => React.ReactNode;
  options?: Option[];
  customProps: Obj;
}

interface IFilterItemProps {
  itemData: ICondition;
  value: any;
  active: boolean;

  onVisibleChange: (visible: boolean) => void;
  onChange: (data: { key: string; value: any }, extra?: { forceChange?: boolean }) => void;
  onQuickOperation: (data: { key: string; value: any }) => void;
}

const filterMatch = (v: string, f: string) => v.toLowerCase().includes(f.toLowerCase());

const getSelectOptions = (options: Option[], filterKey: string) => {
  if (!filterKey) return options;
  const useableOptions: Option[] = [];

  options.forEach((item) => {
    let curOp: Option | null = null;
    if (has(item, 'children')) {
      curOp = { ...item, children: [] };
      item.children?.forEach((cItem) => {
        if (filterMatch(`${cItem.label}`, filterKey)) {
          curOp?.children?.push(cItem);
        }
      });
      if (curOp.children?.length) useableOptions.push(curOp);
    } else if (filterMatch(`${item.label}`, filterKey)) {
      curOp = item;
      curOp && useableOptions.push(curOp);
    }
  });
  return useableOptions;
};

interface IOptionItemProps {
  value: Array<string | number>;
  option: Option;
  onClick: (option: Option) => void;
  onDelete?: (option: Option) => void;
}
const OptionItem = (props: IOptionItemProps) => {
  const { value, option, onClick, onDelete } = props;
  return (
    <div
      className={`relative option-item ${(value || []).includes(option.value) ? 'checked-item' : ''}`}
      key={option.value}
      onClick={() => onClick(option)}
    >
      <div className="flex justify-between items-center w-full">
        <span>
          {option.icon && <CustomIcon type={option.icon} />}
          {option.label}
        </span>
        <span>
          {value.includes(option.value) ? <ErdaIcon type="check" size="14" color="green" className="ml-2" /> : null}
        </span>
      </div>
      {onDelete ? (
        <div
          className="absolute option-item-delete"
          onClick={(e) => {
            e.stopPropagation();
            onDelete(option);
          }}
        >
          <div className="option-item-delete-box pl-2">
            <ErdaIcon type="shanchu" className="mr-1" size="14" />
          </div>
        </div>
      ) : null}
    </div>
  );
};

const FilterItem = ({ itemData, value, active, onVisibleChange, onChange, onQuickOperation }: IFilterItemProps) => {
  const {
    key,
    label,
    haveFilter,
    type,
    firstShowLength = 200,
    placeholder,
    quickSelect,
    disabled,
    quickDelete,
    quickAdd,
    options,
    required,
    customProps,
    emptyText = i18n.t('dop:all'),
    getComp,
  } = itemData;
  const [filterMap, setFilterMap] = React.useState({});
  const memberSelectorRef = React.useRef(null as any);
  const [inputVal, setInputVal] = React.useState(value);
  const [hasMore, setHasMore] = React.useState(firstShowLength ? (options?.length || 0) > firstShowLength : false);
  // const inputRef = React.useRef(null);

  const debouncedChange = React.useRef(debounce(onChange, 1000));

  useUpdateEffect(() => {
    setInputVal(value);
  }, [value]);

  useUpdateEffect(() => {
    if (inputVal !== value) {
      debouncedChange?.current({ key, value: inputVal }, { forceChange: true });
    }
  }, [inputVal]);

  React.useEffect(() => {
    if (memberSelectorRef?.current?.show && active) {
      memberSelectorRef.current.show(active);
    }
  }, [active]);
  if (type === 'input') {
    return (
      <Input
        // autoFocus // 默认全部展示，不需要自动获取焦点
        value={inputVal}
        disabled={disabled}
        size="small"
        style={{ width: 180 }}
        allowClear
        className="bg-hover-gray-bg"
        bordered={false}
        // ref={inputRef}
        prefix={<ErdaIcon size="16" type="search1" />}
        placeholder={placeholder || i18n.t('press enter to search')}
        // onPressEnter={() => inputRef.current?.blur()}
        onChange={(e) => setInputVal(e.target.value)}
        // onPressEnter={() => onChange({ key, value: inputVal })}
      />
    );
  }

  if (type === 'select') {
    const _value = value ? (isString(value) || isNumber(value) ? [value] : value) : [];
    const _options = options || [];
    const { mode = 'multiple' } = customProps || {};
    const isSingleMode = mode === 'single';
    const valueText =
      _options
        .reduce((_optArr: Option[], _curOpt: Option) => _optArr.concat(_curOpt.children ?? _curOpt), [])
        .filter((a) => _value.includes(a.value))
        .map((a) => a.label)
        .join(',') || emptyText;

    const filterOptions = getSelectOptions(_options, filterMap[key]);
    const useOptions = hasMore ? filterOptions?.slice(0, firstShowLength) : filterOptions;
    const ops = (
      <Menu>
        {haveFilter && [
          <Menu.Item key="search-item options-item">
            <Input
              autoFocus
              size="small"
              placeholder={i18n.t('search')}
              prefix={<ErdaIcon size="16" type="search1" />}
              value={filterMap[key]}
              onChange={(e) => {
                const v = e.target.value;
                setFilterMap((prev) => {
                  return {
                    ...prev,
                    [key]: v.toLowerCase(),
                  };
                });
              }}
            />
          </Menu.Item>,
          <Menu.Divider key="divider1" />,
        ]}
        {!isSingleMode && [
          // 单选模式下不展示已选择n项
          <Menu.Item key="select-info" className="flex justify-between items-center not-select px6 py-0 options-item">
            <span>
              {i18n.t('common:selected')} {_value.length} {i18n.t('common:items')}
            </span>
            {!required ? (
              <span className="fake-link ml-2" onClick={() => onChange({ key, value: undefined })}>
                {i18n.t('common:clear selected')}
              </span>
            ) : null}
          </Menu.Item>,
          <Menu.Divider key="divider2" />,
        ]}
        {quickSelect && !isEmpty(quickSelect)
          ? [
              <Menu.Item key="quick-select-menu-item options-item">
                <span
                  className="fake-link flex justify-between items-center"
                  onClick={() => onQuickOperation({ key: quickSelect.operationKey, value: itemData })}
                >
                  {quickSelect.label}
                </span>
              </Menu.Item>,
              <Menu.Divider key="divider3" />,
            ]
          : null}
        {quickAdd?.operationKey && quickAdd.show !== false
          ? [
              <Menu.Item key="quick-select-menu-item options-item">
                <QuickSave
                  onSave={(v) => onQuickOperation({ key: quickAdd.operationKey, value: v })}
                  quickAdd={quickAdd}
                  options={options}
                />
              </Menu.Item>,
              <Menu.Divider key="divider4" />,
            ]
          : null}
        <Menu.Item key="options" className="p-0 options-container options-item block">
          {useOptions.map((op) => {
            if (has(op, 'children') && !op.children?.length) {
              return null;
            }
            const isGroup = op.children?.length;
            const onClickOptItem = (_curOpt: Option) => {
              if (isSingleMode) {
                if (required && _value.includes(_curOpt.value)) return;
                onChange({
                  key,
                  value: _value.includes(_curOpt.value) ? undefined : _curOpt.value,
                });
                onVisibleChange(false);
              } else {
                const newVal = _value.includes(_curOpt.value)
                  ? _value.filter((v: string | number) => v !== _curOpt.value)
                  : _value.concat(_curOpt.value);
                if (required && !newVal.length) return;
                onChange({
                  key,
                  value: newVal,
                });
              }
            };
            const onDelete = quickDelete?.operationKey
              ? (optItem: Option) => {
                  onQuickOperation({ key: quickDelete.operationKey, value: optItem.value });
                }
              : undefined;

            if (isGroup) {
              return (
                <GroupOpt
                  key={op.value || op.label}
                  value={_value}
                  firstShowLength={firstShowLength}
                  onDelete={onDelete}
                  onClickOptItem={onClickOptItem}
                  option={op}
                />
              );
            } else {
              return (
                <OptionItem
                  onDelete={onDelete}
                  key={op.value}
                  value={_value}
                  option={op}
                  onClick={() => onClickOptItem(op)}
                />
              );
            }
          })}
          {hasMore ? (
            <div className="fake-link hover-active py-1 pl-3  load-more" onClick={() => setHasMore(false)}>
              {`${i18n.t('load more')}...`}
            </div>
          ) : null}
        </Menu.Item>
      </Menu>
    );
    return (
      <Dropdown
        trigger={['click']}
        visible={active}
        onVisibleChange={onVisibleChange}
        overlay={ops}
        disabled={disabled}
        overlayClassName="contractive-filter-item-dropdown"
        placement="bottomLeft"
      >
        <span className="contractive-filter-item">
          <span className="text-desc mr-0.5">{label}</span>
          <span className="contractive-filter-item-value nowrap">{valueText}</span>
          <ErdaIcon type="caret-down" className="hover" size="16" />
        </span>
      </Dropdown>
    );
  }

  if (type === 'dateRange') {
    const [_startDate, _endDate] = value || [];
    const startDate = typeof _startDate === 'string' ? +_startDate : _startDate;
    const endDate = typeof _endDate === 'string' ? +_endDate : _endDate;
    const { borderTime } = customProps || {};

    const disabledDate = (isStart: boolean) => (current: Moment | undefined) => {
      return (
        !!current &&
        (isStart
          ? endDate
            ? (borderTime ? current.startOf('dates') : current) > moment(endDate)
            : false
          : startDate
          ? (borderTime ? current.endOf('dates') : current) < moment(startDate)
          : false)
      );
    };

    const getTimeValue = (v: any[]) => {
      if (borderTime) {
        const startVal = v[0]
          ? moment(isString(v[0]) ? +v[0] : v[0])
              .startOf('dates')
              .valueOf()
          : v[0];
        const endVal = v[1]
          ? moment(isString(v[1]) ? +v[1] : v[1])
              .endOf('dates')
              .valueOf()
          : v[1];
        return [startVal, endVal];
      }
      return v;
    };

    return (
      <span className="contractive-filter-item contractive-filter-date-picker">
        <span className="text-desc mr-0.5">{label}</span>
        <DatePicker
          size="small"
          bordered={false}
          disabled={disabled}
          value={startDate ? moment(startDate) : undefined}
          disabledDate={disabledDate(true)}
          format={'YYYY/MM/DD'}
          allowClear={!required}
          onChange={(v) => onChange({ key, value: getTimeValue([v?.valueOf(), endDate]) })}
          placeholder={i18n.t('common:startDate')}
        />
        <span className="text-desc">{i18n.t('common:to')}</span>
        <DatePicker
          size="small"
          bordered={false}
          disabled={disabled}
          allowClear={!required}
          value={endDate ? moment(endDate) : undefined}
          disabledDate={disabledDate(false)}
          format={'YYYY/MM/DD'}
          placeholder={i18n.t('common:endDate')}
          onChange={(v) => onChange({ key, value: getTimeValue([startDate, v?.valueOf()]) })}
        />
      </span>
    );
  }

  if (type === 'rangePicker') {
    const { ranges, borderTime, selectableTime, ...customRest } = customProps;
    const valueConvert = (val: number[] | Moment[]) => {
      const convertItem = (v: number | Moment) => {
        if (moment.isMoment(v)) {
          return moment(v).valueOf();
        } else {
          return v && moment(v);
        }
      };
      return Array.isArray(val) ? val.map((vItem) => convertItem(vItem)) : convertItem(val);
    };

    const rangeConvert = (_ranges?: Obj<number[]>) => {
      const reRanges = {};
      map(_ranges, (v, k) => {
        reRanges[k] = valueConvert(v);
      });
      return reRanges;
    };
    const disabledDate = (_current: Moment) => {
      return (
        _current &&
        !(
          (selectableTime[0] ? _current > moment(selectableTime[0]) : true) &&
          (selectableTime[1] ? _current < moment(selectableTime[1]) : true)
        )
      );
    };
    return (
      <span className="contractive-filter-item contractive-filter-date-picker">
        <span className="text-desc mr-0.5">{label}</span>
        <RangePicker
          value={valueConvert(value)}
          ranges={rangeConvert(ranges)}
          size="small"
          disabled={disabled}
          bordered={false}
          disabledDate={selectableTime ? disabledDate : undefined}
          onChange={(v) => {
            const val =
              borderTime && Array.isArray(v)
                ? v.map((vItem, idx) => {
                    if (idx === 0 && vItem) {
                      return vItem.startOf('dates');
                    } else if (idx === 1 && vItem) {
                      return vItem.endOf('dates');
                    }
                    return vItem;
                  })
                : v;
            onChange({ key, value: valueConvert(val) });
          }}
          {...customRest}
        />
      </span>
    );
  }

  if (type === 'memberSelector') {
    const memberResultsRender = (displayValue: any[]) => {
      const usersText = map(displayValue, (d) => d.label || d.value).join(',');
      return (
        <span
          className="contractive-filter-item-value nowrap member-value"
          onClick={(e) => {
            e.stopPropagation();
            onVisibleChange(true);
          }}
        >
          {usersText}
        </span>
      );
    };
    return (
      <span
        className="contractive-filter-item"
        onClick={() => {
          onVisibleChange(true);
        }}
      >
        <span className="text-desc mr-0.5">{label}</span>
        <MemberSelector
          {...((customProps || {}) as any)}
          onChange={(v) => {
            onChange({ key, value: v });
          }}
          allowClear={!required}
          value={value}
          dropdownMatchSelectWidth={false}
          onDropdownVisible={(vis: boolean) => onVisibleChange(vis)}
          ref={memberSelectorRef}
          resultsRender={memberResultsRender}
          placeholder={' '}
          className="contractive-member-selector"
          showSearch={haveFilter}
        />
        {value?.length ? null : <span>{emptyText}</span>}
        <ErdaIcon type="caret-down" className="hover" size="16" />
      </span>
    );
  }
  if (getComp) {
    const comp = getComp({
      onChange: (v) => {
        onChange({ key, value: v });
      },
    });
    return (
      <span className="contractive-filter-item flex items-center">
        <span className="text-desc mr-0.5">{label}</span>
        {comp}
      </span>
    );
  }
  return null;
};

interface IQuickSaveProps {
  onSave: (val: string) => void;
  options?: Option[];
  quickAdd?: { placeholder?: string };
}
const QuickSave = (props: IQuickSaveProps) => {
  const { onSave, options, quickAdd } = props;
  const [v, setV] = React.useState('');
  const [tip, setTip] = React.useState(`${i18n.t('can not be empty')}`);

  useUpdateEffect(() => {
    const labels = map(options, 'label') || [];
    if (!v) {
      setTip(i18n.t('can not be empty'));
    } else if (labels.includes(v)) {
      setTip(`${i18n.t('{name} already exists', { name: i18n.t('name') })}`);
    } else {
      setTip('');
    }
  }, [v]);

  const save = () => {
    !tip && onSave(v);
    setV('');
  };
  return (
    <div className="flex justify-between items-center">
      <Input
        size="small"
        placeholder={quickAdd?.placeholder || i18n.t('please enter')}
        value={v}
        onChange={(e: React.ChangeEvent<HTMLInputElement>) => setV(e.target.value)}
      />
      <Tooltip title={tip}>
        <span className={`ml-2 ${!tip ? 'fake-link' : 'not-allowed'}`} onClick={save}>
          {i18n.t('save')}
        </span>
      </Tooltip>
    </div>
  );
};

interface IGroupOptProps {
  value: Array<string | number>;
  option: Option;
  firstShowLength?: number;
  onClickOptItem: (option: Option) => void;
  onDelete?: (option: Option) => void;
}

const GroupOpt = (props: IGroupOptProps) => {
  const { option, onClickOptItem, value, onDelete, firstShowLength } = props;
  const [expand, setExpand] = React.useState(true);
  const [hasMore, setHasMore] = React.useState(
    firstShowLength ? (option.children?.length || 0) > firstShowLength : false,
  );

  const useOption = hasMore ? option.children?.slice(0, firstShowLength) : option.children;

  return (
    <div className={'option-group'}>
      <div className="option-group-label flex items-center justify-between" onClick={() => setExpand(!expand)}>
        <div className="flex items-center">
          {option.icon && <CustomIcon type={option.icon} />}
          {option.label}
        </div>
        <ErdaIcon type="down" className={`expand-icon flex items-center ${expand ? 'expand' : ''}`} size="16" />
      </div>
      <div className={`option-group-content ${expand ? '' : 'no-expand'}`}>
        {useOption?.map((cItem) => {
          return (
            <OptionItem
              onDelete={onDelete}
              key={cItem.value}
              value={value}
              option={cItem}
              onClick={() => onClickOptItem(cItem)}
            />
          );
        })}
        {hasMore ? (
          <div className="fake-link hover-active py-1 pl-8  load-more" onClick={() => setHasMore(false)}>
            {`${i18n.t('load more')}...`}
          </div>
        ) : null}
      </div>
    </div>
  );
};
const noop = () => {};
interface ContractiveFilterProps {
  initValue?: Obj; // 初始化
  values?: Obj; // 完全受控
  conditions: ICondition[];
  delay: number;
  fullWidth?: boolean;
  onConditionsChange?: (data: ICondition[]) => void;
  onChange: (valueMap: Obj, key?: string) => void;
  onQuickOperation?: (data: { key: string; value: any }) => void;
}

const setConditionShowIndex = (conditions: ICondition[], key: string, show: boolean) => {
  const showIndexArr = map(conditions, 'showIndex');
  const maxShowIndex = max(showIndexArr) as number;
  return map(conditions, (item) => {
    return {
      ...item,
      showIndex: key === item.key ? (show ? (maxShowIndex || 0) + 1 : 0) : item.showIndex,
    };
  });
};

const getInitConditions = (conditions: ICondition[], valueMap: Obj) => {
  const showIndexArr = map(conditions, 'showIndex');
  const maxShowIndex = max(showIndexArr) as number;
  let curMax = maxShowIndex;
  const reConditions = map(conditions, (item) => {
    const curValue = valueMap[item.key];
    // 有值默认展示
    if ((!has(item, 'showIndex') && curValue !== undefined) || (isArray(curValue) && !isEmpty(curValue))) {
      curMax += 1;
      return { ...item, showIndex: curMax };
    }
    return { ...item };
  });
  return reConditions;
};

const ContractiveFilter = ({
  initValue,
  values,
  conditions: propsConditions,
  delay,
  onChange,
  onQuickOperation = noop,
  onConditionsChange = noop,
  fullWidth = false,
  className,
}: ContractiveFilterProps) => {
  const [conditions, setConditions] = React.useState(
    getInitConditions(propsConditions || [], values || initValue || {}),
  );
  const [hideFilterKey, setHideFilterKey] = React.useState('');
  const [closeAll, setCloseAll] = React.useState(false);
  const [valueMap, setValueMap] = React.useState(values || initValue || {});
  const [activeMap, setActiveMap] = React.useState({});
  const debouncedChange = React.useRef(debounce(onChange, delay));

  const valueMapRef = React.useRef<Obj>();

  const inputList = conditions.filter((a) => a.type === 'input' && a.fixed !== false);
  const mainList = conditions.filter((a) => a.split);
  const displayConditionsLen = conditions.filter(
    (item) => (!item.fixed && item.type !== 'input' && !item.split) || (item.fixed === false && item.type === 'input'),
  ).length;

  useUpdateEffect(() => {
    setValueMap(values || {});
  }, [values]);

  React.useEffect(() => {
    valueMapRef.current = { ...valueMap };
  }, [valueMap]);

  // 当从props传进来的conditions变化时调用setConditions
  React.useEffect(() => {
    const preShowIndexMap = conditions.reduce((acc, x) => ({ ...acc, [x.key]: x.showIndex }), {});
    // 记录已选中的标签项，保留已选中标签项的showIndex
    const keepShowIndexConditions =
      propsConditions?.map((item) => ({
        ...item,
        showIndex: preShowIndexMap[item.key] || item.showIndex,
      })) || [];

    setConditions(keepShowIndexConditions);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [propsConditions]);

  React.useEffect(() => {
    onConditionsChange(conditions);
  }, [conditions, onConditionsChange]);

  React.useEffect(() => {
    // 控制点击外部关闭 dropdown
    const handleCloseDropdown = (e: MouseEvent) => {
      const wrappers = Array.from(document.querySelectorAll('.contractive-filter-item-wrap'));
      const dropdowns = Array.from(document.querySelectorAll('.contractive-filter-item-dropdown'));

      const datePickers = Array.from(document.querySelectorAll('.contractive-filter-date-picker'));
      const node = e.target as Node;
      const inner = wrappers.concat(dropdowns).some((wrap) => wrap.contains(node));
      const isDatePicker = datePickers.some((wrap) => wrap.contains(node));

      if (!inner && isDatePicker) {
        setCloseAll(true);
      }
    };
    document.body.addEventListener('click', handleCloseDropdown);
    return () => document.body.removeEventListener('click', handleCloseDropdown);
  }, []);

  const handelItemChange = (
    newValueMap: { key: string; value: any } | Obj,
    extra?: { batchChange?: boolean; forceChange?: boolean },
  ) => {
    const { batchChange = false, forceChange = false } = extra || {};
    let curValueMap = valueMapRef.current;
    if (batchChange) {
      setValueMap((prev) => {
        return {
          ...prev,
          ...newValueMap,
        };
      });
      curValueMap = { ...curValueMap, ...newValueMap };
    } else {
      const { key, value } = newValueMap;
      setValueMap((prev) => {
        return {
          ...prev,
          [key]: value,
        };
      });
      curValueMap = { ...curValueMap, [key]: value };
    }
    if (delay && !forceChange) {
      debouncedChange.current(curValueMap, newValueMap?.key);
    } else {
      onChange(curValueMap, newValueMap?.key);
    }
  };

  // 清除选中
  const handleClearSelected = () => {
    setConditions((prev) =>
      map(prev, (pItem) => {
        if (pItem.fixed || (pItem.type === 'input' && pItem.fixed !== false)) {
          return { ...pItem };
        } else {
          return { ...pItem, showIndex: 0 };
        }
      }),
    );
    const newValueMap = { ...valueMap };
    map(newValueMap, (_v, _k) => {
      const curConditions = conditions[_k] || {};
      if (!(curConditions.fixed || (curConditions.type === 'input' && curConditions.fixed !== false))) {
        newValueMap[_k] = initValue?.[_k] ?? undefined;
      }
    });
    handelItemChange(newValueMap, { batchChange: true });
  };

  const showList = sortBy(
    conditions.filter((a) => {
      if (a.split) {
        return false;
      }
      const curValue = valueMap[a.key];
      // 有值默认展示
      if (a.type !== 'input' && (curValue !== undefined || (isArray(curValue) && !isEmpty(curValue)))) {
        return true;
      }

      let flag = false;
      if (a.type !== 'input') {
        flag = !!a.showIndex || !!a.fixed;
      } else {
        flag = !!a.showIndex && a.fixed === false;
      }
      return flag;
    }),
    'showIndex',
  );

  return (
    <div className={`contractive-filter-bar ${className || ''}`}>
      {[...mainList, ...inputList, ...showList].map((item) => (
        <span
          className={`contractive-filter-item-wrap ${fullWidth ? 'w-full' : ''}`}
          key={item.key}
          onClick={() => {
            setCloseAll(false);
          }}
        >
          {!item.fixed && (
            <ErdaIcon
              fill="gray"
              color="gray"
              className="contractive-filter-item-close"
              type="guanbi-fill"
              size="16"
              onClick={() => {
                setConditions(setConditionShowIndex(conditions, item.key, false));
                if (valueMap[item.key] !== undefined) handelItemChange({ key: item.key, value: undefined });
              }}
            />
          )}
          <FilterItem
            itemData={item}
            value={valueMap[item.key]}
            active={closeAll ? false : activeMap[item.key]}
            onVisibleChange={(v) => setActiveMap((prev) => ({ ...prev, [item.key]: v }))}
            onChange={handelItemChange}
            onQuickOperation={onQuickOperation}
          />
          {item.split ? <div className="ml-1 contractive-filter-split mr-1" /> : null}
        </span>
      ))}

      {displayConditionsLen > 0 && (
        <span className={`contractive-filter-item-wrap ${fullWidth ? 'w-full' : ''}`}>
          <Dropdown
            trigger={['click']}
            overlayClassName="contractive-filter-item-dropdown"
            overlay={
              <Menu>
                <Menu.Item className="not-select">
                  <Input
                    autoFocus
                    size="small"
                    prefix={<ErdaIcon size="16" type="search1" />}
                    onClick={(e) => e.stopPropagation()}
                    value={hideFilterKey}
                    onChange={(e) => setHideFilterKey(e.target.value.toLowerCase())}
                    placeholder={i18n.t('common:Filter conditions')}
                  />
                </Menu.Item>
                <Menu.Divider />
                <Menu.Item className="not-select px6 py-0">
                  <div className="flex justify-between items-center">
                    <span>
                      {i18n.t('common:selected')} {showList.filter((a) => a.fixed !== true).length}{' '}
                      {i18n.t('common:items')}
                    </span>
                    <span className="fake-link" onClick={handleClearSelected}>
                      {i18n.t('common:clear selected')}
                    </span>
                  </div>
                </Menu.Item>
                <Menu.Divider />
                {conditions.map((item) => {
                  const { key, label, fixed, type } = item;
                  if (
                    fixed ||
                    (type === 'input' && fixed !== false) ||
                    (item.label && !item.label.toLowerCase().includes(hideFilterKey))
                  ) {
                    return null;
                  }
                  const handleClick = () => {
                    const haveShow = !!showList.find((a) => a.key === key);
                    setConditions(setConditionShowIndex(conditions, item.key, !haveShow));
                    if (!haveShow) {
                      setCloseAll(false);
                      setActiveMap((prev) => ({ ...prev, [item.key]: true }));
                    }
                  };
                  return (
                    <Menu.Item key={key} className="option-item" onClick={handleClick}>
                      <Checkbox checked={!!showList.find((a) => a.key === key)} className="mr-2" /> {label}
                    </Menu.Item>
                  );
                })}
              </Menu>
            }
            placement="bottomLeft"
          >
            <span className="contractive-filter-item more-conditions">
              <ErdaIcon color="black-800" type="plus" className="mr-0.5 color-text" />
              <span>{i18n.t('filter')}</span>
              <ErdaIcon type="caret-down" className="hover" size="16" />
            </span>
          </Dropdown>
        </span>
      )}
    </div>
  );
};

export default ContractiveFilter;
