import { Bullseye, PageSection, Spinner } from '@patternfly/react-core';
import React from 'react';

export const PageLoader: React.FunctionComponent = () => {
  return (
    <PageSection>
      <Bullseye>
        <Spinner size={'lg'} />
      </Bullseye>
    </PageSection>
  );
};
