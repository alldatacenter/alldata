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
import { LinearGradient } from 'echarts/lib/util/graphic';
import { colorToRgb } from 'common/utils';

export const areaColors = [
  'rgba(3, 91, 227, 0.2)',
  'rgba(45, 192, 131, 0.2)',
  'rgba(91, 69, 194, 0.2)',
  'rgba(254, 171, 0, 0.2)',
  'rgba(216, 66, 218, 0.2)',
  'rgba(80, 163, 227, 0.2)',
  'rgba(223, 52, 9, 0.2)',
  'rgba(246, 213, 26, 0.2)',
  'rgba(16, 41, 151, 0.2)',
  'rgba(46, 139, 54, 0.2)',
  'rgba(162, 56, 183, 0.2)',
  'rgba(156, 195, 255, 0.2)',
];

export const colorMap = {
  blue: '#4169E1',
  green: '#6CB38B',
  purple: '#6a549e',
  orange: '#F7A76B',
  darkcyan: '#498E9E',
  steelblue: '#4E6097',
  red: '#DE5757',
  yellow: '#F7C36B',
  lightgreen: '#8DB36C',
  maroon: '#BF0000',
  darkgoldenrod: '#B8860B',
  teal: '#008080',
  brown: '#A98C72',
  darksalmon: '#DE6F57',
  darkslategray: '#2F4F4F',
  darkseagreen: '#8FBC8F',
  darkslateblue: '#483D8B',
  gray: '#666666',
  lightgray: '#00000099',
};

export const bgColor = 'rgba(0,0,0,0.1)';
export const newColorMap = {
  primary8: '#1E2059', // darkest blue
  primary7: '#2D3280',
  primary6: '#424CA6',
  primary5: '#5C6BCC',
  primary4: '#798CF1',
  primary3: '#A8BAFF',
  primary2: '#D1DCFF',
  primary1: '#F0F4FF',
  warning8: '#400C1C', // darkest red
  warning7: '#66142C',
  warning6: '#8C233D',
  warning5: '#B33651',
  warning4: '#D84B65',
  warning3: '#F2A2AC',
  warning2: '#FFD4D7',
  warning1: '#FFF0F0',
};

export const genLinearGradient = (color) => {
  return new LinearGradient(0, 0, 0, 1, [
    { offset: 0, color: colorToRgb(color, 0.2) },
    { offset: 1, color: colorToRgb(color, 0.01) },
  ]);
};

