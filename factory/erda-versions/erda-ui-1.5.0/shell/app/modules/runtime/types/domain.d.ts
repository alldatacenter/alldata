// Copyright (c) 2021 Terminus, Inc.
//
// This program is free software: you can use, redistribute, and/or modify
// it under the terms of the GNU Affero General Public License, version 3
// or later ("AGPL"), as published by the Free Software Foundation.
//
// This program is distributed in the hope that it will be useful, but WITHOUT
// ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
// FITNESS FOR A PARTICULAR PURPOSE.
//
// You should have received a copy of the GNU Affero General Public License
// along with this program. If not, see <http://www.gnu.org/licenses/>.

declare namespace RUNTIME_DOMAIN {
  interface Query {
    runtimeId: string;
    domainType: string;
  }

  interface Item {
    packageId: string;
    tenantGroup: string;
    appName: string;
    domain: string;
    domainType: string;
    customDomain: string;
    rootDomain: string;
    useHttps: boolean;
  }

  interface DomainMap {
    [k: string]: Item[];
  }

  interface UpdateDomainBody {
    runtimeId: string;
    serviceName: string;
    domains: string[];
    releaseId: string;
  }

  interface UpdateK8SDomainBody {
    runtimeId: string | number;
    serviceName: string;
    domains: string[];
    releaseId: string;
  }
}
