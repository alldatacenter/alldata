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

declare namespace CP_TEST {
  interface Inf {
    // inf comment
    a: number; // prop comment
    b: string;
    c: boolean;
    d: string[];
    e: any;
    f?: null;
  }
  enum en {
    UP = 'UP1', // en type
    LEFT = 'LEFT2',
  }
  // line comment
  type Ta = 'a' | 'b'; // type comment
  type Tb = string;
  type Tc = Omit<Inf, 'a'>;
}
