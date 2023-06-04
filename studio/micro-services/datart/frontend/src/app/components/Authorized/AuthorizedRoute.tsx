import React from 'react';
import { Redirect, RedirectProps, Route, RouteProps } from 'react-router-dom';
import { Authorized } from './Authorized';

interface AuthorizedRouteProps {
  authority: boolean;
  routeProps: RouteProps;
  redirectProps: RedirectProps;
}

export function AuthorizedRoute({
  authority,
  routeProps,
  redirectProps,
}: AuthorizedRouteProps) {
  return (
    <Authorized authority={authority} denied={<Redirect {...redirectProps} />}>
      <Route {...routeProps} />
    </Authorized>
  );
}
