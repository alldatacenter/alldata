import React, { Component } from 'react';
import { connect } from 'dva'
import { loadChartData } from '../../../../../utils/loadChartData';
import safeEval from '../../../../../utils/SafeEval';
import {
    getTheme,
    WordCloudChart
} from "bizcharts";
import _ from 'lodash'
@connect(({ node }) => ({
    nodeParams: node.nodeParams,
}))
class BizWordCloud extends Component {
    constructor(props) {
        super(props);
        this.state = {
            chartData: null,
            nodeParams: _.cloneDeep(props.nodeParams)
        }
        this.timerInterval = null
    }
    componentDidMount() {
        let { widgetConfig = {} } = this.props;
        let { period } = widgetConfig
        if (period) {
            this.intervalLoad()
            this.timerInterval = setInterval(() => {
                this.intervalLoad()
            }, Number(period) || 10000)
        }
    }
    async intervalLoad() {
        let { widgetConfig = {} } = this.props;
        let allProps = { ...this.props }
        let data = await loadChartData(allProps, widgetConfig);
        this.setState({
            chartData: data
        })
    }
    componentWillUnmount() {
        if (this.timerInterval) {
            clearInterval(this.timerInterval)
        }
    }
    render() {
        const { widgetConfig = {}, widgetData } = this.props;
        let { chartData } = this.state;
        let { theme, appendPadding, backgroundColor, chartTitle, randomNum, height = '', width, advancedConfig = {} } = widgetConfig;
        if (appendPadding && appendPadding.indexOf(',') > -1) {
            appendPadding = appendPadding.split(',').map(item => Number(item))
        }
        // function getRandomColor() {
        //     const arr = [
        //         '#5B8FF9',
        //         '#5AD8A6',
        //         '#5D7092',
        //         '#F6BD16',
        //         '#E8684A',
        //         '#6DC8EC',
        //         '#9270CA',
        //         '#FF9D4D',
        //         '#269A99',
        //         '#FF99C3',
        //     ];
        //     return arr[Math.floor(Math.random() * (arr.length - 1))];
        // }
        function getDataList(data) {
            const list = [];
            // change data type
            data.forEach((d) => {
                list.push({
                    word: d.name,
                    weight: d.value,
                    id: list.length,
                });
            });
            return list;
        }
        const data = [
            {
                "value": 12,
                "name": "G2Plot"
            },
            {
                "value": 9,
                "name": "AntV"
            },
            {
                "value": 8,
                "name": "F2"
            },
            {
                "value": 8,
                "name": "G2"
            },
            {
                "value": 8,
                "name": "G6"
            },
            {
                "value": 8,
                "name": "DataSet"
            },
            {
                "value": 8,
                "name": "墨者学院"
            },
            {
                "value": 6,
                "name": "Analysis"
            },
            {
                "value": 6,
                "name": "Data Mining"
            },
            {
                "value": 6,
                "name": "Data Vis"
            },
            {
                "value": 6,
                "name": "Design"
            },
            {
                "value": 6,
                "name": "Grammar"
            },
            {
                "value": 6,
                "name": "Graphics"
            },
            {
                "value": 6,
                "name": "Graph"
            },
            {
                "value": 6,
                "name": "Hierarchy"
            },
            {
                "value": 6,
                "name": "Labeling"
            },
            {
                "value": 6,
                "name": "Layout"
            },
            {
                "value": 6,
                "name": "Quantitative"
            },
            {
                "value": 6,
                "name": "Relation"
            },
            {
                "value": 4,
                "name": "Arc Diagram"
            },
            {
                "value": 4,
                "name": "Bar Chart"
            },
            {
                "value": 4,
                "name": "Canvas"
            },
            {
                "value": 4,
                "name": "Chart"
            },
            {
                "value": 4,
                "name": "DAG"
            },
            {
                "value": 4,
                "name": "DG"
            },
            {
                "value": 4,
                "name": "Facet"
            },
            {
                "value": 4,
                "name": "Geo"
            },
            {
                "value": 4,
                "name": "Line"
            },
            {
                "value": 4,
                "name": "MindMap"
            },
            {
                "value": 4,
                "name": "Pie"
            },
            {
                "value": 4,
                "name": "Pizza Chart"
            },
            {
                "value": 4,
                "name": "Punch Card"
            },
            {
                "value": 4,
                "name": "SVG"
            },
            {
                "value": 4,
                "name": "Sunburst"
            },
            {
                "value": 4,
                "name": "Tree"
            },
            {
                "value": 4,
                "name": "UML"
            },
            {
                "value": 3,
                "name": "Chart"
            },
            {
                "value": 3,
                "name": "View"
            },
            {
                "value": 3,
                "name": "Geom"
            },
            {
                "value": 3,
                "name": "Shape"
            },
            {
                "value": 3,
                "name": "Scale"
            },
            {
                "value": 3,
                "name": "Animate"
            },
            {
                "value": 3,
                "name": "Global"
            },
            {
                "value": 3,
                "name": "Slider"
            },
            {
                "value": 3,
                "name": "Connector"
            },
            {
                "value": 3,
                "name": "Transform"
            },
            {
                "value": 3,
                "name": "Util"
            },
            {
                "value": 3,
                "name": "DomUtil"
            },
            {
                "value": 3,
                "name": "MatrixUtil"
            },
            {
                "value": 3,
                "name": "PathUtil"
            },
            {
                "value": 3,
                "name": "G"
            },
            {
                "value": 3,
                "name": "2D"
            },
            {
                "value": 3,
                "name": "3D"
            },
            {
                "value": 3,
                "name": "Line"
            },
            {
                "value": 3,
                "name": "Area"
            },
            {
                "value": 3,
                "name": "Interval"
            },
            {
                "value": 3,
                "name": "Schema"
            },
            {
                "value": 3,
                "name": "Edge"
            },
            {
                "value": 3,
                "name": "Polygon"
            },
            {
                "value": 3,
                "name": "Heatmap"
            },
            {
                "value": 3,
                "name": "Render"
            },
            {
                "value": 3,
                "name": "Tooltip"
            },
            {
                "value": 3,
                "name": "Axis"
            },
            {
                "value": 3,
                "name": "Guide"
            },
            {
                "value": 3,
                "name": "Coord"
            },
            {
                "value": 3,
                "name": "Legend"
            },
            {
                "value": 3,
                "name": "Path"
            },
            {
                "value": 3,
                "name": "Helix"
            },
            {
                "value": 3,
                "name": "Theta"
            },
            {
                "value": 3,
                "name": "Rect"
            },
            {
                "value": 3,
                "name": "Polar"
            },
            {
                "value": 3,
                "name": "Dsv"
            },
            {
                "value": 3,
                "name": "Csv"
            },
            {
                "value": 3,
                "name": "Tsv"
            },
            {
                "value": 3,
                "name": "GeoJSON"
            },
            {
                "value": 3,
                "name": "TopoJSON"
            },
            {
                "value": 3,
                "name": "Filter"
            },
            {
                "value": 3,
                "name": "Map"
            },
            {
                "value": 3,
                "name": "Pick"
            },
            {
                "value": 3,
                "name": "Rename"
            },
            {
                "value": 3,
                "name": "Filter"
            },
            {
                "value": 3,
                "name": "Map"
            },
            {
                "value": 3,
                "name": "Pick"
            },
            {
                "value": 3,
                "name": "Rename"
            },
            {
                "value": 3,
                "name": "Reverse"
            },
            {
                "value": 3,
                "name": "sort"
            },
            {
                "value": 3,
                "name": "Subset"
            },

            {
                "value": 2,
                "name": "祯逸"
            },
            {
                "value": 2,
                "name": "绝云"
            },
            {
                "value": 2,
                "name": "罗宪"
            },
            {
                "value": 2,
                "name": "萧庆"
            },
            {
                "value": 2,
                "name": "哦豁"
            },
            {
                "value": 2,
                "name": "逍为"
            },
            {
                "value": 2,
                "name": "翎刀"
            },
            {
                "value": 2,
                "name": "陆沉"
            },
            {
                "value": 2,
                "name": "顾倾"
            },
            {
                "value": 2,
                "name": "Domo"
            },
            {
                "value": 2,
                "name": "GPL"
            },
            {
                "value": 2,
                "name": "PAI"
            },
            {
                "value": 2,
                "name": "SPSS"
            },
            {
                "value": 2,
                "name": "SYSTAT"
            },
            {
                "value": 2,
                "name": "Tableau"
            },
            {
                "value": 2,
                "name": "D3"
            },
            {
                "value": 2,
                "name": "Vega"
            },
            {
                "value": 2,
                "name": "统计图表"
            }
        ]
        let finalData = chartData || widgetData || data;
        let advConf = {};
        if (advancedConfig && advancedConfig.length > 40) {
            advConf = safeEval("(" + advancedConfig + ")(widgetData)", { widgetData: finalData });
        }
        return (
            <WordCloudChart appendPadding={appendPadding || [10, 0, 0, 10]}
                theme={theme || 'light'}
                height={height && Number(height)}
                width={width && Number(width)}
                selected={1}
                autoFit={true}
                data={getDataList(finalData)}
                title={{
                    visible: !!chartTitle,
                    text: chartTitle || '',
                    style: {
                        fontSize: 14,
                        color: 'var(--PrimaryColor)',
                    }
                }}
                wordStyle={{
                    rotation: [-Math.PI / 2, Math.PI / 2],
                    rotationSteps: 4,
                    fontSize: [10, 60],
                    active: {
                        shadowColor: '#999999',
                        shadowBlur: 10,
                    },
                    padding: 2,
                }}
                random={randomNum || 0.5}
                backgroundColor={backgroundColor || '#fff'}
                tooltip={{
                    visible: true,
                }}
                {...advConf}
            />
        );
    }
}

export default BizWordCloud;