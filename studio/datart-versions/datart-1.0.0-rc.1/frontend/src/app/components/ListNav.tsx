import {
  Children,
  cloneElement,
  memo,
  ReactElement,
  useCallback,
  useLayoutEffect,
  useState,
} from 'react';
import styled from 'styled-components/macro';
import { ListTitle } from './ListTitle';

interface ListNavProps {
  className?: string;
  defaultActiveKey: string;
  children: ReactElement | ReactElement[];
}

export const ListNav = memo(
  ({ className, defaultActiveKey, children }: ListNavProps) => {
    const [panes, setPanes] = useState<{ [key: string]: ReactElement }>({});
    const [keys, setKeys] = useState<string[]>([]);
    const [activeKey, setActiveKey] = useState(defaultActiveKey);

    useLayoutEffect(() => {
      let panes = {};
      let keys: string[] = [];

      Children.forEach(children, e => {
        if (e.type === ListPane) {
          panes[e.key!] = e;
          keys.push(e.key!.toString());
        }
      });
      setPanes(panes);
      setKeys(keys);
    }, [children]);

    const previous = useCallback(() => {
      const index = keys.findIndex(k => k === activeKey);
      if (index) {
        setActiveKey(keys[index - 1]);
      }
    }, [keys, activeKey]);

    const next = useCallback(() => {
      const index = keys.findIndex(k => k === activeKey);
      if (index < keys.length - 1) {
        setActiveKey(keys[index + 1]);
      }
    }, [keys, activeKey]);

    return (
      <Wrapper className={className}>
        {panes[activeKey] &&
          cloneElement(panes[activeKey], {
            onPrevious: previous,
            onNext: next,
          })}
      </Wrapper>
    );
  },
);

interface ListPaneProps {
  key: string;
  children: ReactElement | ReactElement[];
  onPrevious?: () => void;
  onNext?: () => void;
}

export const ListPane = memo(
  ({ children, onPrevious, onNext }: ListPaneProps) => {
    return (
      <>
        {Children.map(children, e =>
          e.type === ListTitle ? cloneElement(e, { onPrevious, onNext }) : e,
        )}
      </>
    );
  },
);

const Wrapper = styled.div`
  display: flex;
`;
