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
import { map, sortBy, get, set, findIndex, isEqual, filter, reduce, reverse } from 'lodash';
import classnames from 'classnames';
import { Icon as CustomIcon, DeleteConfirm, RenderForm } from 'common';
import { Popover, Tooltip, Button } from 'antd';
import { regRules } from 'common/utils';
import { getTableList } from './utils';
import i18n from 'i18n';
import { useMount } from 'react-use';
import './perm-editor.scss';

const blankKey = 'blk';
const noop = () => {};
const actionData = {
  pass: false,
  role: [],
};
interface IProps {
  data: any;
  scope: string;
  isEdit: boolean;
  originData: Obj;
  originRoleMap: Obj;
  currentData: Obj;
  roleMap: Obj;
  filterKey: string;
  onChangeRole?: (arg: IRoleChange) => void;
  deleteData?: (dataKey: string) => void;
  editData?: (arg: any) => void;
  onChangeData?: (arg: any) => void;
}

export interface IRoleChange {
  key: string;
  role: string;
  checked: boolean;
}

export const PermTable = (props: IProps) => {
  const { data, scope, roleMap, filterKey, ...rest } = props;
  const curRoleMap = roleMap[scope];
  const [startInit, setStartInit] = React.useState(false);
  const [hoverKey, setHoverKey] = React.useState('');
  const headScrollRef = React.useRef(null as any);
  const bodyScrollRef = React.useRef(null as any);

  const tableList = getTableList(data, scope, filterKey);

  React.useEffect(() => {
    setStartInit(true);
  }, []);

  const onHover = (hk: string) => setHoverKey(hk);
  const outHover = () => setHoverKey('');

  const onBodyScroll = () => {
    headScrollRef.current.scrollLeft = bodyScrollRef.current.scrollLeft;
  };

  const onHeadScroll = () => {
    bodyScrollRef.current.scrollLeft = headScrollRef.current.scrollLeft;
  };

  const permColumnKeys = sortBy(Object.keys(get(tableList, '[0]') || {}), (item) => {
    return item.startsWith('depth') ? Number(item.slice(5)) : item === 'action' ? 1000 : 10000;
  });
  const roleColumnKeys = map(curRoleMap, (item) => `role-${item.value}`);
  return (
    <div className="perm-editor-container h-full">
      <div className="perm-head">
        <div className="head-container column-head">{getPermHead({ columnKeys: permColumnKeys, scope })}</div>
        <div className="head-container role-head" ref={headScrollRef} onScroll={onHeadScroll}>
          {getPermHead({ columnKeys: roleColumnKeys, scope, curRoleMap })}
        </div>
      </div>
      <div className="perm-body">
        <div className="perm-body-scroll-box">
          <div className="column-container column-perm">
            <PermColumn
              columnKeys={permColumnKeys}
              list={tableList}
              scope={scope}
              key={filterKey}
              hoverKey={hoverKey}
              onLoad={() => setStartInit(!startInit)}
              onHover={onHover}
              outHover={outHover}
              {...rest}
            />
          </div>
          <div className="column-container column-role" ref={bodyScrollRef} onScroll={onBodyScroll}>
            <RoleColumn
              columnKeys={roleColumnKeys}
              permColumnKeys={permColumnKeys}
              list={tableList}
              scope={scope}
              key={filterKey}
              onLoad={() => setStartInit(!startInit)}
              hoverKey={hoverKey}
              onHover={onHover}
              outHover={outHover}
              {...rest}
            />
          </div>
        </div>
      </div>
    </div>
  );
};

const getPermHead = (params: { columnKeys: string[]; scope: string; curRoleMap?: Obj }) => {
  const { columnKeys, scope, curRoleMap = {} } = params;
  return map(columnKeys, (item) => {
    const headText = item.startsWith('depth')
      ? `${i18n.t('user:permissions level {level}', { level: +item.slice(5) + 1 })}`
      : item === 'action'
      ? i18n.t('user:permission operation')
      : get(curRoleMap, `${item.slice(5)}.name`) || '-';
    const isRoleHead = item.startsWith('role-');
    let width;
    if (!isRoleHead) {
      const eles = document.getElementsByClassName(`${scope}-perm-column-${item}`);
      width = get(eles, '[0].offsetWidth');
    }
    const roleText = isRoleHead ? item.replace('role-', '') : '';
    return (
      <div className={`column-head-item ${scope}-head-column-${item}`} key={item}>
        <div className={`head-text nowrap head-column-${item}`} style={{ width }}>
          {headText}
          {roleText ? <div className="text-xs">{roleText}</div> : null}
        </div>
      </div>
    );
  });
};

