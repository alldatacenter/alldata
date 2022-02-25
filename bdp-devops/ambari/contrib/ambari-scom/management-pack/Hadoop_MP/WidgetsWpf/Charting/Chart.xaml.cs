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
using System.ComponentModel;
using System.ComponentModel.Composition;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Controls.DataVisualization.Charting;
using Microsoft.EnterpriseManagement.CompositionEngine;
using Microsoft.Practices.Unity;

namespace Ambari.SCOM.Widgets.Charting {
    using IDataObject = Microsoft.EnterpriseManagement.Presentation.DataAccess.IDataObject;

    /// <summary>
    /// Encapsulates SCOM built-in chart control to add custom styles as resources.
    /// </summary>
    [Export]
    [PartCreationPolicy(CreationPolicy.NonShared)]
    [ComponentBuildUpRequired]
    public partial class Chart : UserControl, INotifyPropertyChanged, INotifyInitialized {
        public Chart() {
            InitializeComponent();
#if !SILVERLIGHT
            // Fix Area overflow for WPF
            var stackedAreaStyle = new Style(typeof(StackedAreaSeries));
            stackedAreaStyle.Setters.Add(new Setter(StackedAreaSeries.ClipToBoundsProperty, true));
            Resources.Add(stackedAreaStyle.TargetType, stackedAreaStyle);
#endif
            ChartControl.PropertyChanged += OnPropertyChanged;
        }

        public event PropertyChangedEventHandler PropertyChanged;

        public bool AllowSelection { get; set; }

        #region ChartComponent Properties

        public ObservableCollection<IDataObject> InputData {
            get { return ChartControl.InputData; }
            set { ChartControl.InputData = value; }
        }

        public IEnumerable<IDataObject> Axes {
            get { return ChartControl.Axes; }
            set { ChartControl.Axes = value; }
        }

        public ICollection<ISeries> Series {
            get { return ChartControl.Series; }
        }

        public IDataObject Interval {
            get { return ChartControl.Interval; }
            set { ChartControl.Interval = value; }
        }

        public IDataObject SelectedSeries {
            get { return ChartControl.SelectedSeries; }
            set { ChartControl.SelectedSeries = value; }
        }

        #endregion ChartComponent Properties

        #region CompositionEngine Properties

        [Dependency]
        public IUnityContainer Container {
            get { return ChartControl.Container; }
            set { ChartControl.Container = value; }
        }

#pragma warning disable 3003    // Base type is not CLS-compliant
        [Dependency]
        public ICompositionEngine CompositionEngine {
            get { return ChartControl.CompositionEngine; }
            set { ChartControl.CompositionEngine = value; }
        }

#pragma warning disable 3003    // Base type is not CLS-compliant
        [Dependency]
        public ITypeSystem TypeSystem {
            get { return ChartControl.TypeSystem; }
            set { ChartControl.TypeSystem = value; }
        }

        #endregion CompositionEngine Properties

        private void OnPropertyChanged(object sender, PropertyChangedEventArgs e) {
            if (!AllowSelection && e.PropertyName == "SelectedSeries") {
                // Set maximum opacity for all series
                UnSelectSeries();
            }

            if (PropertyChanged != null)
                PropertyChanged(sender, e);
        }

        private void UnSelectSeries() {
            foreach (var series in Series) {
                ((UIElement)series).Opacity = (double)1;
            }
        }

        public void Notify() {
            ChartControl.Notify();
        }
    }
}
