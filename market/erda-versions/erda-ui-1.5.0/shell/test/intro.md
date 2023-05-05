# Intro

## 测试方式：

### 单元测试

应用仔细拆分为一个一个组件，方法，然后针对这些方法进行单个单个的测试

### 集成测试

多个组件之间通过上下文共同进行测试

### E2E 测试

端到端测试，即模拟终端中的操作，比如页面跳转等
E2E 测试步骤:

1. 开启一个临时的 server
2. 用浏览器打开测试用的 html，其中含有测试 spec.js
3. spec.js 测试完毕后，把结果返回给 terminal
4. terminal 处理结果，结束。
   测试库有 Puppeteer（headless browser，用 Node 模拟浏览器环境，适合 chrome 测试）、cypress（有浏览器管理界面）

## 测试库简介：

1. Jest: React 官方推荐的测试框架，包含测试运行器、整理测试报告、基本断言库、代码覆盖率测试等功能
2. Enzyme: 虚拟 dom 渲染及操作库，可以浅渲染（不渲染子组件），提供 dom 模拟操作功能 [文档](https://airbnb.io/enzyme/docs/api/shallow.html)
3. jest-enzyme: 包含一些 dom 判断相关的断言，例如 `isExist()` 、 `toBeDisabled()`等，[常用断言](https://www.npmjs.com/package/jest-enzyme)
4. react-test-renderer: 虚拟 dom 渲染，可以生成 snapshot 进行快照测试

其他测试库：
mocha：测试运行器、测试报告整理、其他
chai：测试断言库，例如 `expect(state.params.a).to.equal('3');`

## 添加测试用例

添加测试用例时，新建`__tests__`目录，然后新建`name.test.tsx`文件放置测试用例
执行时`npm test`即可运行测试

## 单元测试示例

```tsx
import React from 'react';
import { JsonShow } from 'common';
/** 常用方法已在 test/helpers 文件中作为全局变量初始化，测试文件中无需再导入
global.mount = mount;
global.render = render;
global.shallow = shallow;

global.renderer = renderer;
*/

const json = {
  a: {
    b: 'hello',
  },
};
describe('JsonShow', () => {
  it('render with data', () => {
    const wrapper = shallow(<JsonShow data={null} />); // 浅渲染，不会渲染子组件和children
    expect(wrapper.find('pre')).toHaveText(''); // 期望pre内的字符串完全匹配''
    expect(wrapper.children().find('span').at(0)).toHaveText('JSON'); // 期望第一个span内字符串完全匹配'JSON'

    // console.log(wrapper.debug()); // 打印浅渲染出的html
    /**
      <div>
        <span style={{...}}>
          JSON
        </span>

        <Switch checked={false} onChange={[Function]} prefixCls="ant-switch" />
        <div className="json-detail ">
          <Button className="json-detail-btn for-copy" shape="circle" icon="copy" prefixCls="ant-btn" loading={false} ghost={false} block={false} />
          <Copy selector=".for-copy" opts={{...}} />
          <Button className="json-detail-btn" shape="circle" icon="close-circle-o" onClick={[Function: onClick]} prefixCls="ant-btn" loading={false} ghost={false} block={false} />
          <pre />
        </div>
      </div>
     */

    wrapper.setProps({ data: json, name: 'test' }); // 更新props
    expect(wrapper.find('pre')).toHaveText(JSON.stringify(json, null, 2)); // 期望更新props后的渲染也是正确的
    expect(wrapper.children().find('span').at(0)).toHaveText('test');
  });

  it('toggle visible by click button or switch', () => {
    const wrapper = mount(<JsonShow data={json} />); // 需要完全渲染组件，例如需要把当前组件内的Button渲染为最终的button，这样才能模拟触发click

    // const detailDom = wrapper.find('.json-detail'); // 存疑：不能使用变量保存对象，否则会不符合预期，可能是更新后会丢失引用
    expect(wrapper.find('.json-detail')).not.toHaveClassName('slide-in'); // .json-detail元素上不应有.slide-in样式类
    wrapper.find('.ant-switch').first().simulate('click'); // 触发click antd的switch
    expect(wrapper.find('.json-detail')).toHaveClassName('slide-in'); // .json-detail元素上应该有.slide-in样式类

    wrapper.find('.json-detail-btn').last().simulate('click');
    expect(wrapper.find('.json-detail')).not.toHaveClassName('slide-in');
  });
});

describe.skip('JsonShow Snapshot', () => {
  // 使用.skip跳过此测试用例，同样适用于test.skip
  test('renders', () => {
    const component = renderer.create(<JsonShow data={json} />); // 使用renderer渲染的可以生成snapshot
    const tree = component.toJSON();
    expect(tree).toMatchSnapshot(); // 期望snapshot和上一次的结果一致，如果不一致需要看更新是否正确，正确时按照输出提示更新snapshot，该文件夹应一起提交
  });
});
```

## 注意

mount 时类和渲染出的 dom 会同时存在，所以使用`at(index)`时需要注意

![image-20190110202554325](/Users/Jun/Library/Application Support/typora-user-images/image-20190110202554325.png)

参考文章：
https://www.robinwieruch.de/react-testing-tutorial
https://reactjs.org/community/testing.html
