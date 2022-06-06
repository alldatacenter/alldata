/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import React, { Component } from "react";
import axios from "axios";
import { parseString } from "xml2js";
import styled from "styled-components";

const TeamListStyle = styled.div`
  width: 100%;
  overflow: auto;

  > table {
    font-family: "Inconsolata", monospace;
    font-size: 14px;
    display: inline-table;
    table-layout: auto;
    color: #13161f;
    width: 98%;
    padding: 0;
    box-shadow: 0 0 0 1px #529d8b;
    background-color: transparent;
    border-spacing: 0;
    border-collapse: collapse;
    border-style: hidden;
    border-radius: 2px;
    overflow-y: hidden;
    overflow-x: initial;
    margin: 5px 10px;
  }
  > table tr {
    display: table-row;
    vertical-align: inherit;
    border-color: inherit;
  }
  > table tr > td {
    padding: 10px;
    line-height: 2;
    font-weight: 200;
    white-space: pre;
  }
  > table > thead {
    color: #7d899c;
    background: #f5f6f7;
  }
  > table > tbody tr {
    display: table-row;
    border-top: 1px solid #529d8b;
  }
  > table > thead > tr > th {
    font-weight: 400;
    padding: 10px;
    text-align: left;
  }
`;

export default class TeamList extends Component {
  constructor(props) {
    super(props);
    this.state = {
      isLoading: true,
      displayData: []
    };
    this.fetchData();
  }

  fetchData() {
    axios
      .get(`https://raw.githubusercontent.com/apache/atlas/master/pom.xml`)
      .then(res => {
        // Transform the raw data by extracting the nested posts
        parseString(res.data, (err, result) => {
          const developersList = result.project.developers[0].developer;
          const developersListLength = developersList.length;
          let t_displayData = [];
          const keys = Object.keys(developersList[0]);
          for (var i = 0; i < developersListLength; i++) {
            const obj = {};
            keys.map(k => (obj[k] = developersList[i][k]));
            t_displayData.push(obj);
          }
          this.setState({ displayData: t_displayData, isLoading: false });
        });
      })
      .catch(err => {
        console.log("fetching data from pom.xml is failed.");
      });
  }

  render() {
    const { displayData, isLoading } = this.state;
    return (
      <TeamListStyle>
        <table>
          <thead>
            <tr>
              <th>Id</th>
              <th>Name</th>
              <th>Email</th>
              <th>Organization</th>
              <th>Roles</th>
              <th>Time Zone</th>
            </tr>
          </thead>
          <tbody>
            {isLoading ? (
              <tr>
                <td colSpan="6">loading...</td>
              </tr>
            ) : displayData.length === 0 ? (
              "Not found"
            ) : (
              displayData.map((data, i) => {
                return (
                  <tr key={data.id.toString()}>
                    <td>{data.id.toString()}</td>
                    <td>{data.name.toString()}</td>
                    <td>{data.email.toString()}</td>
                    <td>{data.organization.toString()}</td>
                    <td>{data.roles.map(r => r.role.toString())}</td>
                    <td>{data.timezone.toString()}</td>
                  </tr>
                );
              })
            )}
          </tbody>
        </table>
      </TeamListStyle>
    );
  }
}
