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
using System.Collections.Specialized;
using System.ComponentModel.Composition;
using System.Linq;
using Microsoft.EnterpriseManagement.Presentation.DataAccess;

namespace Ambari.SCOM.Widgets.Components {
    [Export, PartCreationPolicy(CreationPolicy.NonShared)]
    public class EntitiesToInstancesWithRecursionLevelConverter : InitializableComponentBase {
        private const string OutputDataType = "xsd://Microsoft.SystemCenter.Visualization.Library!Microsoft.SystemCenter.Visualization.DataProvider/MPInstanceWithRecursionLevel";
        private const string MpInstanceDataType = "mpinstance://System.Library!System.Entity";
        private const string IdProperty = "Id";
        private const string MpInstanceProperty = "MPInstance";
        private const string RecursionLevelProperty = "RecursionLevel";

        private ObservableDataModel model;

        private IEnumerable<IDataObject> entities;
        public IEnumerable<IDataObject> Entities {
            get { return entities; }
            set {
                if (entities == value) return;

                var notifiableCollection = entities as INotifyCollectionChanged;
                if (notifiableCollection != null)
                    notifiableCollection.CollectionChanged -= OnEntitiesCollectionChanged;
                entities = value;
                notifiableCollection = entities as INotifyCollectionChanged;
                if (notifiableCollection != null)
                    notifiableCollection.CollectionChanged += OnEntitiesCollectionChanged;

                NotifyPropertyChanged("Entities");
                Convert();
            }
        }

        private int recursionLevel;
        public int RecursionLevel {
            get { return recursionLevel; }
            set {
                if (recursionLevel == value) return;
                recursionLevel = value;
                NotifyPropertyChanged("RecursionLevel");
                Convert();
            }
        }

        private IEnumerable<IDataObject> instancesWithRecursionLevel;
        public IEnumerable<IDataObject> InstancesWithRecursionLevel {
            get { return instancesWithRecursionLevel; }
            set {
                if (instancesWithRecursionLevel == value) return;
                instancesWithRecursionLevel = value;
                NotifyPropertyChanged("InstancesWithRecursionLevel");
            }
        }

        public EntitiesToInstancesWithRecursionLevelConverter() {
            InitializeDataModel();
        }

        protected override void Initialize() {
            Convert();
        }

        private void Convert() {
            if (!IsInitialized) return;
            var outputCollection = model.CreateCollection(OutputDataType);
            if (entities == null || !entities.Any()) {
                outputCollection.Add(ConvertEntity(null));
            } else {
                foreach (var enitity in entities)
                    outputCollection.Add(ConvertEntity(enitity));
            }
            InstancesWithRecursionLevel = outputCollection;
        }

        private IDataObject ConvertEntity(IDataObject enitity) {
            var dataObject = model.CreateInstance(model.Types[OutputDataType]);
            dataObject[MpInstanceProperty] = enitity;
            dataObject[RecursionLevelProperty] = recursionLevel;
            return dataObject;
        }

        private void InitializeDataModel() {
            model = new ObservableDataModel();
            var dataType = model.Types.Create(OutputDataType);
            dataType.Properties.Create(MpInstanceProperty, typeof(IDataObject));
            dataType.Properties.Create(RecursionLevelProperty, typeof(int));
            dataType = model.Types.Create(MpInstanceDataType);
            dataType.Properties.Create(IdProperty, typeof(string));
        }

        private void OnEntitiesCollectionChanged(object sender, NotifyCollectionChangedEventArgs e) {
            Convert();
        }
    }
}
