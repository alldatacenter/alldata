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

function _interopDefault(ex) {
  return ex && typeof ex === "object" && "default" in ex ? ex["default"] : ex;
}
import {
  createContext,
  createElement,
  useContext,
  Component,
  useMemo,
  useRef,
  Fragment,
  SFC,
  useState,
  useEffect
} from "react";
import * as React from "react";

import {
  useMenus,
  useWindowSize,
  usePrevious,
  doczState
} from "../../../../docz-lib/docz/dist";
import styled from "styled-components";
import _unionBy from "lodash/fp/unionBy";
import _get from "lodash/fp/get";
import { Logo } from "../Logo";
import { Search } from "../Search";
import { Menu } from "./Menu";
import { Docz } from "./Docz";
import { Hamburger } from "./Hamburger";
import { MenuLink } from "./MenuLink";
import { SubMenu } from "./SubMenu";
import { get } from "../../../utils/theme";
import { mq, breakpoints } from "../../../styles/responsive";
import Utils from "../../../utils/utils";

const _pipe = _interopDefault(require("lodash/fp/pipe"));
const _omit = _interopDefault(require("lodash/fp/omit"));
const sort = _interopDefault(require("array-sort"));
const _flattenDepth = _interopDefault(require("lodash/fp/flattenDepth"));
const match = _interopDefault(require("match-sorter"));
const ulid = require("ulid");
const WrapperProps = {
  opened: true,
  theme: ""
};

const sidebarBg = get("colors.sidebarBg");
const sidebarText = get("colors.sidebarText");
const sidebarBorder = get("colors.sidebarBorder");
const Wrapper = styled.div`
  position: relative;
  width: 320px;
  min-width: 320px;
  min-height: 100vh;
  background: ${sidebarBg};
  transition: transform 0.2s, background 0.3s;
  z-index: 1000;

  ${mq({
    position: ["absolute", "absolute", "absolute", "relative"]
  })};

  dl {
    padding: 0;
    margin: 0 10px;
  }

  dl a {
    font-weight: 400;
  }

  @media screen and (max-width: ${breakpoints.desktop - 1}px) {
    transform: translateX(${p => (p.opened ? "-100%" : "0")});
    position: ${p => (p.opened ? "auto" : "fixed")};
  }

  ${get("styles.sidebar")};
`;

Wrapper.defaultProps = WrapperProps;
const GitHubDivContainer = styled.div`
  position: relative;
`;
const GitHubDivContent = styled.div`
  position: fixed;
  top: 0px;
  right: 10px;
`;

const Content = styled.div`
  position: fixed;
  top: 0;
  left: 0;
  display: flex;
  flex-direction: column;
  width: 280px;
  min-width: 320px;
  height: 100%;
  max-height: 100vh;
  background: ${sidebarBg};
`;

const Menus = styled.nav`
  flex: 1;
  overflow-y: auto;
  margin-bottom: 10px;
`;

const Empty = styled.div`
  flex: 1;
  opacity: 0.7;
  padding: 0 24px;
  color: ${sidebarText};
`;

const Footer = styled.div`
  padding: 10px 0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  color: ${sidebarText};
  border-top: 1px dashed ${sidebarBorder};
`;

const FooterLink = styled.a`
  padding: 0;
  margin: 0;
  margin-left: 5px;
`;

const FooterLogo = styled(Docz)`
  fill: ${sidebarText};
`;

const OpenProps = {
  opened: false
};
const ToggleBackground = styled.div`
  content: "";
  display: ${p => (p.opened ? "none" : "block")};
  position: fixed;
  background-color: rgba(0, 0, 0, 0.4);
  width: 100vw;
  height: 100vh;
  top: 0;
  bottom: 0;
  left: 0;
  right: 0;
  cursor: pointer;
  z-index: 99;
`;
const UNKNOWN_POS = Infinity;
function compare(a, b, reverse) {
  if (a < b) return reverse ? 1 : -1;
  if (a > b) return reverse ? -1 : 1;
  return 0;
}
const sortByName = (a, b) => {
  return a.name < b.name ? -1 : a.name > b.name ? 1 : 0;
};
const findPos = (item, orderedList = []) => {
  const name = typeof item !== "string" ? _get("name", item) : item;
  const pos = orderedList.findIndex(item => item === name);
  return pos !== -1 ? pos : UNKNOWN_POS;
};
const compareWithMenu = (to = []) => (a, b) => {
  const list = to.map(i => i.name || i);
  return compare(findPos(a, list), findPos(b, list));
};
const noMenu = entry => !entry.submenu;
const parseItemStr = item =>
  typeof item === "string"
    ? {
        name: item
      }
    : item;
