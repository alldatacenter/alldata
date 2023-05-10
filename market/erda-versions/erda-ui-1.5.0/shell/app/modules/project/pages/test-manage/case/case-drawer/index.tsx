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
import { Drawer, Spin, Input, Tooltip, message } from 'antd';
import { Avatar, Copy, ErdaIcon, MarkdownEditor, UserInfo } from 'common';
import { useUpdate } from 'common/use-hooks';
import testCaseStore from 'project/stores/test-case';
import i18n from 'i18n';
import { fromNow, mergeSearch, qs, updateSearch } from 'common/utils';
import testSetStore from 'project/stores/test-set';
import routeInfoStore from 'core/stores/route';
import { CaseAPI, IApi, getEmptyApi } from './case-api';
import classnames from 'classnames';
import ContentPanel from './content-panel';
import CaseStep, { getEmptyStep } from './case-step';
import CaseMeta from './case-meta';
import SelectEnv from './select-env';
import CaseFooter from './case-footer';
import testEnvStore from 'project/stores/test-env';
import { cloneDeep, isUndefined, omitBy, pick } from 'lodash';
import { useLoading } from 'core/stores/loading';
import './index.scss';
import RelatedBugs from 'project/pages/test-manage/case/case-drawer/related-bugs';

interface IProps {
  caseList?: TEST_CASE.TestCaseItem[];
  scope: 'testPlan' | 'testCase';
  visible: boolean;
  onClose: () => void;
  afterClose?: (saved: boolean) => void;
  afterSave?: (value: TEST_CASE.CaseBody, isEdit: boolean, response: any) => Promise<void>;
}

interface ICaseDetail extends TEST_CASE.CaseDetail {
  id: number;
  apisFormat: IApi[];
}

interface IState {
  titleIsEmpty: boolean;
  fullData: ICaseDetail;
}

const pickKeys = ['headers', 'method', 'params', 'body', 'outParams', 'asserts'];

const bodyTypeMap = {
  Text: 'text',
  'Text(text/plain)': 'text/plain',
  'JSON(application/json)': 'application/json',
};

const defaultData = {
  name: '',
  preCondition: '',
  desc: '',
  stepAndResults: [],
  priority: 'P3',
  apis: [],
  apisFormat: [],
} as any as ICaseDetail;

const initState: IState = {
  titleIsEmpty: false,
  fullData: defaultData,
};

const checkParamKeyDuplicate = (list: any[]) => {
  const dupMap = {};
  list.forEach((item, i) => {
    const keyMap = {};
    item.outParams.forEach((p: any) => {
      if (!keyMap[p.key]) {
        keyMap[p.key] = true;
      } else {
        dupMap[i + 1] = true;
      }
    });
  });
  return Object.keys(dupMap);
};

const doCheck = (data: ICaseDetail) => {
  if (!data.name) {
    return i18n.t('dop:title required');
  }
  if (!(data.stepAndResults || []).length) {
    return i18n.t('dop:steps and results are not filled out');
  }
  const inValidNum = [] as number[];
  data.stepAndResults.forEach((item, i) => {
    if (!item.step || !item.result) {
      inValidNum.push(i + 1);
    }
  });
  const dupList = checkParamKeyDuplicate(data.apisFormat);
  if (dupList.length) {
    return i18n.t('dop:there are duplicates in the {index} interface', { index: dupList.join(',') });
  }
  if (inValidNum.length) {
    return i18n.t('dop:the {index} step in the steps and results is not completed', {
      index: inValidNum.join(','),
    });
  }
  return '';
};

