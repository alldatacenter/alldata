import React from 'react';
import { Switch, Route } from 'react-router';
import { useRouteMatch } from 'react-router-dom';
import SLAsNoticeList from './SLAsNoticeList';
import SLAs from './SLAsMetric';
import SLAsSetting from './SLAsSetting';

const Index = () => {
    const match = useRouteMatch();
    return (
        <div className="dv-page-paddinng">
            <Switch>
                <Route path={match.path} exact component={SLAsNoticeList} />
                <Route path={`${match.path}/SLAs`} exact component={SLAs} />
                <Route path={`${match.path}/SLAsSetting`} exact component={SLAsSetting} />
            </Switch>
        </div>
    );
};

export default Index;
