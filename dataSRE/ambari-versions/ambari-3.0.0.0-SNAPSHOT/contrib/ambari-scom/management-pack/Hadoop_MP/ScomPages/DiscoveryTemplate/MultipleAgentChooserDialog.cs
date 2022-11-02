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
using System.Collections.ObjectModel;
using System.ComponentModel;
using System.Windows.Forms;
using Microsoft.EnterpriseManagement.Administration;
using Microsoft.EnterpriseManagement.Configuration;
using Microsoft.EnterpriseManagement.Internal.UI.Authoring.Pages.Azure;
using Microsoft.EnterpriseManagement.Mom.Internal.UI;
using Microsoft.EnterpriseManagement.Mom.Internal.UI.Common;
using Microsoft.EnterpriseManagement.Mom.Internal.UI.Controls;

namespace Ambari.SCOM.ScomPages.DiscoveryTemplate {
    class MultipleAgentChooserDialog : AgentSimpleChooserDialog{
        public MultipleAgentChooserDialog(IContainer parentContainer)
            : base(new SCOMServerPickerControl(), parentContainer)
        {
            InitializeComponent();
        }

        public MultipleAgentChooserDialog(IAsyncChooserControlSearch searchControl, IContainer parentContainer)
            : base(searchControl, parentContainer) {
            InitializeComponent();
        }

		private void InitializeComponent() {
			mainChooserControl.MultiSelect = true;
		}
	}


    class SCOMServerPickerControl : IAsyncChooserControlSearch
    {
        private AgentPickerControl innerControl = new AgentPickerControl();


        public ReadOnlyCollection<IChooserControlItem> Search(CancelFlagWrapper cancelFlag)
        {
            if (cancelFlag.Cancel)
                return (ReadOnlyCollection<IChooserControlItem>)null;

            var templist = innerControl.Search(cancelFlag);
            var res = new List<IChooserControlItem>();
            foreach (var ci in templist)
            {
                if (ci.Item is ManagementServer)
                {
                    res.Add(ci);
                }
            }
            return new ReadOnlyCollection<IChooserControlItem>(res);
        }

        public Control SearchControl
        {
            get { return innerControl; }
        }
    }
}
