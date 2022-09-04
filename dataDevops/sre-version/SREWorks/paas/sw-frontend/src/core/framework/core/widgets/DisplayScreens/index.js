import React, { Component } from 'react';
import ScreenOne from './ScreenOne';
import ScreenTwo from './ScreenTwo';
import {
	BulletChart,
} from "bizcharts";

export default class DisplayScreens extends Component {
    render() {
		const {widgetModel, widgetConfig={}} = this.props;
        let {chartDisplayConfig} = widgetConfig;
		let chartBlocksArr = widgetModel.nodeModel.getBlocks();
        let cloneChartDisplayConfig = {...chartDisplayConfig}
        for(let key in chartDisplayConfig) {
			let targetBlock = chartBlocksArr.find(item => item.elementId === chartDisplayConfig[key])
            cloneChartDisplayConfig[key] = targetBlock.elements[0]['config']['rows'][0]['elements'];
        }
        let {displayType='displayOne'} = widgetConfig;
		if(displayType && displayType === 'displayTwo') {
			return <ScreenTwo {...this.props} screenDisplayConfig={cloneChartDisplayConfig} />
		}
        return (
			<ScreenOne {...this.props} screenDisplayConfig={cloneChartDisplayConfig} />		
        );
    }
}
