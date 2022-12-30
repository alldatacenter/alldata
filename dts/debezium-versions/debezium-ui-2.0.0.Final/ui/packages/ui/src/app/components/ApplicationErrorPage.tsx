import './ApplicationErrorPage.css';
import {
  Button,
  EmptyState,
  EmptyStateBody,
  EmptyStateIcon,
  EmptyStateSecondaryActions,
  EmptyStateVariant,
  PageSection,
  PageSectionVariants,
  Text,
  Title,
} from '@patternfly/react-core';
import { ExclamationTriangleIcon } from '@patternfly/react-icons';
import * as React from 'react';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';

export interface IApplicationErrorPageProps {
  icon?: React.ComponentType<any>;
  title?: string;
  msg?: string;
  error?: Error;
  errorInfo?: React.ErrorInfo;
}

export const ApplicationErrorPage: React.FC<IApplicationErrorPageProps> = (
  props
) => {
  const { t } = useTranslation();

  const [showErrorInfo, setShowErrorInfo] = useState(false);
  const reloadPage = (): void => {
    window.location.reload();
  };

  const errorDetail = () => {
    const msg = props.errorInfo
      ? props.errorInfo.componentStack
      : props.error
      ? JSON.stringify(props.error)
      : t('noDetailsAvailable');
    return (
      <Text
        component={'pre'}
        className={'pf-u-text-align-left application-error-msg_error_msg'}
      >
        {msg}
      </Text>
    );
  };

  return (
    <React.Fragment>
      <PageSection className="ps_error" variant={PageSectionVariants.light}>
        <div className="application-error-page">
          <EmptyState variant={EmptyStateVariant.large}>
            <EmptyStateIcon icon={props.icon || ExclamationTriangleIcon} />
            <Title headingLevel="h5" size="lg">
              {props.title || t('applicationErrorTitle')}
            </Title>
            <EmptyStateBody>
              {props.msg || t('applicationErrorMsg')}
            </EmptyStateBody>
            <Button variant="primary" onClick={reloadPage}>
              {t('reloadPage')}
            </Button>
            <EmptyStateSecondaryActions>
              <Button
                variant="link"
                data-testid="error-btn-artifacts"
                // isDisabled={true}
                // tslint:disable-next-line: jsx-no-lambda
                onClick={() => alert('Coming soon!')}
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
              {errorDetail()}
            </div>
          ) : (
            <div />
          )}
        </div>
      </PageSection>
    </React.Fragment>
  );
};
