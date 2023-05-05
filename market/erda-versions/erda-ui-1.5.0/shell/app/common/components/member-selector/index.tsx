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

/* eslint-disable react-hooks/exhaustive-deps */
import React from 'react';
import projectMemberStore from 'common/stores/project-member';
import orgMemberStore from 'common/stores/org-member';
import appMemberStore from 'common/stores/application-member';
import { map, debounce, isEmpty, get, isArray, isString, difference, compact } from 'lodash';
import { getUsers, getMembers, getUsersNew, getPlatformUserList, searchPlatformUserList } from 'common/services';
import { MemberScope } from 'common/stores/member-scope';
import { LoadMoreSelector } from 'common';
import { Tag, Select, Avatar } from 'antd';
import { useMount } from 'react-use';
import i18n from 'i18n';
import { ILoadMoreSelectorProps } from './load-more-selector';
import routeInfoStore from 'core/stores/route';
import orgStore from 'app/org-home/stores/org';
import userStore from 'app/user/stores';
import sysMemberStore from 'common/stores/sys-member';
import { useUserMap } from 'core/stores/userMap';
import mspProjectMember from 'common/stores/msp-project-member';
import { getAvatarChars } from 'app/common/utils';

const storeMap = {
  [MemberScope.PROJECT]: projectMemberStore,
  [MemberScope.ORG]: orgMemberStore,
  [MemberScope.APP]: appMemberStore,
  [MemberScope.MSP]: mspProjectMember,
  [MemberScope.SYS]: sysMemberStore,
};

interface IOption {
  label: string;
  value: string | number;
}

interface IProps extends ILoadMoreSelectorProps {
  scopeType: 'org' | 'project' | 'app' | 'uc' | 'sys';
  showRole?: boolean;
  scopeId?: string;
  showSelfChosen?: boolean;
  selectSelfInOption?: boolean;
  selectNoneInOption?: boolean;
}

interface IPropsWithCategory extends ILoadMoreSelectorProps {
  categorys: IOption[];
  getData: (arg: any) => Promise<any>;
}

const { Option } = Select;

const optionRender = (user: IMember, roleMap?: object, _type?: string, showRole?: boolean) => {
  const { avatar, nick, name, roles } = user;
  return (
    <>
      <Avatar src={avatar || undefined} size="small">
        {nick ? getAvatarChars(nick) : i18n.t('none')}
      </Avatar>
      {
        <span className="ml-2" title={name}>
          {nick || i18n.t('common:none')}
          {_type === 'normal' && roleMap && showRole
            ? `(${map(roles, (role) => roleMap[role] || i18n.t('common:none')).join(',')})`
            : ''}
        </span>
      }
    </>
  );
};

const valueItemRender =
  (size = 'normal') =>
  (user: any, deleteValue: (item: any) => void, isMultiple?: boolean) => {
    const { avatar, nick, name, label, value } = user;
    const displayName = value === USER_NONE ? i18n.t('unspecified') : nick || label || value || i18n.t('common:none');
    const cls = {
      normal: {
        size: 20,
        name: 'ml-2 text-sm',
        tag: 'py-1 px-2',
      },
      small: {
        size: 14,
        name: 'ml-2',
        tag: 'py-0.5 px-1 member-value-small',
      },
    };
    const curCls = cls[size] || {};
    const item = (
      <>
        <Avatar size={curCls.size} src={avatar || undefined}>
          {nick ? getAvatarChars(nick) : i18n.t('none')}
        </Avatar>
        <span className={curCls.name} title={name}>
          {displayName}
        </span>
      </>
    );
    if (!isMultiple) {
      return item;
    }
    return (
      <Tag key={name} size="small" closable className={curCls.tag} onClose={() => deleteValue(user)}>
        {item}
      </Tag>
    );
  };

// existUser: 来自loginUser和userMap
export const chosenItemConvert = (values: any, existUser: object) => {
  // existUser
  const curValues = isArray(values) ? values : values && [values];
  const reValue = map(curValues, (item) => {
    const curUser = existUser[item.value] || {};
    return item.label ? item : { ...curUser, ...item, label: curUser.nick || curUser.name };
  });

  return reValue;
};

export const getNotFoundContent = (scopeType: string) => {
  let tip;
  switch (scopeType) {
    case MemberScope.ORG:
      tip = i18n.t('common:please confirm that the user has joined the organization');
      break;
    case MemberScope.PROJECT:
      tip = i18n.t('common:please confirm that the user has joined the project');
      break;
    case MemberScope.APP:
      tip = i18n.t('common:please confirm that the user has joined the application');
      break;
    default:
      break;
  }
  return tip;
};

