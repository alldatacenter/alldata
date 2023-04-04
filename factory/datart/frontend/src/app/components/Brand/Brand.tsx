import logo from 'app/assets/images/logo.svg';
import styled from 'styled-components';
import {
  FONT_SIZE_ICON_XXL,
  FONT_WEIGHT_BOLD,
  LINE_HEIGHT_ICON_XXL,
  SPACE_MD,
  SPACE_TIMES,
} from 'styles/StyleConstants';

export function Brand() {
  return (
    <Header>
      <img src={logo} alt="logo" />
      <h1>datart</h1>
    </Header>
  );
}

const Header = styled.header`
  display: flex;
  align-items: center;

  h1 {
    padding: 0 ${SPACE_TIMES(7)} 0 ${SPACE_MD};
    font-size: ${FONT_SIZE_ICON_XXL};
    font-weight: ${FONT_WEIGHT_BOLD};
    line-height: ${LINE_HEIGHT_ICON_XXL};
  }

  img {
    width: 56px;
    height: 56px;
  }
`;
