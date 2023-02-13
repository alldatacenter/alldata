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

import { createStore } from 'core/cube';
import { getLS, removeLS, setLS, getDefaultPaging, goTo } from 'common/utils';
import { getTranslateAddonList } from 'app/locales/utils';
import { CATEGORY_NAME } from 'addonPlatform/pages/common/configs';
import { reduce, zipWith, isEmpty, cloneDeep, get } from 'lodash';
import appStore from 'application/stores/application';
import {
  getFromRepo,
  getBuildId,
  getRepoInfo,
  createBranch,
  getBlobRange,
  parsePipelineYmlStructure,
  deleteBranch,
  getTags,
  getBranches,
  setDefaultBranch,
  createTag,
  deleteTag,
  getCommitDetail,
  getCommits,
  getMRs,
  commit,
  getCompareDetail,
  getCIResource,
  getAvailableAddonList,
  createMR,
  getMRStats,
  getMRDetail,
  getAddonVersions,
  getAddonInstanceList,
  addComment,
  getComments,
  operateMR,
  getPipelineTemplateYmlContent,
  getPipelineTemplates,
  getTemplateConfig,
  deleteBackup,
  getBackupList,
  addBackup,
  getLatestCommit,
  setRepoLock,
} from 'application/services/repo';
import layoutStore from 'layout/stores/layout';
import routeInfoStore from 'core/stores/route';
import { getInfoFromRefName, getSplitPathBy } from 'application/pages/repo/util';
import i18n from 'i18n';
import { getEnvFromRefName, isPipelineWorkflowYml } from 'application/common/yml-flow-util';
import { eventHub } from 'common/utils/event-hub';

import { getLatestSonarStatistics, getSonarResults } from 'application/services/quality';
import { PAGINATION } from 'app/constants';

const buildIdParams = (appId: string, info: REPOSITORY.IInfo) => {
  const { after } = getSplitPathBy('tree');
  let currentBranch = '';
  info.branches.forEach((b) => {
    if (after.includes(b) && b.length > currentBranch.length) {
      currentBranch = b;
    }
  });
  if (!info.commitId || !currentBranch) {
    return null;
  }
  return { appId, commitId: info.commitId, branch: currentBranch };
};

const getSubList = (info: Obj, { projectId, appId }: { projectId: string; appId: string }) => {
  const location = window.location as any;
  const repoRoot = goTo.resolve.repo({ projectId, appId });
  const branchInQuery = location.query && location.query.cb;

  const lsBranch = getLS(`branch-${appId}`);
  const lsTag = getLS(`tag-${appId}`);
  const lsBranchOrTag = isEmpty(lsBranch) ? (isEmpty(lsTag) ? undefined : lsTag) : lsBranch;
  const { branch, commitId } = getInfoFromRefName(info.refName);

  const currentBranch = branchInQuery || commitId || lsBranchOrTag || branch || info.defaultBranch;
  const getHref = (path = '') => repoRoot + path;

  return [
    {
      text: i18n.t('dop:code'),
      href: getHref(currentBranch ? `/tree/${currentBranch}` : ''),
      isActive: (key: string) =>
        key === repoRoot || key.startsWith(`${repoRoot}/tree`) || key.startsWith(`${repoRoot}/backup`),
    },
    {
      text: i18n.t('dop:commit history'),
      href: getHref(currentBranch ? `/commits/${currentBranch}` : '/commits/'),
      prefix: getHref('/commit'),
    },
    {
      text: i18n.t('dop:branch management'),
      href: getHref(currentBranch ? `/branches?cb=${currentBranch}` : '/branches'),
      isActive: (key: string) => key.startsWith(`${repoRoot}/branches`) || key.startsWith(`${repoRoot}/tags`),
    },
    {
      text: i18n.t('dop:merge requests'),
      href: getHref(currentBranch ? `/mr/open?cb=${currentBranch}` : '/mr/open'),
      isActive: (key: string) => key.startsWith(`${repoRoot}/mr`),
    },
  ];
};

const getAppDetail: () => Promise<IApplication> = () =>
  new Promise((resolve) => {
    const { appId } = routeInfoStore.getState((s) => s.params);
    let appDetail = appStore.getState((s) => s.detail);
    const notSameApp = appId && String(appId) !== String(appDetail.id);
    if (!appId || notSameApp) {
      eventHub.once('appStore/getAppDetail', () => {
        appDetail = appStore.getState((s) => s.detail);
        resolve(appDetail);
      });
    } else {
      resolve(appDetail);
    }
  });

