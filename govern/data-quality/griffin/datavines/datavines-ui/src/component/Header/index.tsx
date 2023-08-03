import React from 'react';
import Logo from './Logo';
import Settings from './Settings';
import WorkSpaceSwitch from './WorkSpaceSwitch';
import './index.less';

const Header = () => (
    <div className="dv-header">
        <Logo />
        <WorkSpaceSwitch />
        <Settings />
    </div>
);

export default Header;
