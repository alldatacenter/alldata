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

import { ciNodeStatusSet, ciStatusMap } from 'application/pages/build-detail/config';
import React from 'react';
import { Icon as CustomIcon } from 'common';
import { secondsToTime } from 'common/utils';
import classnames from 'classnames';
import { Popover, Tooltip } from 'antd';
import { isNumber, isEmpty, isEqual, debounce, get, findLast } from 'lodash';
import PointComponentAbstract from './point-component-abstract';
import { approvalStatusMap } from 'application/pages/deploy-list/deploy-list';
import i18n from 'i18n';
import './pipeline-node.scss';

const { executeStatus } = ciNodeStatusSet;

export interface IDiceYamlEditorItem {
  id: number;
  title?: string;
  data: any;
  name: string;
  icon?: any;
  itemStatus: string;
  allowMove?: boolean;
  disabled: boolean;
  // 只有为false时，才不能删除节点
  allowDelete?: boolean;
  status: string;
  lineTo: string[];
  editView?: React.ReactNode;
  createView?: React.ReactNode;
  content: () => string | React.ReactNode;
}

export interface IDiceYamlEditorItemProps {
  className?: string;
  style?: any;
  pointType?: 'all' | 'top' | 'bottom' | 'none';
  item: IDiceYamlEditorItem;
  onClick?: (item: IDiceYamlEditorItem, type: string) => void;
}

export default class DiceYamlEditorItem extends PointComponentAbstract<IDiceYamlEditorItemProps, any> {
  static info = {
    ITEM_WIDTH: 280,
    ITEM_HEIGHT: 74,
    ITEM_MARGIN_BOTTOM: 60,
    ITEM_MARGIN_RIGHT: 60,
    PADDING_LEFT: 40,
    PADDING_TOP: 40,
    ICON_WIDTH: 12,
    RX: 20,
  };

  info = DiceYamlEditorItem.info;

  state = {
    time: 0,
  };

  private interval: any;

  shouldComponentUpdate(nextProps: Readonly<IDiceYamlEditorItemProps>, nextState: Readonly<any>): boolean {
    const { data } = nextProps.item;

    if (!isEqual(data, this.props.item.data) || this.state.time !== nextState.time) {
      return true;
    }

    return false;
  }

  UNSAFE_componentWillReceiveProps(nextProps: Readonly<IDiceYamlEditorItemProps>): void {
    const { data } = nextProps.item;

    if (data.pipelineID !== this.props.item.data.pipelineID) {
      this.setState(
        {
          time: 0,
        },
        () => {
          this.setTime(nextProps);
        },
      );
    } else if (!isEqual(data, this.props.item.data)) {
      this.setTime(nextProps);
    }
  }

  UNSAFE_componentWillMount(): void {
    this.setTime(this.props);
  }

  componentWillUnmount(): void {
    if (this.interval) {
      clearInterval(this.interval);
      this.interval = null;
      this.setState({
        time: 0,
      });
    }
  }