const normalize = item => {
  const selected = parseItemStr(item);
  return Object.assign({}, selected, {
    id: selected.id || ulid.ulid(),
    parent: _get("parent", selected) || _get("parent", item),
    menu: Array.isArray(selected.menu)
      ? selected.menu.map(normalize)
      : selected.menu
  });
};

const clean = item => (item.href || item.route ? _omit("menu", item) : item);
const normalizeAndClean = _pipe(normalize, clean);
const fromMenu = submenu => entry => entry.submenu === submenu;

const entryAsMenu = entry => ({
  name: entry.name,
  route: entry.route,
  parent: entry.parent,
  submenu: entry.submenu,
  menu: entry.menu
});

function flatArrFromObject(arr, prop, menus) {
  const reducer = (arr, obj) => {
    const value = _get(prop)(obj);

    return value ? arr.concat([value]) : arr;
  };

  return Array.from(new Set(arr.reduce(reducer, [])));
}
const entriesOfSubMenu = (submenu, entries) =>
  entries.filter(fromMenu(submenu)).map(entryAsMenu);
const entriesOfMenu = (menu, entries) =>
  entries.filter(fromMenu(menu)).map(entryAsMenu);
const parseMenu = entries => name => ({
  name,
  menu: entriesOfMenu(name, entries)
});

// const parseSubMenu = entries => name => ({
//   name,
//   submenu: entriesOfSubMenu(name, entries),
//   menu:entries.menu
// });

const parseSubMenu = function parseSubMenu(entries) {
  return function(name) {
    return {
      name: name,
      submenu: entriesOfSubMenu(name, entries),
      menu: entriesOfMenu(name, entries)
    };
  };
};
const menusFromEntries = entries => {
  const entriesWithoutMenu = entries.filter(noMenu).map(entryAsMenu);
  const menus = flatArrFromObject(entries, "menu").map(parseMenu(entries));
  const submenus = flatArrFromObject(entries, "submenu", menus).map(
    parseSubMenu(entries)
  );

  for (var x in menus) {
    for (var without of entriesWithoutMenu) {
      if (without.name == menus[x].name) {
        menus[x] = without;
        break;
      }
    }
  }
  for (var x in menus) {
    for (var y in submenus) {
      if (menus[x].name == submenus[y].menu[0].menu) {
        if (menus[x].menu.length > 0) {
          menus[x].menu.push(submenus[y]);
        } else {
          menus[x].menu.push(submenus[y]);
        }
      }
    }
  }
  return _unionBy("name", menus, entriesWithoutMenu);
};
const mergeMenus = (entriesMenu, configMenu) => {
  const first = entriesMenu.map(normalizeAndClean);
  const second = configMenu.map(normalizeAndClean);

  const merged = _unionBy("name", first, second);
  return merged.map(item => {
    if (!item.menu) return item;
    const found = second.find(i => i.name === item.name);
    const foundMenu = found && found.menu;
    return Object.assign({}, item, {
      menu: foundMenu
        ? mergeMenus(item.menu, foundMenu)
        : item.menu || found.menu
    });
  });
};
const sortMenus = (first, second = []) => {
  const sorted = sort(first, compareWithMenu(second), sortByName);
  return sorted.map(item => {
    if (!item.menu) return item;
    const found = second.find(menu => menu.name === item.name);
    const foundMenu = found && found.menu;
    return Object.assign({}, item, {
      menu: foundMenu ? sortMenus(item.menu, foundMenu) : item.menu
    });
  });
};
const search = (val, menu) => {
  const items = menu.map(item => [item].concat(item.menu || []));

  const flattened = _flattenDepth(2, items);

  const flattenedDeduplicated = [...new Set(flattened)];
  const matchedValues = match(flattenedDeduplicated, val, {
    keys: ["name"]
  });
  return matchedValues;
};

const filterMenus = (items, filter) => {
  if (!filter) return items;
  return items.filter(filter).map(item => {
    if (!item.menu) return item;
    return Object.assign({}, item, {
      menu: item.menu.filter(filter)
    });
  });
};
const useMenusCustom = opts => {
  const { query = "" } = opts || {};
  const { entries, config } = useContext(doczState.context);
  if (!entries) return null;
  const arr = entries.map(({ value }) => value);

  const entriesMenu = menusFromEntries(arr);

  const sorted = React.useMemo(() => {
    const merged = mergeMenus(entriesMenu, config.menu);
    const result = sortMenus(merged, config.menu);
    return filterMenus(result, opts && opts.filter);
  }, [entries, config]);
  return query && query.length > 0 ? search(query, sorted) : sorted;
  // return entriesMenu;
};

