import './WithErrorBoundary.css';
import {
  PageSection,
  PageSectionVariants,
  EmptyState,
  EmptyStateVariant,
  EmptyStateIcon,
  Title,
  EmptyStateBody,
  Button,
  EmptyStateSecondaryActions,
} from '@patternfly/react-core';
import { ExclamationTriangleIcon } from '@patternfly/react-icons';
import React from 'react';
import { ErrorBoundary } from 'react-error-boundary';
import { useTranslation } from 'react-i18next';

const ErrorFallback: React.FunctionComponent<{
  error: Error;
  resetErrorBoundary: any;
}> = ({ error, resetErrorBoundary }) => {
  const { t } = useTranslation();

  const [showErrorInfo, setShowErrorInfo] = React.useState(false);

  const openJiraIssue = (url: string) => {
    const newWindow = window.open(url, '_blank', 'noopener,noreferrer');
    if (newWindow) {
      newWindow.opener = null;
    }
  };

  return (
    <React.Fragment>
      <PageSection className="ps_error" variant={PageSectionVariants.light}>
        <div className="application-error-page">
          <EmptyState variant={EmptyStateVariant.large}>
            <EmptyStateIcon icon={ExclamationTriangleIcon} />
            <Title headingLevel="h5" size="lg">
              {t('applicationErrorTitle')}
            </Title>
            <EmptyStateBody>{t('applicationErrorMsg')}</EmptyStateBody>
            <Button variant="primary" onClick={resetErrorBoundary}>
              {t('tryAgain')}
            </Button>
            <EmptyStateSecondaryActions>
              <Button
                variant="link"
                data-testid="error-btn-artifacts"
                // isDisabled={true}
                onClick={() =>
                  openJiraIssue('https://issues.redhat.com/browse/DBZ')
                }
              >
                {t('reportIssue')}
              </Button>
              <Button
                variant="link"
                data-testid="error-btn-details"
                // tslint:disable-next-line: jsx-no-lambda
                onClick={() => setShowErrorInfo(!showErrorInfo)}
              >
                {showErrorInfo ? t('hideDetails') : t('showDetails')}
              </Button>
            </EmptyStateSecondaryActions>
          </EmptyState>
          <div className="separator">&nbsp;</div>
          {showErrorInfo ? (
            <div
              className="application-error-page_details pf-c-empty-state pf-m-lg"
              id="ace-wrapper"
            >
              <pre>{error.message}</pre>
            </div>
          ) : (
            <div />
          )}
        </div>
      </PageSection>
    </React.Fragment>
  );
};

export const WithErrorBoundary: React.FunctionComponent = ({ children }) => (
  <ErrorBoundary FallbackComponent={ErrorFallback} resetKeys={[Date.now()]}>
    {children}
  </ErrorBoundary>
);