  render() {
    const { time } = this.state;
    const { item, className, onClick } = this.props;
    let titleContent = null;
    let status = ciStatusMap[item.data.status];
    const metadata = get(item.data, 'result.metadata', []);
    const approval_status = get(findLast(metadata, { name: 'approval_status' }), 'value');
    let approvalResult = null as any;
    if (approval_status === approvalStatusMap.WaitApprove.value) {
      approvalResult = {
        text: i18n.t('pending approval'),
        color: 'orange',
      };
    } else if (approval_status === approvalStatusMap.Reject.value) {
      const approval_reason = get(findLast(metadata, { name: 'approval_reason' }), 'value');
      approvalResult = {
        text: <Tooltip title={approval_reason}>{i18n.t('approval failed')}</Tooltip>,
        color: 'red',
      };
    } else if (approval_status === 'Canceled') {
      // 后端单独为取消审批列表中部署操作所加
      status = ciStatusMap.StopByUser;
    }

    const statusContent = (
      <span className="flex-1">
        <span
          className="yaml-editor-item-status"
          style={{ background: approvalResult ? approvalResult.color : item.data.itemStatus.toLowerCase() }}
        />
        <span className="inline-flex justify-between items-center">
          {approvalResult ? approvalResult.text : status ? status.text : '-'}
        </span>
      </span>
    );
    if (item.data.name || item.data.displayName) {
      const titleText = item.data.displayName ? `${item.data.displayName}: ${item.data.name}` : item.data.name;
      titleContent = (
        <div className="yaml-editor-pipeline-item-title nowrap">
          <Tooltip title={titleText}>{titleText}</Tooltip>
        </div>
      );
    }

    const mergedClassNames = classnames(
      'yaml-editor-pipeline-item',
      className,
      item.data.status === 'Disabled' ? 'disabled-item' : '',
    );

    const timeContent =
      time >= 0 ? (
        <span>
          <CustomIcon type="shijian" />
          <span>{secondsToTime(time || item.data.costTimeSec)}</span>
        </span>
      ) : null;

    const logoUrl = get(item.data, 'logoUrl');
    const icon = logoUrl ? (
      <img src={logoUrl} alt="logo" className="pipeline-item-icon" />
    ) : (
      <CustomIcon className="pipeline-item-icon" type="wfw" color />
    );

    const Container = this.isEmptyExtraInfo() ? Popover : React.Fragment;
    return (
      <Container {...this.renderTooltipTitle()}>
        <div onClick={() => onClick && onClick(item.data, 'node')} className={mergedClassNames}>
          {icon}
          <span className="yaml-editor-item-title-name">
            <div className="flex justify-between items-center">
              {titleContent}
              <span className="pipeline-node-icon">{this.renderIcon()}</span>
            </div>
            <div className="flex justify-between items-center">
              {statusContent}
              {timeContent}
            </div>
          </span>
          {this.renderPoints()}
        </div>
      </Container>
    );
  }

  /**
   * 重置或设置时间
   */
  private setTime(props: any) {
    const { data } = props.item;
    const { time } = this.state;

    if (data.status === 'Running') {
      let currentTime = time;
      if (time <= data.costTimeSec) {
        currentTime = data.costTimeSec;
      }
      clearTimeout(this.interval);
      this.setState({
        time: currentTime,
      });

      this.interval = setInterval(() => {
        this.setState({
          time: this.state.time + 1,
        });
      }, 1000);
    } else {
      clearTimeout(this.interval);
      this.setState({
        time: data.costTimeSec,
      });
    }
  }

  private isEmptyExtraInfo() {
    const { item } = this.props;
    const { data } = item;

    if (isEmpty(data.result) || (!data.result.version && !data.result.metadata && !data.result.errors)) {
      return false;
    }

    return true;
  }

  private renderTooltipTitle = (): any => {
    const { result } = this.props.item.data;
    const detailInfo = [];

    if (result && this.isEmptyExtraInfo()) {
      const { errors: perError = [], metadata: preMetadata = [] } = result;
      const errors = [...perError] as any[];
      const metadata = [] as any[];
      const files = [] as any[];
      preMetadata.forEach((item: any) => {
        if (item.name && item.name.toLowerCase().startsWith('error.')) {
          errors.push(item);
        } else if (item.type === 'DiceFile') {
          files.push(item);
        } else {
          metadata.push(item);
        }
      });

      const skip = ['linkRuntime'];
      // detailInfo.push(`<h4>版本: ${version.ref || ''}</h4>`);
      if (!isEmpty(metadata)) {
        const temp: any[] = [];
        (metadata || []).forEach(
          (m: any, index: number) =>
            !skip.includes(m.name) &&
            temp.push(
              <div key={`meta-${String(index)}`} className="flow-chart-panel-msg-item">
                <span className="flow-chart-panel-msg-item-name">{m.name}</span>
                <pre>{m.value}</pre>
              </div>,
            ),
        );
        if (temp.length) {
          detailInfo.push(<h4>{i18n.t('dop:details')}</h4>);
          detailInfo.push(...temp);
        }
      }
      if (!isEmpty(files)) {
        detailInfo.push(<h4 className="mt-2">{i18n.t('download')}</h4>);
        detailInfo.push(
          files.map((item, idx) =>
            item.value ? (
              <div className="table-operations" key={`file-${String(idx)}`}>
                <a className="table-operations-btn" download={item.value} href={`/api/files/${item.value}`}>
                  {item.name || item.value}
                </a>
              </div>
            ) : null,
          ),
        );
      }
      if (!isEmpty(errors)) {
        detailInfo.push(<h4 className="mt-2">{i18n.t('error')}</h4>);
        detailInfo.push(
          errors.map((error, idx) => (
            <div key={`error-${String(idx)}`} className="flow-chart-panel-msg-item">
              <span className="flow-chart-panel-msg-item-name error">{error.name || 'error'}</span>
              <pre>{error.value || error.msg}</pre>
            </div>
          )),
        );
        // <pre className="flow-chart-err-block">
        //   {(errors || []).map((e: any, index: number) => <div key={`tooltip-${index}`}><code>{e.msg || e.code}</code></div>)}
        // </pre>
      }
      // if (!isEmpty(errors)) {
      //   detailInfo.push(<h4 className="mt-2">{i18n.t('error')}</h4>);
      //   detailInfo.push(
      //     <pre className="flow-chart-err-block">
      //       {(errors || []).map((e: any, index: number) => <div key={`tooltip-${index}`}><code>{e.msg || e.code}</code></div>)}
      //     </pre>
      //   );
      // }

      return {
        title: null,
        content: (
          <div key={this.props.item.id} className="panel-info">
            {detailInfo.map((e: any, index: number) => (
              <div key={String(index)}>{e}</div>
            ))}
          </div>
        ),
        overlayStyle: result
          ? {
              width: 'auto',
              maxWidth: '420px',
              height: 'auto',
              maxHeight: '520px',
              minWidth: '200px',
              padding: '10px',
              overflow: 'auto',
              wordBreak: 'break-all',
            }
          : null,
        placement: 'right',
      };
    }

    return null;
  };

