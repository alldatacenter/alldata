import React from "react";
import { Space } from "antd"
import JSXRender from "../../../../../components/JSXRender";

function CopyRight(props) {
  let { widgetConfig = {} } = props;
  let { validateTime = '2021', spaceNumber = 400, companyOrGroupName = 'SREWorks', copyStyle, isFixed = false, copyRightLabel = 'Copyright©' } = widgetConfig;
  let baseStyle = {
    marginTop: 30,
    marginBottom: 30,
    color: "#666",
    marginLeft: spaceNumber,
  }
  if (!spaceNumber || !Number(spaceNumber)) {
    baseStyle['marginLeft'] = "50%";
    baseStyle['transform'] = "translate(-50%,0)";
  }
  let fixedStyle = {
    position: 'fixed',
    bottom: 0
  }
  baseStyle = isFixed ? { ...baseStyle, ...fixedStyle } : baseStyle
  // baseStyle = copyStyle ? {...copyStyle,...baseStyle} : baseStyle
  return <Space wrap style={baseStyle}>
    <span>{copyRightLabel}</span>
    <span>{validateTime}</span>
    <JSXRender jsx={companyOrGroupName}></JSXRender>
  </Space>
}

export default CopyRight;