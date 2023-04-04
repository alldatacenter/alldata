import classnames from 'classnames';
import {
  cloneElement,
  memo,
  ReactElement,
  useCallback,
  useEffect,
  useState,
} from 'react';
import styled from 'styled-components/macro';
import {
  BORDER_RADIUS,
  FONT_SIZE_SUBHEADING,
  FONT_WEIGHT_MEDIUM,
  LINE_HEIGHT_TITLE,
  SPACE,
  SPACE_LG,
  SPACE_MD,
  SPACE_XS,
} from 'styles/StyleConstants';

interface ListSwitchProps {
  titles: Array<{ key: string; icon?: ReactElement; text: string }>;
  selectedKey?: string;
  onSelect?: (selectedKey: string) => void;
}

export const ListSwitch = memo(
  ({ titles, selectedKey: selectedKeyProp, onSelect }: ListSwitchProps) => {
    const [selectedKey, setSelectedKey] = useState(
      selectedKeyProp || (titles.length ? titles[0].key : ''),
    );

    useEffect(() => {
      if (selectedKeyProp) {
        setSelectedKey(selectedKeyProp);
      }
    }, [selectedKeyProp]);

    const itemSelect = useCallback(
      key => () => {
        if (key !== selectedKey) {
          setSelectedKey(key);
          onSelect && onSelect(key);
        }
      },
      [onSelect, selectedKey],
    );

    return (
      <Container>
        {titles.map(({ key, icon, text }) => (
          <li
            key={key}
            className={classnames({ selected: key === selectedKey })}
            onClick={itemSelect(key)}
          >
            {icon && cloneElement(icon, { className: 'icon' })}
            {text}
          </li>
        ))}
      </Container>
    );
  },
);

const Container = styled.ul`
  flex-shrink: 0;
  padding: ${SPACE_LG} ${SPACE_MD} ${SPACE_MD};

  li {
    height: 36px;
    padding: ${SPACE} ${SPACE_LG};
    font-size: ${FONT_SIZE_SUBHEADING};
    font-weight: ${FONT_WEIGHT_MEDIUM};
    line-height: ${LINE_HEIGHT_TITLE};
    color: ${p => p.theme.textColor};
    cursor: pointer;
    border-radius: ${BORDER_RADIUS};

    .icon {
      margin-right: ${SPACE_XS};
      color: ${p => p.theme.textColorDisabled};
    }

    &.selected {
      color: ${p => p.theme.primary};
      background-color: ${p => p.theme.bodyBackground};

      .icon {
        color: ${p => p.theme.primary};
      }
    }
  }
`;
