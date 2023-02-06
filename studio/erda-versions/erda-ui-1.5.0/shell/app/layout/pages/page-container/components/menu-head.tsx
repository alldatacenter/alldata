// Copyright (c) 2021 Terminus, Inc.
//
// This program is free software: you can use, redistribute, and/or modify
// it under the terms of the GNU Affero General Public License, version 3
// or later ("AGPL"), as published by the Free Software Foundation.
//
// This program is distributed in the hope that it will be useful, but WITHOUT
// ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
// FITNESS FOR A PARTICULAR PURPOSE.
//
// You should have received a copy of the GNU Affero General Public License
// along with this program. If not, see <http://www.gnu.org/licenses/>.

import React from 'react';
import { Icon as CustomIcon, IF } from 'common';
import { ossImg } from 'common/utils';
import { isFunction } from 'lodash';
import './menu-head.scss';
import devopsSvg from 'app/images/devops.svg';
import cmpSvg from 'app/images/qyzx.svg';
import mspSvg from 'app/images/wfwzl.svg';
import dataSvg from 'app/images/ksj.svg';
import apiManageSvg from 'app/images/fwsc.svg';
import ecpSvg from 'app/images/ecp.svg';
import orgCenterSvg from 'app/images/glzx.svg';

interface IProps {
  siderInfo: Record<string, any>;
  routeMarks: readonly string[];
}

const defaultDetail = {
  name: '',
  displayName: '',
  logo: undefined,
  icon: '',
  logoClassName: '',
};

const MenuHead = ({ siderInfo, routeMarks }: IProps) => {
  const { detail = defaultDetail, getHeadName } = siderInfo || {};
  const { name, displayName, logo, logoClassName = '' } = detail;
  let sideIcon: React.ReactNode = null;
  switch (routeMarks[routeMarks.length - 2]) {
    case 'dop':
      sideIcon = <img className="big-icon" src={devopsSvg} />;
      break;
    case 'sysAdmin':
      sideIcon = <img className="big-icon" src={orgCenterSvg} />;
      break;
    case 'cmp':
      sideIcon = <img className="big-icon" src={cmpSvg} />;
      break;
    case 'orgCenter':
      sideIcon = <img className="big-icon" src={orgCenterSvg} />;
      break;
    case 'msp':
      sideIcon = <img className="big-icon" src={mspSvg} />;
      break;
    case 'fdp':
      sideIcon = <img className="big-icon" src={dataSvg} />;
      break;
    case 'apiManage':
      sideIcon = <img className="big-icon" src={apiManageSvg} />;
      break;
    case 'ecp':
      sideIcon = <img className="big-icon" src={ecpSvg} />;
      break;
    default:
      sideIcon = <CustomIcon color type={detail.icon || 'yy'} />;
      break;
  }
  return (
    <div className="sidebar-info-block">
      <IF check={!!logo}>
        <img key={logo} className={logoClassName} src={ossImg(logo, { w: 120 })} alt="logo" />
        <IF.ELSE />
        {sideIcon}
      </IF>
      {isFunction(getHeadName) ? getHeadName() : <span className="nowrap name">{displayName || name}</span>}
    </div>
  );
};

export default MenuHead;
