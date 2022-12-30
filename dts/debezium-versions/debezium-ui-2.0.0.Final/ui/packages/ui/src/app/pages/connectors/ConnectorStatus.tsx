import { Label } from '@patternfly/react-core';
import {
  CheckCircleIcon,
  ExclamationCircleIcon,
  ExclamationTriangleIcon,
} from '@patternfly/react-icons';
import * as React from 'react';
import { ConnectorState } from 'shared';

export interface IConnectorStatusProps {
  currentStatus: string;
}

/**
 * Component for display of Connector Status
 */
export const ConnectorStatus: React.FunctionComponent<IConnectorStatusProps> = (
  props
) => {
  let color: 'grey' | 'green' | 'red' | 'orange' = 'grey';
  let statusIcon;
  switch (props.currentStatus) {
    case ConnectorState.DESTROYED:
    case ConnectorState.FAILED:
      color = 'red';
      statusIcon = <ExclamationCircleIcon />;
      break;
    case ConnectorState.RUNNING:
      color = 'green';
      statusIcon = <CheckCircleIcon />;
      break;
    case ConnectorState.PAUSED:
      color = 'orange';
      statusIcon = <ExclamationTriangleIcon />;
      break;
    case ConnectorState.UNASSIGNED:
      color = 'grey';
      statusIcon = <ExclamationCircleIcon />;
      break;
  }

  return (
    <Label data-testid={'connector-status-label'} color={color}>
      {statusIcon}&nbsp; {props.currentStatus}
    </Label>
  );
};
