import { Form, FormItemProps } from 'antd';
import styled from 'styled-components/macro';

export function BW(props: FormItemProps) {
  return <Wrapper {...props} colon={false} />;
}

const Wrapper = styled(Form.Item)`
  flex-direction: column;
  margin-bottom: 0;

  .ant-form-item-label {
    text-align: left;
  }
`;