const CaseDrawer = ({ visible, scope, onClose, afterClose, afterSave, caseList }: IProps) => {
  const params = routeInfoStore.useStore((s) => s.params);
  const caseDetail = testCaseStore.useStore((s) => s.caseDetail);
  const envList = testEnvStore.useStore((s) => s.envList);
  const dirName = testSetStore.useStore((s) => s.breadcrumbInfo.pathName);
  const { clearCaseDetail } = testCaseStore.reducers;
  const { editPartial, create: addTestCase, attemptTestApi, getCaseDetail } = testCaseStore.effects;
  const [isExecuting, fetchingDetail] = useLoading(testCaseStore, ['attemptTestApi', 'getCaseDetail']);
  const [{ fullData, titleIsEmpty }, updater] = useUpdate<IState>(initState);
  const drawer = React.useRef<{ saved: boolean }>({ saved: false });
  const activeElementRef = React.useRef<{ target: Element | null }>({ target: null });
  const editMode = !!caseDetail.id;
  React.useEffect(() => {
    if (caseDetail.id) {
      let apis = [];
      if (caseDetail.apis) {
        try {
          // 其他不用透传的放在rest里
          apis = caseDetail.apis.map(({ apiInfo, apiResponse, apiRequest, assertResult, status, ...rest }: any) => ({
            rest,
            ...JSON.parse(apiInfo),
            apiResponse,
            apiRequest,
            assertResult,
            status,
          }));
        } catch (error) {
          // do nothing
        }
      }
      updater.fullData({ ...caseDetail, apisFormat: apis });
    }
  }, [caseDetail, params.projectId, updater]);
  const shareLink = `${location.href.split('?')[0]}?${mergeSearch({ caseId: fullData.id }, true)}`;
  const updateDate = fromNow(caseDetail.updatedAt, { edgeNow: true });
  const handleClose = () => {
    onClose();
    clearCaseDetail();
    updater.fullData({ ...defaultData });
    updateSearch({ caseId: undefined });
  };
  const updateFullData = (key: string, value: any, autoSave = false) => {
    const newData = {
      ...fullData,
      [key]: value,
    };
    updater.fullData(newData);
    if (editMode && autoSave) {
      handleSave(false, newData);
    }
  };
  const handleAddInTitle = (type: string) => {
    switch (type) {
      case 'stepAndResults':
        updateFullData('stepAndResults', (fullData.stepAndResults || []).concat(getEmptyStep()));
        break;
      case 'apisFormat':
        updateFullData('apisFormat', [...fullData.apisFormat, getEmptyApi()]);
        break;
      default:
    }
  };
  const executeApi = (data: any, index: number, extra = { envId: 0 }) => {
    const newData = cloneDeep(data);
    if (newData.body.type) {
      newData.body.type = bodyTypeMap[newData.body.type] || newData.body.type;
    }
    const { url } = qs.parseUrl(newData.url);
    const api = {
      id: String(index),
      ...pick(newData, pickKeys),
      url,
    };
    // 这里的usecaseTestEnvID是testCaseID
    return attemptTestApi({ apis: [api], projectTestEnvID: extra.envId, usecaseTestEnvID: fullData.testCaseID });
  };
  const executeAllApi = (apiList: IApi[], extra = { envId: 0 }) => {
    const apis = apiList.map((item: any, index: number) => {
      const { url } = qs.parseUrl(item.url);
      const api = {
        id: String(index),
        ...pick(item, pickKeys),
        url,
      };
      return api;
    });
    // 这里的usecaseTestEnvID是testCaseID
    return attemptTestApi({ apis, projectTestEnvID: extra.envId, usecaseTestEnvID: fullData.testCaseID }).then(
      (result) => {
        updater.fullData({
          ...fullData,
          apisFormat: fullData.apisFormat.map((a: any, i: number) => ({ ...a, attemptTest: result[i] })),
        });
      },
    );
  };
  const checkName = (e: React.FocusEvent<HTMLInputElement>) => {
    const name = e.target.value;
    if (name === caseDetail.name) {
      return;
    }
    updater.titleIsEmpty(!name);
    if (editMode && name) {
      handleSave(false);
    }
  };
  const handleSave = async (close: boolean, data?: any) => {
    const newData = data || fullData;
    const errorInfo = doCheck(newData);
    if (errorInfo) {
      message.warning(errorInfo);
      return;
    }
    const { apisFormat, ...rests } = newData;
    const saveData = {
      ...rests,
      apis: apisFormat.map((item: IApi) => {
        const { apiResponse, assertResult, status, rest: restParams, attemptTest, ...info } = item;
        const saveApi = {
          ...restParams,
          apiResponse,
          assertResult,
          status,
          apiInfo: JSON.stringify(info),
        };
        return omitBy(saveApi, isUndefined);
      }),
    };
    const payload = editMode ? { ...saveData, id: caseDetail.testCaseID } : saveData;
    const request = editMode ? editPartial : addTestCase;
    const res = await request(payload);
    newData.id && getCaseDetail({ id: newData.id, scope });
    drawer.current.saved = true;
    if (afterSave) {
      await afterSave(saveData, editMode, res);
    }
    if (close) {
      handleClose();
    }
    if (!editMode) {
      updater.fullData({ ...defaultData });
    }
  };

  const handleAnyBlur = (e: React.FocusEvent) => {
    if (activeElementRef.current.target?.className.includes('ant-input')) {
      return;
    }

    if (!editMode) {
      return;
    }
    const { nodeName, classList } = e.target;
    const isMarkdownArea = classList.value.includes('section-container');
    // markdown编辑器需要blur后自行save
    if (['INPUT', 'TEXTAREA'].includes(nodeName) && !isMarkdownArea) {
      handleSave(false);
    }
  };

  const handleAnyMouseDown = React.useCallback((e: MouseEvent) => {
    activeElementRef.current.target = e.target as Element;
  }, []);

  React.useEffect(() => {
    if (visible) {
      window.addEventListener('mousedown', handleAnyMouseDown);
    } else {
      window.removeEventListener('mousedown', handleAnyMouseDown);
    }

    return () => {
      window.removeEventListener('mousedown', handleAnyMouseDown);
    };
  }, [visible, handleAnyMouseDown]);

  const handleVisibleChange = (v: boolean) => {
    if (!v) {
      afterClose && afterClose(drawer.current.saved);
      drawer.current.saved = false;
    }
  };

  const caseMetaData = React.useMemo(() => {
    return {
      priority: fullData.priority,
      createdAt: caseDetail.createdAt,
      creatorID: caseDetail.creatorID,
    };
  }, [fullData.priority, caseDetail.createdAt, caseDetail.creatorID]);

  const append =
    (scope === 'testPlan' && editMode) || !fullData.apisFormat.length ? null : (
      <span className="text-desc hover-active" onClick={() => executeAllApi(fullData.apisFormat, { envId: 0 })}>
        <SelectEnv
          envList={envList}
          onClick={(env: any) => {
            executeAllApi(fullData.apisFormat, { envId: env.id });
          }}
        >
          <>
            <ErdaIcon fill="black-400" type="play" size="16" />
            {i18n.t('execute')}
            <span className="text-xs">
              ({i18n.t('dop:When you click directly, it will execute cases without environment.')})
            </span>
          </>
        </SelectEnv>
      </span>
    );
  return (
    <Drawer
      className="case-drawer"
      width="908"
      placement="right"
      closable={false}
      maskClosable={editMode}
      visible={visible}
      destroyOnClose
      onClose={handleClose}
      afterVisibleChange={handleVisibleChange}
    >
      <Spin spinning={fetchingDetail}>
        <div className="case-drawer-header px-5 py-5">
          <div className="flex justify-between items-center">
            <div className="flex-1">
              <Input
                className={classnames('case-name text-lg font-medium text-normal', titleIsEmpty && 'error')}
                size="large"
                autoFocus
                placeholder={i18n.t('dop:use case title (required)')}
                autoComplete="off"
                value={fullData.name}
                onChange={(e) => {
                  updateFullData('name', e.target.value);
                }}
                onBlur={checkName}
              />
            </div>
            <div className="case-drawer-header-op">
              {editMode ? (
                <>
                  <Copy selector=".copy-share-link" tipName={i18n.t('dop:share link')} />
                  <ErdaIcon
                    type="share-one"
                    className="cursor-copy copy-share-link ml-3 mt-1"
                    size="16"
                    data-clipboard-text={shareLink}
                  />
                </>
              ) : null}
              <ErdaIcon
                type="close"
                onClick={handleClose}
                className="ml-3 mt-1 cursor-pointer"
                size="16"
              />
            </div>
          </div>
          <div className="flex justify-between items-center mt-4">
            <Tooltip title={dirName && dirName.length < 40 ? null : dirName}>
              <div className="flex text-base nowrap mr-5 color-text-desc case-drawer-header-desc">
                <ErdaIcon type="wjj1" size="16" className="mr-1" fill="yellow" />
                <span className="truncate">{dirName}</span>
              </div>
            </Tooltip>
            {editMode && (
              <div className="inline-flex justify-between items-center">
                <Avatar
                  showName
                  name={<UserInfo id={caseDetail.updaterID} render={(data) => data.nick || data.name} />}
                  size={28}
                />
                &nbsp;{i18n.t('dop:updated on')}&nbsp;{updateDate}
              </div>
            )}
          </div>
        </div>
        <div className="case-drawer-body flex justify-between">
          <div className="case-drawer-body-left flex-1 px-5 py-4">
            <div onBlurCapture={handleAnyBlur}>
              <ContentPanel title={i18n.t('dop:preconditions')}>
                <MarkdownEditor
                  value={fullData.preCondition}
                  onBlur={(v: string) => {
                    editMode && handleSave(false, { ...fullData, preCondition: v });
                    updateFullData('preCondition', v);
                  }}
                  placeholder={i18n.t('dop:no content yet')}
                />
              </ContentPanel>
              <ContentPanel
                title={i18n.t('dop:steps and results')}
                mode="add"
                onClick={() => {
                  handleAddInTitle('stepAndResults');
                }}
              >
                <CaseStep
                  value={fullData.stepAndResults}
                  onChange={(stepsData, autoSave) => updateFullData('stepAndResults', stepsData, autoSave)}
                />
              </ContentPanel>
              <ContentPanel
                title={i18n.t('dop:interface')}
                mode="add"
                onClick={() => {
                  handleAddInTitle('apisFormat');
                }}
                loading={isExecuting}
                append={append}
              >
                <CaseAPI
                  value={fullData.apisFormat}
                  onChange={(apis, autoSave) => updateFullData('apisFormat', apis, autoSave)}
                  mode={scope === 'testPlan' && editMode ? 'plan' : ''}
                  executeApi={executeApi}
                />
              </ContentPanel>
              <ContentPanel title={i18n.t('description')}>
                <MarkdownEditor
                  value={fullData.desc}
                  onBlur={(v: string) => {
                    editMode && handleSave(false, { ...fullData, desc: v });
                    updateFullData('desc', v);
                  }}
                  placeholder={i18n.t('dop:supplemental description')}
                />
              </ContentPanel>
            </div>
            <div className="mt-8">
              {visible && scope === 'testPlan' && editMode ? <RelatedBugs relationID={caseDetail.id} /> : null}
            </div>
          </div>
          <div className="case-drawer-body-right px-5 py-4">
            <CaseMeta onBlurCapture={handleAnyBlur} onChange={updateFullData} dataSource={caseMetaData} />
          </div>
        </div>
      </Spin>
      <div className="case-drawer-footer">
        <Spin spinning={false}>
          <CaseFooter
            scope={scope}
            onClose={handleClose}
            onOk={handleSave}
            editMode={editMode}
            caseList={caseList || []}
          />
        </Spin>
      </div>
    </Drawer>
  );
};

export default CaseDrawer;
