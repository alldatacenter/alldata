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
using System.ComponentModel;
using System.ComponentModel.Composition;
using System.Globalization;
using System.Windows;
using System.Windows.Controls.DataVisualization.Charting;
using System.Windows.Media;
using System.Windows.Shapes;
using Microsoft.EnterpriseManagement.Presentation.Controls;
using Microsoft.EnterpriseManagement.Presentation.Controls.Charting;
using Microsoft.EnterpriseManagement.Presentation.Controls.CommonControls;
using Microsoft.EnterpriseManagement.Presentation.DataAccess;

namespace Ambari.SCOM.Widgets.Charting.Axes {
    using IDataObject = Microsoft.EnterpriseManagement.Presentation.DataAccess.IDataObject;

    [Export, PartCreationPolicy(CreationPolicy.NonShared)]
    public class ForegroundDateTimeDataToAxisConverter : IDataToAxisConverter, INotifyPropertyChanged {
        private static readonly Color DefaultGridLinesColor = Color.FromArgb(255, 128, 128, 128);

        private IDataModel model = CreateDataModel();
        private IColorConverter colorConverter = new HexToColorConverter();

        public event PropertyChangedEventHandler PropertyChanged;

        public IAxis Convert(IDataObject data) {
            var axis = new ForegroundDateTimeAxis();
            if (data == null) return axis;

            SetGridLinesStyle(axis, ResolveColor(data[ChartingConstants.GridLinesColorPropertyName] as string));

            var orientation = AxisOrientation.None;
            if (data[AxisDataConstants.Orientation] != null)
                Enum.TryParse<AxisOrientation>(data[AxisDataConstants.Orientation] as string, out orientation);

            var minimum = DateTime.MinValue;
            if (data[AxisDataConstants.Minimum] != null)
                DateTime.TryParse(data[AxisDataConstants.Minimum].ToString(), CultureInfo.CurrentCulture, DateTimeStyles.AssumeLocal, out minimum);

            var maximum = DateTime.MaxValue;
            if (data[AxisDataConstants.Maximum] != null)
                DateTime.TryParse(data[AxisDataConstants.Maximum].ToString(), CultureInfo.CurrentCulture, DateTimeStyles.AssumeLocal, out maximum);

            int interval = 1;
            if (data[AxisDataConstants.Interval] != null)
                int.TryParse(data[AxisDataConstants.Interval].ToString(), NumberStyles.Integer, CultureInfo.CurrentCulture, out interval);

            bool autoCalculate = false;
            if (data[AxisDataConstants.AutoCalculate] != null)
                bool.TryParse(data[AxisDataConstants.AutoCalculate].ToString(), out autoCalculate);

            var intervalType = DateTimeIntervalType.Auto;
            if (data[AxisDataConstants.DateTimeIntervalType] != null)
                Enum.TryParse<DateTimeIntervalType>(data[AxisDataConstants.DateTimeIntervalType] as string, out intervalType);

            axis.Title = data[AxisDataConstants.Title];
            axis.Orientation = orientation;
            // What's the difference if it's foreground or not if you won't show them? :)
            axis.ShowGridLines = true;
            axis.IntervalType = intervalType;
            if (autoCalculate) {
                axis.Interval = null;
                axis.Minimum = null;
                axis.Maximum = null;
            } else {
                if (minimum != DateTime.MinValue)
                    axis.Minimum = minimum;
                if (maximum != DateTime.MaxValue)
                    axis.Maximum = minimum == maximum ? minimum.AddDays(1) : maximum;
                axis.Interval = interval;
            }

            return axis;
        }

        private void SetGridLinesStyle(ForegroundDateTimeAxis axis, Color color) {
            var gridStyle = new Style(typeof(Line));
            gridStyle.Setters.Add(new Setter(Line.StrokeProperty, new SolidColorBrush(color)));
            gridStyle.Setters.Add(new Setter(Line.StrokeThicknessProperty, (double)1));
            gridStyle.Setters.Add(new Setter(Line.OpacityProperty, (double)0.5));
            axis.GridLineStyle = gridStyle;
        }

        protected Color ResolveColor(string hexColor) {
            var resultColor = DefaultGridLinesColor;
            if (string.IsNullOrEmpty(hexColor)) return resultColor;
            var colorObject = model.CreateInstance(model.Types[ChartingConstants.HexColorDataType]);
            colorObject[ChartingConstants.HexColorPropertyName] = hexColor;
            return colorConverter.Convert(colorObject);
        }

        private static IDataModel CreateDataModel() {
            var model = new SimpleDataModel();
            var colorType = model.Types.Create(ChartingConstants.HexColorDataType);
            colorType.Properties.Create(ChartingConstants.HexColorPropertyName, typeof(string));
            return model;
        }
    }
}
