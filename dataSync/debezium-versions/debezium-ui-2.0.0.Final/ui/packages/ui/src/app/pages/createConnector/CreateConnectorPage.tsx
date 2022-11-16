import CreateConnectorComponent from './CreateConnectorComponent';
import './CreateConnectorComponent.css';
import {
  Breadcrumb,
  BreadcrumbItem,
  Level,
  LevelItem,
  PageSection,
  PageSectionVariants,
  TextContent,
  Title,
  TitleSizes,
} from '@patternfly/react-core';
import React from 'react';
import { useTranslation } from 'react-i18next';
import { useHistory, useLocation } from 'react-router-dom';

interface ILocationState {
  value: number;
  connectorNames: string[];
}

export const CreateConnectorPage: React.FunctionComponent = () => {
  const { t } = useTranslation();
  const history = useHistory();

  const onSuccess = () => {
    history.push('/');
  };

  const onCancel = () => {
    history.push('/');
  };

  const location = useLocation<ILocationState>();

  const clusterID = location.state?.value;
  const connectorNames = location.state?.connectorNames;

  return (
    <>
      <PageSection
        variant={PageSectionVariants.light}
        className="create-connector-page_breadcrumb"
      >
        <Breadcrumb>
          <BreadcrumbItem to="/">Connectors</BreadcrumbItem>
          <BreadcrumbItem isActive={true}>
            {t('createConnector')}
          </BreadcrumbItem>
        </Breadcrumb>
        <Level hasGutter={true}>
          <LevelItem>
            <TextContent>
              <Title headingLevel="h3" size={TitleSizes['2xl']}>
                {'Configure a connector'}
              </Title>
            </TextContent>
          </LevelItem>
        </Level>
      </PageSection>
      <div className="app-page-section-border-bottom">
        <CreateConnectorComponent
          onCancelCallback={onCancel}
          onSuccessCallback={onSuccess}
          clusterId={'' + clusterID}
          connectorNames={connectorNames}
        />
      </div>
    </>
  );
};
