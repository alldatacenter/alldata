/**
 * Created by wangkaihua on 2021/3/24.
 * @params
 * @options 默认格式
 *    id: 唯一标识
 *    name: 标题
 *    description: 描述
 *    logo: 图标
 * @defModel
 *  @isSingle boolean 单选模式
 *  @dataSource array 数据源
 *  @checked array 已选项
 *  @paramsMap Object 格式化返回值与组件值
 *    @paramsMap id: 唯一标识
 *    @paramsMap name: 标题
 *    @paramsMap description: 描述
 *    @paramsMap logo: 图标
 */
import React from "react";
import GridCheckBox from "../../GridCheckBox";


export default class GridCheckBoxWrapper extends React.Component {

  constructor(props) {
    super(props);
    let { onChange, value } = this.props;
    this.state = {
      dataSource: [],
    };
    onChange && onChange(value);
  }

  handleChange = (res) => {
    let { onChange } = this.props;
    onChange && onChange(res);
  };

  render() {
    const { value, model = {} } = this.props;
    let { defModel = {} } = model;
    return (
      <GridCheckBox dataSource={model.optionValues || []}
        {
        ...defModel
        }
        checked={value}
        onChange={this.handleChange} />
    );
  }
}