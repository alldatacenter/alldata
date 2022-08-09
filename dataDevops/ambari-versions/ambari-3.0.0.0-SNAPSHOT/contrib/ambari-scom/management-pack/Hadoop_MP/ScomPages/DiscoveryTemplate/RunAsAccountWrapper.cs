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
using System.Globalization;
using System.Text;
using Microsoft.EnterpriseManagement.Security;

namespace Ambari.SCOM.ScomPages.DiscoveryTemplate {
    class RunAsAccountWrapper {
        public Guid? AccountId { get; private set; }
        public string AccountName { get; private set; }
        public byte[] AccountStorageIdByteArray { get; private set; }
        public string AccountStorageIdString { get; private set; }

        public RunAsAccountWrapper(SecureData account) {
            AccountName = account.Name;
            AccountId = account.Id;
            AccountStorageIdByteArray = account.SecureStorageId;
            AccountStorageIdString = ConvertToHex(AccountStorageIdByteArray);
        }

        private string ConvertToHex(byte[] byteArray) {
            var builder = new StringBuilder();
            foreach (var num in byteArray) {
                builder.Append(num.ToString("X2", CultureInfo.InvariantCulture));
            }
            return builder.ToString();
        }
    }
}
