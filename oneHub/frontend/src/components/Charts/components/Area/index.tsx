import React from 'react'
import { ChartDProps } from '@/components/Charts/data'
import AreaChart from 'bizcharts/lib/plots/AreaChart'

const defaultData = [
  { lab_z: 'Asia', lab_x: '1750', lab_y: 502 },
  { lab_z: 'Asia', lab_x: '1800', lab_y: 635 },
  { lab_z: 'Asia', lab_x: '1900', lab_y: 300 },
  { lab_z: 'Asia', lab_x: '2000', lab_y: 900 },
  { lab_z: 'Europe', lab_x: '1750', lab_y: 163 },
  { lab_z: 'Europe', lab_x: '1800', lab_y: 203 },
  { lab_z: 'Europe', lab_x: '1900', lab_y: 400 },
  { lab_z: 'Europe', lab_x: '2000', lab_y: 800 },
]
export default ({ height = 700, data = defaultData }: ChartDProps) => {
  const resetData = data?.map((i) => {
    return { ...i, ...{ lab_y: Number(i.lab_y) } }
  })

  return (
    <AreaChart
      height={height}
      data={resetData}
      forceFit
      smooth
      xField="lab_x"
      yField="lab_y"
      seriesField="lab_z"
      meta={{
        lab_x: {
          alias: '年份',
          range: [0, 1],
        },
        lab_y: {
          alias: '数量',
        },
      }}
      legend={{
        visible: true,
        position: 'bottom-center',
        offsetY: 10,
      }}
    />
  )
}
