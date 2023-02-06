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
import { Tooltip, Drawer } from 'antd';
import moment from 'moment';
import { map } from 'lodash';
import { getDateDuration } from 'common/utils';
import { TimelineActivity } from 'project/common/components/activity';
import { CompSwitcher, Avatar, Copy } from 'common';
import { useUpdate } from 'common/use-hooks';
import DeployLog from 'runtime/common/logs/components/deploy-log';
import { useLoading } from 'core/stores/loading';
import i18n from 'i18n';
import runtimeStore from 'runtime/stores/runtime';
import { useUserMap } from 'core/stores/userMap';
import routeInfoStore from 'core/stores/route';
import commonStore from 'common/stores/common';

import './activity.scss';

const deployStatusCnMap = {
  OK: <span className="tag-success">{i18n.t('succeed')}</span>,
  FAILED: <span className="tag-warning">{i18n.t('failed')}</span>,
  CANCELED: <span className="tag-default">{i18n.t('cancel')}</span>,
};

interface DeployCardProps {
  activity: RUNTIME.DeployRecord;
  userMap: {
    [key: string]: ILoginUser;
  };
  onOpenDeployLog: (id: number) => void;
  onOpenDeployment: (deployment: RUNTIME.DeployRecord) => void;
}

const Activity = () => {
  const [deploymentList, paging] = runtimeStore.useStore((s) => [s.deploymentList, s.deploymentListPaging]);
  const slidePanelComps = commonStore.useStore((s) => s.slidePanelComps);
  const { appId, runtimeId } = routeInfoStore.useStore((s) => s.params);
  const userMap = useUserMap();
  const [loading] = useLoading(runtimeStore, ['getDeploymentList']);
  const [{ visible, detailLogId }, updater, update] = useUpdate({
    visible: false,
    detailLogId: '',
  });

  React.useEffect(() => {
    if (runtimeId) {
      runtimeStore.getDeploymentList({ pageNo: 1, pageSize: paging.pageSize, loadMore: true });
    }
  }, [paging.pageSize, runtimeId]);

  const onOpenDeployLog = (id: number) => {
    update({
      visible: true,
      detailLogId: String(id),
    });
  };

  const loadMore = () => {
    runtimeStore.getDeploymentList({ pageNo: paging.pageNo + 1, pageSize: paging.pageSize, loadMore: true });
  };

  const deployLogProps = {
    detailLogId,
    applicationId: appId,
    hasLogs: false,
  };
  const fullLogComps = [
    {
      getTitle: () => i18n.t('deployment log'),
      getComp: () => <DeployLog {...deployLogProps} />,
    },
    ...slidePanelComps,
  ];

  const RuntimeDeploymentCard = ({ activity }: DeployCardProps) => {
    const { operator, createdAt, finishedAt, status, id } = activity;
    const { nick, name, avatar } = userMap[operator] as ILoginUser;
    const operatorName = nick || name || '';
    const fromNow = moment(createdAt).fromNow();
    const deployTime = moment(createdAt).format('YYYY-MM-DD HH:mm:ss');
    const timeCost = finishedAt
      ? getDateDuration(createdAt, finishedAt)
      : getDateDuration(moment(createdAt).valueOf(), moment().valueOf());

    return (
      <div className="deployment-card">
        <div className=" flex justify-between items-center">
          <Avatar size={32} name={operatorName} url={avatar || ''} className="self-start" />
          <div className="deployment-content ml-2 flex-1">
            <div className="info mb-2">
              <Tooltip title={name || ''}>
                <span className="name text-base font-medium mr-2">{operatorName}</span>
              </Tooltip>
              <Tooltip title={`${i18n.t('runtime:deployment time')}：${deployTime}`}>
                <span className="start-time">{`${i18n.t('runtime:beginning to deploy')} ${fromNow}`}</span>
              </Tooltip>
            </div>
          </div>
          <div className="extra-items self-end">
            <span className="text-primary cursor-pointer" onClick={() => onOpenDeployLog(id)}>
              {i18n.t('log')}
            </span>
          </div>
        </div>
        <div className="status mt-2">
          {deployStatusCnMap[status]}
          <span className="ml-2">{`${i18n.t('runtime:time consuming')} ${timeCost}`}</span>
          <Tooltip title={activity.releaseId}>
            <span data-clipboard-text={activity.releaseId} className="ml-2 cursor-copy">
              releaseId: {activity.releaseId.substr(0, 6)}
            </span>
          </Tooltip>
        </div>
        <Copy selector=".cursor-copy" />
      </div>
    );
  };

  const listWithTimeStamp = map(deploymentList, (item) => ({ ...item, timeStamp: item.createdAt }));

  return (
    <div className="runtime-activity">
      <TimelineActivity
        userMap={userMap}
        list={listWithTimeStamp}
        isLoading={loading}
        loadMore={loadMore}
        hasMore={paging.hasMore}
        CustomCard={RuntimeDeploymentCard}
      />
      <Drawer
        destroyOnClose
        title={fullLogComps[fullLogComps.length - 1].getTitle()}
        width="80%"
        visible={visible}
        onClose={() => updater.visible(false)}
      >
        <CompSwitcher comps={fullLogComps} />
      </Drawer>
    </div>
  );
};

export default Activity;
