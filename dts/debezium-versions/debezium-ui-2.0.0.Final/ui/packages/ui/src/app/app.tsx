import './app.css';
import i18n from 'i18n';
import { AppLayout } from 'layout';
import React from 'react';
import { I18nextProvider, useTranslation } from 'react-i18next';
import { BrowserRouter as Router } from 'react-router-dom';
import {
  ConfirmationButtonStyle,
  ConfirmationDialog,
  RenderRoutes,
  ROUTES,
  WithErrorBoundary,
} from 'shared';

const App: React.FC = () => {
  const [confirm, setConfirm] = React.useState(false);
  const [confirmCallback, setConfirmCallback] = React.useState(null);
  const getConfirmation = (message: any, callback: any) => {
    if (message === 'Code navigation') {
      callback(true);
    } else {
      setConfirmCallback(() => callback);
      setConfirm(true);
    }
  };
  return (
    <Router basename="/#app" getUserConfirmation={getConfirmation}>
      <I18nextProvider i18n={i18n}>
        <AppLayout>
          <React.Suspense fallback={null}>
            <WithErrorBoundary>
              <RenderRoutes routes={ROUTES} />
              {confirm && (
                <UserConfirm
                  confirmCallback={confirmCallback}
                  setConfirm={setConfirm}
                />
              )}
            </WithErrorBoundary>
          </React.Suspense>
        </AppLayout>
      </I18nextProvider>
    </Router>
  );
};
export default App;

const UserConfirm = (props: any) => {
  const { t } = useTranslation();

  function allowTransition() {
    props.setConfirm(false);
    props.confirmCallback(true);
  }

  function blockTransition() {
    props.setConfirm(false);
    props.confirmCallback(false);
  }

  return (
    <ConfirmationDialog
      buttonStyle={ConfirmationButtonStyle.NORMAL}
      i18nCancelButtonText={t('stay')}
      i18nConfirmButtonText={t('leave')}
      i18nConfirmationMessage={t('cancelWarningMsg')}
      i18nTitle={t('exitWizard')}
      showDialog={true}
      onCancel={blockTransition}
      onConfirm={allowTransition}
    />
  );
};
