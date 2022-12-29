/**
 * Datart
 *
 * Copyright 2021
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { CaretRightFilled } from '@ant-design/icons';
import classnames from 'classnames';
import {
  Children,
  cloneElement,
  memo,
  ReactElement,
  ReactNode,
  useCallback,
  useEffect,
  useMemo,
  useState,
} from 'react';
import styled from 'styled-components/macro';
import {
  FONT_WEIGHT_MEDIUM,
  SPACE,
  SPACE_MD,
  SPACE_XS,
} from 'styles/StyleConstants';

interface FlexCollapseProps {
  defaultActiveKeys?: string[];
  activeKeys?: string[];
  children: ReactElement | ReactElement[];
}

const Collapse = memo(
  ({ defaultActiveKeys, activeKeys, children }: FlexCollapseProps) => {
    const panels = useMemo(() => {
      let panels: ReactElement[] = [];
      Children.forEach(children, c => {
        if (c.type === Panel) {
          panels.push(cloneElement(c, { defaultActiveKeys, activeKeys }));
        }
      });
      return panels;
    }, [defaultActiveKeys, activeKeys, children]);

    return <Container>{panels}</Container>;
  },
);

interface PanelProps {
  id: string;
  title?: string;
  defaultActiveKeys?: string[];
  activeKeys?: string[];
  children?: ReactNode;
  onChange?: (active: boolean, key: string) => void;
}

const Panel = memo(
  ({
    id,
    title,
    defaultActiveKeys,
    activeKeys,
    children,
    onChange,
  }: PanelProps) => {
    const [active, setActive] = useState<boolean>(
      defaultActiveKeys ? defaultActiveKeys.includes(id) : false,
    );

    useEffect(() => {
      if (activeKeys) {
        setActive(activeKeys.includes(id));
      }
    }, [id, activeKeys]);

    const toggleActive = useCallback(() => {
      setActive(!active);
      onChange && onChange(!active, id);
    }, [active, id, onChange]);

    return (
      <Block className={classnames({ active })}>
        <div className="title" onClick={toggleActive}>
          <CaretRightFilled className="prefix" rotate={active ? 90 : 0} />
          <h4>{title}</h4>
        </div>
        <div className="content">{children}</div>
      </Block>
    );
  },
);

export const FlexCollapse = Object.assign(Collapse, { Panel });

const Container = styled.div`
  display: flex;
  flex: 1;
  flex-direction: column;
  min-height: 0;
  margin-top: ${SPACE};
  border-top: 1px solid ${p => p.theme.borderColorSplit};
`;

const Block = styled.div`
  display: flex;
  flex-direction: column;
  min-height: 0;
  border-bottom: 1px solid ${p => p.theme.borderColorSplit};

  .title {
    display: flex;
    flex-shrink: 0;
    align-items: center;
    padding: ${SPACE_XS} ${SPACE_MD};
    cursor: pointer;

    .prefix {
      flex-shrink: 0;
      margin-right: ${SPACE_XS};
      color: ${p => p.theme.textColorLight};
    }

    h4 {
      flex: 1;
      font-weight: ${FONT_WEIGHT_MEDIUM};
    }
  }

  .content {
    display: none;
    flex: 1;
  }

  &.active {
    flex: 1;

    .prefix {
      color: ${p => p.theme.primary};
    }

    .title {
      /* background-color: ${p => p.theme.bodyBackground}; */

      h4 {
        /* color: ${p => p.theme.textColor}; */
      }
    }

    .content {
      display: block;
      overflow-y: auto;
    }
  }
`;
