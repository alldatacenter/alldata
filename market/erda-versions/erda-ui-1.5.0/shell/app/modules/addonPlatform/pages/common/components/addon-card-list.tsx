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
import { Select, Input, Spin, Tooltip } from 'antd';
import { get, cloneDeep, isEmpty, pickBy, head, debounce, forEach, find } from 'lodash';
import { CategoriesOrder } from 'app/modules/addonPlatform/pages/common/configs';
import { AddonCards } from './addon-cards';
import { useUpdateEffect } from 'react-use';
import { IF } from 'common';
import './addon-card-list.scss';
import i18n from 'i18n';

const { Option } = Select;
const { Search } = Input;

const envOptions = [
  { cnName: i18n.t('all'), enName: 'ALL' },
  { cnName: i18n.t('develop'), enName: 'DEV' },
  { cnName: i18n.t('test'), enName: 'TEST' },
  { cnName: i18n.t('staging'), enName: 'STAGING' },
  { cnName: i18n.t('prod'), enName: 'PROD' },
].map(({ cnName, enName }) => (
  <Option key={enName} value={enName}>
    {cnName}
  </Option>
));

const dataSourceTypeOptions = [
  { cnName: i18n.t('all'), enName: 'ALL' },
  { cnName: 'Redis', enName: 'redis' },
  { cnName: 'MySQL', enName: 'mysql' },
  { cnName: 'Custom', enName: 'custom' },
].map(({ cnName, enName }) => (
  <Option key={enName} value={enName}>
    {cnName}
  </Option>
));

const CustomDataSourceMap = ['Custom', 'AliCloud-Rds', 'AliCloud-Redis'];

interface IProps {
  addonList?: ADDON.Instance[];
  addonCategory?: any;
  isFetching: boolean;
  searchPlaceHolder: string;
  searchProps: string[];
  isPlatform?: boolean;
  hideSearch?: boolean;
  showDataSourceSearch?: boolean;
  showDataSourceSelect?: boolean;
  onEitAddon?: (addon: ADDON.Instance) => void;
}

const searchFilter = (source: ADDON.Instance, searchKey: string, properties: string[]) => {
  return properties.some((prop: string) => get(source, prop, '').toLowerCase().includes(searchKey.toLowerCase()));
};

