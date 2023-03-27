import { AppLayoutContext } from 'layout';
import * as React from 'react';

export const AppBreadcrumb: React.FunctionComponent = ({ children }) => {
  const appLayoutContext = React.useContext(AppLayoutContext);

  React.useEffect(function setupElement() {
    appLayoutContext.showBreadcrumb(children);

    return function removeElement() {
      appLayoutContext.showBreadcrumb(null);
    };
  });
  return null;
};
