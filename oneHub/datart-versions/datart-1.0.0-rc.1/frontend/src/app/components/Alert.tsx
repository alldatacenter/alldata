import { ExclamationCircleOutlined, LoadingOutlined } from '@ant-design/icons';
import { LayoutWithBrand } from 'app/components';
import * as AuthLayout from 'app/components/styles/AuthLayout';
import { AuthorizationStatus } from 'app/constants';
import useI18NPrefix from 'app/hooks/useI18NPrefix';
import { lighten } from 'polished';
import { ReactNode, useEffect, useState } from 'react';
import styled from 'styled-components/macro';
import { FONT_SIZE_BASE } from 'styles/StyleConstants';

interface AlertProps {
  status: AuthorizationStatus;
  pendingTitle?: string;
  pendingMessage?: string;
  errorTitle?: string;
  errorMessage?: string;
}

export const Alert = ({
  status,
  pendingTitle,
  pendingMessage,
  errorTitle,
  errorMessage,
}: AlertProps) => {
  const [icon, setIcon] = useState<ReactNode>(null);
  const [title, setTitle] = useState('');
  const [desc, setDesc] = useState<ReactNode>(null);
  const t = useI18NPrefix('authorization');

  useEffect(() => {
    switch (status) {
      case AuthorizationStatus.Pending:
        setTitle(pendingTitle || t('authenticating'));
        setDesc(pendingMessage || t('authenticatingDesc'));
        setIcon(<Loading />);
        break;
      case AuthorizationStatus.Error:
        setTitle(errorTitle || t('error'));
        setDesc(errorMessage || t('errorDesc'));
        setIcon(<Error />);
        break;
      default:
        break;
    }
  }, [status, pendingTitle, pendingMessage, errorTitle, errorMessage, t]);

  return (
    <LayoutWithBrand className="alert">
      <AuthLayout.Picture>{icon}</AuthLayout.Picture>
      <AuthLayout.Title>{title}</AuthLayout.Title>
      <AuthLayout.Description>{desc}</AuthLayout.Description>
    </LayoutWithBrand>
  );
};

const Loading = styled(LoadingOutlined)`
  font-size: ${FONT_SIZE_BASE * 2}px;
  color: ${p => p.theme.textColorLight} !important;
`;

const Error = styled(ExclamationCircleOutlined)`
  color: ${p => lighten(0.15, p.theme.error)} !important;
`;
