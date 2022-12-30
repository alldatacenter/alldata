import { PropertyValidationResult } from '@debezium/ui-models';
import React from 'react';

export interface IConnectionPropertiesErrorProps {
  connectionPropsMsg: PropertyValidationResult[];
  i18nFieldValidationErrorMsg: string;
  i18nValidationErrorMsg: string;
}

export const ConnectionPropertiesError: React.FunctionComponent<
  IConnectionPropertiesErrorProps
> = (props) => {
  if (props.connectionPropsMsg.length !== 0) {
    return (
      <ul>
        {props.connectionPropsMsg.map((item, index) => (
          <li key={index}>
            {item.property === 'Generic'
              ? `${item.displayName}: ${item.message}`
              : props.i18nFieldValidationErrorMsg.replace(
                  '**',
                  `${item.displayName}(${item.property})`
                )}
          </li>
        ))}
      </ul>
    );
  } else {
    return <div>{props.i18nValidationErrorMsg}</div>;
  }
};
