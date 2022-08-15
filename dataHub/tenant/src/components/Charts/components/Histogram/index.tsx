import React from 'react'
import StackedColumnChart from 'bizcharts/lib/plots/StackedColumnChart'
import { ChartDProps } from '@/components/Charts/data'
import { mobileCheck } from '@/utils/utils'
import { SourcesColor } from '@/types/const'
import findLast from 'lodash/findLast'

const defaultData = [
  { lab_z: 'London', lab_x: 'Jan.', lab_y: 18.9 },
  { lab_z: 'London', lab_x: 'Feb.', lab_y: 28.8 },
  { lab_z: 'London', lab_x: 'Mar.', lab_y: 39.3 },
  { lab_z: 'London', lab_x: 'Apr.', lab_y: 81.4 },
  { lab_z: 'London', lab_x: 'May', lab_y: 47 },
  { lab_z: 'London', lab_x: 'Jun.', lab_y: 20.3 },
  { lab_z: 'London', lab_x: 'Jul.', lab_y: 24 },
  { lab_z: 'London', lab_x: 'Aug.', lab_y: 35.6 },
  { lab_z: 'Berlin', lab_x: 'Jan.', lab_y: 12.4 },
  { lab_z: 'Berlin', lab_x: 'Feb.', lab_y: 23.2 },
  { lab_z: 'Berlin', lab_x: 'Mar.', lab_y: 34.5 },
  { lab_z: 'Berlin', lab_x: 'Apr.', lab_y: 99.7 },
  { lab_z: 'Berlin', lab_x: 'May', lab_y: 52.6 },
  { lab_z: 'Berlin', lab_x: 'Jun.', lab_y: 35.5 },
  { lab_z: 'Berlin', lab_x: 'Jul.', lab_y: 37.4 },
  { lab_z: 'Berlin', lab_x: 'Aug.', lab_y: 42.4 },
]
export default ({ height = 600, data = defaultData }: ChartDProps) => {
  const resetData = data?.map((i) => {
    return { ...i, ...{ lab_y: Number(i.lab_y) } }
  })

  return (
    <StackedColumnChart
      height={height}
      data={resetData}
      forceFit
      xField="lab_x"
      yField="lab_y"
      stackField="lab_z"
      xAxis={{
        visible: true,
        label: {
          autoRotate: true,
        },
        title: {
          visible: false,
        },
      }}
      yAxis={{
        visible: true,
        label: {
          autoRotate: false,
        },
        title: {
          visible: false,
        },
      }}
      color={(sourceName: string) =>
        (SourcesColor.find((i) => i.source === sourceName) || findLast(SourcesColor))?.color
      }
      legend={{
        visible: true,
        position: mobileCheck ? 'bottom-center' : 'right-center',
      }}
    />
  )
}
