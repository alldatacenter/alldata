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
import { Input, Dropdown, Menu, Tooltip } from 'antd';
import { EmptyHolder, Icon as CustomIcon, Ellipsis } from 'common';
import { useUpdate } from 'common/use-hooks';
import { map, get, find, filter, isEmpty, has, some, isEqual, last, compact } from 'lodash';
import { useUpdateEffect, useEffectOnce } from 'react-use';
import i18n from 'i18n';
import './index.scss';

interface IProps {
  value: string | undefined;
  disabled?: boolean;
  options?: IOption[];
  dropdownClassName?: string;
  showSearch?: boolean;
  dropdownMatchSelectWidth?: boolean;
  className?: string;
  onChange: (v: string) => void;
  onLoadData?: (selectedOptions: IOption[]) => void;
  valueConvert?: (v: number | string | string[]) => string;
  onBlur?: () => void;
  [pro: string]: any;
}
let compKey = 1;
const noop = (v: any) => v;
const InputSelect = (props: IProps) => {
  const {
    options = [],
    value: propsValue,
    onChange = noop,
    onLoadData = noop,
    disabled,
    valueConvert = noop,
    dropdownClassName = '',
    dropdownMatchSelectWidth = true,
    className = '',
    onBlur: propsOnBlur,
    showSearch = true,
    ...rest
  } = props || {};
  const [state, updater, update] = useUpdate({
    value: propsValue || (undefined as string | undefined),
    dropDownVis: false,
    contentWidth: undefined,
    compId: 1,
  });
  const inputRef = React.useRef(null as any);
  const { dropDownVis, value, compId } = state;
  const menuRef = React.useRef(null as any);
  const inputFocRef = React.useRef(false);

  useEffectOnce(() => {
    // 带上select的dropdownMatchSelectWidth特性
    updater.compId(compKey);
    compKey += 1;
    const inputMinWidth = get(inputRef.current, 'input.offsetWidth');
    if (dropdownMatchSelectWidth && inputMinWidth) {
      updater.contentWidth(inputMinWidth);
    }
  });

  useUpdateEffect(() => {
    onChange(state.value as string);
  }, [state.value]);

  React.useEffect(() => {
    // 控制点击外部关闭 dropdown
    const handleCloseDropdown = (e: MouseEvent) => {
      const wrappers = Array.from(document.querySelectorAll(`.input-select-input-${compId}`));
      const dropdowns = Array.from(document.querySelectorAll(`.input-select-dropdown-${compId} .dropdown-box`));
      const node = e.target as Node;
      const inner = wrappers.concat(dropdowns).some((wrap) => wrap.contains(node));
      if (!inner && !inputFocRef.current) {
        updater.dropDownVis(false);
      }
    };
    document.body.addEventListener('click', handleCloseDropdown);
    return () => document.body.removeEventListener('click', handleCloseDropdown);
  }, [compId, updater]);

  useUpdateEffect(() => {
    update({
      value: propsValue,
    });
  }, [propsValue]);

  useUpdateEffect(() => {
    if (!dropDownVis) {
      onBlur();
    }
  }, [dropDownVis]);

  const loadData = (selectedOptions: IOption[]) => {
    onLoadData(selectedOptions);
  };

  const onBlur = () => {
    if (!dropDownVis && propsOnBlur) {
      propsOnBlur();
    }
    inputFocRef.current = false;
  };

  const onChosenChange = (v: string[] | string | number) => {
    inputRef.current.focus();
    setTimeout(() => {
      update({
        value: valueConvert(v) as string,
        dropDownVis: false,
      });
    });
  };

  const getOverlay = () => {
    return (
      <Menu className={'input-select-dropdown-menu'} ref={menuRef}>
        <Menu.Item>
          <Selector
            options={options}
            width={state.contentWidth}
            onChange={onChosenChange}
            loadData={loadData}
            showSearch={showSearch}
          />
        </Menu.Item>
      </Menu>
    );
  };
  if (disabled) {
    return <Input value={value} disabled />;
  }

  const onFocus = (e: any) => {
    e && e.stopPropagation();
    update({
      dropDownVis: true,
    });

    inputFocRef.current = true;
  };

  return (
    <Dropdown
      overlay={getOverlay()}
      visible={dropDownVis}
      overlayClassName={`input-select-dropdown input-select-dropdown-${compId} ${dropdownClassName}`}
    >
      <Input
        {...rest}
        className={`${className} input-select-input input-select-input-${compId}`}
        value={value}
        ref={inputRef}
        autoComplete={'off'}
        onFocus={onFocus}
        disabled={disabled}
        onBlur={onBlur}
        onKeyDown={(e) => {
          e.stopPropagation();
        }}
        onChange={(e) => {
          updater.value(e.target.value);
        }}
      />
    </Dropdown>
  );
};