interface IGetColumnParmas {
  list: any[];
  columnKeys: string[];
  scope: string;
  hoverKey: string;
  isEdit: boolean;
  originData: Obj;
  originRoleMap: Obj;
  currentData: Obj;
  permColumnKeys?: string[];
  onLoad: () => void;
  onChangeRole?: (arg: IRoleChange) => void;
  onChangeData?: (arg: any) => void;
  deleteData?: (dataKey: string) => void;
  editData?: (arg: any) => void;
  onHover: (arg: string) => void;
  outHover: (arg: string) => void;
}
const PermColumn = (props: IGetColumnParmas) => {
  const {
    list: tableList,
    columnKeys,
    scope,
    currentData,
    onHover,
    outHover,
    hoverKey = '',
    isEdit,
    deleteData = noop,
    editData = noop,
    onLoad,
  } = props;
  useMount(() => {
    onLoad();
  });
  return (
    <>
      {map(columnKeys, (curColumnKey, index) => {
        const list = [] as any[];
        map(tableList, (item) => {
          const parentObj = {};
          const parentKeys = columnKeys.slice(
            0,
            findIndex(columnKeys, (k) => k === curColumnKey),
          );
          map(parentKeys, (pKey) => {
            parentObj[pKey] = item[pKey];
          });
          const curData = { ...item[curColumnKey], data: item, parents: parentObj };
          const curIndex = findIndex(list, (lItem) => {
            return lItem.key === curData.key && isEqual(lItem.parents, parentObj);
          });
          if (curIndex !== -1) {
            set(list, `[${curIndex}].count`, list[curIndex].count + 1);
          } else {
            list.push({ ...curData, count: 1 });
          }
        });
        const itemKey = `${scope}-perm-column-${curColumnKey}`;
        return (
          <div className={'perm-column'} key={itemKey}>
            {map(list, (item) => {
              const { count, name, key = blankKey, parents } = item;
              const dataKey = map(parents, (pItem) => pItem.key || blankKey)
                .concat(key)
                .join('.');
              const isActive = hoverKey.includes(dataKey);
              const cls = classnames({
                'perm-column-item': true,
                'hover-active-item': isActive,
              });
              const realDataKey = filter(dataKey.split('.'), (k) => k !== blankKey).join('.');
              return (
                <div
                  key={`${dataKey}-${name || ''}-${key}`}
                  className={cls}
                  style={{ height: 40 * count }}
                  onMouseEnter={() => onHover(dataKey)}
                  onMouseLeave={() => outHover(dataKey)}
                >
                  <Tooltip title={name}>
                    <div className={`nowrap ${itemKey}`}>{name || '-'}</div>
                  </Tooltip>
                  {isEdit && key !== blankKey && hoverKey === dataKey ? (
                    <div className="perm-operation">
                      <PermOperation
                        index={index}
                        curDataKey={realDataKey}
                        curColumnKey={curColumnKey}
                        dataItem={item}
                        deleteData={deleteData}
                        editData={editData}
                        wholeData={currentData}
                      />
                    </div>
                  ) : null}
                </div>
              );
            })}
          </div>
        );
      })}
    </>
  );
};