  private renderIcon() {
    const { isType, status, result, costTimeSec } = this.props.item.data;

    const operations = [];
    // if (!starting) {
    // 右侧图标:analysis config
    // if (isType('dice')) {
    //   operations.push(this.getIconOperation('link', 'config-link', '配置中心'));
    // }
    // }

    // 右侧跳转链接图标
    if (status === 'Success') {
      if (isType('it') || isType('ut')) {
        operations.push(this.getIconOperation('link', 'test-link', i18n.t('test')));
      }
      // if (name === 'sonar') {
      //   operations.push(this.getIconOperation('link', 'sonar-link', '代码质量'));
      // }
    }

    if (result) {
      const { metadata } = result;
      if (metadata != null) {
        const runtimeID = metadata.find((a: any) => a.name === 'runtimeID');
        if (runtimeID) {
          operations.push(this.getIconOperation('link', 'link', i18n.t('overview')));
        }
        const releaseID = metadata.find((a: any) => a.name === 'releaseID');
        if (releaseID) {
          operations.push(this.getIconOperation('link', 'release-link', i18n.t('dop:version details')));
        }
        const publisherID = metadata.find((a: any) => a.name === 'publisherID');
        if (publisherID) {
          operations.push(this.getIconOperation('link', 'publisher-link', i18n.t('publisher:publisher content')));
        }
      }
    }

    if (status === 'Running' || (executeStatus.includes(status) && isNumber(costTimeSec) && costTimeSec !== -1)) {
      operations.push(this.getIconOperation('log', 'log', i18n.t('log')));
    }

    return operations.map((i: any, index: number) => (
      <React.Fragment key={`operation-${String(index)}`}>{i}</React.Fragment>
    ));
  }

  private getIconOperation = (icon: string, mark: string, tip: string) => {
    const clickFunc = debounce((e: any) => this.clickIcon(e, mark), 300);
    return (
      <Tooltip title={tip}>
        <CustomIcon className="operate-icon" type={icon} onClick={(e: any) => clickFunc(e)} />
      </Tooltip>
    );
  };

  private clickIcon = (e: any, mark: string) => {
    const { onClick, item } = this.props;
    onClick && onClick(item.data, mark);
    e.stopPropagation();
  };

  private renderPoints = (): any => {
    const { pointType } = this.props;

    switch (pointType) {
      case 'none':
        return null;
      case 'top':
        return (
          <span className="item-point top-point">
            <CustomIcon type="caret-top" />
          </span>
        );
      case 'bottom':
        return (
          <span className="item-point bottom-point">
            <CustomIcon type="caret-top" />
          </span>
        );
      default:
        return (
          <div>
            <span className="item-point top-point">
              <CustomIcon type="caret-top" />
            </span>
            <span className="item-point bottom-point">
              <CustomIcon type="caret-top" />
            </span>
          </div>
        );
    }
  };
}
