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

import { Button, Checkbox, Alert } from 'antd';
import { IF, Icon as CustomIcon, ErdaIcon } from 'common';
import { goTo } from 'common/utils';
import React from 'react';
import BranchSelect from './branch-select';
import './source-target-select.scss';
import i18n from 'i18n';
import repoStore from 'application/stores/repo';
import routeInfoStore from 'core/stores/route';

const noop = () => {};

interface IMrStats {
  errorMsg: string;
  hasConflict: boolean;
  hasError: boolean;
  isMerged: boolean;
}

export const renderErrorBlock = (mrStats: IMrStats, pipelineID?: string, result?: string) => {
  const params = routeInfoStore.getState((s) => s.params);
  const { errorMsg, hasConflict, hasError, isMerged } = mrStats;
  const hasErrorBlock = hasError || hasConflict || isMerged || pipelineID;
  if (!hasErrorBlock) {
    return null;
  }

  let resultType = result;
  if (resultType === 'false') {
    resultType = 'progress';
  }

  const renderAlert = (msg: any, type: 'success' | 'info' | 'warning' | 'error' | undefined) => (
    <Alert className="mb-4" message={msg ?? ''} type={type} showIcon />
  );

  const msgCheckrunTypeMap = {
    success: 'success',
    failure: 'warning',
    progress: 'info',
  };

  const msgCheckrunTypeTipMap = {
    success: i18n.t('dop:checkrun-success-tip'),
    failure: i18n.t('dop:checkrun-failure-tip'),
    progress: i18n.t('dop:checkrun-process-tip'),
  };

  return (
    <div>
      {hasError && renderAlert(errorMsg, 'error')}
      {hasConflict && renderAlert(i18n.t('dop:have conflict'), 'error')}
      {isMerged && renderAlert(i18n.t('dop:no changes to merge'), 'info')}
      {pipelineID &&
        renderAlert(
          <span>
            <span>{msgCheckrunTypeTipMap[resultType as string]}&nbsp;&nbsp;</span>
            <span
              className="hover-active text-link"
              onClick={() => {
                goTo(goTo.pages.pipeline, {
                  projectId: params.projectId,
                  appId: params.appId,
                  pipelineID,
                  replace: false,
                });
              }}
            >
              {i18n.t('pipeline')}
            </span>
          </span>,
          msgCheckrunTypeMap[resultType as string],
        )}
    </div>
  );
};

interface IProps {
  branches: string[];
  defaultBranch?: string;
  disabled?: boolean;
  disableSourceBranch?: boolean;
  disableTargetBranch?: boolean;
  mrStats: IMrStats;
  sourceBranch?: string;
  defaultSourceBranch?: string;
  targetBranch?: string;
  defaultTargetBranch?: string;
  defaultRemoveSourceBranch?: boolean;
  onCompare: Function | null;
  onChange: (obj: object, isBranchChange: boolean) => void;
  moveToDiff: () => void;
}

interface IState {
  sourceBranch?: string;
  targetBranch?: string;
  removeSourceBranch: boolean;
}
class SourceTargetSelect extends React.Component<IProps, IState> {
  constructor(props: IProps) {
    super(props);
    const sourceBranch = props.sourceBranch || props.defaultSourceBranch;
    const targetBranch = props.targetBranch || props.defaultTargetBranch;
    this.state = {
      sourceBranch,
      targetBranch,
      removeSourceBranch: props.defaultRemoveSourceBranch || false,
    };
    const changed = {} as any;
    if (sourceBranch) {
      changed.sourceBranch = sourceBranch;
    }
    if (targetBranch) {
      changed.targetBranch = targetBranch;
    }
    if (changed.sourceBranch || changed.targetBranch) {
      props.onChange(changed, true);
    }
  }

  static getDerivedStateFromProps(nextProps: IProps) {
    // Should be a controlled component.
    if ('value' in nextProps) {
      const { value = {} as any, defaultSourceBranch, defaultTargetBranch, defaultRemoveSourceBranch } = nextProps;
      const currentState = {
        sourceBranch: value.sourceBranch || defaultSourceBranch,
        targetBranch: value.targetBranch || defaultTargetBranch,
        removeSourceBranch:
          typeof value.removeSourceBranch === 'boolean' ? value.removeSourceBranch : defaultRemoveSourceBranch || false,
      };
      return currentState;
    }
    return null;
  }

  handleChange = (key: 'sourceBranch' | 'targetBranch') => (value: string) => {
    // @ts-ignore
    this.setState({ [key as 'sourceBranch' | 'targetBranch']: value }, () => {
      this.triggerCompare();
    });
    this.triggerChange({ [key]: value }, true);
  };

