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
using System.ComponentModel.Composition;

namespace Ambari.SCOM.Widgets.Components {
    [Export, PartCreationPolicy(CreationPolicy.NonShared)]
    public class StringSplitter : InitializableComponentBase {
        private string input;
        public string Input {
            get { return input; }
            set {
                if (input == value) return;
                input = value;
                NotifyPropertyChanged("Input");
                Split();
            }
        }

        private string separator;
        public string Separator {
            get { return separator; }
            set {
                if (separator == value) return;
                separator = value;
                NotifyPropertyChanged("Separator");
                Split();
            }
        }

        private string[] items;
        public string[] Items {
            get { return items; }
            set {
                if (items == value) return;
                items = value;
                NotifyPropertyChanged("Items");
            }
        }

        protected override void Initialize() {
            Split();
        }

        private void Split() {
            if (!IsInitialized) return;
            if (String.IsNullOrEmpty(input) || String.IsNullOrEmpty(separator))
                Items = new string[0];
            Items = input.Split(new string[] { separator }, StringSplitOptions.RemoveEmptyEntries);
        }
    }
}
