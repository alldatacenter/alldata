import ConnectorsTableComponent from './ConnectorsTableComponent';
import './ConnectorsTableComponent.css';
import { AppLayoutContext } from 'layout';
import React from 'react';
import { useTranslation } from 'react-i18next';
import { useHistory } from 'react-router-dom';

export const ConnectorsPage: React.FunctionComponent = (props) => {
  const history = useHistory();

  const { t } = useTranslation();

  const createConnector = (connectorNames: string[], clusterId: number) => {
    history.push({
      pathname: '/create-connector',
      state: { value: clusterId, connectorNames },
    });
  };

  const appLayoutContext = React.useContext(AppLayoutContext);
  return (
    <ConnectorsTableComponent
      createConnectorCallback={createConnector}
      i18nApiErrorTitle={t('apiErrorTitle')}
      i18nApiErrorMsg={t('apiErrorMsg')}
      title={t('connectors')}
      clusterId={appLayoutContext.clusterId}
    />
  );
};
