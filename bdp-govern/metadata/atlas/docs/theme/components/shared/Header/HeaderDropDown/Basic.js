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

import React, { useEffect, useState, useRef } from "react";
import styled from "styled-components";

const Basic = props => {
  let { options} = props;
  const [toggle, setToggle] = useState(false);
  const dropdownRef = useRef();
  useEffect(() => {
    const setToggleOnEvent = event => {
      if (!(event.target.classList.contains('dropbtn') || event.target.classList.contains('dropdown-item')) && dropdownRef.current.classList.contains('show')) {
        setToggle(false);
      }
    };
    window.addEventListener("mousedown", setToggleOnEvent);
    return () => {
      window.removeEventListener("mousedown", setToggleOnEvent);
    };
  }, []);

  const ComponentStyle = styled.div`
    > div {
      display: inline-block;
      width: 160px;
    }
    label {
      margin-right: 5px;
    }
  `;

  const DropDownComp = styled.div`
    .dropbtn {
      background-color: #ffffff;
      color: rgb(45, 55, 71);
      padding: 5px;
      font-size: 14px;
      border: 1px solid rgb(204, 204, 204);
      cursor: pointer;
      width: 100%;
      text-align: left;
      border-radius: 4px;
      &:hover, &:focus {
        border: 1px solid rgb(55, 187, 155);
      }
    }

    .dropdown {
      position: relative;
      display: inline-block;
      width: 100%;
    }

    .dropdown-content {
      margin-top: 10px;
      display: none;
      position: absolute;
      background-color: #ffffff;
      width: 160px;
      overflow: auto;
      max-height: 300px;
      box-shadow: 0px 8px 16px 0px rgba(0,0,0,0.2);
      z-index: 1;
      .dropdown-item {
        padding: 5px 12px;
        &:hover {
          background-color: rgba(55, 187, 155, 0.1);;
        }
      }
    }

    .dropdown-content a {
      padding: 12px 16px;
      text-decoration: none;
      display: block;
    }

    .dropdown a:hover {background-color: #ddd;}

    .show {display: block;}
    i {
      color: rgb(45, 55, 71);
      border: solid black;
      border-width: 0 1px 1px 0;
      display: inline-block;
      padding: 3px;
      float: right;
    }
    .up {
      margin-top: 5px;
      transform: rotate(-135deg);
      -webkit-transform: rotate(-135deg);
    }

    .down {
      margin-top: 3px;
      transform: rotate(45deg);
      -webkit-transform: rotate(45deg);
    }
  `;

  return (
    <ComponentStyle>
      <DropDownComp>
        <div className="dropdown">
          <button className="dropbtn" onClick={() => setToggle(c => !c)}>Versions <i className={`${toggle ? 'up' : 'down'}`}></i></button>
          <div ref={dropdownRef} id="myDropdown" className={`dropdown-content ${toggle ? 'show' : ''}`}>
            {
              options.map(function(opt, i) {
                return <div className="dropdown-item" key={i} data-href={opt.href} onClick={
                  event => {
                    const dataHref = event.currentTarget.dataset.href;
                    const href = `${window.location.origin}${dataHref}`;
                    window.open(href, "_blank");
                  }}>
                    {opt.title}
                </div>
              })
            }
          </div>
        </div>
      </DropDownComp>
    </ComponentStyle>
  );
};
Basic.propTypes = {};
export default Basic;
