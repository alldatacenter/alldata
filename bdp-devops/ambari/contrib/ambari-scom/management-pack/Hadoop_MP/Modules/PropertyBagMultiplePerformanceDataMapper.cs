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
using System.Linq;
using System.Collections.Generic;
using System.Xml;
using System.Xml.Serialization;
using Microsoft.EnterpriseManagement.HealthService;
using Microsoft.EnterpriseManagement.Mom.Modules.DataItems;
using Microsoft.EnterpriseManagement.Mom.Modules.DataItems.Performance;

namespace Ambari.SCOM.Modules {
    [MonitoringModule(ModuleType.Condition), ModuleOutput(true)]
    public class PropertyBagMultiplePerformanceDataMapper : ModuleBase<MOMPerfDataItem> {
        private readonly object shutdownLock;
        private bool shutdown;

        private PropertyBagMultiplePerformanceDataMapperConfiguration config;

        public PropertyBagMultiplePerformanceDataMapper(ModuleHost<MOMPerfDataItem> moduleHost, XmlReader configuration, byte[] previousState)
            : base(moduleHost) {
            if (moduleHost == null)
                throw new ArgumentNullException("moduleHost");

            if (configuration == null)
                throw new ArgumentNullException("configuration");

            if (previousState != null) {
                // Since this module never calls SaveState this value should be null.
                throw new ArgumentOutOfRangeException("previousState");
            }

            shutdownLock = new object();

            LoadConfiguration(configuration);
        }

        private void LoadConfiguration(XmlReader configuration) {
            try {
                config = (PropertyBagMultiplePerformanceDataMapperConfiguration)new XmlSerializer(typeof(PropertyBagMultiplePerformanceDataMapperConfiguration)).Deserialize(configuration);
            } catch (InvalidOperationException xe) {
                throw new ModuleException("Invalid module configuration.", xe);
            }
        }

        [InputStream(0)]
        public void OnNewDataItems(DataItemBase[] dataItems, bool logicalSet,
                                   DataItemAcknowledgementCallback acknowledgedCallback, object acknowledgedState,
                                   DataItemProcessingCompleteCallback completionCallback, object completionState) {
            // Either both delegates are null or neither should be.
            if ((acknowledgedCallback == null && completionCallback != null) ||
                (acknowledgedCallback != null && completionCallback == null)) {
                throw new ArgumentOutOfRangeException("acknowledgedCallback, completionCallback");
            }

            var ackNeeded = acknowledgedCallback != null;

            lock (shutdownLock) {
                if (shutdown) return;

                var outputDataItems = new List<MOMPerfDataItem>();
                var timeStamp = DateTime.UtcNow;
                foreach (var dataItem in dataItems) {
                    var bag = dataItem is PropertyBagDataItem ? (PropertyBagDataItem)dataItem
                                : new PropertyBagDataItem(dataItem.GetItemXml());
                    foreach (var collection in bag.Collections) {
                        foreach (var record in collection.Value) {
                            double value;
                            if (record.Value == null ||
                                !double.TryParse(record.Value.ToString(), out value)) continue;

                            var mapping = config.Mappings.FirstOrDefault(m => m.PropertyName.Equals(record.Key, StringComparison.InvariantCultureIgnoreCase));
                            if (mapping == null) continue;

                            var perfDataItem = new MOMPerfDataItem(
                                timeStamp,
                                mapping.ObjectName, mapping.CounterName, mapping.InstanceName, 
                                false, value,
                                config.ManagedEntityId, config.RuleId);

                            outputDataItems.Add(perfDataItem);
                        }
                    }
                }

                // Handle output
                if (outputDataItems == null || outputDataItems.Count == 0) return;
                if (ackNeeded) {
                    DataItemAcknowledgementCallback ackDelegate = delegate(object ackState) {
                        lock (shutdownLock) {
                            if (shutdown) return;
                            acknowledgedCallback(acknowledgedState);
                            completionCallback(completionState);
                            ModuleHost.RequestNextDataItem();
                        }
                    };

                    ModuleHost.PostOutputDataItems(outputDataItems.ToArray(), true/*logicalSet*/, ackDelegate, null);
                } else {
                    ModuleHost.PostOutputDataItems(outputDataItems.ToArray(), true/*logicalSet*/);
                    ModuleHost.RequestNextDataItem();
                }
            }
        }

        public override void Shutdown() {
            lock (shutdownLock) {
                shutdown = true;
            }
        }

        public override void Start() {
            lock (shutdownLock) {
                if (shutdown) return;
                ModuleHost.RequestNextDataItem();
            }
        }
    }
}