const AddonCardList = (props: IProps) => {
  const [dataSource, setDataSource] = React.useState([] as any[]);
  const [searchKey, setSearchKey] = React.useState('');
  const [name, setName] = React.useState('');
  const [dataSourceType, setDataSourceType] = React.useState('ALL');
  const [env, setEnv] = React.useState('ALL');
  const [activeCategory, setActiveCategory] = React.useState();
  const categoryRefs = React.useRef([] as any[]);
  const categoryCardsRef = React.useRef(null);
  const { searchPlaceHolder, addonCategory, isFetching, onEitAddon, isPlatform } = props;

  const debounceCheck = React.useCallback(
    debounce(() => {
      const categoryList = categoryCardsRef.current as any;
      categoryRefs.current.forEach((categorySection: any) => {
        const { current } = head(Object.values(categorySection)) as any;
        if (current) {
          const top = current.offsetTop - categoryList.scrollTop - 52;
          const newActiveCategory = head(Object.keys(categorySection));
          if (top <= 0) {
            setActiveCategory(newActiveCategory);
          }
        }
      });
    }, 100),
    [],
  );

  const operateScrollEvent = (isRemove?: boolean) => {
    const categoryList = categoryCardsRef.current as any;
    if (categoryList) {
      !isRemove
        ? categoryList.addEventListener('scroll', debounceCheck)
        : categoryList.removeEventListener('scroll', debounceCheck);
    }
  };

  const scrollToTarget = (targetCategoryName: string) => {
    const targetRef = categoryRefs.current.find((ref: any) => ref[targetCategoryName]);
    const targetCategoryDom = targetRef[targetCategoryName].current;
    if (!targetCategoryDom) {
      return;
    }
    targetCategoryDom.scrollIntoView();
  };

  React.useEffect(() => {
    if (!isEmpty(props.addonCategory) && !isEmpty(dataSource)) {
      const firstCategory = head(dataSource) as any[];
      const targetCategory = head(firstCategory);
      scrollToTarget(targetCategory);
    }
  }, [dataSource, props.addonCategory]);

  React.useEffect(() => {
    operateScrollEvent();
    return () => {
      operateScrollEvent(true);
    };
  });
  useUpdateEffect(() => {
    const { addonList = [], searchProps } = props;
    if (!isEmpty(addonList)) {
      setDataSource(
        addonList.filter((addon: ADDON.Instance) => {
          const isOtherFilter =
            (addon.workspace === env || env === 'ALL') &&
            searchFilter(addon, searchKey, searchProps) &&
            searchFilter(addon, name, searchProps);

          if (dataSourceType === 'custom') {
            const isCustomDataSource = CustomDataSourceMap.includes(addon.displayName);
            return isOtherFilter && isCustomDataSource;
          }
          return isOtherFilter && (addon.displayName?.toLowerCase() === dataSourceType || dataSourceType === 'ALL');
        }),
      );
    } else if (!isEmpty(addonCategory)) {
      const cloneAddonCategory = cloneDeep(addonCategory);
      Object.keys(cloneAddonCategory).forEach((key: string) => {
        const value = cloneAddonCategory[key];
        cloneAddonCategory[key] = value.filter(
          (addon: IAddon) => (addon.workspace === env || env === 'ALL') && searchFilter(addon, searchKey, searchProps),
        );
      });
      const filterDataSource = Object.entries(pickBy(cloneAddonCategory, (x: any[]) => x.length > 0));
      const resolvedFilterDataSource: any[] = [];
      const categories = Object.keys(addonCategory);
      forEach(categories, (v) => {
        const targetItem = find(filterDataSource, (item) => item[0] === v);
        targetItem && resolvedFilterDataSource.push(targetItem);
      });
      setDataSource(resolvedFilterDataSource);
      categoryRefs.current = Object.keys(cloneAddonCategory).map((key: string) => {
        return { [key]: React.createRef() };
      });
      setActiveCategory(head(head(resolvedFilterDataSource)));
    } else {
      setDataSource([]);
    }
  }, [env, searchKey, props.addonList, props.addonCategory, props, name, dataSourceType]);

  const onSearchKeyChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    const value = get(event, 'target.value');
    setSearchKey(value);
  };

  const onNameChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    const value = get(event, 'target.value');
    setName(value);
  };

  const onEnvChange = (value: string) => {
    setEnv(value);
  };

  const onDataSourceChange = (value: any) => {
    setDataSourceType(value);
  };

  const onClickCategory = (event: React.MouseEvent) => {
    const targetCategory = event.currentTarget.getAttribute('value') as string;
    if (activeCategory === targetCategory) {
      return;
    }
    setActiveCategory(targetCategory);
    scrollToTarget(targetCategory);
  };

  const renderCategoryList = () => {
    if (addonCategory) {
      const categories = Object.keys(addonCategory);
      const resolvedCategories = categories.filter((v) => CategoriesOrder.includes(v));
      return resolvedCategories.map((key: string) => (
        <li
          key={key}
          className={`category-item cursor-pointer ${activeCategory === key ? 'active' : ''}`}
          value={key}
          onClick={onClickCategory}
        >
          <Tooltip title={key}>{key}</Tooltip>
        </li>
      ));
    }
    return null;
  };

  return (
    <section className="addon-card-list">
      <Spin wrapperClassName="full-spin-height" spinning={isFetching}>
        <div className="addon-filter">
          {!props.showDataSourceSearch && (
            <Select className="env-select" defaultValue="ALL" onChange={onEnvChange}>
              {envOptions}
            </Select>
          )}
          {props.hideSearch ? null : (
            <Search className="data-select" onChange={onSearchKeyChange} placeholder={searchPlaceHolder} />
          )}
          {props.showDataSourceSearch && (
            <Search className="data-select mr-5" onChange={onNameChange} placeholder={searchPlaceHolder} />
          )}
          {props.showDataSourceSelect && (
            <Select className="data-select" defaultValue="ALL" onChange={onDataSourceChange}>
              {dataSourceTypeOptions}
            </Select>
          )}
        </div>
        <div className="addons-content">
          <IF check={!isEmpty(addonCategory)}>
            <div className="addon-menu">
              <span className="content-title font-medium">{i18n.t('dop:addon category')}</span>
              <ul className="menu-list">{renderCategoryList()}</ul>
            </div>
          </IF>
          <AddonCards
            forwardedRef={categoryCardsRef}
            dataSource={dataSource}
            onEitAddon={onEitAddon}
            isPlatform={isPlatform}
            categoryRefs={categoryRefs.current}
          />
        </div>
      </Spin>
    </section>
  );
};

export { AddonCardList };
