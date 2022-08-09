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
using Microsoft.EnterpriseManagement.Mom.Internal.UI.Common;
using Microsoft.EnterpriseManagement.Security;
using Microsoft.EnterpriseManagement.UI;
using System.Windows.Forms;

namespace Ambari.SCOM.ScomPages.DiscoveryTemplate {
    public partial class AmbariDetailsPage : UIPage {
        public const string SharedUserDataAmbariUri = "AmbariDetails.AmbariUri";
        public const string SharedUserDataRunAsId = "AmbariDetails.RunAsId";
        public const string SharedUserDataRunAsName = "AmbariDetails.RunAsName";
        public const string SharedUserDataRunAsSsid = "AmbariDetails.RunAsSsid";

        private List<RunAsAccountWrapper> runAsAccountsList;

        public AmbariDetailsPage() {
            InitializeComponent();
        }

        public override void LoadPageConfig() {
            if (!IsCreationMode) {
                if (String.IsNullOrEmpty(InputConfigurationXml)) return;

                try {
                    var pageConfig = (AmbariDetailsPageConfig)XmlHelper.Deserialize(InputConfigurationXml, typeof(AmbariDetailsPageConfig), true);
                    PopulateRunAsComboBox();

                    txtAmbariUri.Text = pageConfig.AmbariUri;

                    if (String.IsNullOrEmpty(pageConfig.RunAsAccount)) {
                        cbRunAsAccount.SelectedIndex = -1;
                    } else {
                        cbRunAsAccount.SelectedIndex = runAsAccountsList.FindIndex(a => a.AccountStorageIdString.Equals(pageConfig.RunAsAccount));
                    }

                    SetSharedUserData();
                } catch (Exception) {
                    return;
                }
            }

            IsConfigValid = ValidatePageConfiguration();
            base.LoadPageConfig();
        }

        public override bool OnSetActive() {
            PopulateRunAsComboBox();
            return base.OnSetActive();
        }


        public override bool SavePageConfig() {
            IsConfigValid = ValidatePageConfiguration();
            if (!IsConfigValid) return false;

            OutputConfigurationXml = XmlHelper.Serialize(
                                            new AmbariDetailsPageConfig() {
                                                AmbariUri = txtAmbariUri.Text.Trim().ToLowerInvariant(),
                                                RunAsAccount = runAsAccountsList[cbRunAsAccount.SelectedIndex].AccountStorageIdString },
                                            true);
            SetSharedUserData();
            return true;
        }

        private void PopulateRunAsComboBox() {
            if (runAsAccountsList != null && runAsAccountsList.Count > 0) return;

            var monitoringSecureData = ManagementGroup.Security.GetSecureData();

            runAsAccountsList = new List<RunAsAccountWrapper>();
            foreach (var data in monitoringSecureData) {
                if (data.SecureDataType != SecureDataType.Simple &&
                    data.SecureDataType != SecureDataType.Basic) continue;
                cbRunAsAccount.Items.Add(data.Name);
                runAsAccountsList.Add(new RunAsAccountWrapper(data));
            }
        }

        private void SetSharedUserData() {
            SharedUserData[SharedUserDataAmbariUri] = txtAmbariUri.Text;

            if (cbRunAsAccount.SelectedIndex < 0) return;
            var account = runAsAccountsList[cbRunAsAccount.SelectedIndex];
            SharedUserData[SharedUserDataRunAsId] = account.AccountStorageIdString;
            SharedUserData[SharedUserDataRunAsName] = account.AccountName;
            SharedUserData[SharedUserDataRunAsSsid] = account.AccountStorageIdByteArray;
        }

        private bool ValidatePageConfiguration() {
            if (String.IsNullOrEmpty(txtAmbariUri.Text.Trim()) ||
                cbRunAsAccount.SelectedIndex < 0)
                return false;
            SetSharedUserData();
            return true;
        }

        private void ValidatePageConfigurationEventHandler(object sender, EventArgs e) {
            IsConfigValid = ValidatePageConfiguration();
        }
    }
}
