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
using System.Collections;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Windows.Forms;
using Microsoft.EnterpriseManagement;
using Microsoft.EnterpriseManagement.Administration;
using Microsoft.EnterpriseManagement.Common;
using Microsoft.EnterpriseManagement.Configuration;
using Microsoft.EnterpriseManagement.Mom.Internal.UI.Common;
using Microsoft.EnterpriseManagement.Monitoring;
using Microsoft.EnterpriseManagement.UI;
using Ambari.SCOM.ScomPages.Properties;

namespace Ambari.SCOM.ScomPages.DiscoveryTemplate {
    public partial class WatcherNodesPage : UIPage {
        public const string SharedUserDataWatcherNodesList = "WatcherNodes.WatcherNodesList";
        public const string SharedUserDataTemplateIdString = "WatcherNodes.TemplateIdString";

        private IEnumerable<PartialMonitoringObject> watcherNodes;
        private string templateIdString;

        private string ambariUri;
        private string runAsName;
        private byte[] runAsSsid;

        public WatcherNodesPage() {
            InitializeComponent();
        }

        private IEnumerable<PartialMonitoringObject> GetWatcherNodes(IEnumerable watcherNodesList) {
            if (ManagementGroup == null) return null;

            var monitoringClass = ManagementGroup.EntityTypes.GetClass(SystemMonitoringClass.HealthService);
            if (monitoringClass == null) return null;

            var criteriaBuilder = new StringBuilder();
            foreach (var watcherNodeName in watcherNodesList) {
                criteriaBuilder.AppendFormat("DisplayName = '{0}' OR ", watcherNodeName);
            }
            criteriaBuilder.Remove(criteriaBuilder.Length - 4, 4);

            var partialMonitoringObjects =
                ManagementGroup.EntityObjects.GetObjectReader<PartialMonitoringObject>(
                    new MonitoringObjectGenericCriteria(criteriaBuilder.ToString()),
                    monitoringClass,
                    ObjectQueryOptions.Default);
            
            if (partialMonitoringObjects != null)
                return partialMonitoringObjects.GetRange(0, partialMonitoringObjects.Count);

            return null;
        }

        private void GetSharedUserData() {
            ambariUri = SharedUserData[AmbariDetailsPage.SharedUserDataAmbariUri] as string;
            runAsName = SharedUserData[AmbariDetailsPage.SharedUserDataRunAsName] as string;
            runAsSsid = SharedUserData[AmbariDetailsPage.SharedUserDataRunAsSsid] as byte[];
            watcherNodes = GetWatcherNodes(listWatcherNodes.Items);
        }

        public override void LoadPageConfig() {
            if (IsCreationMode) {
                templateIdString = Guid.NewGuid().ToString("N");
            } else {
                if (String.IsNullOrEmpty(InputConfigurationXml)) return;
                try {
                    var config = (WatcherNodesPageConfig)XmlHelper.Deserialize(InputConfigurationXml, typeof(WatcherNodesPageConfig), true);
                    templateIdString = config.TemplateIdString;
                    PopulateWatcherNodesList(config.WatcherNodesList);

                    SetSharedUserData();
                } catch (ArgumentNullException) {
                    return;
                } catch (InvalidOperationException) {
                    return;
                }
            }
            IsConfigValid = ValidatePageConfiguration();
            base.LoadPageConfig();
        }

        private void PopulateWatcherNodesList(string watcherNodesList) {
            listWatcherNodes.Items.AddRange(watcherNodesList.Split(new[] { ';' }, StringSplitOptions.RemoveEmptyEntries));
        }

        private void btnAdd_Click(object sender, EventArgs e) {
            ShowComputerPickerDialog();
            IsConfigValid = ValidatePageConfiguration();
        }

        private void btnRemove_Click(object sender, EventArgs e) {
            if (listWatcherNodes.SelectedItems == null ||
                listWatcherNodes.SelectedItems.Count == 0) return;
            var selectedItems = new ArrayList(listWatcherNodes.SelectedItems);
            foreach (var selectedItem in selectedItems)
                listWatcherNodes.Items.Remove(selectedItem);
            IsConfigValid = ValidatePageConfiguration();
        }

