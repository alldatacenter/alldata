// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

using System;
using System.Collections.ObjectModel;
using System.Linq;
using Microsoft.EnterpriseManagement.Presentation.Controls;
using Microsoft.EnterpriseManagement.Presentation.DataAccess;
using System.Collections.Generic;
using System.Collections;

namespace Ambari.SCOM.Widgets.Charting {
    // Since StackerAreaSeries does not support dynamic inserts into specific position,
    //   this guy will wait until required number of series will be in the list
    //   and then pass it further to rendering.
    class StackedSeriesManager {
        private ObservableDataModel model;

        private Dictionary<string, IDataObject> stackedSeries = new Dictionary<string,IDataObject>();

        public StackedSeriesManager() {
            model = new ObservableDataModel();
        }

        public IDataObject Add(IDataObject series) {
            var stackedSeriesType = series[ChartingConstants.StackedSeriesTypePropertyName] as string;

            // Stacker Type should be specified. At least default one.
            if (string.IsNullOrEmpty(stackedSeriesType))
                // Pass throught, since series cannot be stacked.
                return series;

            if (!stackedSeries.ContainsKey(stackedSeriesType))
                stackedSeries.Add(stackedSeriesType, CreateStackedSeries(stackedSeriesType));

            var stack = ((ICollection<IDataObject>)stackedSeries[stackedSeriesType][SeriesDataConstants.Data]);
            stack.Add(series);
            return stack.Count >= (int)(series[ChartingConstants.StackedSeriesSizePropertyName] ?? 0)
                    ? stackedSeries[stackedSeriesType]
                    : null;
        }

        public IDataObject Remove(IDataObject series) {
            var stackedSeriesType = series[ChartingConstants.StackedSeriesTypePropertyName] as string;
            if (string.IsNullOrEmpty(stackedSeriesType) ||
                !stackedSeries.ContainsKey(stackedSeriesType)) return null;

            var seriesCollection = (ICollection<IDataObject>)stackedSeries[stackedSeriesType][SeriesDataConstants.Data];

            seriesCollection.Remove(series);
            return seriesCollection.Count >= (int)(series[ChartingConstants.StackedSeriesSizePropertyName] ?? 0)
                    // Stacked series is of required size, do not remove it from the chart
                    ? null
                    // Not all series are in stack, return stacked series to remove it
                    : stackedSeries[stackedSeriesType];
        }

        public void Clear() {
            foreach (var stackedSeriesType in stackedSeries.Keys) {
                var seriesCollection = (ICollection<IDataObject>)stackedSeries[stackedSeriesType][SeriesDataConstants.Data];
                seriesCollection.Clear();
            }
        }

        private IDataObject CreateStackedSeries(string stackedSeriesType) {
            // Create data type for specified stacked series if not exists
            if (model.Types.FirstOrDefault(t => String.Compare(t.Name, stackedSeriesType, StringComparison.InvariantCultureIgnoreCase) == 0) == null)
                AddSeriesTypeToDataModel(stackedSeriesType);

            // Create new specific StackedSeries instance
            var stackedSeries = model.CreateInstance(model.Types[stackedSeriesType]);
            stackedSeries[SeriesDataConstants.ID] = stackedSeriesType;
            stackedSeries[SeriesDataConstants.Data] = new ObservableCollection<IDataObject>();
            stackedSeries[SeriesDataConstants.Visibility] = true;

            return stackedSeries;
        }

        private void AddSeriesTypeToDataModel(string typeName) {
            var type = model.Types.Create(typeName);
            type.Properties.Create(SeriesDataConstants.ID, typeof(string));
            type.Properties.Create(SeriesDataConstants.Data, typeof(IDataObject));
            type.Properties.Create(SeriesDataConstants.Color, typeof(IDataObject));
            type.Properties.Create(SeriesDataConstants.Visibility, typeof(bool));
            type.Properties.Create(SeriesDataConstants.Tooltip, typeof(string));
        }
    }
}
