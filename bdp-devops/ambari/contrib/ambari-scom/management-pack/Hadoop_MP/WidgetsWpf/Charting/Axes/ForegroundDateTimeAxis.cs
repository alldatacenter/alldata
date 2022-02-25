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

using System.Reflection;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Controls.DataVisualization;
using System.Windows.Controls.DataVisualization.Charting;
using System.Windows.Shapes;

namespace Ambari.SCOM.Widgets.Charting.Axes {
    [StyleTypedProperty(Property = "GridLineStyle", StyleTargetType = typeof(Line))]
    [StyleTypedProperty(Property = "MajorTickMarkStyle", StyleTargetType = typeof(Line))]
    [StyleTypedProperty(Property = "MinorTickMarkStyle", StyleTargetType = typeof(Line))]
    [StyleTypedProperty(Property = "AxisLabelStyle", StyleTargetType = typeof(DateTimeAxisLabel))]
    [StyleTypedProperty(Property = "TitleStyle", StyleTargetType = typeof(Title))]
    [TemplatePart(Name = AxisGridName, Type = typeof(Grid))]
    [TemplatePart(Name = AxisTitleName, Type = typeof(Title))]
    public class ForegroundDateTimeAxis : DateTimeAxis {
        private Canvas foregroundGridLines;
        private Canvas ForegroundGridLines {
            get { return foregroundGridLines; }
            set {
                if (foregroundGridLines == value) return;
                var oldValue = foregroundGridLines;
                foregroundGridLines = value;
                OnGridLinesPropertyChanged(oldValue, value);
            }
        }

        private PropertyInfo baseGridLines;
        private Canvas BaseGridLines {
            get { return baseGridLines.GetValue(this, null) as Canvas; }
            set { baseGridLines.SetValue(this, value, null); }
        }

        public ForegroundDateTimeAxis() {
            baseGridLines = typeof(DisplayAxis).GetProperty("GridLines", BindingFlags.NonPublic | BindingFlags.Instance);
        }

        protected override void OnShowGridLinesPropertyChanged(bool oldValue, bool newValue) {
            // This is the only place GridLines property gets assigned in base class.
            // Wait while GridLines are created
            base.OnShowGridLinesPropertyChanged(oldValue, newValue);
            // Asssign it to local member so it will get to foreground
            ForegroundGridLines = BaseGridLines;
            // Clear base GridLines property, so it will be removed from background
            BaseGridLines = null;
        }

        private void OnGridLinesPropertyChanged(Canvas oldValue, Canvas newValue) {
            if (SeriesHost != null && oldValue != null)
                SeriesHost.ForegroundElements.Remove(oldValue);
            if (SeriesHost != null && newValue != null)
                SeriesHost.ForegroundElements.Add(newValue);
        }

        protected override void OnSeriesHostPropertyChanged(ISeriesHost oldValue, ISeriesHost newValue) {
            if (oldValue != null && ForegroundGridLines != null)
                oldValue.ForegroundElements.Remove(ForegroundGridLines);
            if (newValue != null && ForegroundGridLines != null)
                newValue.ForegroundElements.Add(ForegroundGridLines);
        }
    }
}
