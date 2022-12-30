import './layout.css';
import { Page, PageSection, PageSectionVariants } from '@patternfly/react-core';
import { AppHeader, AppLayoutContext } from 'layout';
import * as React from 'react';

export const AppLayout: React.FC = ({ children }) => {
  const [breadcrumb, setHasBreadcrumb] = React.useState(null);
  const [clusterId, setClusterId] = React.useState<number>(1);
  const showBreadcrumb = (b: any) => setHasBreadcrumb(b);

  const handleClusterIdChange = (id: number) => {
    setClusterId(id);
  };
  return (
    <AppLayoutContext.Provider value={{ showBreadcrumb, clusterId }}>
      <Page
        header={<AppHeader handleClusterChange={handleClusterIdChange} />}
        breadcrumb={breadcrumb}
        className="app-page"
      >
        <PageSection variant={PageSectionVariants.light}>
          {children}
        </PageSection>
      </Page>
    </AppLayoutContext.Provider>
  );
};
