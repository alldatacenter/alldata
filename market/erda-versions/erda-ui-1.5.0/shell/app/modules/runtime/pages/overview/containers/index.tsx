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
import { Spin, Modal, message } from 'antd';
import { goTo, getLS } from 'common/utils';
import { useUpdate } from 'common/use-hooks';
import { isEmpty } from 'lodash';
import PureRuntimeOverView from '../components';
import i18n from 'i18n';
import { useUnmount, useMount } from 'react-use';
import runtimeStore from 'app/modules/runtime/stores/runtime';
import runtimeServiceStore from 'app/modules/runtime/stores/service';
import runtimeDomainStore from 'app/modules/runtime/stores/domain';
import routeInfoStore from 'core/stores/route';
import { useLoading } from 'core/stores/loading';

const { confirm } = Modal;

export const confirmRedeploy = () => {
  confirm({
    title: i18n.t('runtime:confirm restart Runtime?'),
    onOk: () => {
      runtimeDomainStore.updateDomains().then(() => {
        runtimeStore.redeployRuntime();
      });
    },
  });
};

const RuntimeOverView = () => {
  const params = routeInfoStore.useStore((s) => s.params);
  const [runtimeDetail, showRedirect, hasChange] = runtimeStore.useStore((s) => [
    s.runtimeDetail,
    s.showRedirect,
    s.hasChange,
  ]);
  const [loading] = useLoading(runtimeStore, ['getRuntimeDetail']);
  const [state, updater] = useUpdate({
    redirectVisible: false,
  });

  useMount(() => {
    const _hasChange = !isEmpty(getLS(`${params.runtimeId}`) || getLS(`${params.runtimeId}_domain`));
    runtimeStore.setHasChange(_hasChange);
  });

  React.useEffect(() => {
    runtimeStore.toggleRedirect(false);
    if (hasChange) {
      const content = (
        <span>
          {i18n.t('runtime:configuration has changed')}，{i18n.t('runtime:please')}
          <span className="redeploy-tip-btn" onClick={confirmRedeploy}>
            {i18n.t('runtime:restart')}
          </span>
          Runtime
        </span>
      );

      if (hasChange) {
        message.info(content, 0);
      } else {
        message.destroy();
      }
    }
  }, [params.runtimeId, hasChange]);

  React.useEffect(() => {
    if (showRedirect && !state.redirectVisible) {
      updater.redirectVisible(true);
      confirm({
        title: `Runtime ${i18n.t('runtime:deleted')}`,
        content: i18n.t('runtime:the current runtime has been deleted, back to the pipeline?'),
        onOk() {
          goTo('../../../');
        },
        onCancel() {
          updater.redirectVisible(false);
        },
      });
    }
  }, [showRedirect, state.redirectVisible, updater]);

  useUnmount(() => {
    runtimeStore.toggleRedirect(false);
    runtimeStore.clearServiceConfig();
    runtimeServiceStore.clearServiceInsMap();
    // keep as last clear
    runtimeStore.clearRuntimeDetail();
    message.destroy();
  });

  const isDeleting = runtimeDetail.deleteStatus === 'DELETING';

  return (
    <Spin spinning={loading || isDeleting} tip={isDeleting ? `${i18n.t('runtime:deleting')}...` : undefined}>
      <PureRuntimeOverView />
    </Spin>
  );
};

export default RuntimeOverView;
