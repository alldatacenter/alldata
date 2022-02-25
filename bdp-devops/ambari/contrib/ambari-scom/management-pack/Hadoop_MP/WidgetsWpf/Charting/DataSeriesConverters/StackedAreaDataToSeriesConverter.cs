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

using System.Collections.Generic;
using System.Collections.ObjectModel;
using System.ComponentModel.Composition;
using System.Linq;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Controls.DataVisualization.Charting;
using System.Windows.Media;
using Microsoft.EnterpriseManagement.Presentation.Controls;
using Microsoft.EnterpriseManagement.Presentation.Controls.Charting;

namespace Ambari.SCOM.Widgets.Charting.DataSeriesConverters {
    using IDataObject = Microsoft.EnterpriseManagement.Presentation.DataAccess.IDataObject;
    
    [Export, PartCreationPolicy(CreationPolicy.NonShared)]
    public class StackedAreaDataToSeriesConverter : DataToSeriesConverterBase {
        private object singletonSyncObj = new object();

        private IDataToSeriesConverter seriesDefinitionConverter;
        protected IDataToSeriesConverter SeriesDefinitionConverter {
            get {
                if (seriesDefinitionConverter == null) lock (singletonSyncObj) if (seriesDefinitionConverter == null)
                    seriesDefinitionConverter = CreateNewSeriesDefinitionConverter();
                return seriesDefinitionConverter;
            }
        }

        private IDataToSeriesConverter CreateNewSeriesDefinitionConverter() {
            return new SeriesDefinitionDataToSeriesConverter {
                // Pass through all dependent properties
                CompositionEngine = this.CompositionEngine,
                Container = this.Container,
                TypeSystem = this.TypeSystem
            };
        }

        protected override ISeries ConvertInternal(IDataObject data, ResourceDictionary availableResources,
                Color resolvedColor, ObservableCollection<IDataObject> seriesCollection, Control tooltip) {
            // Chart control uses SeriesDataConstants.Visibility property from Tag
            var series = new StackedAreaSeries {
                Tag = data
            };

            var orderedSeriescollection = seriesCollection.OrderBy(s => s[ChartingConstants.SeriesOrderPropertyName] ?? 0);
            foreach (var stackableSeries in orderedSeriescollection) {
                var idoCollection = stackableSeries[SeriesDataConstants.Data] as ICollection<IDataObject>;
                var dataPoints = new IDataObject[idoCollection.Count];
                idoCollection.CopyTo(dataPoints, 0);
                var observableDataPoints = new BatchObservableCollection<IDataObject>(dataPoints);

                SeriesDefinitionConverter.Convert(stackableSeries, observableDataPoints, availableResources,
                    (s) => { series.SeriesDefinitions.Add((SeriesDefinition)s); });
            }

            return series;

        }
    }
}
