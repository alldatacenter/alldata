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

// if MemberScope is exported in 'common/stores/_member', jest runtime will get the error "Cannot read property 'ORG’ of undefined"
// it is really confusing
export enum MemberScope {
  ORG = 'org',
  PROJECT = 'project',
  APP = 'app',
  MSP = 'msp',
  SYS = 'sys',
}
