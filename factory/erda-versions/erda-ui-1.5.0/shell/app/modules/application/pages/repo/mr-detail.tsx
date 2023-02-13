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

import { Spin, Button, Tooltip, Rate, Alert } from 'antd';
import { isEmpty, find, get } from 'lodash';
import React from 'react';
import { IF, FormModal, Avatar, Icon as CustomIcon, BackToTop } from 'common';

import { goTo, fromNow, replaceEmoji, getLS, removeLS, insertWhen, connectCube } from 'common/utils';
import Markdown from 'common/utils/marked';
import RepoMRForm from './components/repo-mr-form';
import RepoCompareDetail from './components/compare-detail';
import { renderErrorBlock } from './components/source-target-select';
import i18n from 'i18n';
import { AppPermType } from 'app/user/stores/_perm-state';
import { usePerm } from 'app/user/common';
import { WithAuth, getAuth, isCreator, isAssignee } from 'user/common';
import './mr-detail.scss';
import routeInfoStore from 'core/stores/route';
import repoStore from 'application/stores/repo';
import { useLoading } from 'core/stores/loading';
import appStore from 'application/stores/application';
import { useUserMap } from 'core/stores/userMap';

interface IProps {
  mrDetail: REPOSITORY.IMrDetail;
  isFetching: boolean;
  isLocked: boolean;
  mrStats: any;
  defaultBranch: string;
  userMap: Obj;
  params: any;
  permMap: AppPermType;
  branchInfo: APPLICATION.IBranchInfo[];
  getMRDetail: typeof repoStore.effects.getMRDetail;
  getMRStats: typeof repoStore.effects.getMRStats;
  operateMR: typeof repoStore.effects.operateMR;
  getCompareDetail: typeof repoStore.effects.getCompareDetail;
  clearMRStats: typeof repoStore.reducers.clearMRStats;
  clearMRDetail: typeof repoStore.reducers.clearMRDetail;
  clearComments: typeof repoStore.reducers.clearComments;
  clearCompareDetail: typeof repoStore.reducers.clearCompareDetail;
}

interface IState {
  modalVisible: boolean;
  editMode: boolean;
  checkRunData: any;
}

class RepoMR extends React.PureComponent<IProps, IState> {
  constructor(props: IProps) {
    super(props);
    this.state = {
      modalVisible: false,
      editMode: false,
      checkRunData: {},
    };
  }

  componentDidMount() {
    this.props.getMRDetail().then((mrDetail) => {
      this.initFetch(mrDetail);
      this.setState({ checkRunData: mrDetail.checkRuns });
    });
    this.handleCleanTimeOutComment();
  }

  componentDidUpdate({ mrDetail }: { mrDetail: REPOSITORY.IMrDetail }) {
    if (mrDetail !== this.props.mrDetail) {
      this.initFetch(this.props.mrDetail);
    }
  }

  componentWillUnmount(): void {
    this.props.clearMRDetail();
    this.props.clearMRStats();
    this.props.clearComments();
    this.props.clearCompareDetail();
  }

  handleCleanTimeOutComment = () => {
    for (let i = 0; i < localStorage.length; i++) {
      const key = localStorage.key(i) || '';

      // 只有前缀为 `mr-comment-` 时才会进行清除操作
      if (!key.startsWith('mr-comment-')) {
        continue;
      }

      try {
        const item = JSON.parse(localStorage.getItem(key) || '{}');

        if (Object.prototype.toString.call(item) === '[object Object]') {
          const nowTime = new Date().getTime();
          const createTime = item.timestamp;

          if (nowTime - createTime >= 86400000 * 7) {
            localStorage.removeItem(key);
          }
        }
      } catch (error) {
        continue;
      }
    }
  };

  initFetch = (mrDetail: REPOSITORY.IMrDetail) => {
    if (!isEmpty(mrDetail)) {
      this.props.getCompareDetail({
        compareA: mrDetail.sourceSha,
        compareB: mrDetail.targetSha,
      });
      if (mrDetail.state === 'open') {
        this.props.getMRStats({
          sourceBranch: mrDetail.sourceBranch,
          targetBranch: mrDetail.targetBranch,
        });
      }
    }
  };

  toggleEditMode = (editMode: boolean) => {
    this.setState({ editMode });
  };

  handleAction = (action: REPOSITORY.MROperation, data?: Obj) => {
    this.props
      .operateMR({
        action,
        ...data,
      })
      .then(() => goTo('../'));
  };

  afterEdit = () => {
    this.props.getMRDetail();
    this.toggleEditMode(false);
  };

  toggleModal = (modalVisible: boolean) => {
    this.setState({ modalVisible });
  };

  getSourceBranchAuth = () => {
    const {
      branchInfo,
      mrDetail: { sourceBranch },
      permMap: appPerm,
    } = this.props;
    const sourceBranchProtect = get(find(branchInfo, { name: sourceBranch }), 'isProtect');
    const repoPerm = appPerm.repo;
    return sourceBranchProtect ? repoPerm.branch.writeProtected.pass : repoPerm.branch.writeNormal.pass; // 源将被删除且源分支被保护
  };

