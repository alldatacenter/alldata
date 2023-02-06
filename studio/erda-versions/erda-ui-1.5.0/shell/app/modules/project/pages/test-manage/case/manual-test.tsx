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

import { debounce } from 'lodash';
import i18n from 'i18n';
import React from 'react';
import { Button, Input, Tooltip } from 'antd';
import { ErdaIcon } from 'common';
import { SplitPage } from 'layout/common';
import { setSearch, updateSearch } from 'common/utils';
import routeInfoStore from 'core/stores/route';
import testCaseStore from 'project/stores/test-case';
import { CaseTable, CaseTree } from '../components';
import { columns } from './columns';
import BatchProcessing from './header/batch-processing';
import ExportFile from './header/export-file';
import ImportFile from './header/import-file';
import ImportExportRecord from './header/import-export-record';
import AddTestSet from './new-set';
import CaseFilterDrawer from './filter-drawer';
import ProjectTreeModal from './project-tree-modal';
import CaseDrawer from 'project/pages/test-manage/case/case-drawer';
import testEnvStore from 'project/stores/test-env';
import { useEffectOnce, useUpdateEffect } from 'react-use';
import moment from 'moment';
import './manual-test.scss';

const ManualTest = () => {
  const [query, params] = routeInfoStore.useStore((s) => [s.query, s.params]);
  const { getCases, getCaseDetail } = testCaseStore.effects;
  const { getTestEnvList } = testEnvStore;
  const caseRef = React.useRef(null as any);
  const [drawerVisible, setDrawerVisible] = React.useState(false);
  const [searchQuery, setSearchQuery] = React.useState(query.query);
  const [enhanceFilterVisible, setEnhanceFilterVisible] = React.useState(false);
  const [showRefresh, setShowRefresh] = React.useState(false);
  const [importExportRecordKey, setImportExportRecordKey] = React.useState(0);

  useEffectOnce(() => {
    getTestEnvList({ envID: +params.projectId, envType: 'project' });
  });

  React.useEffect(() => {
    setShowRefresh(false);
  }, [query.testSetID]);

  const closeEnhanceFilter = () => {
    setEnhanceFilterVisible(false);
  };

  const onSearch = React.useCallback((q: any) => {
    const { timestampSecUpdatedAtBegin, timestampSecUpdatedAtEnd } = q;
    if (timestampSecUpdatedAtBegin) {
      // eslint-disable-next-line no-param-reassign
      q.timestampSecUpdatedAtBegin = moment(+timestampSecUpdatedAtBegin)
        .startOf('day')
        .format('X');
    }
    if (timestampSecUpdatedAtEnd) {
      // eslint-disable-next-line no-param-reassign
      q.timestampSecUpdatedAtEnd = moment(+timestampSecUpdatedAtEnd)
        .endOf('day')
        .format('X');
    }
    updateSearch(q);
    getCases(q);
    setEnhanceFilterVisible(false);
  }, []);

  const debouncedSearch = React.useCallback(
    debounce((val: string | undefined) => {
      onSearch({ pageNo: 1, query: val });
    }, 1000),
    [onSearch],
  );

  useUpdateEffect(() => {
    debouncedSearch(searchQuery);
  }, [searchQuery]);

  const handleAddTestSetFromOut = (data: TEST_SET.TestSet) => {
    caseRef.current && caseRef.current.addNodeFromOuter(data);
  };

  const afterImport = () => {
    const { testSetID, eventKey } = query;
    caseRef.current && caseRef.current.reloadLoadData(testSetID, eventKey, false);
    setImportExportRecordKey(importExportRecordKey + 1);
  };

  const afterExport = () => {
    setImportExportRecordKey(importExportRecordKey + 1);
  };

  const showCaseDrawer = () => {
    getTestEnvList({ envID: +params.projectId, envType: 'project' });
    setDrawerVisible(true);
  };

  const refreshList = () => {
    if (query.query) {
      setSearch({}, ['testSetID', 'eventKey'], true);
    } else {
      getCases();
    }
    setShowRefresh(false);
  };
  return (
    <SplitPage>
      <SplitPage.Left>
        <div className="section-title mb-0">
          <span>{i18n.t('dop:test set')}</span>
          <AddTestSet afterCreate={handleAddTestSetFromOut} />
        </div>

        <div style={{ position: 'relative', overflow: 'auto' }}>
          <CaseTree
            ref={(e) => {
              caseRef.current = e;
            }}
            needBreadcrumb
            needRecycled
            mode="testCase"
          />
        </div>
      </SplitPage.Left>
      <SplitPage.Right>
        <div className="section-title mb-0">
          <span>
            {i18n.t('dop:use case list')}
            <Tooltip title={i18n.t('dop:there is a new import case, click to refresh the list')}>
              <ErdaIcon
                color="black-800"
                size="16"
                fill="black"
                className={`ml-3 cursor-pointer ${showRefresh ? '' : 'hidden'}`}
                type="redo"
                onClick={() => refreshList()}
              />
            </Tooltip>
          </span>
        </div>
        <div className="flex justify-between items-center mb-3">
          <div className="ml-3-group flex flex-wrap">
            {query.recycled !== 'true' && (
              <>
                <Button
                  type="primary"
                  icon={
                    <div>
                      <ErdaIcon type="plus" className="mr-1 mt-1" size="14" />
                    </div>
                  }
                  onClick={showCaseDrawer}
                  className="mb-2 flex"
                >
                  <span>{i18n.t('dop:add use case')}</span>
                </Button>
                <ImportFile afterImport={afterImport} />
                <ExportFile afterExport={afterExport} />
              </>
            )}
            <CaseDrawer
              scope="testCase"
              visible={drawerVisible}
              onClose={() => {
                setDrawerVisible(false);
              }}
              afterClose={(saved) => {
                saved && getCases();
              }}
            />
            <BatchProcessing recycled={query.recycled === 'true'} />
            <ImportExportRecord
              key={importExportRecordKey}
              setShowRefresh={setShowRefresh}
              testSetId={+query.testSetID}
            />
            <ProjectTreeModal />
          </div>
          <div className="mr-3-group inline-flex" style={{ minWidth: '220px' }}>
            <Input
              style={{ width: '160px' }}
              placeholder={i18n.t('dop:search for')}
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              prefix={<ErdaIcon type="search1" size="14" className="mr-1" />}
            />
            <Button onClick={() => setEnhanceFilterVisible(true)}>
              <ErdaIcon stroke="black-800" className="mt-0.5" width="16" height="18" type="filter" />
            </Button>
            <CaseFilterDrawer visible={enhanceFilterVisible} onSearch={onSearch} onClose={closeEnhanceFilter} />
          </div>
        </div>
        <CaseTable
          columns={columns}
          scope="testCase"
          onClickRow={(record: any) => {
            if (record.id) {
              getCaseDetail({ id: record.id, scope: 'testCase' });
              showCaseDrawer();
            }
          }}
        />
      </SplitPage.Right>
    </SplitPage>
  );
};

export default ManualTest;
