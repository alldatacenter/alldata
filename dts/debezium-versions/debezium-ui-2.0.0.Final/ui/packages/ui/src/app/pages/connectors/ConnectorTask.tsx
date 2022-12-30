import './ConnectorTask.css';
import { Button, Flex, FlexItem, Label, Popover } from '@patternfly/react-core';
import { InfoCircleIcon } from '@patternfly/react-icons';
import * as React from 'react';
import { ConnectorState } from 'shared';

export interface IConnectorTaskProps {
  status: string;
  connName: string;
  taskId: string;
  errors?: any;
  i18nTask: string;
  i18nRestart: string;
  i18nTaskStatusDetail: string;
  i18nTaskErrorTitle: string;
  i18nMoreInformation: string;
  connectorTaskToRestart: (connName: string, taskId: string) => void;
  showConnectorTaskToRestartDialog: () => void;
}

/**
 * Component for display of Connector Task
 */
export const ConnectorTask: React.FunctionComponent<IConnectorTaskProps> = (
  props
) => {
  let color: 'grey' | 'green' | 'red' | 'orange' = 'grey';
  switch (props.status) {
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
  const errors: JSX.Element[] = [];
  if (props.errors) {
    props.errors.forEach((error: string, index: any) => {
      errors.push(
        <div key={index} className="connector-task-error">
          {error}
        </div>
      );
    });
  }
  const doConnectorTaskRestart = (connName: string, taskId: string) => {
    props.connectorTaskToRestart(connName, taskId);
    props.showConnectorTaskToRestartDialog();
  };

  return (
    <Flex justifyContent={{ default: 'justifyContentSpaceBetween' }}>
      <FlexItem className="taskInfo" flex={{ default: 'flex_1' }}>
        <Label
          className="status-indicator"
          color={color}
          data-testid={'connector-status-id-div'}
        >
          {props.taskId}
        </Label>
      </FlexItem>
      <FlexItem className="taskInfo" flex={{ default: 'flex_2' }}>
        {errors && errors.length > 0 ? (
          <Popover
            aria-label={props.status}
            headerContent={
              <div>
                {props.i18nTaskErrorTitle} {props.status}!
              </div>
            }
            bodyContent={<div>{errors}</div>}
          >
            <Label
              className="status-indicator"
              title={props.i18nMoreInformation}
              color={color}
              data-testid={'task-status-label'}
            >
              {props.status} &nbsp; <InfoCircleIcon size="sm" />
            </Label>
          </Popover>
        ) : (
          <Label
            className="status-indicator"
            color={color}
            data-testid={'task-status-label'}
          >
            {props.status}
          </Label>
        )}
      </FlexItem>
      <FlexItem flex={{ default: 'flex_1' }}>
        <Button
          variant="link"
          onClick={() => {
            doConnectorTaskRestart(props.connName, props.taskId);
          }}
        >
          {props.i18nRestart}
        </Button>
      </FlexItem>
    </Flex>
  );
};
