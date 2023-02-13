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

declare namespace CONFIGURATION {
  type ILang = 'DOT_NET' | 'GO' | 'JAVA' | 'NODEJS' | 'PHP';

  type ILangConf = {
    language: string;
    displayName: string;
    beta: boolean;
  };

  interface IStrategy {
    displayName: string;
    strategy: string;
    languages: Array<ILangConf>;
  }

  interface IDocs {
    language: string;
    strategy: string;
    scopeId: string;
  }

  interface IAllToken {
    subjectType: number;
    accessKey?: string;
    pageNo: number;
    pageSize: number;
    scope?: string;
    scopeId: string;
  }

  interface ICreateKey {
    description?: string;
    subjectType: number;
    scopeId: string;
  }

  interface IDelAndFindToken {
    id: string;
  }

  interface IAllTokenData {
    token: string;
    createdAt: string;
    description: string;
    id: string;
    status: string;
    subjectType: number;
    scope: string;
    scopeId: string;
  }

  interface ITokenList {
    list: IAllTokenData[];
    total: number;
  }
}