  getTargetBranchAuth = () => {
    const {
      branchInfo,
      mrDetail: { targetBranch },
      permMap: appPerm,
    } = this.props;
    const targetBranchProtect = get(find(branchInfo, { name: targetBranch }), 'isProtect');
    const repoPerm = appPerm.repo;
    return targetBranchProtect ? repoPerm.branch.writeProtected.pass : repoPerm.branch.writeNormal.pass; // 目标分支被保护
  };

  getSlots = () => {
    const { mrDetail, permMap: appPerm, userMap, isLocked } = this.props;
    const { state, authorId, mergeUserId, closeUserId, createdAt, mergeAt, closeAt, removeSourceBranch, assigneeId } =
      mrDetail;
    const repoPerm = appPerm.repo;
    const mergable = get(this.state, 'checkRunData.mergable', false);

    if (state === 'open') {
      const checkRole = [isCreator(authorId), isAssignee(assigneeId)];
      const editAuth = getAuth(repoPerm.mr.edit, checkRole);
      const closeAuth = getAuth(repoPerm.mr.close, checkRole);

      let mergeNoAuthTip = '';
      let mergeAuth = true;
      mergeAuth = this.getTargetBranchAuth();

      if (!mergeAuth) {
        mergeNoAuthTip = i18n.t('dop:target branch is protected, you have no permission yet');
      } else if (removeSourceBranch) {
        mergeAuth = this.getSourceBranchAuth();
        !mergeAuth && (mergeNoAuthTip = i18n.t('dop:source branch is protected, you have no permission yet'));
      }
      const authorUser = userMap[authorId] || {};
      return {
        headerAction: (
          <React.Fragment>
            <WithAuth pass={editAuth} tipProps={{ placement: 'bottom' }}>
              <Button onClick={() => this.toggleEditMode(true)}>{i18n.t('edit')}</Button>
            </WithAuth>
            <WithAuth pass={closeAuth} tipProps={{ placement: 'bottom' }}>
              <Button type="primary" ghost onClick={() => this.handleAction('close')}>
                {i18n.t('close')}
              </Button>
            </WithAuth>
            <WithAuth pass={mergeAuth} tipProps={{ placement: 'bottom' }} noAuthTip={mergeNoAuthTip}>
              <Button type="primary" ghost onClick={() => this.toggleModal(true)} disabled={isLocked || !mergable}>
                {i18n.t('merge')}
              </Button>
            </WithAuth>
          </React.Fragment>
        ),
        update: [
          <React.Fragment>
            <Avatar className="mr-1 mb-1" name={authorUser.nick} />
            <Tooltip title={authorUser.name}>{authorUser.nick}</Tooltip>
            {i18n.t('created at')}&nbsp;{fromNow(createdAt)}
          </React.Fragment>,
        ],
      };
    } else if (state === 'merged') {
      const mergeUser = userMap[mergeUserId] || {};

      return {
        headerAction: null,
        // headerAction: (
        //   <React.Fragment>
        //     <WithAuth pass={repoPerm.mr.create.pass}  tipProps={{ placement: 'bottom' }}>
        //       <Button type='primary' onClick={() => this.handleAction('revert')}>{i18n.t('dop:rollback')}</Button>
        //     </WithAuth>
        //   </React.Fragment>
        // ),
        update: [
          <React.Fragment>
            <Avatar showName wrapClassName="mr-1" name={mergeUser.nick} />
            {i18n.t('merged at')}&nbsp;{fromNow(mergeAt)}
            {`${removeSourceBranch ? `, ${i18n.t('dop:source branch has been deleted')}` : ''}`}
          </React.Fragment>,
        ],
      };
    }

    const closeUser = userMap[closeUserId] || {};
    return {
      headerAction: null,
      update: [
        <React.Fragment>
          <Avatar className="mr-1 mb-1" name={closeUser.nick} />
          <Tooltip title={closeUser.name}>{closeUser.nick}</Tooltip>
          {i18n.t('closed at')}&nbsp;{fromNow(closeAt)}
          {`${removeSourceBranch ? `, ${i18n.t('dop:source branch has been deleted')}` : ''}`}
        </React.Fragment>,
      ],
    };
  };

  getFieldsList = () => {
    const { defaultBranch, mrDetail } = this.props;
    return [
      ...insertWhen(defaultBranch !== mrDetail.sourceBranch, [
        {
          label: i18n.t('dop:whether to delete the source branch'),
          name: 'removeSourceBranch',
          type: 'checkbox',
          required: false,
          itemProps: {
            disabled: !this.getSourceBranchAuth(),
          },
        },
      ]),
      {
        label: i18n.t('dop:commit message'),
        name: 'commitMessage',
        type: 'textArea',
        itemProps: {
          maxLength: 200,
          autoSize: { minRows: 3, maxRows: 7 },
        },
      },
    ];
  };

