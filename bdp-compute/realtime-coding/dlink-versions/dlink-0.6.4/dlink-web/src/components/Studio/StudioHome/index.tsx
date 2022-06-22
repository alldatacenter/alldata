import {Typography,Divider} from 'antd';
import React from 'react';
import {connect} from 'umi';
import {StateType} from '@/pages/DataStudio/model';
import {Scrollbars} from 'react-custom-scrollbars';

const {Title, Paragraph, Text} = Typography;

const StudioHome = (props: any) => {
  const {toolHeight} = props;

  return (
    <Scrollbars style={{height: toolHeight}}>
      <Typography style={{padding:'15px'}}>
        <Title level={4}>欢迎使用批流一体平台</Title>
      </Typography>
    </Scrollbars>
  );
};

export default connect(({Studio}: { Studio: StateType }) => ({
  current: Studio.current,
  sql: Studio.sql,
  tabs: Studio.tabs,
  toolHeight: Studio.toolHeight,
}))(StudioHome);