        private bool RunAsAccountDistributionDialog() {
            GetSharedUserData();

            var securedData = ManagementGroup.Security.GetSecureData(runAsSsid);
            if (securedData == null) return false;

            var newState = ManagementGroup.Security.GetApprovedHealthServicesForDistribution<PartialMonitoringObject>(securedData);
            if (newState == null) return false;

            if (newState.Result == ApprovedHealthServicesResults.All) return true;
            var missingAgents = watcherNodes.Except(newState.HealthServices).ToList();
            if (!missingAgents.Any()) return true;

            var text = String.Format(Resources.AllowDistributionConfirmationFormat, runAsName, String.Join(Environment.NewLine, missingAgents.Select(a => a.DisplayName)));
            if (MessageBox.Show(ParentForm, text, Resources.Warning, MessageBoxButtons.YesNo, MessageBoxIcon.Exclamation) != DialogResult.Yes)
                return false;
                
            newState.Result = ApprovedHealthServicesResults.Specified;
            missingAgents.ForEach(a => newState.HealthServices.Add(a));
            ManagementGroup.Security.SetApprovedHealthServicesForDistribution<PartialMonitoringObject>(securedData, newState);
            return true;
        }

        private bool AllowProxyDialog() {
            GetSharedUserData();

            var filterCriteria = new StringBuilder();
            foreach (var watcherNode in watcherNodes) {
                filterCriteria.Append("Name = '" + watcherNode.DisplayName + "' OR ");
            }
            filterCriteria.Remove(filterCriteria.Length - 4, 4); 

            var proxyDiabledAgents = ManagementGroup.Administration
                            .GetAgentManagedComputers(new AgentManagedComputerCriteria(filterCriteria.ToString()))
                            .Where(a => !a.ProxyingEnabled.Value);
            if (!proxyDiabledAgents.Any()) return true;

            var text = String.Format(Resources.AllowProxyConfirmationFormat, String.Join(Environment.NewLine, proxyDiabledAgents.Select(a => a.DisplayName)));
            if (MessageBox.Show(ParentForm, text, Resources.Warning, MessageBoxButtons.YesNo, MessageBoxIcon.Exclamation) != DialogResult.Yes)
                return false;

            foreach (var agent in proxyDiabledAgents) {
                agent.ProxyingEnabled = true;
                agent.ApplyChanges();
            }

            return true;
        }

        public override bool SavePageConfig() {
            if (!(IsConfigValid = ValidatePageConfiguration()) ||
                !RunAsAccountDistributionDialog() ||
                !AllowProxyDialog()) return false;

            OutputConfigurationXml = XmlHelper.Serialize(
                                        new WatcherNodesPageConfig {
                                            TemplateIdString = templateIdString,
                                            WatcherNodesList = String.Join(";", listWatcherNodes.Items.OfType<string>()),
                                            SeedComputerName = listWatcherNodes.Items.OfType<string>().FirstOrDefault() },
                                        true);
            SetSharedUserData();
            return true;
        }

        private void SetSharedUserData() {
            SharedUserData[SharedUserDataTemplateIdString] = templateIdString;
            SharedUserData[SharedUserDataWatcherNodesList] = listWatcherNodes.Items.OfType<string>();
        }

        private void ShowComputerPickerDialog() {
            using (var dialog = new MultipleAgentChooserDialog(Container)) {
                if (dialog.ShowDialog(this) != DialogResult.OK) return;

                if (dialog.SelectedItems == null) return;
                foreach (var selectedItem in dialog.SelectedItems) {
                    if (selectedItem == null || selectedItem.Item == null) continue;
                    var item = selectedItem.Item as ComputerHealthService;
                    if (!String.IsNullOrEmpty(item.PrincipalName) &&
                        !listWatcherNodes.Items.Contains(item.PrincipalName)) {
                        listWatcherNodes.Items.Add(item.PrincipalName);
                    }
                }
            }
        }

        private bool ValidatePageConfiguration() {
            if (listWatcherNodes.Items.Count <= 0)
                return false;

            SetSharedUserData();
            return true;
        }

        private void listWatcherNodes_SelectedIndexChanged(object sender, EventArgs e) {
            btnRemove.Enabled = (listWatcherNodes.SelectedIndex != ListBox.NoMatches);
        }
    }
}