const initState = {
  info: {} as REPOSITORY.IInfo,
  tree: {} as REPOSITORY.ITree,
  blob: {} as REPOSITORY.IBlob,
  blame: [] as REPOSITORY.IBlame[],
  branch: [] as REPOSITORY.IBranch[],
  tag: [] as any[],
  commit: [] as REPOSITORY.ICommit[],
  pipelineTemplates: [] as REPOSITORY.IPipelineTemplate[],
  mr: {},
  mrList: [] as any[],
  mrStats: {},
  mrDetail: {},
  comments: [] as REPOSITORY.MrNote[],
  commitDetail: {} as REPOSITORY.CommitDetail,
  compareDetail: {},
  sonarMessage: {},
  commitPaging: {
    pageNo: 1,
    pageSize: PAGINATION.pageSize,
    hasMore: true,
  },
  mode: {
    addFile: false,
    editFile: false,
    fileBlame: false,
    addFileName: '',
  },
  buildId: '',
  groupedAddonList: [],
  templateConfig: {
    names: [],
    branch: '',
    path: '',
  } as REPOSITORY.MrTemplate,
  pipelineYmlStructure: {} as IPipelineYmlStructure,
  mrPaging: getDefaultPaging(),
  backupList: [],
  backupPaging: {
    pageNo: 1,
    pageSize: PAGINATION.pageSize,
    total: 0,
  },
  backupLatestCommit: {},
};

