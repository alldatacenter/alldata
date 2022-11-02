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

using System.Collections.ObjectModel;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Controls.DataVisualization.Charting;
using System.Windows.Media;
using System.Windows.Shapes;
using Microsoft.EnterpriseManagement.Presentation.Controls.Charting;

namespace Ambari.SCOM.Widgets.Charting.DataSeriesConverters {
    using IDataObject = Microsoft.EnterpriseManagement.Presentation.DataAccess.IDataObject;
    
    public class SeriesDefinitionDataToSeriesConverter : ExtendedDataToSeriesConverterBase {
        protected override ISeries ConvertInternal(IDataObject data, ResourceDictionary availableResources, Color resolvedColor, ObservableCollection<IDataObject> pointCollection, Control tooltip) {
            var series = new SeriesDefinition {
                Title = GetSeriesId(data),
                Opacity = GetSeriesOpacity(data),
                ItemsSource = pointCollection,
                DependentValueBinding = CreateBinding(DataToSeriesConverterBase.YParameter),
                IndependentValueBinding = CreateBinding(DataToSeriesConverterBase.XParameter),
                Tag = data
            };
#if !SILVERLIGHT
            series.ToolTip = tooltip;
#endif
            SetSeriesStyle(series, resolvedColor);
            return series;
        }

        private void SetSeriesStyle(SeriesDefinition seriesDefinition, Color color) {
            var polygonStyle = new Style(typeof(Polygon));
            polygonStyle.Setters.Add(new Setter(Polygon.FillProperty, CreateGradientBrush(color)));
            seriesDefinition.DataShapeStyle = polygonStyle;
        }
    }
}
