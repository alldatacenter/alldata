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
import i18n from 'i18n';
import { Pagination, Spin, Select } from 'antd';
import { map, get } from 'lodash';
import { EmptyListHolder, EmptyHolder, IF, DebounceSearch } from 'common';
import { useUpdate } from 'common/use-hooks';
import { updateSearch } from 'common/utils';
import { useLoading } from 'core/stores/loading';
import { ReleaseItem } from './components/release-item';
import ReleaseDetail from './components/release-detail';
import releaseStore from 'application/stores/release';
import { useEffectOnce, useUpdateEffect } from 'react-use';
import routeInfoStore from 'core/stores/route';

import { getBranchInfo } from 'application/services/application';
import appStore from 'application/stores/application';
import { AppSelector } from 'application/common/app-selector';

import './release-list.scss';

const { ELSE } = IF;
const { Option } = Select;

const ReleaseList = () => {
  const [list, paging] = releaseStore.useStore((s) => [s.list, s.paging]);
  const { pageNo: initPageNo, pageSize, total } = paging;
  const { getReleaseList } = releaseStore.effects;
  const { clearReleaseList } = releaseStore.reducers;
  const appDetail = appStore.useStore((s) => s.detail);
  const [params, query] = routeInfoStore.useStore((s) => [s.params, s.query]);
  const { projectId } = params;
  const [loading] = useLoading(releaseStore, ['getReleaseList']);
  const [state, updater, update] = useUpdate({
    pageNo: initPageNo,
    applicationId: appDetail.isProjectLevel ? query.applicationId : params.appId,
    chosenPos: 0, // TODO: use id
    branchInfo: [] as APPLICATION.IBranchInfo[],
    queryObj: {
      q: query.q,
      branchName: query.branchName as undefined | string,
    } as null | { q?: string; branchName?: string },
  });
  const { pageNo, applicationId, queryObj, chosenPos, branchInfo } = state;

  React.useEffect(() => {
    const arg: any = {
      pageNo,
      pageSize,
      applicationId,
      ...queryObj,
    };
    if (projectId) {
      arg.projectId = +projectId;
    }

    getReleaseList(arg);
  }, [applicationId, getReleaseList, pageNo, pageSize, projectId, queryObj]);

  useEffectOnce(() => {
    return () => clearReleaseList();
  });

  useUpdateEffect(() => {
    const { q, branchName } = queryObj || {};
    const urlObj = { branchName } as any;
    urlObj.q = q || undefined;
    urlObj.applicationId = applicationId;
    updateSearch(urlObj);
  }, [queryObj, applicationId]);

  React.useEffect(() => {
    if (applicationId) {
      getBranchInfo({ appId: +applicationId }).then((res: any) => {
        updater.branchInfo(res.data || []);
      });
    } else {
      updater.branchInfo([]);
    }
  }, [applicationId, updater]);

  const changePage = (num: number) => {
    if (num !== pageNo) {
      updater.pageNo(num);
    }
  };

  const releaseId = get(list, `[${chosenPos}].releaseId`) || ('' as string);
  return (
    <div className="release-list-container">
      <div className="release-list-page flex flex-col h-full">
        <IF check={appDetail.isProjectLevel}>
          <AppSelector
            projectId={`${projectId}`}
            className="mb-2 mx-4"
            allowClear
            onChange={(_appId: number) => {
              update({
                applicationId: _appId,
                queryObj: null,
              });
            }}
            value={applicationId}
          />
        </IF>
        <Select
          className="mb-2 mx-4"
          value={queryObj?.branchName}
          onChange={(v: any) => updater.queryObj({ ...queryObj, branchName: v })}
          placeholder={i18n.t('filter by {name}', { name: i18n.t('dop:branch') })}
          allowClear
        >
          {map(branchInfo, (branch) => (
            <Option key={branch.name} value={branch.name}>
              {branch.name}
            </Option>
          ))}
        </Select>
        <div className="mb-2 mx-4">
          <DebounceSearch
            className="w-full"
            value={queryObj?.q}
            placeholder={i18n.t('search by keywords')}
            onChange={(v: string) => {
              updater.queryObj({
                ...queryObj,
                q: v,
              });
            }}
          />
        </div>
        <Spin spinning={loading} wrapperClassName="flex-1 overflow-auto">
          {map(list, (item, index) => (
            <ReleaseItem
              data={item}
              key={item.releaseId}
              isActive={index === chosenPos}
              onClick={() => updater.chosenPos(index)}
            />
          ))}
          <IF check={list.length === 0 && !query.q}>
            <EmptyListHolder />
          </IF>
        </Spin>
        <IF check={query.q}>
          <div className="search-tip">
            <span>{i18n.t('cmp:No results found? Please try other keywords to search.')}</span>
          </div>
          <ELSE />
          <>
            {total && (
              <Pagination
                className="release-pagination"
                simple
                defaultCurrent={1}
                total={total}
                onChange={changePage}
              />
            )}
          </>
        </IF>
      </div>
      <div className="release-detail-container">
        <IF check={releaseId}>
          <ReleaseDetail releaseId={releaseId} data={list[chosenPos]} />
          <ELSE />
          <EmptyHolder relative style={{ justifyContent: 'start' }} />
        </IF>
      </div>
    </div>
  );
};

export default ReleaseList;
