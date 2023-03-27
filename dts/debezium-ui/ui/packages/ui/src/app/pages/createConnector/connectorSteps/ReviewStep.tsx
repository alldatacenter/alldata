import {
  Text,
  TextVariants,
} from '@patternfly/react-core';
import { JsonViewer } from 'components';
import * as React from 'react';
import { mapToObject } from 'shared';

export interface IReviewStepProps {
  i18nReviewTitle: string;
  i18nReviewMessage: string;
  propertyValues: Map<string, string>;
}

export const ReviewStep: React.FC<IReviewStepProps> = (props) => {
  return (
    <>
      <Text component={TextVariants.h2}>{props.i18nReviewMessage}</Text>
      <JsonViewer  propertyValues={mapToObject(props.propertyValues)}/>
    </>
  );
};