export default InputSelect;

interface IOption {
  label: string;
  value: string | number;
  disabled?: boolean;
  isLeaf?: boolean;
  children?: IOption[];
  tooltip?: string;
}

interface SelectorProps {
  showSearch?: boolean;
  width?: string;
  dropdownMatchSelectWidth?: boolean;
  options: IOption[];
  onChange: (v: string | number | string[], op?: IOption) => void;
  loadData?: (selectOptions: IOption[]) => void;
}

// 是否是单纯select组件
const isSelectorData = (option: IOption[]) => {
  return !some(option, (op) => has(op, 'isLeaf'));
};

const getFilterOptions = (options: IOption[], searchValue: string | undefined) => {
  return filter(options, (_op) => (_op.label || '').toLowerCase().includes((searchValue || '').toLowerCase()));
};

const Selector = (props: SelectorProps) => {
  const { options } = props;
  const isSelect = isSelectorData(options);
  if (isSelect) {
    return <PureSelect {...props} />;
  }

  return <Cascader {...props} />;
};

const PureSelect = (props: SelectorProps) => {
  const { options, width, onChange = noop, showSearch } = props;
  const [{ value, searchValue }, updater, update] = useUpdate({
    value: undefined as string | number | undefined,
    searchValue: undefined as string | undefined,
  });

  const onSelect = (op: IOption) => {
    updater.value(op.value);
    onChange(op.value, op);
  };

  const onChangeSearch = (e: React.ChangeEvent<HTMLInputElement>) => {
    updater.searchValue(e.target.value);
  };

  const useOptions = getFilterOptions(options, searchValue);
  return (
    <div className={'input-select-dropdown-box  dropdown-box column'} style={width ? { width } : undefined}>
      {showSearch ? (
        <div className="p-1">
          <Input
            size="small"
            placeholder={i18n.t('filter')}
            value={searchValue}
            onFocus={(e) => e.stopPropagation()}
            onChange={onChangeSearch}
            onClick={(e) => e.stopPropagation()}
          />
        </div>
      ) : null}
      <div className="flex-1 input-select-options">
        {map(useOptions, (op) => {
          return (
            <div
              key={op.value}
              className={`option-item ${value === op.value ? 'text-primary bg-light-active' : ''}`}
              onClick={(e) => {
                e.stopPropagation();
                onSelect(op);
              }}
            >
              {op.tooltip ? (
                <Tooltip title={op.tooltip} placement="right">
                  <span className="w-full">{op.label}</span>
                </Tooltip>
              ) : (
                <Ellipsis placement="right" title={op.label}>
                  {op.label}
                </Ellipsis>
              )}
            </div>
          );
        })}
      </div>
      {isEmpty(useOptions) ? <EmptyHolder relative /> : null}
    </div>
  );
};

