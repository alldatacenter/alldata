import './ConnectorDrawer.css';
import { ConnectorStatus } from './ConnectorStatus';
import { Connector } from '@debezium/ui-models';
import { Services } from '@debezium/ui-services';
import {
  Drawer,
  DrawerActions,
  DrawerCloseButton,
  DrawerContent,
  DrawerHead,
  DrawerPanelBody,
  DrawerPanelContent,
  Flex,
  FlexItem,
  Skeleton,
  Tab,
  Tabs,
  TabTitleText,
  Text,
  TextContent,
  TextList,
  TextListItem,
  TextListItemVariants,
  TextListVariants,
  TextVariants,
  Title,
  TitleSizes,
} from '@patternfly/react-core';
import { JsonViewer } from 'components';
import React, {
  FunctionComponent,
  ReactNode,
  useState,
  MouseEvent,
  useEffect,
  Fragment,
} from 'react';
import { useTranslation } from 'react-i18next';
import { WithLoader, ApiError, fetch_retry } from 'shared';
import { AppLayoutContext } from 'layout';


export interface ConnectorDrawerProps {
  children: ReactNode;
  connector: Connector | undefined;
  onClose: () => void;
}

export const ConnectorDrawer: FunctionComponent<ConnectorDrawerProps> = ({
  children,
  connector,
  onClose,
}) => {
  const appLayoutContext = React.useContext(AppLayoutContext);
  return (
    <Drawer isExpanded={connector !== undefined}>
      <DrawerContent
        panelContent={
          connector ? (
            <ConnectorDrawerPanelContent
              clusterID={appLayoutContext.clusterId}
              name={connector.name}
              status={connector.connectorStatus}
              onClose={onClose}
            />
          ) : undefined
        }
      >
        {children}
      </DrawerContent>
    </Drawer>
  );
};

export interface ConnectorDrawerPanelContentProps {
  clusterID: number;
  name: string;
  status: string;
  onClose: () => void;
}

export const ConnectorDrawerPanelContent: FunctionComponent<
  ConnectorDrawerPanelContentProps
> = ({
  clusterID: clusterID,
  name,

  status,
  onClose,
}) => {
  const { t } = useTranslation();
  const [activeTabKey, setActiveTabKey] = useState<string | number>(0);

  const [connector, setConnector] = useState<object>({});

  const [loading, setLoading] = React.useState(true);
  const [apiError, setApiError] = React.useState<boolean>(false);
  const [errorMsg, setErrorMsg] = React.useState<Error>(new Error());

  const selectActiveKey = (_: MouseEvent, eventKey: string | number) => {
    setActiveTabKey(eventKey);
  };

  const textListItem = (title: string, value?: any) => (
    <Fragment key={title}>
      {value && (
        <>
          <TextListItem component={TextListItemVariants.dt}>
            {title}
          </TextListItem>
          <TextListItem component={TextListItemVariants.dd}>
            {value}
          </TextListItem>
        </>
      )}
    </Fragment>
  );

  useEffect(() => {
    setLoading(true);
    const connectorService = Services.getConnectorService();
    fetch_retry(connectorService.getConnectorConfig, connectorService, [
      clusterID,
      name,
    ])
      .then((cConnector) => {
        setLoading(false);
        setConnector(cConnector);
        console.log(cConnector);
      })
      .catch((err: React.SetStateAction<Error>) => {
        setApiError(true);
        setErrorMsg(err);
      });
  }, [clusterID, name]);

  return (
    <DrawerPanelContent widths={{ default: 'width_50' }}>
      <DrawerHead>
        <TextContent>
          <Text
            component={TextVariants.small}
            className="connector-drawer__header-text"
          >
            Connector name
          </Text>

          <Flex>
            <FlexItem>
              <Title
                headingLevel={'h2'}
                size={TitleSizes['xl']}
                className="connector-drawer__header-title"
              >
                {name}
              </Title>
            </FlexItem>
            <FlexItem spacer={{ default: 'spacerSm' }}>
              <ConnectorStatus currentStatus={status} />
            </FlexItem>
          </Flex>
        </TextContent>
        <DrawerActions>
          <DrawerCloseButton onClick={onClose} />
        </DrawerActions>
      </DrawerHead>
      <DrawerPanelBody>
        <Tabs activeKey={activeTabKey} onSelect={selectActiveKey}>
          <Tab
            eventKey={0}
            title={<TabTitleText>{t('Table view')}</TabTitleText>}
          >
            <WithLoader
              error={apiError}
              loading={loading}
              loaderChildren={
                <div style={{ height: '250px' }} className="pf-u-pt-lg">
                  <Skeleton width="25%" screenreaderText="Loading contents" />
                  <br />
                  <Skeleton width="33%" />
                  <br />
                  <Skeleton width="50%" />
                  <br />
                  <Skeleton width="66%" />
                  <br />
                  <Skeleton width="75%" />
                  <br />
                  <Skeleton />
                </div>
              }
              errorChildren={
                <ApiError
                  i18nErrorTitle={t('apiErrorTitle')}
                  i18nErrorMsg={t('apiErrorMsg')}
                  error={errorMsg}
                />
              }
            >
              {() => (
                <div className="connector-drawer__tab-content">
                  <TextContent>
                    <TextList component={TextListVariants.dl}>
                      {Object.entries(connector).map((list) => {
                        return textListItem(list[0], list[1]);
                      })}
                    </TextList>
                  </TextContent>
                </div>
              )}
            </WithLoader>
          </Tab>

          <Tab
            eventKey={1}
            title={<TabTitleText>{t('JSON view')}</TabTitleText>}
          >
            <WithLoader
              error={apiError}
              loading={loading}
              loaderChildren={
                <div style={{ height: '250px' }} className="pf-u-pt-lg">
                  <Skeleton
                    height="100%"
                    screenreaderText="Loading large rectangle contents"
                  />
                </div>
              }
              errorChildren={
                <ApiError
                  i18nErrorTitle={t('apiErrorTitle')}
                  i18nErrorMsg={t('apiErrorMsg')}
                  error={errorMsg}
                />
              }
            >
              {() => (
                <div className="connector-drawer__tab-content">
                  <JsonViewer propertyValues={connector} />
                </div>
              )}
            </WithLoader>
          </Tab>
        </Tabs>
      </DrawerPanelBody>
    </DrawerPanelContent>
  );
};
