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
using System.ComponentModel;
using System.ComponentModel.Composition;
using System.Linq;
using Microsoft.EnterpriseManagement.Presentation.DataAccess;

namespace Ambari.SCOM.Widgets.Components {
    [Export, PartCreationPolicy(CreationPolicy.NonShared)]
    public class StringFormatter : InitializableComponentBase {
        private string formatString;
        public string FormatString {
            get { return formatString; }
            set {
                if (formatString == value) return;
                formatString = value;
                NotifyPropertyChanged("FormatString");
                Format();
            }
        }

        private IEnumerable<IDataObject> arguments;
        public IEnumerable<IDataObject> Arguments {
            get { return arguments; }
            set {
                if (arguments == value) return;
                ForEachObservableArgument(a => a.PropertyChanged -= ArgumentChanged);
                arguments = value;
                ForEachObservableArgument(a => a.PropertyChanged += ArgumentChanged);
                NotifyPropertyChanged("Arguments");
                Format();
            }
        }

        private void ForEachObservableArgument(Action<ObservableDataObject> action) {
            if (arguments == null) return;
            foreach (var argument in arguments) {
                if (!(argument is ObservableDataObject)) continue;
                action((ObservableDataObject)argument);
            }
        }

        private void ArgumentChanged(object sender, PropertyChangedEventArgs e) {
            Format();
        }

        private string output;
        public string Output {
            get { return output; }
            set {
                if (output == value) return;
                output = value;
                NotifyPropertyChanged("Output");
            }
        }

        protected override void Initialize() {
            Format();
        }

        private void Format() {
            if (!IsInitialized) return;
            if (String.IsNullOrEmpty(formatString) || arguments == null)
                Output = null;
            Output = String.Format(formatString, arguments.OrderBy(a => a["Index"]).Select(a => a["Value"]).ToArray());
        }
    }
}