ToggleBackground.defaultProps = OpenProps;
const getActiveMenu = () => {
  const { localStorageKeys } = Utils;
  let act_menu = JSON.parse(localStorage.getItem(localStorageKeys.ACTIVEMENU));
  if (!act_menu) {
    act_menu = ["Documentation"];
    localStorage.setItem(localStorageKeys.ACTIVEMENU, JSON.stringify(act_menu));
  }
  return act_menu;
};
export const Sidebar = () => {
  const [activeMenu, setActiveMenu] = useState(getActiveMenu());
  const [hidden, setHidden] = useState(true);
  const [query, setQuery] = useState("");
  const menus = useMenusCustom({ query });
  const windowSize = useWindowSize();
  const isDesktop = windowSize.innerWidth >= breakpoints.desktop;
  const prevIsDesktop = usePrevious(isDesktop);
  const navRef = useRef();

  useEffect(() => {
    if (!hidden && !prevIsDesktop && isDesktop) {
      setHidden(true);
      document.documentElement.classList.remove("with-overlay");
    }
  });

  useEffect(() => {
    const { localStorageKeys } = Utils;
    const navTop = parseInt(localStorage.getItem(localStorageKeys.NAVPOSITION));
    if (navTop) {
      navRef.current.scrollTop = navTop;
    }
  }, []);

  const addOverlayClass = isHidden => {
    const method = !isHidden ? "add" : "remove";

    if (typeof window !== "undefined" && !isDesktop) {
      document.documentElement.classList[method]("with-overlay");
    }
  };

  const handleSidebarToggle = () => {
    if (isDesktop) return;
    setHidden(s => !s);
    addOverlayClass(!hidden);
  };
  const handleScroll = () => {
    const { localStorageKeys } = Utils;
    localStorage.setItem(
      localStorageKeys.NAVPOSITION,
      navRef.current.scrollTop
    );
  };
  const handleActiveMenu = menu => {
    const { localStorageKeys } = Utils;
    const t_activeMenu = JSON.parse(JSON.stringify(activeMenu));
    const index = t_activeMenu.findIndex(a => a === menu.name);
    if (index === -1) {
      t_activeMenu.push(menu.name);
      setActiveMenu(t_activeMenu);
    } else {
      t_activeMenu.splice(index, 1);
      setActiveMenu([...t_activeMenu]);
    }
    localStorage.setItem(
      localStorageKeys.ACTIVEMENU,
      JSON.stringify(t_activeMenu)
    );
  };
  let outputHtml = (
    <Fragment>
      <Wrapper opened={hidden}>
        <Content>
          <Hamburger opened={!hidden} onClick={handleSidebarToggle} />
          <Logo showBg={true} />

          {menus && menus.length === 0 ? (
            <Empty>No documents founda.</Empty>
          ) : (
            <Menus ref={navRef} onScroll={handleScroll}>
              {menus &&
                menus.map(menu => (
                  <Menu
                    key={menu.id}
                    item={menu}
                    sidebarToggle={handleSidebarToggle}
                    activeMenu={activeMenu}
                    handleActiveMenu={handleActiveMenu}
                  />
                ))}
            </Menus>
          )}
          {/* <Footer>
            Built with
            <FooterLink href="https://docz.site" target="_blank">
              <FooterLogo width={40} />
            </FooterLink>
          </Footer>*/}
        </Content>
      </Wrapper>
      <ToggleBackground opened={hidden} onClick={handleSidebarToggle} />
    </Fragment>
  );
  if (query.length > 0) {
    outputHtml = (
      <Fragment>
        <Wrapper>
          <Content>
            <Hamburger opened={!hidden} onClick={handleSidebarToggle} />
            <Logo showBg={!hidden} />

            <MenuLink item={menus}></MenuLink>
            <Menus ref={navRef} onScroll={handleScroll}>
              {menus &&
                menus.map(menu => (
                  <SubMenu
                    key={menu.id}
                    item={menu}
                    sidebarToggle={handleSidebarToggle}
                    collapseAll={Boolean(query.length)}
                  />
                ))}
            </Menus>
          </Content>
        </Wrapper>
      </Fragment>
    );
  }
  return outputHtml;
};