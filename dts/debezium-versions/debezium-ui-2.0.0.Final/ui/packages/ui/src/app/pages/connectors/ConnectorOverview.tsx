import { Flex, FlexItem, Title } from '@patternfly/react-core';
import * as React from 'react';

export interface IConnectorcOverviewProps {
  i18nOverview: string;
  i18nMessagePerSec: string;
  i18nMaxLagInLastMin: string;
  i18nPercentiles: string;
}

/**
 * Component for display of Connector Overview
 */
export const ConnectorOverview: React.FunctionComponent<
  IConnectorcOverviewProps
> = (props) => {
  return (
    <Flex>
      <FlexItem>
        <Title headingLevel="h3" size={'md'}>
          {props.i18nOverview}
        </Title>
        <div>{props.i18nMessagePerSec}: 0</div>
        <div>{props.i18nMaxLagInLastMin}: 0</div>
        <div>{props.i18nPercentiles}: 0</div>
      </FlexItem>
    </Flex>
  );
};
