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
import { LogRoller, CompSwitcher, ErdaIcon } from 'common';
import { Switch, Drawer } from 'antd';
import { map } from 'lodash';
import DeployLog from 'runtime/common/logs/components/deploy-log';
import i18n from 'i18n';
import commonStore from 'common/stores/common';

const linkMark = '##to_link:';

interface IProps {
  logId: any;
  customFetchAPIPrefix?: string;
  downloadAPI?: string;
  pipelineID: string;
  taskID: string;
  visible: boolean;
  slidePanelComps: COMMON.SlideComp[];
  title?: string;
  withoutDrawer?: boolean;
  hideLog: (payload: any) => void;
  clearLog: (logKey?: string) => void;
  pushSlideComp: (payload: any) => void;
  popSlideComp: () => void;
}

interface IState {
  visible: boolean;
  isStdErr: boolean;
}

export class PureBuildLog extends React.PureComponent<IProps, IState> {
  constructor(props: IProps) {
    super(props);

    this.state = {
      isStdErr: false,
      visible: false,
    };
  }

  toggleSlideVisible = () => {
    this.setState({ visible: !this.state.visible });
  };

  pushSlideComp = (deploymentId: any, applicationId: string) => {
    this.props.pushSlideComp({
      getComp: () => <DeployLog detailLogId={deploymentId} applicationId={applicationId} />,
      getTitle: () => (
        <span>
          <ErdaIcon
            type="left-one"
            className="hover-active align-middle"
            size="16"
            onClick={() => this.props.popSlideComp()}
          />
          &nbsp;
          {i18n.t('deployment log')}
        </span>
      ),
    });
  };

  transformContentToLink = (content: any) => {
    if (!content.includes(linkMark)) return { content };

    /* eslint-disable-next-line */
    const contentInfo = content.match(/msg=\"(.*)\"/)[1];
    const extraInfo = content.match(/(.*)msg=/)[1];

    const logInfo = contentInfo.split(linkMark);
    const paramsInfo = logInfo[1].split(',');

    const logParams: any = {};
    map(paramsInfo, (item) => {
      const param = item.split(':');
      logParams[param[0]] = (param[1] || '').trim();
    });

    if (logParams.deploymentId && logParams.applicationId) {
      return {
        content: `${extraInfo}${logInfo[0]}`,
        suffix: (
          <a onClick={() => this.pushSlideComp(logParams.deploymentId, logParams.applicationId)}>
            {i18n.t('dop:view deployment log')}
          </a>
        ),
      };
    }
    return {
      content: `${extraInfo}${logInfo[0]}: lost deploymentId, cannot render link to deployment log`,
    };
  };

  toggleLogName = () => {
    this.setState({
      isStdErr: !this.state.isStdErr,
    });
    const { logId, clearLog } = this.props;
    clearLog(logId);
  };

  render() {
    const {
      slidePanelComps,
      title,
      logId,
      downloadAPI,
      customFetchAPIPrefix,
      pipelineID,
      taskID,
      visible,
      hideLog,
      withoutDrawer = false,
    } = this.props;
    const { isStdErr } = this.state;
    const switchLog = (
      <Switch
        checkedChildren={i18n.t('error')}
        unCheckedChildren={i18n.t('standard')}
        checked={isStdErr}
        onChange={this.toggleLogName}
      />
    );
    if (withoutDrawer) {
      return (
        <CompSwitcher comps={slidePanelComps}>
          <LogRoller
            key={String(isStdErr)}
            query={{
              fetchApi: customFetchAPIPrefix || `/api/cicd/${pipelineID}/tasks/${taskID}/logs`,
              downloadAPI,
              taskID,
              stream: isStdErr ? 'stderr' : 'stdout',
            }}
            logKey={logId}
            extraButton={switchLog}
            transformContent={this.transformContentToLink}
          />
        </CompSwitcher>
      );
    }
    return (
      <Drawer
        destroyOnClose
        title={
          slidePanelComps.length
            ? slidePanelComps[slidePanelComps.length - 1].getTitle()
            : title || i18n.t('dop:build log')
        }
        width="80%"
        visible={visible}
        onClose={hideLog}
      >
        <CompSwitcher comps={slidePanelComps}>
          <LogRoller
            key={String(isStdErr)}
            query={{
              // 此处如果使用了customFetchAPIPrefix，在drawer关闭瞬间又触发了请求会使用后面的api，此时pipelineID为undefined(见自动化测试)
              fetchApi:
                customFetchAPIPrefix || (pipelineID && taskID ? `/api/cicd/${pipelineID}/tasks/${taskID}/logs` : false),
              downloadAPI,
              taskID,
              stream: isStdErr ? 'stderr' : 'stdout',
            }}
            logKey={logId}
            extraButton={switchLog}
            transformContent={this.transformContentToLink}
          />
        </CompSwitcher>
      </Drawer>
    );
  }
}

export const BuildLog = (p: any) => {
  const { clearLog } = commonStore.reducers;
  const slidePanelComps = commonStore.useStore((s) => s.slidePanelComps);
  const { pushSlideComp, popSlideComp } = commonStore.reducers;
  return (
    <PureBuildLog
      {...p}
      clearLog={clearLog}
      slidePanelComps={slidePanelComps}
      pushSlideComp={pushSlideComp}
      popSlideComp={popSlideComp}
    />
  );
};
