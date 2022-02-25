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
using System.Collections.Specialized;
using System.ComponentModel;
using System.Linq;
using Microsoft.EnterpriseManagement.Presentation.Controls.DataGrid.CollectionView;
using Microsoft.EnterpriseManagement.Presentation.Collections;
using Microsoft.EnterpriseManagement.Presentation.DataAccess;
using Ambari.SCOM.Widgets.Components;
using Ambari.SCOM.Widgets.Extensions;
using System.ComponentModel.Composition;

namespace Ambari.SCOM.Widgets.Charting {
    [Export, PartCreationPolicy(CreationPolicy.NonShared)]
    public class SeriesStacker : InitializableComponentBase {
        private OperationsQueue operations = new OperationsQueue();

        private StackedSeriesManager stackedSeriesManager = new StackedSeriesManager();

        private IEnumerable<IDataObject> input;
        public IEnumerable<IDataObject> Input {
            get { return input; }
            set {
                if (input == value) return;
                operations.ClearOperations();

                if (input is INotifyCollectionChanged)
                    ((INotifyCollectionChanged)input).CollectionChanged -= HandleInputCollectionChanged;

                input = value;

                if (input is INotifyCollectionChanged)
                    ((INotifyCollectionChanged)input).CollectionChanged += HandleInputCollectionChanged;

                HandleInputCollectionChanged(this, new NotifyCollectionChangedEventArgs(NotifyCollectionChangedAction.Reset, new List<IDataObject>(input), 0));
                NotifyPropertyChanged("Input");
            }
        }

        private ObservableCollection<IDataObject> output;
        public IEnumerable<IDataObject> Output { get { return output; } }

        private void SetOutput() {
            output = new ObservableCollection<IDataObject>();
            NotifyPropertyChanged("Output");
        }

        #region Handle Changes

        private void HandleInputCollectionChanged(object sender, NotifyCollectionChangedEventArgs e) {
            if (!IsInitialized) return;
            operations.QueueOperation(new Operation(o => {
                OnInputChanged(o.UserData[0], (NotifyCollectionChangedEventArgs)o.UserData[1]);
                return true;
            }, sender, e));
        }

        private void OnInputChanged(object sender, NotifyCollectionChangedEventArgs e) {
            switch (e.Action) {
                case NotifyCollectionChangedAction.Add:
                    AddItems(e.NewItems<IDataObject>());
                    break;
                case NotifyCollectionChangedAction.Remove:
                    RemoveItems(e.OldItems<IDataObject>());
                    break;
                case NotifyCollectionChangedAction.Reset:
                    var currentItems = sender as ReadOnlyObservableCollection<IDataObject>;
                    ClearOutput();
                    if (currentItems == null) break;
                    AddItems(currentItems);
                    break;
                case NotifyCollectionChangedAction.Replace:
                    RemoveItems(e.OldItems<IDataObject>());
                    AddItems(e.NewItems<IDataObject>());
                    break;
                default: break;
            }
        }

        private void AddItems(IList<IDataObject> items) {
            foreach (var series in items) {
                if (!series.Type.IsAssignableTo(ChartingConstants.StackableSeriesType)) {
                    // Pass through non-stackable series
                    output.Add(series);
                    continue;
                }

                var stackedSeries = stackedSeriesManager.Add(series);
                if (stackedSeries != null && !output.Contains(stackedSeries))
                    output.Add(stackedSeries);
            }
        }

        private void RemoveItems(IList<IDataObject> items) {
            foreach (var series in items) {
                if (!series.Type.IsAssignableTo(ChartingConstants.StackableSeriesType)) {
                    // Pass through non-stackable series
                    output.Remove(series);
                    continue;
                }

                var stackedSeries = stackedSeriesManager.Remove(series);
                if (stackedSeries != null)
                    output.Remove(stackedSeries);
            }
        }

        private void ClearOutput() {
            output.Clear();
            stackedSeriesManager.Clear();
        }

        #endregion Handle Changes

        protected override void Initialize() {
            SetOutput();
            HandleInputCollectionChanged(this, new NotifyCollectionChangedEventArgs(NotifyCollectionChangedAction.Reset, new List<IDataObject>(input), 0));
        }
    }
}