export const theme = {
  color: [
    colorMap.blue,
    colorMap.green,
    colorMap.purple,
    colorMap.orange,
    colorMap.darkcyan,
    colorMap.steelblue,
    colorMap.red,
    colorMap.yellow,
    colorMap.lightgreen,
    colorMap.maroon,
    colorMap.darkgoldenrod,
    colorMap.teal,
    colorMap.brown,
    colorMap.darksalmon,
    colorMap.darkslategray,
    colorMap.darkseagreen,
    colorMap.darkslateblue,
  ],
  backgroundColor: '#ffffff',
  textStyle: {},
  title: {
    textStyle: {
      color: '#7c7c9e',
    },
    subtextStyle: {
      color: '#7c7c9e',
    },
  },
  line: {
    itemStyle: {
      normal: {
        borderWidth: '2',
      },
    },
    lineStyle: {
      normal: {
        width: '2',
      },
    },
    symbolSize: 1,
    symbol: 'emptyCircle',
    smooth: true,
  },
  radar: {
    itemStyle: {
      normal: {
        borderWidth: '2',
      },
    },
    lineStyle: {
      normal: {
        width: '2',
      },
    },
    symbolSize: 1,
    symbol: 'emptyCircle',
    smooth: true,
  },
  bar: {
    itemStyle: {
      normal: {
        barBorderWidth: '0',
        barBorderColor: '#cccccc',
      },
      emphasis: {
        barBorderWidth: '0',
        barBorderColor: '#cccccc',
      },
    },
  },
  pie: {
    itemStyle: {
      normal: {
        borderWidth: '0',
        borderColor: '#cccccc',
      },
      emphasis: {
        borderWidth: '0',
        borderColor: '#cccccc',
      },
    },
  },
  scatter: {
    itemStyle: {
      normal: {
        borderWidth: '0',
        borderColor: '#cccccc',
      },
      emphasis: {
        borderWidth: '0',
        borderColor: '#cccccc',
      },
    },
  },
  boxplot: {
    itemStyle: {
      normal: {
        borderWidth: '0',
        borderColor: '#cccccc',
      },
      emphasis: {
        borderWidth: '0',
        borderColor: '#cccccc',
      },
    },
  },
  parallel: {
    itemStyle: {
      normal: {
        borderWidth: '0',
        borderColor: '#cccccc',
      },
      emphasis: {
        borderWidth: '0',
        borderColor: '#cccccc',
      },
    },
  },
  sankey: {
    itemStyle: {
      normal: {
        borderWidth: '0',
        borderColor: '#cccccc',
      },
      emphasis: {
        borderWidth: '0',
        borderColor: '#cccccc',
      },
    },
  },
  funnel: {
    itemStyle: {
      normal: {
        borderWidth: '0',
        borderColor: '#cccccc',
      },
      emphasis: {
        borderWidth: '0',
        borderColor: '#cccccc',
      },
    },
  },
  gauge: {
    itemStyle: {
      normal: {
        borderWidth: '0',
        borderColor: '#cccccc',
      },
      emphasis: {
        borderWidth: '0',
        borderColor: '#cccccc',
      },
    },
  },
  candlestick: {
    itemStyle: {
      normal: {
        color: colorMap.orange,
        color0: 'transparent',
        borderColor: colorMap.red,
        borderColor0: colorMap.green,
        borderWidth: '2',
      },
    },
  },
  graph: {
    itemStyle: {
      normal: {
        borderWidth: '0',
        borderColor: '#cccccc',
      },
    },
    lineStyle: {
      normal: {
        width: 1,
        color: '#aaa',
      },
    },
    symbolSize: 1,
    symbol: 'emptyCircle',
    smooth: true,
    color: [
      colorMap.blue,
      colorMap.green,
      colorMap.purple,
      colorMap.orange,
      colorMap.darkcyan,
      colorMap.steelblue,
      colorMap.red,
      colorMap.yellow,
      colorMap.lightgreen,
      colorMap.maroon,
      colorMap.darkgoldenrod,
      colorMap.teal,
      colorMap.brown,
      colorMap.darksalmon,
      colorMap.darkslategray,
      colorMap.darkseagreen,
      colorMap.darkslateblue,
    ],
    label: {
      normal: {
        textStyle: {
          color: '#ffffff',
        },
      },
    },
  },
  map: {
    itemStyle: {
      normal: {
        areaColor: '#f3f3f3',
        borderColor: '#999999',
        borderWidth: 0.5,
      },
      emphasis: {
        areaColor: 'rgba(255,178,72,1)',
        borderColor: '#eb8146',
        borderWidth: 1,
      },
    },
    label: {
      normal: {
        textStyle: {
          color: '#893448',
        },
      },
      emphasis: {
        textStyle: {
          color: 'rgb(137,52,72)',
        },
      },
    },
  },
  geo: {
    itemStyle: {
      normal: {
        areaColor: '#f3f3f3',
        borderColor: '#999999',
        borderWidth: 0.5,
      },
      emphasis: {
        areaColor: 'rgba(255,178,72,1)',
        borderColor: '#eb8146',
        borderWidth: 1,
      },
    },
    label: {
      normal: {
        textStyle: {
          color: '#893448',
        },
      },
      emphasis: {
        textStyle: {
          color: 'rgb(137,52,72)',
        },
      },
    },
  },
  categoryAxis: {
    axisLine: {
      show: false,
      lineStyle: {
        color: '#aaaaaa',
      },
    },
    axisTick: {
      show: false,
      lineStyle: {
        color: '#333',
      },
    },
    axisLabel: {
      show: true,
      textStyle: {
        color: '#343659',
      },
    },
    splitLine: {
      show: true,
      lineStyle: {
        color: ['#f1f1f1'],
      },
    },
    splitArea: {
      show: false,
      areaStyle: {
        color: ['rgba(250,250,250,0.05)', 'rgba(200,200,200,0.02)'],
      },
    },
  },
  valueAxis: {
    axisLine: {
      show: false,
      lineStyle: {
        color: '#aaaaaa',
      },
    },
    axisTick: {
      show: false,
      lineStyle: {
        color: '#333',
      },
    },
    axisLabel: {
      show: true,
      textStyle: {
        color: '#343659',
      },
    },
    splitLine: {
      show: true,
      lineStyle: {
        color: ['#f1f1f1'],
      },
    },
    splitArea: {
      show: false,
      areaStyle: {
        color: ['rgba(250,250,250,0.05)', 'rgba(200,200,200,0.02)'],
      },
    },
  },
  logAxis: {
    axisLine: {
      show: false,
      lineStyle: {
        color: '#aaaaaa',
      },
    },
    axisTick: {
      show: false,
      lineStyle: {
        color: '#333',
      },
    },
    axisLabel: {
      show: true,
      textStyle: {
        color: '#343659',
      },
    },
    splitLine: {
      show: true,
      lineStyle: {
        color: ['#f1f1f1'],
      },
    },
    splitArea: {
      show: false,
      areaStyle: {
        color: ['rgba(250,250,250,0.05)', 'rgba(200,200,200,0.02)'],
      },
    },
  },
  timeAxis: {
    axisLine: {
      show: false,
      lineStyle: {
        color: '#aaaaaa',
      },
    },
    axisTick: {
      show: false,
      lineStyle: {
        color: '#333',
      },
    },
    axisLabel: {
      show: true,
      textStyle: {
        color: '#343659',
      },
    },
    splitLine: {
      show: true,
      lineStyle: {
        color: ['#f1f1f1'],
      },
    },
    splitArea: {
      show: false,
      areaStyle: {
        color: ['rgba(250,250,250,0.05)', 'rgba(200,200,200,0.02)'],
      },
    },
  },
  toolbox: {
    iconStyle: {
      normal: {
        borderColor: '#999999',
      },
      emphasis: {
        borderColor: '#666666',
      },
    },
  },
  legend: {
    textStyle: {
      color: '#343659',
    },
  },
  tooltip: {
    trigger: 'axis',
    backgroundColor: 'rgba(48,38,71,0.96)',
    borderWidth: 0,
    padding: [8, 16],
    textStyle: {
      color: '#fff',
    },
    axisPointer: {
      type: 'line',
      label: {
        show: false,
      },
      lineStyle: {
        type: 'dashed',
        color: 'rgba(48,38,71,0.40)',
      },
    },
    borderColor: 'rgba(0, 0, 0, .1)',
  },
  timeline: {
    lineStyle: {
      color: '#893448',
      width: 1,
    },
    itemStyle: {
      normal: {
        color: '#893448',
        borderWidth: 1,
      },
      emphasis: {
        color: '#ffb248',
      },
    },
    controlStyle: {
      normal: {
        color: '#893448',
        borderColor: '#893448',
        borderWidth: 0.5,
      },
      emphasis: {
        color: '#893448',
        borderColor: '#893448',
        borderWidth: 0.5,
      },
    },
    checkpointStyle: {
      color: '#eb8146',
      borderColor: 'rgba(255,178,72,0.41)',
    },
    label: {
      normal: {
        textStyle: {
          color: '#893448',
        },
      },
      emphasis: {
        textStyle: {
          color: '#893448',
        },
      },
    },
  },
  visualMap: {
    color: ['#893448', '#d95850', '#eb8146', '#ffb248', '#f2d643', 'rgb(247,238,173)'],
  },
  dataZoom: {
    // dataBackgroundColor: 'rgba(255,178,72,0.5)',
    backgroundColor: 'rgba(45,50,128,0.06)', // primary7
    fillerColor: 'rgba(209,220,255,0.8)', // primary2
    borderColor: 'transparent',
    handleColor: 'white',
    handleIcon:
      'image://data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAACgAAAAwCAYAAABjezibAAAAAXNSR0IArs4c6QAAAERlWElmTU0AKgAAAAgAAYdpAAQAAAABAAAAGgAAAAAAA6ABAAMAAAABAAEAAKACAAQAAAABAAAAKKADAAQAAAABAAAAMAAAAAAqdrrvAAAFFklEQVRYCe2Zz2vjRhTHZ0ayJXstvCFku02zUHLIJSwsS/8A00MLhV6WQi89tGXpoaf+ByF/Q6Gn/rj02H+gPeUPKEuh5JJDKGya/WHS3WBvLVuamc535CfLsmQrsZuTBfKMRu9930dvRrJ4YmyF24O9D77CvkJJJlYp9n9oVQLUWvMqOwFWsYUN2c9r3XknF4kcHh5OBSH7/PjBwYEuikP2nPPC8/ApBSTnrHA+8PHx8RQg2ebHjR+dsm0eGLHKIAsD5OGyYBS82+0a3w7r9c5Tjejyr8cgqLXf/Z6IgmDbZOeIbW1t2Szt7++n2cqDFkGm4iSINgtIcADLQu2Gr6xvf9i37WBnwF+f17+E/+3t0Y+Ns4YFaXkt2576GzoLS6BZyEqA8+B6vT0OMEANdt7iw+EbPhqFFjCKhtwN1RcAjH3xU63mWbB63deed0s3zl5owCagJxoZrQJZugYRCBtlDnB3Lp+KH5788mGr1fjOXO07iUXh77f5UXPhf/f7g68/ffjJry/ZnmLsBNqMIPP2dFz6mKGpxbQSHLJWAY60p1pcEHyhgQuFZrJkGKNYUw7jg1JAnE9uiA7DtEK417sQCzJXFCMdgy80oJWs4c44Rmoy05kLCGvcpVhzEA5Cf6H9TITcADSglWhOngA5s/SwNCCtPcreprkJBsN+qX2quKADDas1ziKmOZmpYsfCgJM10bFe9m6VbR63I3vHFktVG4XGyGhBM/Ho2GYSc1qnEJBMaHrxKIniIQ9ka2lAaEALmlWmeeFjhmBjGXEuxdKA0ImlMjrVQi+0wj9E9E+du8xhUsWlgM+fddmT3/+01/Pwvfvs7ttbdG1TbaIhGB7sg50RZ72p0zMHc6eYrO+is8mYlOWAgBv8G9qdQMk/21oNo2U1sydK+pUA4RvH828QwNGW7dNYtl2klbWtDJh1usn+GnDZbK8zuM7gshlY1n+9BtcZXDYDy/qvbA02mn7Kku2ng9fsrAwQr1gAw47+qraF74NVA+H976OP369qXtluZRmsHPGKhpUBXbeWFn2uGGPG/CpalQCfI8QFY47jLg1pNYyW1ZxBnx1YCIgqFRWCHLECwLEGNKkCNos1Gal8k7hOTbuOWDqD0NGO0jGrJjU3g6jnoWSGElrN9XTP6Wul1Pnk+q7Wgy80oAVNaCc1w3KdQsBJUfHIeqK+V3cutXtZ02dnz765DiR84AsNaEEzwUpiTGImo/RbOsWo26F+d9rb0HfOnuqXbU8Hnq8ePfr8t3ro3B9FdeH5QxFJVygluQHgWonPIMyF+lkIoYVwdM2J1TD0VL02UiNfysDbUBe10Gi+0Kfte3p3i6WFTILKtoUZzBrQNAfBpur5ofL7TRuo4SkZj6RUkRM3fRE3PB5zbkjNjj7GcA42sAUcfKEBrSrTC465gEn184ihbIsSLoTDlpZBuCGjgZLyFo+DphPrWMRa8lgpLFGt0McYzsEGtvCxvkYDWtBEcf3aFVZaE6glB8GJmeJ7Vpgy2W6a6RqDAkANDaQjJHb0MUZgsKXMAQ5a0KTKP8XKzhz1S9cgGdBaRE0ZteXd7iveGMZ6sLM9LqLji5Fray2miG5qz4wNmkziOYdHiaqbH2/brrlW+MauO4JblD1oFRaD5lX66dtIUsJlDCU0C3VTnyEQLAuI42xxkaqh2W8msMF2Yx9yECwPibEsKI4JFn1sJ3+8fox278Ht9EsTjvNTWbTmTIG98K/lP77xU/T/81GnAAAAAElFTkSuQmCC',
    handleStyle: {
      shadowColor: 'rgba(23,24,26,0.20)',
      shadowBlur: '3',
      shadowOffsetY: '2',
    },
    textStyle: {
      color: '#333333',
    },
  },
  markPoint: {
    label: {
      normal: {
        textStyle: {
          color: '#ffffff',
        },
      },
      emphasis: {
        textStyle: {
          color: '#ffffff',
        },
      },
    },
  },
};