const USER_NONE = 'unassigned';

const MemberSelector = React.forwardRef((props: XOR<IProps, IPropsWithCategory>, ref: any) => {
  const {
    scopeType = 'org',
    scopeId: _scopeId,
    showRole,
    type,
    notFoundContent,
    value,
    categorys: staticCategory,
    getData: _getData,
    className = '',
    size,
    showSelfChosen = false,
    placeholder,
    selectSelfInOption,
    selectNoneInOption,
    onChange: _onChange,
    ...rest
  } = props;
  const { projectId, appId } = routeInfoStore.useStore((s) => s.params);
  const { id: loginUserId } = userStore.getState((s) => s.loginUser);
  const orgId = orgStore.useStore((s) => s.currentOrg.id);
  const isUCMember = scopeType === 'uc';
  const scopeIdMap = {
    app: appId,
    project: projectId,
    org: orgId,
  };
  let scopeId = _scopeId;
  let getRoleMap = undefined as ((arg: any) => Promise<any>) | undefined;
  let roleMap = undefined as Obj | undefined;
  if (scopeType && !isUCMember) {
    // scope模式
    const memberStore = storeMap[scopeType];
    ({ getRoleMap } = memberStore.effects);
    scopeId = _scopeId || (scopeIdMap[scopeType] as string);
    roleMap = memberStore.useStore((s) => s.roleMap);
  }
  const [categories, setCategories] = React.useState([] as IOption[]);
  const [query, setQuery] = React.useState({} as any);
  const isCategoryMode = type === 'Category';
  const isStaticCategory = !isEmpty(staticCategory); // 静态category模式
  const userMap = useUserMap();
  const loginUser = userStore.getState((s) => s.loginUser);
  const existUser = {
    ...(userMap || {}),
    [loginUser.id]: { ...loginUser },
  };
  React.useEffect(() => {
    if (isStaticCategory) {
      setCategories(staticCategory as IOption[]);
      return;
    }
    isCategoryMode && !isUCMember && setCategories(map(roleMap, (val, key) => ({ label: val, value: key })));
  }, [isCategoryMode, roleMap, isStaticCategory]);

  const getData = (q: any = {}) => {
    const { category, ...qRest } = q;
    setQuery(q);
    if (scopeType === MemberScope.SYS) {
      if (qRest.q) {
        return searchPlatformUserList({ ...qRest }).then((res) => ({
          list: res.data.users,
          total: res.data.users.length,
        }));
      }
      return getPlatformUserList({ ...qRest }).then((res) => res.data);
    }
    if (!scopeId) return;
    return getMembers({ scopeId, scopeType, roles: [category], ...qRest }).then((res: any) => res.data);
  };

  useMount(() => {
    !isUCMember && !isStaticCategory && getRoleMap && getRoleMap({ scopeType, scopeId });
  });
  const notFoundContentTip = !isUCMember && (notFoundContent || getNotFoundContent(scopeType || ''));

  if (isUCMember) {
    return <UserSelector {...(props as any)} />;
  }

  const selectSelfOp = () => {
    if (`${value}` !== `${loginUserId}`) onChange(rest?.mode === 'multiple' ? [loginUserId] : loginUserId);
  };

  const selectSelf = selectSelfInOption ? (
    <a
      onClick={() => !rest.disabled && selectSelfOp()}
      className={`${rest.disabled ? 'not-allowed' : 'text-primary cursor-pointer'}`}
    >
      {i18n.t('choose self')}
    </a>
  ) : null;

  const selectNoneOp = () => {
    onChange(rest?.mode === 'multiple' ? [USER_NONE] : USER_NONE);
  };

  const selectNone = selectNoneInOption ? (
    <a
      onClick={() => !rest.disabled && selectNoneOp()}
      className={`${rest.disabled ? 'not-allowed' : 'text-primary cursor-pointer'}`}
    >
      {i18n.t('unspecified')}
    </a>
  ) : null;

  const quickSelect = compact([selectSelf, selectNone]);

  const onChange = (val: string[] | string, options?: IOption | IOption[]) => {
    let _val = val;
    if (isArray(val) && val.includes(USER_NONE) && val.length > 1) {
      // delete user_none when select other user
      _val = _val.filter((vItem: string) => vItem !== USER_NONE);
    }
    _onChange?.(_val, options);
  };

  return (
    <>
      <LoadMoreSelector
        getData={_getData || getData}
        type={type}
        allowClear
        key={`${scopeType}-${type}`}
        className={`member-selector ${className}`}
        notFoundContent={notFoundContentTip && query.q ? notFoundContentTip : undefined}
        placeholder={placeholder || i18n.t('please choose {name}', { name: i18n.t('user') })}
        category={categories}
        dataFormatter={({ list, total }) => ({
          total,
          list: map(list, ({ name, userId, nick, id, ..._rest }) => ({
            ..._rest,
            id,
            userId,
            nick,
            name,
            label: nick || name,
            value: userId || id,
          })),
        })}
        optionRender={(item, _type) => optionRender(item as any, roleMap, _type, isCategoryMode || showRole)} // 若为category模式，默认normal情况显示角色
        valueItemRender={valueItemRender(size)}
        chosenItemConvert={(v: any[]) => chosenItemConvert(v, existUser)}
        value={value}
        forwardedRef={ref}
        quickSelect={quickSelect}
        size={size}
        onChange={onChange}
        {...rest}
      />
      {showSelfChosen ? (
        <a
          onClick={() => !rest.disabled && selectSelfOp()}
          className={`${rest.disabled ? 'not-allowed' : 'text-primary cursor-pointer'} ml-2`}
        >
          {i18n.t('choose self')}
        </a>
      ) : null}
    </>
  );
});

