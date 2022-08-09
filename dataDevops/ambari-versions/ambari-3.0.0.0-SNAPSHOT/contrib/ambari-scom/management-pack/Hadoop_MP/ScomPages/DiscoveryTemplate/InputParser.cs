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
using System.Text.RegularExpressions;
using Microsoft.EnterpriseManagement.Common;
using Microsoft.EnterpriseManagement.Configuration;
using Microsoft.EnterpriseManagement.Internal.UI.Authoring.Extensibility;
using Microsoft.EnterpriseManagement.Mom.Internal.UI.Common;
using Microsoft.EnterpriseManagement.UI;

namespace Ambari.SCOM.ScomPages.DiscoveryTemplate {
    public class InputParser : Component, IInputConfigurationParser {
        private const string ClassNamePrefix = "Ambari.SCOM.AmbariSeed.";
        private const string SeedDiscoveryNamePrefix = "Ambari.SCOM.Discovery.AmbariSeed.";
        private const string ClustersDiscoveryNamePrefix = "Ambari.SCOM.Discovery.Clusters.";
        private const string AccountOverrideNamePrefix = "Ambari.SCOM.Profile.Ambari.";
        private static Regex AmbariUriNodeRegex = new Regex("<AmbariUri>([^<]*)</AmbariUri>", RegexOptions.CultureInvariant | RegexOptions.Compiled);
        private static Regex WatcherNodesListNodeRegex = new Regex("<WatcherNodesList>([^<]*)</WatcherNodesList>", RegexOptions.CultureInvariant | RegexOptions.Compiled);
        private static Regex SeedComputerNameNodeRegex = new Regex("<ComputerName>([^<]*)</ComputerName>", RegexOptions.CultureInvariant | RegexOptions.Compiled);

        public InputParser(IContainer parentContainer) {
            if (parentContainer != null) {
                parentContainer.Add(this);
            }
        }

        private void GetDiscoveryConfig(ITemplateContext templateContext, TemplateInputConfig templateConfig) {
            foreach (var discovery in SDKHelper.GetFolderItems<ManagementPackDiscovery>(this, templateContext.OutputFolder)) {
                var discoveryConfig = discovery.DataSource.Configuration;

                if (discovery.Name.StartsWith(ClustersDiscoveryNamePrefix, StringComparison.OrdinalIgnoreCase)) {
                    if (WatcherNodesListNodeRegex.IsMatch(discoveryConfig))
                        templateConfig.WatcherNodesList = WatcherNodesListNodeRegex.Match(discoveryConfig).Groups[1].Value;
                } else if (discovery.Name.StartsWith(SeedDiscoveryNamePrefix, StringComparison.OrdinalIgnoreCase)) {
                    if (SeedComputerNameNodeRegex.IsMatch(discoveryConfig))
                        templateConfig.SeedComputerName = SeedComputerNameNodeRegex.Match(discoveryConfig).Groups[1].Value;
                    if (AmbariUriNodeRegex.IsMatch(discoveryConfig))
                        templateConfig.AmbariUri = AmbariUriNodeRegex.Match(discoveryConfig).Groups[1].Value;
                }
            }
        }

        private string GetRunAsAccounts(ITemplateContext templateContext) {
            var folderItems = SDKHelper.GetFolderItems<ManagementPackSecureReferenceOverride>(this, templateContext.OutputFolder);
            foreach (var @override in folderItems) {
                if (@override.Name.StartsWith(AccountOverrideNamePrefix, StringComparison.OrdinalIgnoreCase)) {
                    return @override.Value;
                }
            }
            return String.Empty;
        }

        private string GetTemplateIdString(ITemplateContext templateContext) {
            foreach (var mpClass in SDKHelper.GetFolderItems<ManagementPackClass>(this, templateContext.OutputFolder)) {
                if (mpClass.Name.StartsWith(ClassNamePrefix, StringComparison.OrdinalIgnoreCase)) {
                    return mpClass.Name.Remove(0, ClassNamePrefix.Length);
                }
            }
            throw new ObjectNotFoundException("No Hadoop Cluster class found in the template. Template instance may be corrupted. Try to remove and recreate the template.");
        }

        public bool LoadConfigurationXml(IPageContext pageContext) {
            var templateContext = pageContext as ITemplateContext;
            if (templateContext == null)
                throw new InvalidOperationException("No template context provided for Input Parser. Configuration could not be loaded.");
            
            var templateConfig = new TemplateInputConfig {
                Name = templateContext.OutputFolder.DisplayName,
                Description = templateContext.OutputFolder.Description,
                RunAsAccount = GetRunAsAccounts(templateContext),
                TemplateIdString = GetTemplateIdString(templateContext)
            };
            GetDiscoveryConfig(templateContext, templateConfig);
            pageContext.ConfigurationXml = XmlHelper.Serialize(templateConfig, false);
            return true;
        }
    }
}
