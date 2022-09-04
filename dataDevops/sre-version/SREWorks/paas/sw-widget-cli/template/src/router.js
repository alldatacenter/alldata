import React from 'react';
import { Router, Route } from 'react-router'
import HomePage from './HomePage';

const Routers = function () {
    return (
        <Router history={history}>
                <Route path="/" component={HomePage} />
        </Router>
    );
};

export default Routers;
