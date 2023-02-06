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

import { Input, Collapse } from 'antd';
import { Icon as CustomIcon, EmptyListHolder } from 'common';
import { useUpdate } from 'common/use-hooks';
import classnames from 'classnames';
import React from 'react';
import './action-select.scss';
import { groupBy, map, get } from 'lodash';
import i18n from 'i18n';

interface IProps {
  value?: any;
  label: string;
  disabled?: boolean;
  placeholder: string;
  originActions: any[];
  onChange: (params: any) => void;
}

const { Panel } = Collapse;

interface IAddOnProps {
  className?: string | null;
  addon: IStageAction;
  editing?: boolean;
  onClick?: (addon: IStageAction) => void;
}

const AddOn = ({ addon, className, onClick, editing }: IAddOnProps) => {
  const { desc, name, logoUrl, displayName } = addon;

  return (
    <div onClick={() => editing && onClick && onClick(addon)} className={classnames('dice-yml-actions', className)}>
      {logoUrl ? (
        <img alt="logo" src={logoUrl} className="actions-icon" />
      ) : (
        <CustomIcon type="wfw" color className="actions-icon" />
      )}
      <span className="actions-info">
        <div className="actions-info-name">{displayName || name}</div>
        <div className="actions-info-description">{desc || '-'}</div>
      </span>
      <div className="actions-border-bottom" />
    </div>
  );
};

export default (props: IProps) => {
  const { label, disabled, placeholder, onChange, value, originActions } = props;
  const [{ actions, isFocus, isSelected, searchValue, selectedItem }, updater, update] = useUpdate({
    actions: [],
    isFocus: true,
    isSelected: true,
    searchValue: undefined,
    selectedItem: {} as any,
  });

  React.useEffect(() => {
    const actionArr = [] as DEPLOY.ExtensionAction[];
    map(originActions || [], (item) => {
      map(item.items, (subItem) => {
        actionArr.push({ ...subItem, group: item.name, groupDisplayName: item.displayName });
      });
    });
    updater.actions(actionArr);
  }, [originActions, updater]);
  React.useEffect(() => {
    const _selectedItem = actions.find((i: any) => i.name === value);
    update({
      selectedItem: _selectedItem,
      isSelected: _selectedItem !== null,
    });
  }, [actions, update, value]);

  const openSelect = () => {
    updater.isSelected(!isSelected);
  };

  const onFocus = (e: any) => {
    e.stopPropagation();
    updater.isFocus(true);
  };

  const renderSelectContent = () => {
    if (!isFocus && !isSelected) {
      return null;
    }
    if (actions.length === 0) {
      return (
        <div tabIndex={1} className="new-yml-editor-actions-list">
          <EmptyListHolder />
        </div>
      );
    }
    const actionsGroup = groupBy(actions, 'group');
    const addonsContent = (
      <Collapse className="new-yml-editor-collapse" defaultActiveKey={get(Object.keys(actionsGroup), 0)}>
        {map(actionsGroup, (actionArr, groupKey) => {
          const header = get(actionArr, '[0].groupDisplayName') || get(actionArr, '[0].group') || groupKey;
          return (
            <Panel header={header} key={groupKey}>
              {map(actionArr, (addon: IStageAction) => {
                let activeClass = null;
                // @ts-ignore
                if (selectedItem && selectedItem.name === addon.name) {
                  activeClass = 'actions-selected';
                }
                return (
                  <AddOn
                    editing={!disabled}
                    className={activeClass}
                    addon={addon}
                    key={addon.name}
                    onClick={selectedAddonAction}
                  />
                );
              })}
            </Panel>
          );
        })}
      </Collapse>
    );
    return (
      <div tabIndex={1} className="new-yml-editor-actions-list">
        {addonsContent}
      </div>
    );
  };

  const selectedAddonAction = (addon: IStageAction) => {
    if (onChange) {
      onChange(addon.name);
    }
  };

  const searchInputChange = (e: any) => {
    const actionArr = [] as DEPLOY.ExtensionAction[];
    const val = (e.target.value || '').toLowerCase();
    map(originActions || [], (item: DEPLOY.ExtensionAction) => {
      if (item.name?.toLowerCase().includes(val) || item.displayName?.toLowerCase().includes(val)) {
        map(item.items, (subItem) => {
          actionArr.push({ ...subItem, group: item.name, groupDisplayName: item.displayName });
        });
      } else {
        map(item.items, (subItem) => {
          if (subItem.name.toLowerCase().includes(val) || subItem.displayName.toLowerCase().includes(val)) {
            actionArr.push({ ...subItem, group: item.name, groupDisplayName: item.displayName });
          }
        });
      }
    });
    update({
      searchValue: val,
      actions: actionArr,
    });
  };

  const clear = () => {
    if (onChange) {
      onChange(null);
    }
  };

  let content = (
    <React.Fragment>
      <Input
        disabled={disabled}
        autoFocus
        onFocus={onFocus}
        onClick={openSelect}
        onChange={searchInputChange}
        value={searchValue}
        className="actions-input"
        placeholder={placeholder || `${i18n.t('dop:please choose')} Add-on`}
      />
      {renderSelectContent()}
    </React.Fragment>
  );
  if (selectedItem) {
    content = (
      <React.Fragment>
        <AddOn addon={selectedItem} />
      </React.Fragment>
    );
  }
  return (
    <div className="new-yml-editor-actions-select">
      <div className="actions-select-label">
        <span className="actions-select-label-required">*</span>
        {label}:
        {selectedItem && !disabled ? (
          <a onClick={clear} className="reselect">
            {i18n.t('dop:reselect')}
          </a>
        ) : null}
      </div>
      {content}
    </div>
  );
};