const RoleColumn = (props: IGetColumnParmas) => {
  const {
    list: tableList,
    columnKeys,
    scope,
    onHover,
    outHover,
    hoverKey,
    permColumnKeys = [],
    isEdit,
    onChangeRole = noop,
    originData,
    originRoleMap,
    onLoad,
  } = props;
  useMount(() => {
    onLoad();
  });
  return (
    <>
      {map(columnKeys, (curColumnKey) => {
        return (
          <div className={`perm-column ${scope}-perm-column-${curColumnKey}`} key={curColumnKey}>
            {map(tableList, (item, idx) => {
              const curRoleKey = curColumnKey.slice(5);
              const dataKey = map(permColumnKeys, (pKey) => item[pKey].key || blankKey)
                .concat(blankKey)
                .join('.');
              const isActive = hoverKey.includes(dataKey);
              const isChecked = item.action.role.includes(curRoleKey);
              const realDataKey = filter(dataKey.split('.'), (k) => k !== blankKey).join('.');
              const cls = classnames({
                iconfont: true,
                'perm-column-item': true,
                'role-column': true,
                [`${isEdit ? 'edit' : 'readonly'}-${isChecked ? 'checked' : 'un-checked'}`]: true,
                'hover-active-item': isActive,
                changed:
                  !get(originRoleMap, `${scope}.${curRoleKey}`) ||
                  get(originData, `${realDataKey}`) === undefined ||
                  (get(originData, `${realDataKey}.role`) || []).includes(curRoleKey) !== isChecked, // 跟源数据相比，是否改变
              });
              const eles = document.getElementsByClassName(`${scope}-head-column-${curColumnKey}`);
              const width = get(eles, '[0].offsetWidth');
              return (
                <div
                  key={`${scope}-perm-column-${curColumnKey}-${idx}`}
                  className={cls}
                  style={{ height: 40, width }}
                  onMouseEnter={() => onHover(dataKey)}
                  onMouseLeave={() => outHover(dataKey)}
                  onClick={() => {
                    isEdit && onChangeRole({ key: realDataKey, role: curRoleKey, checked: !isChecked });
                  }}
                />
              );
            })}
          </div>
        );
      })}
    </>
  );
};

interface IOperationProps {
  index: number;
  curDataKey: string;
  curColumnKey: string;
  dataItem: Obj;
  wholeData: Obj;
  deleteData?: (dataKey: string) => void;
  editData?: (arg: any) => void;
}

const addTip = '格式: key1:权限1>key2:权限2>[key1:操作1,key2:操作2]';
const regPermKey = (str: string) => {
  let errorTip = '';
  if (str) {
    const [key, name] = str.split(':');
    if (key && name) {
      if (!regRules.commonStr.pattern.test(key)) {
        errorTip = `key${regRules.commonStr.message}`;
      }
    } else {
      errorTip = addTip;
    }
  } else {
    errorTip = addTip;
  }
  return errorTip;
};

const fieldLayout = {
  labelCol: { span: 4 },
  wrapperCol: { span: 20 },
};

