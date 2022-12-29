/**
 * Datart
 *
 * Copyright 2021
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

const ReactXYPlot = ({
  React,
  XYPlot,
  XAxis,
  YAxis,
  HorizontalGridLines,
  LineSeries,
  ...rest
}) => {
  class ReactXYPlot extends React.Component {
    static displayName = 'PieDemo';

    render() {
      return (
        <XYPlot width={300} height={300}>
          <HorizontalGridLines />
          <LineSeries
            data={[
              { x: 1, y: 10 },
              { x: 2, y: 5 },
              { x: 3, y: 15 },
            ]}
          />
          <XAxis />
          <YAxis />
        </XYPlot>
      );
    }
  }
  return ReactXYPlot;
};
export default ReactXYPlot;
