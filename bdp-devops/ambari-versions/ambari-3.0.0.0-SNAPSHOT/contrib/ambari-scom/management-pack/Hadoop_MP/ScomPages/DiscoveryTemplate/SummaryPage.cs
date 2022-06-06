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
using System.Collections.Generic;
using System.Windows.Forms;
using Microsoft.EnterpriseManagement.Configuration;
using Microsoft.EnterpriseManagement.Internal.UI.Authoring.Pages;
using Microsoft.EnterpriseManagement.UI;
using Ambari.SCOM.ScomPages.Properties;

namespace Ambari.SCOM.ScomPages.DiscoveryTemplate {
    public partial class SummaryPage : UIPage {
        public SummaryPage() {
            InitializeComponent();
        }

        private void AddSummaryItem(string name, string value) {
            var item = new ListViewItem(name);
            item.SubItems.Add(value);
            listviewConfiguration.Items.Add(item);
        }

        public override bool OnSetActive() {
            SetListViewItems();
            IsConfigValid = true;
            return base.OnSetActive();
        }

        private void SetListViewItems() {
            listviewConfiguration.Items.Clear();
            IsConfigValid = false;
            
            AddSummaryItem(Resources.Name, SharedUserData[NameAndDescriptionPage.SharedUserDataName] as string);

            var description = SharedUserData[NameAndDescriptionPage.SharedUserDataDescription] as string;
            if (!String.IsNullOrEmpty(description))
                AddSummaryItem(Resources.Description, description);

            var outputManagementPack = SharedUserData[NameAndDescriptionPage.SharedUserDataManagementPack] as ManagementPack;
            if (DestinationManagementPack != null) {
                AddSummaryItem(Resources.ManagementPack, DestinationManagementPack.DisplayName);
            } else if (outputManagementPack != null) {
                AddSummaryItem(Resources.ManagementPack, outputManagementPack.DisplayName);
            }

            AddSummaryItem(Resources.AmbariUri, SharedUserData[AmbariDetailsPage.SharedUserDataAmbariUri] as string);
            AddSummaryItem(Resources.RunAsAccount, SharedUserData[AmbariDetailsPage.SharedUserDataRunAsName] as string);
            var watcherNodes = SharedUserData[WatcherNodesPage.SharedUserDataWatcherNodesList] as IEnumerable<string>;
            if (watcherNodes != null) {
                var firstItem = true;
                foreach (var watcherNode in watcherNodes) {
                    AddSummaryItem(firstItem ? Resources.WatcherNodes : String.Empty, watcherNode);
                    firstItem = false;
                }
            }

            listviewConfiguration.Columns[0].AutoResize(ColumnHeaderAutoResizeStyle.ColumnContent);
            listviewConfiguration.Columns[1].AutoResize(ColumnHeaderAutoResizeStyle.ColumnContent);
        }
    }
}
