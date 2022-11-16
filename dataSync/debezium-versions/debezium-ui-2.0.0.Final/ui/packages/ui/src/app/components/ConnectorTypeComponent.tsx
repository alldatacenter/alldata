import { ConnectorIcon } from './ConnectorIcon';
import './ConnectorTypeComponent.css';
import {
  Split,
  SplitItem,
  Text,
  TextContent,
  TextVariants,
} from '@patternfly/react-core';
import React from 'react';
import { ConnectorTypeId } from 'shared';

export interface IConnectorTypeComponentProps {
  connectorType?: string;
  showIcon: boolean;
}

export const ConnectorTypeComponent: React.FunctionComponent<
  IConnectorTypeComponentProps
> = (props) => {
  const displayName = () => {
    if (props.connectorType === ConnectorTypeId.MONGO) {
      return 'MongoDB';
    } else if (props.connectorType === ConnectorTypeId.POSTGRES) {
      return 'PostgreSQL';
    } else if (props.connectorType === ConnectorTypeId.MYSQL) {
      return 'MySQL';
    } else if (props.connectorType === ConnectorTypeId.SQLSERVER) {
      return 'SQL Server';
    } else if ( props.connectorType === ConnectorTypeId.ORACLE ) {
      return 'Oracle';
    }
    return 'Unknown';
  };

  return (
    <Split>
      {props.showIcon && props.connectorType ? (
        <SplitItem className="connector-type-icon">
          <ConnectorIcon
            connectorType={props.connectorType}
            alt={props.connectorType}
            width={30}
            height={30}
          />
        </SplitItem>
      ) : null}
      <SplitItem>
        <TextContent>
          <Text component={TextVariants.p}>{displayName()}</Text>
        </TextContent>
      </SplitItem>
    </Split>
  );
};
