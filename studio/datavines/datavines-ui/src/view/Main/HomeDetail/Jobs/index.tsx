import React from 'react';
import { Switch, Route } from 'react-router';
import { useRouteMatch } from 'react-router-dom';
import List from './JobsList';
import Instance from './JobsInstance';

const Index = () => {
    const match = useRouteMatch();
    return (
        <Switch>
            <Route path={`${match.path}/jobs`} exact component={List} />
            <Route path={`${match.path}/jobs/instance`} exact component={Instance} />
        </Switch>

    );
};

export default Index;
