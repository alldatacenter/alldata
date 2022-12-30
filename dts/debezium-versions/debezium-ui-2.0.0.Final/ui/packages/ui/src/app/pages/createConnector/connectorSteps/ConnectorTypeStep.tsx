import './ConnectorTypeStep.css';
import { ConnectorType } from '@debezium/ui-models';
import {
  Card,
  CardActions,
  CardBody,
  CardHeader,
  CardHeaderMain,
  CardTitle,
  Flex,
  FlexItem,
} from '@patternfly/react-core';
import { ExclamationTriangleIcon } from '@patternfly/react-icons';
import { PageLoader, ConnectorIcon } from 'components';
import React from 'react';
import { getConnectorTypeDescription, ApiError, WithLoader } from 'shared';

export interface IConnectorTypeStepProps {
  selectedConnectorType?: string;
  onSelectionChange: (connectorType: string | undefined) => void;
  connectorTypesList: ConnectorType[];
  i18nApiErrorTitle?: string;
  i18nApiErrorMsg?: string;
  loading: boolean;
  apiError: boolean;
  errorMsg: Error;
}

export const ConnectorTypeStep: React.FunctionComponent<
  IConnectorTypeStepProps
> = (props) => {
  const onCardSelection = (event: { currentTarget: { id: string } }) => {
    // The id is the connector className
    const newId = event.currentTarget.id;
    const selectedConn = props.connectorTypesList.find(
      (cType) => cType.id === newId
    );

    // Set selection undefined if connector is not enabled or no change in type (deselection)
    const newSelection =
      !selectedConn!.enabled || props.selectedConnectorType === newId
        ? undefined
        : newId;
    props.onSelectionChange(newSelection);
  };
  return (
    <WithLoader
      error={props.apiError}
      loading={props.loading}
      loaderChildren={<PageLoader />}
      errorChildren={
        <ApiError
          i18nErrorTitle={props.i18nApiErrorTitle}
          i18nErrorMsg={props.i18nApiErrorMsg}
          error={props.errorMsg}
        />
      }
    >
      {() => (
        <Flex className="connector-type-step-component_flex">
          {props.connectorTypesList.map((cType, index) => (
            <FlexItem key={index}>
              <Card
                id={cType.id}
                // tslint:disable-next-line: no-empty
                onClick={cType.enabled ? onCardSelection : () => {}}
                isSelectable={cType.enabled}
                isSelected={props.selectedConnectorType === cType.id}
                className={
                  !cType.enabled
                    ? 'connector-type-step-component_flex_disableCard'
                    : ''
                }
              >
                <CardHeader>
                  {!cType.enabled && (
                    <CardActions className="connector-type-step-component_cardAction">
                      <ExclamationTriangleIcon className="connector-type-step-component_cardAction--icon" />{' '}
                      coming soon
                    </CardActions>
                  )}
                  <CardHeaderMain
                    className={'connector-type-step-component_dbIcon'}
                  >
                    <ConnectorIcon
                      connectorType={cType.id}
                      alt={cType.displayName}
                      width={50}
                    />
                  </CardHeaderMain>
                </CardHeader>
                <CardTitle>{cType.displayName}</CardTitle>
                <CardBody>{getConnectorTypeDescription(cType)}</CardBody>
              </Card>
            </FlexItem>
          ))}
        </Flex>
      )}
    </WithLoader>
  );
};
