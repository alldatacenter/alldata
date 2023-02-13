import React, { Component } from 'react';
import BChart from '../BChart';
import Bullet from '../Bullet';
import Dashboard from '../Dashboard';
import FluidCharts from '../FluidCharts';
import Pie from '../Pie';
import SliderChart from '../SliderChart';
import BizAreaChart from '../BizAreaChart';
import BizRadarChart from '../BizRadarChart'
import BizStackedAreaChart from '../BizStackedAreaChart'
import BizStackedColumnChart from '../BizStackedColumnChart'
import BizStackedBarChart from '../BizStackedBarChart'
import BizTreemapChart from '../BizTreemapChart'
 class ChartFactory{
    static chartTypeArray = [
        "Pie",
        "SliderChart",
        "FluidCharts",
        "Dashboard",
        'Bullet',
        "BChart",
        "BizAreaChart",
        "BizRadarChart",
        "BizStackedAreaChart",
        "BizStackedColumnChart",
        "BizStackedBarChart",
        "BizTreemapChart"
    ];
    static  createScreenChart(widgetConfig) {
        let itemElement = <div>未定义的图表组件</div>,type = (widgetConfig && widgetConfig.type) || ''
        switch(type) {
            case "Pie": itemElement = <Pie widgetConfig={widgetConfig.config} /> 
            break;
            case "SliderChart": itemElement = <SliderChart widgetConfig={widgetConfig.config} /> 
            break;
            case "FluidCharts": itemElement = <FluidCharts widgetConfig={widgetConfig.config} /> 
            break;
            case "Dashboard": itemElement = <Dashboard widgetConfig={widgetConfig.config} /> 
            break;
            case "Bullet": itemElement = <Bullet widgetConfig={widgetConfig.config} /> 
            break;
            case "BChart": itemElement = <BChart widgetConfig={widgetConfig.config} /> 
            break;
            case "BizAreaChart": itemElement = <BizAreaChart widgetConfig={widgetConfig.config} /> 
            break;
            case "BizRadarChart": itemElement = <BizRadarChart widgetConfig={widgetConfig.config} /> 
            break;
            case "BizStackedAreaChart": itemElement = <BizStackedAreaChart widgetConfig={widgetConfig.config} /> 
            break;
            case "BizStackedColumnChart": itemElement = <BizStackedColumnChart widgetConfig={widgetConfig.config} /> 
            break;
            case "BizStackedBarChart": itemElement = <BizStackedBarChart widgetConfig={widgetConfig.config} /> 
            break;
            case "BizTreemapChart": itemElement = <BizTreemapChart widgetConfig={widgetConfig.config} /> 
            break;
            default:itemElement = <div>未定义的图表组件</div>
            break;
        }
        return itemElement
    }
}
export default ChartFactory;