const repoStore = createStore({
  name: 'repository',
  state: initState,
  subscriptions({ listenRoute }: IStoreSubs) {
    listenRoute(({ isEntering, isIn, isMatch, params }: IRouteInfo) => {
      if (isIn('application')) {
        repoStore.reducers.resetMode();
      }
      if (isIn('repo') || isEntering('application')) {
        repoStore.reducers.clearRepoTree();
        repoStore.effects.getRepoInfo().then((info) => {
          const { branches, tags, SKIP } = info;
          if (SKIP) {
            return;
          }
          const { appId } = params;
          const branchKey = `branch-${appId}`;
          const tagKey = `tag-${appId}`;
          const lsBranch = getLS(branchKey);
          const lsTag = getLS(tagKey);
          if (lsBranch && !branches.includes(lsBranch)) {
            removeLS(branchKey);
          }
          if (lsTag && !tags.includes(lsTag)) {
            removeLS(tagKey);
          }
          const isRepoRoot = isMatch(/\/dop\/projects\/\w+\/apps\/\w+\/repo$/);
          if (isRepoRoot || isIn('repoTree')) {
            // repoStore.reducers.clearRepoTree();
            repoStore.effects.getRepoTree();
          }
        });
      }
      if (isIn('repoCompare')) {
        const [compareB, compareA] = params.branches.split('...');
        repoStore.effects.getCompareDetail({
          compareA: decodeURIComponent(compareA),
          compareB: decodeURIComponent(compareB),
        });
      }
    });
  },
  effects: {
    async getRepoInfo({ getParams, update, call }): Promise<Obj> {
      const url: string = window.location.pathname;
      const branch = zipWith(
        // 截取链接中的branch
        url.split('repo/tree/'),
        url.split('repo/commits/'),
        (v1, v2) => v1 || v2,
      )[1];
      const appDetail = await getAppDetail();
      if (appDetail.isExternalRepo) {
        layoutStore.reducers.setSubSiderSubList({
          // 外置仓库不显示二级菜单
          repo: [],
        });
        return { SKIP: true };
      }
      const info = await call(getRepoInfo, {
        repoPrefix: appDetail.gitRepoAbbrev,
        branch,
      });
      update({ info });
      const params = getParams();

      layoutStore.reducers.setSubSiderSubList({
        repo: getSubList(info, params),
      });
      return info;
    },
    async getBuildId({ select, call, update }, payload): Promise<any> {
      let { info } = payload;
      if (!info) {
        info = select((state) => state.info);
      }
      const param = buildIdParams(payload.appId, info);
      if (!param) {
        return;
      }
      const buildId = await call(getBuildId, param);
      update({ buildId });
    },
    async getRepoTree({ getParams, select, update, call }, { force }: REPOSITORY.QueryRepoTree = { force: false }) {
      const appDetail = appStore.getState((s) => s.detail);
      const { appId } = getParams();
      let info = select((state) => state.info);
      if (info.empty) {
        if (force) {
          await repoStore.effects.getRepoInfo();
          info = select((state) => state.info);
        } else {
          return;
        }
      }
      const lsBranch = getLS(`branch-${appId}`);
      const lsTag = getLS(`tag-${appId}`);
      const lsBranchOrTag = isEmpty(lsBranch) ? (isEmpty(lsTag) ? undefined : lsTag) : lsBranch;
      const isRepoRoot = window.location.pathname.endsWith(
        `/dop/projects/${appDetail.projectId}/apps/${appDetail.id}/repo`,
      );
      const tree = (await call(getFromRepo, {
        type: 'tree',
        repoPrefix: appDetail.gitRepoAbbrev,
        path: isRepoRoot ? `/${lsBranchOrTag || info.defaultBranch}` : undefined,
      })) as REPOSITORY.ITree;
      update({ tree });
    },
    async getRepoBlame({ call, update }, payload = {}) {
      const appDetail = await getAppDetail();
      const blame = (await call(getFromRepo, {
        type: 'blame',
        repoPrefix: appDetail.gitRepoAbbrev,
        ...payload,
      })) as REPOSITORY.IBlame[];
      update({ blame });
      return blame;
    },
    async getRepoBlob({ call, update }, payload: Obj) {
      const { gitRepoAbbrev } = appStore.getState((s) => s.detail);
      const blob = (await call(getFromRepo, {
        type: 'blob',
        repoPrefix: gitRepoAbbrev,
        ...payload,
      })) as REPOSITORY.IBlob;
      update({ blob });
      const { content, path: fileName } = blob;
      if (isPipelineWorkflowYml(fileName)) {
        repoStore.effects.parsePipelineYmlStructure({ pipelineYmlContent: content });
      }
      return blob;
    },
    async parsePipelineYmlStructure({ call, update }, payload: { pipelineYmlContent: any }) {
      const pipelineYmlStructure = await call(parsePipelineYmlStructure, {
        ...payload,
      });
      update({ pipelineYmlStructure });
      return pipelineYmlStructure;
    },
    async getBlobRange(
      { getParams, select, call, update },
      payload: Omit<REPOSITORY.QueryBlobRange, 'repoPrefix' | 'commitId'>,
    ) {
      const params = getParams();
      const { type } = payload;
      let targetKey;
      let target: any;
      let commitId;
      if (type === 'compare') {
        target = select((state) => state.compareDetail);
        commitId = target && get(target, 'from');
        targetKey = 'compareDetail';
      } else if (type === 'commit' && params.commitId) {
        target = select((state) => state.commitDetail);
        commitId = params.commitId;
        targetKey = 'commitDetail';
      }
      if (!commitId || !targetKey || !target) {
        return;
      }
      const appDetail = await getAppDetail();
      const latestCommitDetail = await call(getBlobRange, {
        repoPrefix: appDetail.gitRepoAbbrev,
        commitId,
        ...payload,
      });
      target = cloneDeep(target);
      // if request to greater than total line number remove last empty section
      if (payload.bottom) {
        if (latestCommitDetail.lines.length <= 20) {
          target.diff.files
            .find((x: REPOSITORY.IFile) => x.name === payload.path)
            .sections.splice(payload.sectionIndex + 1, 1);
        } else {
          latestCommitDetail.lines = latestCommitDetail.lines.slice(0, -1);
        }
      }
      const file = target.diff.files.find((x: REPOSITORY.IFile) => x.name === payload.path);
      const section = file.sections[payload.sectionIndex];
      if (payload.bottom) {
        section.lines = section.lines.concat(latestCommitDetail.lines);
      } else {
        section.lines = latestCommitDetail.lines.concat(section.lines.slice(1));
      }
      switch (targetKey) {
        case 'compareDetail':
          update({ compareDetail: target });
          break;
        case 'commitDetail':
          update({ commitDetail: target });
          break;
        default:
          break;
      }
      return file;
    },
    async getRepoRaw({ call }) {
      const raw = await call(getFromRepo, { type: 'raw' });
      return raw;
    },
    async getListByType({ call, update }, payload) {
      const appDetail = await getAppDetail();
      const { type, ...rest } = payload;
      const apiMap = {
        branch: getBranches,
        tag: getTags,
      };
      const result = await call(apiMap[type], {
        repoPrefix: appDetail.gitRepoAbbrev,
        ...rest,
      });
      // 不要用动态key对象，会报错
      switch (type) {
        case 'branch':
          update({ branch: result as any[] });
          break;
        case 'tag':
          update({ tag: result as any[] });
          break;
        default:
      }
    },
    async createBranch({ call, getParams }, payload: { refValue: string; branch: string }) {
      const appDetail = appStore.getState((s) => s.detail);
      try {
        await call(
          createBranch,
          {
            repoPrefix: appDetail.gitRepoAbbrev,
            ...payload,
          },
          { successMsg: i18n.t('dop:new branch added successfully') },
        );
        const { appId } = getParams();
        setLS(`branch-${appId}`, payload.branch);
        await repoStore.effects.getRepoInfo();
        await appStore.effects.getBranchInfo({ enforce: true });
      } catch (error) {
        return error;
      }
    },
    async deleteBranch({ call, getParams }, payload: { branch: string }) {
      const appDetail = await getAppDetail();
      await call(
        deleteBranch,
        {
          repoPrefix: appDetail.gitRepoAbbrev,
          ...payload,
        },
        {
          successMsg: i18n.t('dop:branch deleted successfully'),
          errorMsg: i18n.t('dop:failed to delete branch'),
        },
      );
      await repoStore.effects.getListByType({ type: 'branch' });
      const params = getParams();
      const branchKey = `branch-${params.appId}`;
      const lsBranch = getLS(branchKey);
      if (lsBranch === payload.branch) {
        removeLS(branchKey);
        await repoStore.effects.getRepoInfo();
      }
    },
    async deleteTag({ call }, payload: { tag: string }) {
      const appDetail = await getAppDetail();
      await call(
        deleteTag,
        {
          repoPrefix: appDetail.gitRepoAbbrev,
          ...payload,
        },
        { successMsg: i18n.t('default:deleted successfully'), errorMsg: i18n.t('dop:failed to delete tag') },
      );
      await repoStore.effects.getListByType({ type: 'tag' });
    },
    async createTag({ call }, payload: { ref: string; tag: string; message: string }) {
      const appDetail = appStore.getState((s) => s.detail);
      const result = await call(
        createTag,
        {
          repoPrefix: appDetail.gitRepoAbbrev,
          ...payload,
        },
        { fullResult: true },
      );
      await repoStore.effects.getListByType({ type: 'tag' });
      return result;
    },
    async setDefaultBranch({ call, select, update }, branch: string) {
      const appDetail = await getAppDetail();
      await call(
        setDefaultBranch,
        {
          repoPrefix: appDetail.gitRepoAbbrev,
          branch,
        },
        { successMsg: i18n.t('dop:set default branch successfully') },
      );
      const curBranches = select((s) => s.branch);
      update({
        branch: curBranches.map((b) => ({ ...b, isDefault: b.name === branch })),
      });
    },
    async getMrList({ call, select, update }, payload: REPOSITORY.QueryMrs) {
      const appDetail = await getAppDetail();
      const { pageNo = 1 } = payload;
      const [originalList, mrPaging] = select((state) => [state.mrList, state.mrPaging]);
      try {
        const { list, total } = await call(
          getMRs,
          { repoPrefix: appDetail.gitRepoAbbrev, ...payload },
          { paging: { key: 'mrPaging' } },
        );
        const mrList = pageNo === 1 ? list : [...originalList, ...list];
        update({ mrList });
        return { list: mrList, total };
      } catch (e) {
        update({ mrPaging: { ...mrPaging, hasMore: false } });
        return { list: [], total: 0 };
      }
    },
    async getCommitList({ call, select, update }, payload: REPOSITORY.QueryCommit) {
      const appDetail = await getAppDetail();
      const { commitPaging: paging, commit: prevList } = select((state) => state);
      try {
        const result = await call(getCommits, {
          repoPrefix: appDetail.gitRepoAbbrev,
          ...paging,
          ...(payload || {}),
        });
        const newList = payload.pageNo && payload.pageNo > 1 ? prevList.concat(result) : result;
        const hasMore = result.length === paging.pageSize;
        update({ commit: newList, commitPaging: { ...paging, ...payload, hasMore } });
      } catch (e) {
        update({ commitPaging: { ...paging, ...payload, hasMore: false } });
      }
    },
    async getCommitDetail({ getParams, call, update }) {
      const params = getParams();
      const appDetail = await getAppDetail();
      const commitDetail = await call(getCommitDetail, {
        repoPrefix: appDetail.gitRepoAbbrev,
        ...params,
      });
      update({ commitDetail });
    },
    async checkCommitId({ call }, payload: { commitId: string }) {
      const appDetail = appStore.getState((s) => s.detail);
      try {
        const commitDetail = await call(getCommitDetail, {
          repoPrefix: appDetail.gitRepoAbbrev,
          ...payload,
        });
        return commitDetail;
      } catch (error) {
        return 'error';
      }
    },
    async getSonarMessage({ getParams, call, update }) {
      const params = getParams();
      const sonarMessage = await call(getCIResource, {
        type: 'issuesStatistics',
        commitId: params.commitId,
      });
      update({ sonarMessage });
    },
    async getCompareDetail({ call, update }, payload: REPOSITORY.QueryCompareDetail) {
      const appDetail = await getAppDetail();
      const compareDetail = await call(getCompareDetail, {
        repoPrefix: appDetail.gitRepoAbbrev,
        ...payload,
      });
      update({ compareDetail });
    },
    async commit({ call }, payload: Merge<REPOSITORY.Commit, { isDelete?: boolean }>) {
      const appDetail = await getAppDetail();
      const { isDelete, ...rest } = payload;
      const commitResult = await call(
        commit,
        {
          repoPrefix: appDetail.gitRepoAbbrev,
          data: rest,
        },
        { fullResult: true },
      );
      !isDelete && repoStore.effects.getRepoTree();
      return commitResult;
    },
    async getMRStats({ call, update }, payload: REPOSITORY.MrStats) {
      const appDetail = await getAppDetail();
      const mrStats = await call(getMRStats, {
        repoPrefix: appDetail.gitRepoAbbrev,
        ...payload,
      });
      update({ mrStats });
    },
    async createMR({ call }, payload: Omit<REPOSITORY.Mr, 'action'>) {
      const appDetail = await getAppDetail();
      const res = await call(
        createMR,
        {
          repoPrefix: appDetail.gitRepoAbbrev,
          ...payload,
        },
        { fullResult: true },
      );
      return res;
    },
    async getAvailableAddonList({ getParams, select, call }) {
      const tree = select((state) => state.tree);
      const params = getParams();
      const options = {
        workspace: getEnvFromRefName(getInfoFromRefName(tree.refName).branch),
        projectId: params.projectId,
      };
      const availableAddonList = await call(getAvailableAddonList, options);
      let addonInstanceList = await call(getAddonInstanceList, { type: 'addon' });
      addonInstanceList = addonInstanceList.filter((item: any) => item.public !== false && item.deployable !== false);
      await repoStore.reducers.getAvailableAddonListSuccess({ availableAddonList, addonInstanceList });
    },
    async getAddonVersions({ call }, payload: string) {
      const res = await call(getAddonVersions, { addonName: payload });
      return res;
    },
    async getMRDetail({ getParams, call, update }) {
      const params = getParams();
      const appDetail = await getAppDetail();
      const mrDetail = await call(getMRDetail, {
        repoPrefix: appDetail.gitRepoAbbrev,
        mergeId: params.mergeId,
      });
      update({ mrDetail });
      return mrDetail;
    },
    async operateMR({ getParams, call }, payload: Omit<REPOSITORY.OperateMR, 'mergeId'>) {
      const appDetail = await getAppDetail();
      const { mergeId } = getParams();
      const res = await call(
        operateMR,
        {
          repoPrefix: appDetail.gitRepoAbbrev,
          mergeId,
          ...payload,
        },
        { fullResult: true },
      );
      return res;
    },
    async getComments({ getParams, call, update }) {
      const appDetail = await getAppDetail();
      const { mergeId } = getParams();
      const comments = await call(getComments, {
        repoPrefix: appDetail.gitRepoAbbrev,
        mergeId,
      });
      update({ comments });
    },
    async addComment({ getParams, call }, payload: Obj) {
      const appDetail = await getAppDetail();
      const { mergeId } = getParams();
      await call(addComment, {
        repoPrefix: appDetail.gitRepoAbbrev,
        mergeId,
        ...payload,
      });
      await repoStore.effects.getComments();
      await repoStore.effects.getMRDetail();
    },
    async getQaResult({ call }, payload: { appId: string; type: string }) {
      const { commitId } = await call(getLatestSonarStatistics, { applicationId: payload.appId });
      let result: any = Promise.reject();
      if (commitId) {
        result = await call(getSonarResults, { key: commitId, type: payload.type });
      }
      return result;
    },
    async getTemplateConfig({ call, update }, payload: Obj) {
      const appDetail = await getAppDetail();
      const templateConfig = await call(getTemplateConfig, {
        repoPrefix: appDetail.gitRepoAbbrev,
        ...payload,
      });
      update({ templateConfig });
      return templateConfig;
    },
    async getPipelineTemplates({ call, update }, payload: REPOSITORY.IPipelineTemplateQuery) {
      const pipelineTemplates = await call(getPipelineTemplates, payload);
      update({ pipelineTemplates: pipelineTemplates.data });
      return pipelineTemplates;
    },
    async getPipelineTemplateYmlContent({ call }, payload: REPOSITORY.IPipelineTemplateContentQuery) {
      const res = await call(getPipelineTemplateYmlContent, payload);
      return res;
    },
    async addBackup({ call }, payload: REPOSITORY.IBackupAppendBody) {
      const appDetail = await getAppDetail();
      await call(
        addBackup,
        {
          repoPrefix: appDetail.gitRepoAbbrev,
          ...payload,
        },
        { successMsg: i18n.t('added successfully'), errorMsg: i18n.t('dop:failed to delete tag') },
      );
    },
    async getBackupList({ call, update }, payload: REPOSITORY.ICommitPaging) {
      const appDetail = await getAppDetail();
      const { list = [] } = await call(
        getBackupList,
        {
          repoPrefix: appDetail.gitRepoAbbrev,
          ...payload,
        },
        { paging: { listKey: 'files', key: 'backupPaging' } },
      );
      update({ backupList: list });
    },
    async deleteBackup({ call }, payload: REPOSITORY.IBackupUuid) {
      const appDetail = await getAppDetail();
      await call(
        deleteBackup,
        {
          repoPrefix: appDetail.gitRepoAbbrev,
          ...payload,
        },
        {
          successMsg: i18n.t('default:deleted successfully'),
          errorMsg: i18n.t('dop:failed to delete backup'),
        },
      );
    },
    async getLatestCommit({ call, update }, payload: REPOSITORY.IBackupBranch) {
      const appDetail = await getAppDetail();
      const { commit: result } = await call(getLatestCommit, {
        repoPrefix: appDetail.gitRepoAbbrev,
        ...payload,
      });
      update({ backupLatestCommit: result });
      return { backupLatestCommit: result };
    },
    async setRepoLock({ call }, payload: { isLocked: boolean }) {
      const appDetail = await getAppDetail();
      const result = await call(setRepoLock, {
        isLocked: payload.isLocked,
        repoPrefix: appDetail.gitRepoAbbrev,
      });
      return result;
    },
  },
  reducers: {
    getAvailableAddonListSuccess(state, { availableAddonList, addonInstanceList }) {
      const groupedAddonList = reduce(
        [
          ...getTranslateAddonList(availableAddonList, 'displayName'),
          ...getTranslateAddonList(addonInstanceList, 'displayName'),
        ],
        (result, value) => {
          if (CATEGORY_NAME[value.category]) {
            // eslint-disable-next-line no-param-reassign
            (result[CATEGORY_NAME[value.category]] || (result[CATEGORY_NAME[value.category]] = [])).push(value);
          }
          return result;
        },
        {},
      );

      return { ...state, groupedAddonList };
    },
    clearRepoTree(state) {
      return { ...state, tree: {} };
    },
    clearRepoBlob(state) {
      return { ...state, blob: {} };
    },
    clearListByType(state, payload) {
      return { ...state, [payload]: [] };
    },
    clearCommitDetail(state) {
      return { ...state, commitDetail: {} };
    },
    clearSonarMessage(state) {
      return { ...state, sonarMessage: {} };
    },
    clearCompareDetail(state) {
      return { ...state, compareDetail: {} };
    },
    clearMRDetail(state) {
      return { ...state, mrDetail: {} };
    },
    clearMRStats(state) {
      return { ...state, mrStats: {} };
    },
    clearComments(state) {
      return { ...state, comments: [] };
    },
    changeMode(state, payload) {
      return { ...state, mode: { ...state.mode, ...payload } };
    },
    resetMode(state) {
      return { ...state, mode: { addFile: false, editFile: false, fileBlame: false, addFileName: '' } };
    },
    resetCommitPaging(state) {
      return {
        ...state,
        commit: [],
        commitPaging: {
          pageNo: 1,
          pageSize: PAGINATION.pageSize,
          hasMore: true,
        },
      };
    },
    clearMrList(state) {
      return {
        ...state,
        mrList: [],
        mrPaging: getDefaultPaging(),
      };
    },
  },
});

export default repoStore;
