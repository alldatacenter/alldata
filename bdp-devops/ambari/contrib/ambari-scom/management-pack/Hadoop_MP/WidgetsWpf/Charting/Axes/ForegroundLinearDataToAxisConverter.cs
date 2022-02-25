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
using System.Windows.Controls.DataVisualization.Charting;
using Microsoft.EnterpriseManagement.Presentation.Controls;
using Microsoft.EnterpriseManagement.Presentation.Controls.Charting;
using Microsoft.EnterpriseManagement.Presentation.DataAccess;

namespace Ambari.SCOM.Widgets.Charting.Axes {
    [Export, PartCreationPolicy(CreationPolicy.NonShared)]
    public class ForegroundLinearDataToAxisConverter : IDataToAxisConverter, INotifyPropertyChanged {
        public event PropertyChangedEventHandler PropertyChanged;

        public IAxis Convert(IDataObject data) {
            var axis = new ForegroundLinearAxis();
            if (data == null) return axis;

            var orientation = AxisOrientation.None;
            if (data[AxisDataConstants.Orientation] != null)
                Enum.TryParse<AxisOrientation>(data[AxisDataConstants.Orientation] as string, out orientation);

            int minimum = 0;
            if (data[AxisDataConstants.Minimum] != null)
                int.TryParse(data[AxisDataConstants.Minimum].ToString(), NumberStyles.Integer, CultureInfo.CurrentCulture, out minimum);

            int maximum = 0;
            if (data[AxisDataConstants.Maximum] != null)
                int.TryParse(data[AxisDataConstants.Maximum].ToString(), NumberStyles.Integer, CultureInfo.CurrentCulture, out maximum);

            int interval = int.MinValue;
            if (data[AxisDataConstants.Interval] != null)
                int.TryParse(data[AxisDataConstants.Interval].ToString(), NumberStyles.Integer, CultureInfo.CurrentCulture, out interval);

            bool autoCalculate = true;
            if (data[AxisDataConstants.AutoCalculate] != null)
                bool.TryParse(data[AxisDataConstants.AutoCalculate].ToString(), out autoCalculate);

            axis.Title = data[AxisDataConstants.Title];
            axis.Orientation = orientation;
            // What's the difference if it's foreground or not if you won't show them? :)
            axis.ShowGridLines = true;
            if (autoCalculate) {
                axis.Interval = null;
                // Only maximum and interval values are auto calculated
                axis.Minimum = minimum;
                axis.Maximum = null;
            } else {
                if (interval <= 0) {
                    axis.Interval = null;
                } else {
                    axis.Interval = interval;
                }

                axis.Minimum = minimum;
                if (minimum == maximum) {
                    axis.Maximum = interval > 0 ? minimum + interval : minimum + 1;
                } else {
                    axis.Maximum = maximum;
                }
            }

            return axis;
        }
    }
}