  handleSubmit = (data: { commitMessage: string; removeSourceBranch: boolean }) => {
    this.handleAction('merge', data);
    this.toggleModal(false);
    // 如果当前ls里的分支被删除。 立刻清除ls
    if (data.removeSourceBranch) {
      const { sourceBranch } = this.props.mrDetail;
      const { appId } = this.props.params;
      const lsBranch = getLS(`branch-${appId}`);
      if (lsBranch === sourceBranch) {
        removeLS(`branch-${appId}`);
      }
    }
  };

  getStateIcon = () => {
    const {
      mrDetail: { state },
    } = this.props;
    return (
      <span className={`mr-${state}-icon`}>
        {state === 'merged' ? i18n.t('dop:have merged') : state === 'open' ? i18n.t('dop:committed') : i18n.t('closed')}
      </span>
    );
  };

  render() {
    const { isFetching, mrStats, mrDetail, isLocked } = this.props;
    const { checkrun, result, mergable } = get(this.state, 'checkRunData', {
      checkrun: [],
      result: 'success',
      mergable: false,
    });
    const { modalVisible } = this.state;
    const { title, description, sourceBranch, targetBranch, score, scoreNum } = mrDetail;

    if (isEmpty(mrDetail)) {
      return null;
    }
    const { editMode } = this.state;
    if (editMode) {
      return (
        <RepoMRForm editMode formData={mrDetail} onOk={this.afterEdit} onCancel={() => this.toggleEditMode(false)} />
      );
    }

    const slots = this.getSlots();
    const { defaultCommitMessage, ...rest } = mrDetail;
    let average_score = score / (scoreNum * 20);
    average_score -= average_score % 0.5; // 评分组件需要按 0.5 取整，如 1.7 会只显示一颗心，1.5 才会显示一颗半
    return (
      <Spin spinning={isFetching}>
        <div className="repo-mr-detail">
          <BackToTop />
          <div className="top-button-group">{slots.headerAction}</div>
          {/*  由 mergable 区分 进行中 和 已完成， 进行中不可合并，已完成可合并。已完成又分为成功和失败。 进行中、失败和成功分别作不同表示 */}
          {mergable && renderErrorBlock(mrStats, get(checkrun, '0.pipelineId'), result)}
          {!mergable && renderErrorBlock(mrStats, get(checkrun, '0.pipelineId'), String(mergable))}
          <IF check={isLocked}>
            <Alert message={i18n.t('lock-repository-tip')} type="error" className="repo-locked-alert" />
          </IF>

          <FormModal
            width={620}
            title={i18n.t('dop:merge requests')}
            fieldsList={this.getFieldsList()}
            formData={{ ...rest, commitMessage: replaceEmoji(defaultCommitMessage) }}
            visible={modalVisible}
            onOk={this.handleSubmit}
            onCancel={() => this.toggleModal(false)}
          />

          <div className="section-title mb-0">
            <div>
              {this.getStateIcon()}
              {title}
            </div>
          </div>
          <div className="branch-info">
            <span>
              {i18n.t('dop:source branch')}：<span className="branch-name">{sourceBranch}</span>
            </span>
            <span>
              <CustomIcon type="arrow-right" />
            </span>
            <span>
              {i18n.t('dop:target branch')}：<span className="branch-name">{targetBranch}</span>
            </span>
            <span className="mr-rate">
              {i18n.t('dop:overall score')}：<Rate allowHalf disabled value={average_score} />
            </span>
          </div>
          <div className="detail-desc-block">
            <div className="title">
              {slots.update.map((item, i) => (
                <span key={String(i)}>{item}</span>
              ))}
            </div>
            <article
              className="md-content"
              // eslint-disable-next-line react/no-danger
              dangerouslySetInnerHTML={{ __html: Markdown(description || '') }}
            />
          </div>

          <div className="section-title mt-8">{i18n.t('comparison results')}</div>
          <RepoCompareDetail />
        </div>
      </Spin>
    );
  }
}

const Mapper = () => {
  const permMap = usePerm((s) => s.app);
  const params = routeInfoStore.useStore((s) => s.params);
  const branchInfo = appStore.useStore((s) => s.branchInfo);
  const userMap = useUserMap();
  const [mrDetail, mrStats, defaultBranch, isLocked] = repoStore.useStore((s) => [
    s.mrDetail,
    s.mrStats,
    s.info.defaultBranch,
    s.info.isLocked,
  ]);
  const { getMRDetail, getMRStats, operateMR, getCompareDetail } = repoStore.effects;
  const { clearMRDetail, clearMRStats, clearCompareDetail, clearComments } = repoStore.reducers;
  const [isFetching] = useLoading(repoStore, ['getMRDetail']);
  return {
    permMap,
    params,
    isLocked,
    isFetching,
    mrDetail,
    mrStats,
    defaultBranch,
    branchInfo,
    userMap,
    getMRDetail,
    getMRStats,
    clearMRDetail,
    clearMRStats,
    operateMR,
    getCompareDetail,
    clearCompareDetail,
    clearComments,
  };
};

export default connectCube(RepoMR, Mapper);
