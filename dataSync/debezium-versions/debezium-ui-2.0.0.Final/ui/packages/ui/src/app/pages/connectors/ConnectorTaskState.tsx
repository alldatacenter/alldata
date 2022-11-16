import './ConnectorTask.css';
import { Connector } from '@debezium/ui-models';
import { Flex, FlexItem, Label } from '@patternfly/react-core';
import * as React from 'react';
import { ConnectorState } from 'shared';

export interface IConnectorTaskStateProps {
  connector: Connector;
}

/**
 * Component for display of Connector Task Status
 */
export const ConnectorTaskState: React.FunctionComponent<
  IConnectorTaskStateProps
> = (props) => {
  let color: 'grey' | 'green' | 'red' | 'orange' = 'grey';
  const { connector } = props;
  const totalNumberOfTasks = Object.keys(connector.taskStates).length;
  const statesMap = new Map(Object.entries(connector.taskStates));

  const connectorStatusCount = {};
  statesMap.forEach((taskState: any) => {
    connectorStatusCount[taskState.taskStatus] =
      ++connectorStatusCount[taskState.taskStatus] || 1;
    return connectorStatusCount;
  });

  const taskStatusList: JSX.Element[] = [];
  for (const [key, value] of Object.entries(connectorStatusCount)) {
    switch (key) {
      case ConnectorState.DESTROYED:
      case ConnectorState.FAILED:
        color = 'red';
        break;
      case ConnectorState.RUNNING:
        color = 'green';
        break;
      case ConnectorState.PAUSED:
        color = 'orange';
        break;
      case ConnectorState.UNASSIGNED:
        color = 'grey';
        break;
    }
    taskStatusList.push(
      <FlexItem key={key}>
        <Label
          className="status-indicator"
          color={color}
          data-testid={'connector-status-count-div'}
        >
          {key} : {value}
        </Label>
      </FlexItem>
    );
  }
  return (
    <>
      <Flex className="taskStates" flex={{ default: 'flex_3' }}>
        <FlexItem>
          <Label
            className="status-indicator"
            color={color}
            data-testid={'connector-count-div'}
          >
            {totalNumberOfTasks}
          </Label>
        </FlexItem>
        <FlexItem>
          <Flex>{taskStatusList}</Flex>
        </FlexItem>
      </Flex>
    </>
  );
};