  handleCheck = (e: any) => {
    const value = e.target.checked;
    this.setState({ removeSourceBranch: value });
    this.triggerChange({ removeSourceBranch: value }, false);
  };

  triggerChange = (changedValue: object, isBranchChange: boolean) => {
    // Should provide an event to pass value to Form.
    const { onChange } = this.props;
    const newValue = { ...this.state, ...changedValue };
    (onChange || noop)(newValue, isBranchChange);
  };

  triggerCompare = () => {
    const { onCompare } = this.props;
    const { sourceBranch, targetBranch } = this.state;
    if (sourceBranch && targetBranch) {
      (onCompare || noop)({ sourceBranch, targetBranch });
    }
  };

  render() {
    const {
      branches,
      disabled = false,
      disableSourceBranch = false,
      disableTargetBranch = false,
      mrStats,
      onCompare,
      defaultBranch,
    } = this.props;
    const { sourceBranch, targetBranch, removeSourceBranch } = this.state;
    const couldShowDiff = onCompare && sourceBranch && targetBranch;

    if (disabled) {
      return (
        <div className="repo-source-target-select">
          <div className="branch-select-row">
            <BranchSelect {...{ branches: [], hideTagList: true, current: sourceBranch || '' }} disabled>
              <span>{i18n.t('compare')}:</span>
              <span className="branch-name font-bold nowrap">{sourceBranch || null}</span>
              <IF check={sourceBranch}>
                <ErdaIcon type="caret-down" size="20" />
              </IF>
            </BranchSelect>
            <div className="branch-merge-arrow">
              <CustomIcon type="arrow-right" />
            </div>
            <BranchSelect {...{ branches: [], hideTagList: true, current: targetBranch || '' }} disabled>
              <span>{i18n.t('based on')}:</span>
              <span className="branch-name font-bold nowrap">{targetBranch || null}</span>
              <IF check={targetBranch}>
                <ErdaIcon type="caret-down" size="20" />
              </IF>
            </BranchSelect>
          </div>
          {renderErrorBlock(mrStats)}
          <span className={`show-diff-btn ${couldShowDiff ? '' : 'invisible'}`}>
            <Button type="primary" onClick={this.props.moveToDiff}>
              {i18n.t('view comparison results')}
            </Button>
          </span>
        </div>
      );
    }
    const disableRemoveSource = defaultBranch === sourceBranch;
    return (
      <div className="repo-source-target-select">
        <div className="branch-select-row">
          <BranchSelect
            {...{ branches, hideTagList: true, current: targetBranch || '' }}
            onChange={this.handleChange('targetBranch')}
            disabled={disableTargetBranch}
          >
            <span>{i18n.t('based on')}:</span>
            <span className="branch-name font-bold nowrap">{targetBranch || null}</span>
            <IF check={targetBranch}>
              <ErdaIcon type="caret-down" size="20" />
            </IF>
          </BranchSelect>

          <div className="branch-merge-arrow">
            <CustomIcon type="arrow-left" />
          </div>
          <BranchSelect
            {...{ branches, hideTagList: true, current: sourceBranch || '' }}
            onChange={this.handleChange('sourceBranch')}
            disabled={disableSourceBranch}
          >
            <span>{i18n.t('compare')}:</span>
            <span className="branch-name font-bold nowrap">{sourceBranch || null}</span>
            <IF check={sourceBranch}>
              <ErdaIcon type="caret-down" size="20" />
            </IF>
          </BranchSelect>
          {/* <IF check={!hideCompareBtn}>
            <Button type="primary" className="compare-button" disabled={!targetBranch} onClick={this.triggerCompare}>对比</Button>
          </IF> */}
        </div>
        {renderErrorBlock(mrStats)}
        {disableRemoveSource ? (
          <Checkbox className="delete-after-merge" disabled checked={false}>
            {i18n.t('dop:delete source branch after merge')}({i18n.t('dop:The default branch cannot be deleted.')})
          </Checkbox>
        ) : (
          <Checkbox className="delete-after-merge" onChange={this.handleCheck} checked={removeSourceBranch}>
            {i18n.t('dop:delete source branch after merge')}
          </Checkbox>
        )}
        <span className={`show-diff-btn ${couldShowDiff ? '' : 'invisible'}`}>
          <Button type="primary" onClick={this.props.moveToDiff}>
            {i18n.t('view comparison results')}
          </Button>
        </span>
      </div>
    );
  }
}

export default (p: IProps) => {
  const defaultBranch = repoStore.useStore((s) => s.info.defaultBranch);

  return <SourceTargetSelect {...p} defaultBranch={defaultBranch} />;
};
