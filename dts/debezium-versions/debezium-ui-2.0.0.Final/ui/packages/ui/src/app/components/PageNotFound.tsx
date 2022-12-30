// tslint:disable-next-line: ordered-imports
import {
  Button,
  EmptyState,
  EmptyStateBody,
  EmptyStateIcon,
  EmptyStateVariant,
  PageSection,
  PageSectionVariants,
  Title,
} from '@patternfly/react-core';
import { ExclamationCircleIcon } from '@patternfly/react-icons';
import * as React from 'react';
import { useTranslation } from 'react-i18next';
import { useHistory } from 'react-router-dom';

export const PageNotFound: React.FC = () => {
  const { t } = useTranslation();
  const history = useHistory();

  const onHomeClick = () => {
    history.push('/');
  };
  return (
    <PageSection
      className="ps_rules-header"
      variant={PageSectionVariants.light}
    >
      <EmptyState variant={EmptyStateVariant.full}>
        <EmptyStateIcon icon={ExclamationCircleIcon} />
        <Title headingLevel="h5" size="lg">
          404 Error: {t('pageNotFound')}
        </Title>
        <EmptyStateBody>{t('pageNotFoundMsg')}</EmptyStateBody>
        <Button
          variant="primary"
          data-testid="btn-not-found-home"
          onClick={onHomeClick}
        >
          {t('takeBackToHome')}
        </Button>
      </EmptyState>
    </PageSection>
  );
};
