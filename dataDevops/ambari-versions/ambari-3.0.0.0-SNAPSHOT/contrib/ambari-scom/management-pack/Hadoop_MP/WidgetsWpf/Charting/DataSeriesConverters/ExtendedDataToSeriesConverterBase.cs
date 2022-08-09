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
using System.Windows;
using System.Windows.Controls;
using System.Windows.Controls.DataVisualization.Charting;
using System.Windows.Media;
using Microsoft.EnterpriseManagement.Presentation.Controls;
using Microsoft.EnterpriseManagement.Presentation.Controls.Charting;

namespace Ambari.SCOM.Widgets.Charting.DataSeriesConverters {
    using IDataObject = Microsoft.EnterpriseManagement.Presentation.DataAccess.IDataObject;

    public abstract class ExtendedDataToSeriesConverterBase : DataToSeriesConverterBase {
        private const double VisualOpacity = 1.0;
        private const double HiddenOpacity = 0.0;

        protected abstract override ISeries ConvertInternal(IDataObject data, ResourceDictionary availableResources, Color resolvedColor, ObservableCollection<IDataObject> pointCollection, Control tooltip);

        protected double GetSeriesOpacity(IDataObject data) {
            return data[SeriesDataConstants.Visibility] != null && (bool)data[SeriesDataConstants.Visibility]
                        ? VisualOpacity
                        : HiddenOpacity;
        }

        protected string GetSeriesId(IDataObject data) {
            var id = data[SeriesDataConstants.ID] as string;
            if (string.IsNullOrEmpty(id)) id = Guid.NewGuid().ToString();
            return id;
        }

        protected Brush CreateGradientBrush(Color baseColor) {
            const byte brightness = 60;
            var gradientStops = new GradientStopCollection();
            gradientStops.Add(new GradientStop() { Color = LightenColor(baseColor, brightness), Offset = 0.0 });
            gradientStops.Add(new GradientStop() { Color = baseColor, Offset = 1.0 });
            return new LinearGradientBrush(gradientStops, 90);
        }

        private Color LightenColor(Color baseColor, byte lighting) {
            return Color.FromArgb(baseColor.A,
                        MinByte(byte.MaxValue, (int)baseColor.R + lighting),
                        MinByte(byte.MaxValue, (int)baseColor.G + lighting),
                        MinByte(byte.MaxValue, (int)baseColor.B + lighting)
                    );
        }

        private byte MinByte(int left, int right) {
            return (byte)(left < right ? left : right);
        }
    }
}
