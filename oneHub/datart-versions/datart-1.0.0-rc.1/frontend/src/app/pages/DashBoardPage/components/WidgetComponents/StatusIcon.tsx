/**
 * Datart
 *
 * Copyright 2021
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import {
  ApiOutlined,
  ClockCircleOutlined,
  LinkOutlined,
  Loading3QuartersOutlined,
  LockOutlined,
  WarningTwoTone,
} from '@ant-design/icons';
import { Button, Tooltip } from 'antd';
import useI18NPrefix from 'app/hooks/useI18NPrefix';
import React, { memo, useCallback, useContext } from 'react';
import styled from 'styled-components/macro';
import { ERROR, PRIMARY } from 'styles/StyleConstants';
import { WidgetErrorType } from '../../pages/Board/slice/types';
import { Widget } from '../../types/widgetTypes';
import { WidgetActionContext } from '../ActionProvider/WidgetActionProvider';

export const LockIconFn: React.FC<{
  boardEditing: boolean;
  wid: string;
  lock: boolean;
}> = memo(({ boardEditing, wid, lock }) => {
  const { onEditWidgetUnLock } = useContext(WidgetActionContext);
  const t = useI18NPrefix(`viz.widget.tips`);
  if (!boardEditing) return null;
  if (!lock) return null;
  return (
    <LockIcon title={t('unlock')} onClick={() => onEditWidgetUnLock(wid)} />
  );
});
export const LockIcon: React.FC<{
  title: React.ReactNode;
  onClick?: React.MouseEventHandler<HTMLElement> | undefined;
}> = ({ title, onClick }) => {
  return (
    <Tooltip title={title}>
      <Button
        icon={<LockOutlined style={{ color: PRIMARY, opacity: 0.5 }} />}
        type="link"
        onClick={onClick}
      />
    </Tooltip>
  );
};
export const WaitIconFn: React.FC<{ rendered: boolean; widget: Widget }> = memo(
  ({ rendered, widget }) => {
    const { onWidgetGetData } = useContext(WidgetActionContext);
    const t = useI18NPrefix(`viz.widget.tips`);
    const onRefreshWidget = useCallback(() => {
      onWidgetGetData(widget);
    }, [onWidgetGetData, widget]);
    if (rendered) return null;
    return (
      <WaitingIcon
        onClick={onRefreshWidget}
        onMouseEnter={onRefreshWidget}
        title={t('waiting')}
      />
    );
  },
);
export const WaitingIcon: React.FC<{
  title: React.ReactNode;
  onClick?: React.MouseEventHandler<HTMLSpanElement> | undefined;
  onMouseEnter?: React.MouseEventHandler<HTMLSpanElement> | undefined;
}> = ({ title, onClick, onMouseEnter }) => {
  return (
    <Tooltip title={title ?? 'waitingLoad'}>
      <Button
        onClick={onClick}
        onMouseEnter={onMouseEnter}
        icon={<ClockCircleOutlined style={{ color: PRIMARY }} />}
        type="link"
      />
    </Tooltip>
  );
};
export const LoadingIcon: React.FC<{ loading?: boolean }> = ({ loading }) => {
  if (!loading) return null;
  return (
    <Button
      icon={
        <Loading3QuartersOutlined
          spin
          style={{ color: PRIMARY, opacity: 0.4 }}
        />
      }
      type="link"
    />
  );
};

export const CancelLinkageIcon: React.FC<{
  title: React.ReactNode | undefined;
  onClick: React.MouseEventHandler<HTMLSpanElement> | undefined;
}> = ({ title, onClick }) => {
  return (
    <Tooltip title={title ?? 'cancel Linkage'}>
      <ApiOutlined style={{ color: PRIMARY }} onClick={onClick} />
    </Tooltip>
  );
};
export const CanLinkageIcon: React.FC<{
  title: React.ReactNode | undefined;
}> = ({ title }) => {
  return (
    <Tooltip title={title}>
      <Button icon={<LinkOutlined style={{ color: PRIMARY }} />} type="link" />
    </Tooltip>
  );
};
export const LinkageIconFn: React.FC<{
  inLinking: boolean;
  widget: Widget;
}> = memo(({ inLinking, widget }) => {
  const { onWidgetClearLinkage } = useContext(WidgetActionContext);
  const t = useI18NPrefix(`viz.widget.tips`);
  if (inLinking) {
    return (
      <CancelLinkageIcon
        title={t('cancelLinkage')}
        onClick={() => onWidgetClearLinkage(widget)}
      />
    );
  } else {
    return widget.config?.linkageConfig?.open ? (
      <CanLinkageIcon title={t('canLinkage')} />
    ) : null;
  }
});
const StyledErrorIcon = styled(Button)`
  background: ${p => p.theme.componentBackground};
  &:hover,
  &:focus {
    background: ${p => p.theme.componentBackground};
  }
`;

export const ErrorIcon: React.FC<{
  errInfo?: Record<WidgetErrorType, string>;
}> = memo(({ errInfo }) => {
  if (!errInfo) return null;
  const errorInfos = Object.values(errInfo);
  if (!errorInfos.length) return null;
  const errHtml = (
    <div style={{ maxHeight: '200px', maxWidth: '400px', overflow: 'auto' }}>
      {errorInfos.map((item, i) => {
        return <p key={i}>{String(item)}</p>;
      })}
    </div>
  );
  return (
    <Tooltip title={errHtml}>
      <StyledErrorIcon
        icon={<WarningTwoTone twoToneColor={ERROR} />}
        type="link"
      />
    </Tooltip>
  );
});
