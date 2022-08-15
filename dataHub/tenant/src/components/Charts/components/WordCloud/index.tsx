import React from 'react'
import WordCloudChart from 'bizcharts/lib/plots/WordCloudChart'
import { WordCloudProps, DataItem } from '@/components/Charts/data'

// 数据源
const defaultData = [
  { lab_y: 12, lab_x: 'G2Plot' },
  { lab_y: 9, lab_x: 'AntV' },
  { lab_y: 8, lab_x: 'F2' },
  { lab_y: 8, lab_x: 'G2' },
  { lab_y: 8, lab_x: 'G6' },
  { lab_y: 8, lab_x: 'DataSet' },
  { lab_y: 8, lab_x: '墨者学院' },
  { lab_y: 6, lab_x: 'Analysis' },
  { lab_y: 6, lab_x: 'Data Mining' },
  { lab_y: 6, lab_x: 'Data Vis' },
  { lab_y: 6, lab_x: 'Design' },
  { lab_y: 6, lab_x: 'Grammar' },
  { lab_y: 6, lab_x: 'Graphics' },
  { lab_y: 6, lab_x: 'Graph' },
  { lab_y: 6, lab_x: 'Hierarchy' },
  { lab_y: 6, lab_x: 'Labeling' },
  { lab_y: 6, lab_x: 'Layout' },
  { lab_y: 6, lab_x: 'Quantitative' },
  { lab_y: 6, lab_x: 'Relation' },
]

export default ({ data = defaultData }: WordCloudProps) => {
  const getDataList = (dataArg: DataItem[]) => {
    const list: any = []
    dataArg.forEach((d) => {
      list.push({
        id: list.length,
        word: d.lab_x,
        weight: d.lab_y,
      })
    })
    return list
  }

  const getRandomColor = () => {
    const arr = [
      '#5B8FF9',
      '#5AD8A6',
      '#5D7092',
      '#F6BD16',
      '#E8684A',
      '#6DC8EC',
      '#9270CA',
      '#FF9D4D',
      '#269A99',
      '#FF99C3',
    ]
    return arr[Math.floor(Math.random() * (arr.length - 1))]
  }

  return (
    <WordCloudChart
      data={getDataList(data)}
      // maskImage="https://gw.alipayobjects.com/mdn/rms_2274c3/afts/img/A*07tdTIOmvlYAAAAAAAAAAABkARQnAQ"
      forceFit
      wordStyle={{
        rotation: [-Math.PI / 2, Math.PI / 2],
        rotateRatio: 0.5,
        rotationSteps: 4,
        fontSize: [20, 100],
        color: () => {
          return getRandomColor()
        },
        active: {
          shadowColor: '#333333',
          shadowBlur: 10,
        },
        gridSize: 8,
      }}
      shape="square"
      shuffle={false}
      backgroundColor="#fff"
      tooltip={{
        visible: true,
        items: [{ name: 'lab_x', value: 'lab_y' }],
      }}
      selected={-1}
      // onWordCloudHover={hoverAction}
    />
  )
}
