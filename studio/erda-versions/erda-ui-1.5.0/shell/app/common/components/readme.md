# Erda 公共组件

## 规范

### 目录规范

每个组件放在独立的文件夹下，在 index 文件中提供默认导出，如果是有层级关系的，例如 Button.ButtonGroup 这种，参考 antd 的方式，把子组件挂载父组件上。

```
components
  - compA
    - index.tsx
    - index.scss
    - child.tsx
```

在 components/index 文件中，从对应组件的 index 导入组件和类型并导出。
这样配置编译插件后，可以转为如下形式做 treeShake。

```
import { CompA, CompB } from 'shell/common';
// 转为
import CompA from 'common/components/compA';
import CompB from 'common/components/compB';
```

### 命名规范

统一使用连字符形式，例如：`empty-holder`，避免使用多个大写字符，比如 `CRUDTable`，这样对应的目录名称就是 `c-r-u-d-table`，会很丑。

### 样式规范

因为只给 erda-ui 使用，样式文件直接在组件中导入。
组件内如果不需要对外提供 className、style 等 prop，则尽量使用 tailwind 的原子样式类。
为了统一样式风格，设置如间距、高度等尺寸时，参考现有的组件，一般分大、中、小三个级别，如果不对外提供 className，最好提供数值类型枚举，例如 mt={8}, 指 `margin-top: 8px`。

如果需要对外提供 className，组件内可能需要外部设置样式的地方，都提供一个 className，即使该类名实际没有样式，这样方便外部提供样式覆写。
并且这些地方不要使用 tailwind，因为目前原子类都是 !important 的。

样式类前缀统一为 `ec`，即 erda-component 的简写。

### 引用规范

引入其他 common 组件时，使用相对路径引入

### 测试规范

新增公共组件，需要同时提供单元测试。
修改公共组件，需要所有组件单元测试通过。