interface IAddProps extends ILoadMoreSelectorProps {
  scopeType: string;
  scopeId?: string;
}

export const UserSelector = (props: any) => {
  const { value } = props;
  const [searchKey, setSearchKey] = React.useState('');
  const [searchResult, setSearchResult] = React.useState([] as any[]);
  const [searchLackUser, setSearchLackUser] = React.useState(false);

  React.useEffect(() => {
    if (!isEmpty(value)) {
      // 兼容选项有值无list情况
      const curValue = isString(value) ? [value] : value;
      const existUserId = map(searchResult || [], 'id'); // 当前存在的user
      const lackUser = difference(curValue, existUserId);
      if (lackUser.length && !searchLackUser) {
        setSearchLackUser(true); // 请求过一次后就不再请求
        getUsers({ userID: lackUser }).then((res: any) => {
          setSearchResult(get(res, 'data.users'));
        });
      }
    }
  }, [value, searchResult, searchLackUser]);

  const handleSearch = (q: string) => {
    const query = {
      q,
      pageNo: 1,
      pageSize: 15,
    };
    setSearchKey(q);
    if (q.trim() !== '') {
      getUsersNew(query).then((res: any) => {
        setSearchResult(get(res, 'data.users'));
      });
    }
  };
  const userOptionRender = (member: IMember) => {
    const { avatar, nick, name } = member;
    const id = member.id || member.userId;
    return (
      <Option key={id} value={id}>
        <Avatar src={avatar} size="small">
          {nick ? getAvatarChars(nick) : i18n.t('none')}
        </Avatar>
        <span className="ml-2" title={name}>
          {nick || i18n.t('common:none')}
        </span>
      </Option>
    );
  };
  return (
    <Select
      className="w-full"
      showSearch
      notFoundContent={searchKey ? i18n.t('common:please confirm that the user is registered') : ''}
      showArrow={false}
      filterOption={false}
      defaultActiveFirstOption={false}
      placeholder={i18n.t('Please enter nickname, name, email or mobile phone number to search.')}
      onSearch={debounce(handleSearch, 800)}
      {...props}
    >
      {(searchResult || []).map(userOptionRender)}
    </Select>
  );
};

// 添加企业成员时，请求用户接口不一样
const AddMemberSelector = (props: IAddProps) => {
  const { scopeType } = props;
  const orgId = orgStore.useStore((s) => s.currentOrg.id);
  const projectId = routeInfoStore.getState((s) => s.params.projectId);

  const scopeInfoMap = {
    [MemberScope.ORG]: {}, // 添加企业成员，无scope
    [MemberScope.PROJECT]: {
      // 添加项目成员：从org成员中选择
      scopeType: MemberScope.ORG,
      scopeId: orgId,
    },
    [MemberScope.MSP]: {
      // 添加项目成员：从org成员中选择
      scopeType: MemberScope.ORG,
      scopeId: orgId,
    },
    [MemberScope.APP]: {
      // 添加项目成员：从project中选择
      scopeType: MemberScope.PROJECT,
      scopeId: projectId,
    },
  };
  return scopeType === MemberScope.ORG ? (
    <UserSelector {...(props as any)} />
  ) : (
    <MemberSelector {...props} {...scopeInfoMap[scopeType]} />
  );
};

MemberSelector.Add = AddMemberSelector;
export default MemberSelector;
