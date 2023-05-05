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

module.exports = `
@font-face {
  font-family: "Roboto-Regular";
  src: url('/static/roboto-regular.ttf') format('opentype');
}

body{
  margin: 0
  font-family: "Roboto-Regular";
}

#erda-skeleton {
  position: fixed;
  top: 0;
  left: 0;
  z-index: 999;
  display: flex;
  width: 100%;
  height: 100%;
  pointer-events: none;
}

.skeleton-nav {
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  width: 48px;
  height: 100%;
  padding: 0 8px;
  color: #ffffff;
  box-sizing: border-box;
  text-align: center;
  background: #6a549e;
}

.skeleton-nav .nav-item {
  width: 32px;
  height: 32px;
  margin-bottom: 8px;
}

.skeleton-nav i {
  margin-right: 0;
  font-size: 20px;
}

.skeleton-sidebar-info {
  padding-top: 16px;
  padding-bottom: 36px;
  text-align: center;
}

.skeleton-header {
  padding: 10px 16px 9px;
  border-bottom: 1px solid rgba(0, 0, 0, .1);
}

.skeleton-body {
  position: relative;
  display: flex;
  flex: 1;
  flex-direction: column;
  overflow: hidden;
}

.skeleton-bg {
  background-color: #e4e4e4;
}

.skeleton-logo {
  display: inline-block;
  width: 56px;
  height: 56px;
  margin-bottom: 30px;
  border-radius: 100%;
}

.skeleton-line {
  height: 20px;
  margin-top: 6px;
  margin-bottom: 12px;
}

.skeleton-icon-line {
  height: 24px;
  margin-left: 28px;
}

.skeleton-icon-line:before {
  position: relative;
  left: -28px;
  display: inline-block;
  width: 24px;
  height: 24px;
  margin-right: 8px;
  background-color: #e4e4e4;
  border-radius: 100%;
  content: "";
}

.main-holder {
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  height: 80%;
  color: #bbbbbb;
  font-size: 30px;
}

.main-holder span {
  margin-top: 30px;
}

#enter-loading {
  position: relative;
  width: 300px;
  height: 4px;
  margin: 46px auto 20px;
  overflow: hidden;
  background-color: #bbbbbb;
  border-radius: 2px;
}

#enter-loading::before {
  position: absolute;
  display: block;
  width: 100%;
  height: 100%;
  background-color: #6a549e;
  border-radius: 2px;
  transform: translateX(-300px);
  animation: a-lb 6s .2s linear forwards;
  content: "";
}

@keyframes a-lb {0% {transform:translateX(-300px);}
5% {transform:translateX(-240px);}
15% {transform:translateX(-30px);}
25% {transform:translateX(-30px);}
30% {transform:translateX(-20px);}
45% {transform:translateX(-20px);}
50% {transform:translateX(-15px);}
65% {transform:translateX(-15px);}
70% {transform:translateX(-10px);}
95% {transform:translateX(-10px);}
100% {transform:translateX(-5px);}
}

`;
