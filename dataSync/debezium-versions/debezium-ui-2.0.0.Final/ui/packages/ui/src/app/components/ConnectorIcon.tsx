import PostgresIcon from 'assets/images/PostgreSQL-120x120.png';
import MongoIcon from 'assets/images/mongodb-128x128.png';
import MySqlIcon from 'assets/images/mysql-170x115.png';
import SqlServerIcon from 'assets/images/sql-server-144x144.png';
import PlaceholderIcon from 'assets/images/placeholder-120x120.png';

import * as React from 'react';
import { ConnectorTypeId } from 'shared';

export interface IConnectorIconProps {
  connectorType: string;
  alt: string;
  width?: number;
  height?: number;
  className?: string;
}

export const ConnectorIcon: React.FunctionComponent<IConnectorIconProps> = (
  props
) => {
  let connIcon = null;
  if (props.connectorType === ConnectorTypeId.MYSQL) {
    connIcon = MySqlIcon;
  } else if (props.connectorType === ConnectorTypeId.POSTGRES) {
    connIcon = PostgresIcon;
  } else if (props.connectorType === ConnectorTypeId.SQLSERVER) {
    connIcon = SqlServerIcon;
  } else if (props.connectorType === ConnectorTypeId.MONGO) {
    connIcon = MongoIcon;
  } else if (props.connectorType === ConnectorTypeId.ORACLE) {
    connIcon = PlaceholderIcon;
  }

  return (
    <img
      src={connIcon || ''}
      alt={props.alt}
      width={props.width}
      height={props.height}
      className={props.className}
    />
  );
};
