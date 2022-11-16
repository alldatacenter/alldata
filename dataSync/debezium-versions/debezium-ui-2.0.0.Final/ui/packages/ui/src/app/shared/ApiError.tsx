import { PageSection, PageSectionVariants } from '@patternfly/react-core';
import { ApplicationErrorPage } from 'components';
import * as React from 'react';

export interface IApiErrorProps {
  error: Error | string;
  errorInfo?: React.ErrorInfo;
  i18nErrorTitle?: string;
  i18nErrorMsg?: string;
}

export const ApiError: React.FC<IApiErrorProps> = (props) => (
  <PageSection
    variant={PageSectionVariants.light}
    className="app-page-section-border-bottom"
  >
    <ApplicationErrorPage
      title={props.i18nErrorTitle}
      msg={props.i18nErrorMsg}
      error={
        typeof props.error === 'string' ? new Error(props.error) : props.error
      }
      errorInfo={props.errorInfo}
    />
  </PageSection>
);