const PermOperation = (props: IOperationProps) => {
  const { index, curDataKey, wholeData, curColumnKey, dataItem, deleteData = noop, editData = noop } = props;
  const keyArr = curDataKey.split('.');
  const curKey = keyArr.pop();
  const [editFormVis, setEditFormVis] = React.useState(false);
  const editFormRef = React.useRef(null as any);
  const [addFormVis, setAddFormVis] = React.useState(false);
  const addFormRef = React.useRef(null as any);

  const handleAddSubmit = (form: FormInstance) => {
    form.validateFields().then((values: { addStr: string }) => {
      const { addStr = '' } = values;
      const permArr = addStr.split('>');
      const newData = reduce(
        reverse(permArr),
        (sumObj, item, idx) => {
          let reObj = { ...sumObj };
          if (idx === 0) {
            const actionStrArr = item.split('');
            const actions = actionStrArr
              .slice(1, actionStrArr.length - 1)
              .join('')
              .split(',');
            const actionObj = {};
            map(actions, (aItem) => {
              const [aKey, aName] = aItem.split(':');
              actionObj[aKey] = { ...actionData, name: aName };
            });
            reObj = { ...reObj, ...actionObj };
            return reObj;
          } else {
            const [key, name] = item.split(':');
            reObj = { [key]: { name, ...reObj } };
          }
          return reObj;
        },
        {},
      );
      editData({ keyPath: keyArr.join('.'), preKey: curKey, subData: newData });
      setAddFormVis(false);
    });
  };

  const addFieldList = [
    {
      label: '添加项',
      name: 'addStr',
      className: 'mb-2',
      rules: [
        {
          validator: (rule: any, v: string, callback: Function) => {
            if (v) {
              const strArr = v.split('>');
              let errorTip = '';
              for (let i = 0, len = strArr.length; i < len; i++) {
                if (i !== strArr.length - 1) {
                  errorTip = regPermKey(strArr[i]);
                } else if (!(strArr[i].startsWith('[') && strArr[i].endsWith(']'))) {
                  errorTip = addTip;
                } else {
                  const actionStrArr = strArr[i].split('');
                  const actions = actionStrArr
                    .slice(1, actionStrArr.length - 1)
                    .join('')
                    .split(',');
                  for (let j = 0, len2 = actions.length; j < len2; j++) {
                    !errorTip && (errorTip = regPermKey(actions[j]));
                  }
                }
                if (errorTip) break;
              }
              if (errorTip) {
                return callback(errorTip);
              }
            }
            callback();
          },
        },
      ],
      extraProps: { ...fieldLayout },
      itemProps: {
        autoComplete: 'off',
        placeholder: `${addTip}`,
      },
    },
    {
      label: '',
      className: 'mb-1',
      extraProps: { ...fieldLayout },
      getComp: ({ form }: { form: FormInstance }) => (
        <div className="flex flex-wrap justify-center items-center">
          <Button type="primary" onClick={() => handleAddSubmit(form)}>
            添加
          </Button>
        </div>
      ),
    },
  ];

  const handleEditSubmit = (form: FormInstance) => {
    form.validateFields().then((values: any) => {
      editData({ ...values, keyPath: keyArr.join('.'), preKey: curKey });
      setEditFormVis(false);
    });
  };

  const editFieldList = [
    {
      label: 'key',
      name: 'key',
      className: 'mb-2',
      initialValue: dataItem.key,
      rules: [
        { ...regRules.commonStr },
        {
          validator: (rule: any, v: string, callback: Function) => {
            const dKey = keyArr.concat(v).join('.');
            if (v && v !== curKey && get(wholeData, dKey)) {
              return callback('存在相同的key,请重新填写');
            }
            callback();
          },
        },
      ],
      itemProps: {
        disabled: index === 0,
        autoComplete: 'off',
      },
      extraProps: { ...fieldLayout },
    },
    {
      label: '名称',
      name: 'name',
      className: 'mb-2',
      initialValue: dataItem.name,
      itemProps: {
        autoComplete: 'off',
      },
      extraProps: { ...fieldLayout },
    },
    {
      label: '',
      className: 'mb-1',
      extraProps: { ...fieldLayout },
      getComp: ({ form }: { form: FormInstance }) => (
        <div className="flex flex-wrap justify-center items-center">
          <Button type="primary" onClick={() => handleEditSubmit(form)}>
            保存
          </Button>
        </div>
      ),
    },
  ];

  const deleteOp =
    index !== 0 ? (
      <DeleteConfirm
        key="delete"
        title="确认删除?"
        secondTitle={`${curColumnKey !== 'action' ? `删除 (${dataItem.name}), 将会删除其所有关联子权限` : ''}`}
        onConfirm={() => {
          deleteData(curDataKey);
        }}
      >
        <CustomIcon className="perm-op-item" type="shanchu" />
      </DeleteConfirm>
    ) : null;

  const addOp =
    curColumnKey !== 'action' ? (
      <Popover
        key="add"
        placement="right"
        overlayClassName="dice-perm-edit-form add"
        title="添加"
        content={<RenderForm ref={addFormRef} list={addFieldList} />}
        visible={addFormVis}
        onVisibleChange={setAddFormVis}
      >
        <CustomIcon type="cir-add" className="perm-op-item" />
      </Popover>
    ) : null;

  const editOp = (
    <Popover
      overlayClassName="dice-perm-edit-form edit"
      content={<RenderForm ref={editFormRef} list={editFieldList} layout="vertical" />}
      placement="right"
      title="编辑"
      key="edit"
      visible={editFormVis}
      onVisibleChange={setEditFormVis}
    >
      <CustomIcon className="perm-op-item" type="setting" />
    </Popover>
  );
  const optArr = [deleteOp, editOp, addOp];
  return <div className="perm-operation-box">{optArr}</div>;
};