const getValueByOptions = (options: IOption[], value: IOption[]) => {
  const reValue = [] as IOption[];
  map(value, (vItem) => {
    if (reValue.length === 0) {
      reValue.push(find(options, { value: vItem.value }) as IOption);
    } else {
      const lastVal = last(reValue) as IOption;
      lastVal && reValue.push(find(lastVal.children, { value: vItem.value }) as IOption);
    }
  });
  return compact(reValue);
};

const Cascader = (props: SelectorProps) => {
  const { options, onChange = noop, showSearch, loadData } = props;
  const [{ values }, updater, update] = useUpdate({
    values: [] as IOption[],
  });

  const onSelect = (op: IOption, idx: number) => {
    const curValue = idx === 0 ? [op] : values.slice(0, idx).concat(op);
    updater.values(curValue);
  };

  useUpdateEffect(() => {
    const lastVal = last(values);
    if (lastVal) {
      if (lastVal.isLeaf === false) {
        // 选中了非叶子节点，且无子，触发请求
        loadData &&
          (lastVal.children === undefined || lastVal.children === null) &&
          loadData(
            map(values, (vItem) => {
              const { children, ...vRest } = vItem;
              return { ...vRest };
            }),
          );
      } else {
        // 选中叶子，触发onChange
        onChange(map(values, 'value') as string[]);
      }
    }
  }, [values]);

  React.useEffect(() => {
    const reVal = getValueByOptions(options, values);
    if (!isEqual(reVal, values)) {
      updater.values(reVal);
    }
  }, [options, updater, values]);

  return (
    <div className="input-cascader-dropdown-box">
      <OptionGroup
        options={options}
        showSearch={showSearch}
        onSelect={(v) => onSelect(v, 0)}
        chosenOption={values[0]}
      />
      {map(values, (vItem, idx) => {
        if (vItem.children !== undefined && vItem.children !== null) {
          return isEmpty(vItem.children) ? (
            <div className="option-group" key={idx}>
              <EmptyHolder relative />
            </div>
          ) : (
            <OptionGroup
              key={idx}
              options={vItem.children || []}
              showSearch={showSearch}
              onSelect={(v) => onSelect(v, idx + 1)}
              chosenOption={values[idx + 1]}
            />
          );
        }
        return null;
      })}
    </div>
  );
};

interface IOptionGroupProps {
  chosenOption?: IOption;
  showSearch?: boolean;
  options: IOption[];
  onSelect: (v: IOption) => void;
}

const OptionGroup = (props: IOptionGroupProps) => {
  const { chosenOption, showSearch, options, onSelect } = props;
  const [{ searchValue, showShadow }, updater, update] = useUpdate({
    searchValue: undefined as undefined | string,
    showShadow: false,
  });

  const handleScroll = (e: any) => {
    updater.showShadow(e.target.scrollTop !== 0);
  };
  return (
    <div className="option-group dropdown-box">
      {showSearch ? (
        <div className={`option-group-search p-1 ${showShadow ? 'shadow' : ''}`}>
          <Input
            size="small"
            value={searchValue}
            placeholder={i18n.t('filter')}
            onChange={(e: React.ChangeEvent<HTMLInputElement>) => {
              updater.searchValue(e.target.value);
            }}
          />
        </div>
      ) : null}
      <div className={'option-group-box '} onScroll={handleScroll}>
        {map(getFilterOptions(options, searchValue), (op) => {
          return (
            <div
              key={op.value}
              className={`option-item ${op.value === chosenOption?.value ? 'text-primary bg-light-active' : ''}`}
              onClick={(e) => {
                e.stopPropagation();
                onSelect(op);
              }}
            >
              {op.tooltip ? (
                <Tooltip title={op.tooltip} placement="left">
                  <span className="w-full">{op.label}</span>
                </Tooltip>
              ) : (
                <Ellipsis placement="left" title={op.label}>
                  {op.label}
                </Ellipsis>
              )}
              {op.isLeaf === false ? <CustomIcon type="chevronright" className="arrow" /> : null}
            </div>
          );
        })}
      </div>
    </div>
  );
};
