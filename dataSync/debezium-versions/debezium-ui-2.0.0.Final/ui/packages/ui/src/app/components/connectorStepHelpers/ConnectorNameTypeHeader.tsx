import { ConnectorTypeComponent } from '..';
import './ConnectorNameTypeHeader.css';
import {
  Split,
  SplitItem,
  Text,
  TextContent,
  TextVariants,
} from '@patternfly/react-core';
import React from 'react';

export interface IConnectorNameTypeHeaderProps {
  connectorName?: string;
  connectorType?: string;
  showIcon: boolean;
}

export const ConnectorNameTypeHeader: React.FunctionComponent<
  IConnectorNameTypeHeaderProps
> = (props) => {
  return (
    <Split>
      <SplitItem>
        <TextContent>
          <Text component={TextVariants.p}>{props.connectorName}</Text>
        </TextContent>
      </SplitItem>
      <SplitItem>
        <TextContent>
          <Text className={'connector-name-type-header_divider'}>|</Text>
        </TextContent>
      </SplitItem>
      <SplitItem>
        <ConnectorTypeComponent
          connectorType={props.connectorType}
          showIcon={props.showIcon}
        />
      </SplitItem>
    </Split>
  );
};
