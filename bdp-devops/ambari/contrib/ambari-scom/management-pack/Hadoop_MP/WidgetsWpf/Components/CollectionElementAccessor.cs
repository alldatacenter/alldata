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
using System.ComponentModel.Composition;
using System.Linq;
using System.Collections.Specialized;

namespace Ambari.SCOM.Widgets.Components {
    [Export, PartCreationPolicy(CreationPolicy.NonShared)]
    public class CollectionElementAccessor : InitializableComponentBase {
        private IEnumerable<object> collection;
        public IEnumerable<object> Collection {
            get { return collection; }
            set {
                if (collection == value) return;

                if (collection is INotifyCollectionChanged)
                    ((INotifyCollectionChanged)collection).CollectionChanged -= OnInputCollectionChanged;

                collection = value;

                if (collection is INotifyCollectionChanged)
                    ((INotifyCollectionChanged)collection).CollectionChanged += OnInputCollectionChanged;

                NotifyPropertyChanged("Collection");
                SelectElement();
            }
        }

        private int elementIndex;
        public int ElementIndex {
            get { return elementIndex; }
            set {
                if (elementIndex == value) return;
                elementIndex = value;
                NotifyPropertyChanged("ElementIndex");
                SelectElement();
            }
        }

        private object element;
        public object Element {
            get { return element; }
            set {
                if (element == value) return;
                element = value;
                NotifyPropertyChanged("Element");
            }
        }

        protected override void Initialize() {
            SelectElement();
        }

        private void SelectElement() {
            if (!IsInitialized) return;
            if (elementIndex < 0 || collection == null ||
                elementIndex >= collection.Count()) {
                Element = null;
            } else {
                Element = collection.Skip(elementIndex).First();
            }
        }

        private void OnInputCollectionChanged(object sender, NotifyCollectionChangedEventArgs e) {
            SelectElement();
        }
    }
}
