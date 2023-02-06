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
import { forEach, map, isEmpty } from 'lodash';
import { Button, Spin, message } from 'antd';
import { Holder } from 'common';
import { useUpdate } from 'common/use-hooks';
import { goTo, fromNow, formatTime } from 'common/utils';
import { useMount, useUnmount } from 'react-use';
import routeInfoStore from 'core/stores/route';
import jvmStore, { ProfileStateMap } from '../../stores/jvm';
import addonStore from 'common/stores/addon';

import './jvm-overview.scss';

const JVM_INFO_SCOPES = ['jvm_process', 'jvm_options', 'jvm_properties'];

const JvmOverview = () => {
  const { realInstanceId: insId } = addonStore.useStore((s) => s.addonDetail);
  const { profileId } = routeInfoStore.useStore((s) => s.params);
  const jvmInfo = jvmStore.useStore((s) => s.jvmInfo);
  const pendingTimer = React.useRef(-1);
  const failedTimer = React.useRef(-1);
  const [{ isPending, isRunning, createTime, finishTime }, updater] = useUpdate({
    isPending: true,
    isRunning: false,
    createTime: 0,
    finishTime: 0,
  });

  useMount(() => {
    rollingState();
  });

  useUnmount(() => {
    clearTimeout(failedTimer.current);
    clearTimeout(pendingTimer.current);
  });

  React.useEffect(() => {
    if (isPending) return;
    forEach(JVM_INFO_SCOPES, (scope) => {
      jvmStore.getJVMInfo({
        insId,
        profileId,
        scope,
      });
    });
  }, [insId, isPending, profileId]);

  const rollingState = React.useCallback(() => {
    jvmStore.getProfileStatus({ insId, profileId }).then((res) => {
      updater.createTime(res.createTime);
      updater.finishTime(res.finishTime);
      switch (res.state) {
        case ProfileStateMap.PENDING:
          pendingTimer.current = window.setTimeout(() => {
            rollingState();
          }, 3000);
          break;
        case ProfileStateMap.RUNNING:
          updater.isRunning(true);
          updater.isPending(false);
          break;
        case ProfileStateMap.FAILED:
          message.error(res.message);
          failedTimer.current = window.setTimeout(() => {
            goTo('../');
          }, 500);
          break;
        default:
          updater.isPending(false);
          break;
      }
    });
  }, [insId, profileId, updater]);

  const stopProfile = () => {
    jvmStore
      .stopProfile({
        insId,
        profileId,
      })
      .then(() => {
        rollingState();
      });
  };

  const getPanelBody = (data: Array<{ key: string; value: string }>) => (
    <Holder when={isEmpty(data)}>
      {map(data, ({ key, value }) => (
        <p className="info-item">
          <span className="label">{key}</span>
          <span className="value">{value}</span>
        </p>
      ))}
    </Holder>
  );

  return (
    <div className="jvm-overview">
      <Spin spinning={isPending}>
        <div className="p-5 mb-5 bg-white border-all">
          <div className="jvm-profiler flex justify-between items-center">
            <div className="profiler-info flex-1">
              <p className="info-item">
                <span className="label">{`${i18n.t('dop:analyze ID')}: `}</span>
                <span className="value">{profileId}</span>
              </p>
              <p className="info-item">
                <span className="label">{`${i18n.t('create time')}: `}</span>
                <span className="value">{formatTime(createTime, 'YYYY-MM-DD HH:mm:ss')}</span>
              </p>
              {isRunning ? (
                <p className="info-item">
                  <span className="label">{`${i18n.t('dop:started at')}: `}</span>
                  <span className="value">{fromNow(createTime)}</span>
                </p>
              ) : (
                <p className="info-item">
                  <span className="label">{`${i18n.t('common:end at')}: `}</span>
                  <span className="value">{formatTime(finishTime, 'YYYY-MM-DD HH:mm:ss')}</span>
                </p>
              )}
            </div>
            <div className="profiler-actions ml-6">
              <Button type="primary" disabled={!isRunning} onClick={stopProfile}>
                {i18n.t('dop:stop analysis')}
              </Button>
            </div>
          </div>
        </div>
        <div className="panel block mb-5">
          <div className="panel-title">{i18n.t('dop:jvm process info')}</div>
          <div className="panel-body">{getPanelBody(jvmInfo.jvm_process)}</div>
        </div>
        <div className="flex justify-between items-center">
          <div className="panel block flex-1 mr-5">
            <div className="panel-title">{i18n.t('dop:jvm properties')}</div>
            <div className="panel-body">{getPanelBody(jvmInfo.jvm_options)}</div>
          </div>
          <div className="panel block flex-1">
            <div className="panel-title">{i18n.t('dop:system properties')}</div>
            <div className="panel-body">{getPanelBody(jvmInfo.jvm_properties)}</div>
          </div>
        </div>
      </Spin>
    </div>
  );
};

export default JvmOverview;
