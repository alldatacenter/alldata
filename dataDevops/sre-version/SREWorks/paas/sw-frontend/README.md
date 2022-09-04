#### 环境准备
1. 安装nvm
```
  curl -o- https://raw.githubusercontent.com/creationix/nvm/v0.33.2/install.sh | bash
```
2. 安装 node,版本大于14
```
nvm install 8.9.1
nvm alias default 8.9.1
```

3.安装 npm install


```

#### 开始
1. 源码仓库

https://github.com/alibaba/sreworks       

2. 安装依赖         
```

  tnpm install
```

3. 调试应用         
```
tnpm start
```

<hr />     

#### 目录结构
.
├── assets
│   ├── deskstop
│   │   ├── deskstop_four.jpg
│   │   ├── deskstop_one.jpg
│   │   ├── deskstop_three.jpg
│   │   └── deskstop_two.jpg
│   └── img
│       ├── abm_logo.png
│       ├── aliyun-logo.svg
│       ├── bigdata.png
│       ├── logo-gif-line.gif
│       ├── logo-gif-out-nofont.png
│       ├── logo-white.png
│       ├── logo.png
│       └── no-data.png
├── components
│   ├── ABMLoading
│   │   └── index.js
│   ├── CardWrapper
│   │   └── index.jsx
│   ├── DateRange
│   │   └── index.js
│   ├── DraggableTabs
│   │   ├── index.js
│   │   └── index.scss
│   ├── Dynamic
│   │   └── index.js
│   ├── EditTableCut
│   │   ├── index.js
│   │   └── index.less
│   ├── EditableTable
│   │   ├── index.js
│   │   └── index.less
│   ├── EllipsisTip
│   │   └── index.js
│   ├── ErrorBoundary
│   │   └── index.js
│   ├── FormBuilder
│   │   ├── FormEditor.js
│   │   ├── FormEditorDemo.js
│   │   ├── FormElementFactory.js
│   │   ├── FormElementType.js
│   │   ├── FormItem
│   │   │   ├── AceViewer.js
│   │   │   ├── CascadeGroup.js
│   │   │   ├── DatePickerWrapper.js
│   │   │   ├── EnhancedInput.js
│   │   │   ├── FileUpload.js
│   │   │   ├── FileUploadNoDefer.js
│   │   │   ├── FileUploadSingle.js
│   │   │   ├── GridCheckBoxWrapper.js
│   │   │   ├── GroupFormItem.js
│   │   │   ├── HandleTag.js
│   │   │   ├── ImageUpload.js
│   │   │   ├── JSONEditor.js
│   │   │   ├── JSONSchemaItem.js
│   │   │   ├── OamWidgetItem.js
│   │   │   ├── PopoverAceEditor.js
│   │   │   ├── SelectInput.js
│   │   │   ├── SelectItemWrapper.js
│   │   │   ├── SliderItem.js
│   │   │   ├── TableItem.js
│   │   │   └── UserSelector.js
│   │   ├── SimpleForm.js
│   │   ├── SuperForm.js
│   │   └── api.js
│   ├── FullScreenTool
│   │   └── index.js
│   ├── GridCheckBox
│   │   ├── index.js
│   │   └── index.less
│   ├── JSXRender
│   │   └── index.js
│   ├── JsonEditor
│   │   └── index.js
│   ├── Login
│   │   ├── abm_logo.png
│   │   ├── account
│   │   │   └── formWrapper.js
│   │   ├── api.js
│   │   ├── bgback.webp
│   │   ├── formWrapper.js
│   │   ├── index.js
│   │   ├── index.less
│   │   ├── login.png
│   │   └── logo.png
│   ├── NotFound
│   │   └── index.js
│   ├── NoticeBoardBar
│   │   ├── index.js
│   │   └── index.less
│   ├── NotifyCenter
│   │   └── index.js
│   ├── PagingTable
│   │   ├── index.js
│   │   └── index.less
│   ├── ParameterMappingBuilder
│   │   ├── Parameter.js
│   │   ├── ParameterDefiner.js
│   │   ├── ParameterMappingForm.js
│   │   ├── ParameterMappingTree.js
│   │   └── index.js
│   ├── PopoverConfirm
│   │   └── index.js
│   ├── RenderFactory
│   │   ├── common
│   │   │   ├── ActionWrapper
│   │   │   │   └── index.js
│   │   │   ├── AntdSteps
│   │   │   │   └── index.js
│   │   │   ├── AntdTags
│   │   │   │   └── index.js
│   │   │   ├── Condition
│   │   │   │   └── index.js
│   │   │   ├── DateTime
│   │   │   │   └── index.js
│   │   │   └── NumberTooltip
│   │   │       └── index.js
│   │   └── index.js
│   ├── SRECron
│   │   ├── Cron
│   │   │   ├── README.md
│   │   │   ├── index.js
│   │   │   ├── index.stories.js
│   │   │   └── language
│   │   │       ├── cn.js
│   │   │       ├── en.js
│   │   │       ├── index.js
│   │   │       └── mo.js
│   │   └── index.js
│   ├── SRESearch
│   │   ├── components
│   │   │   ├── FlatList.js
│   │   │   ├── TabContentListRender.js
│   │   │   ├── TabsRender.js
│   │   │   └── WrapLogForComponent.js
│   │   ├── index.jsx
│   │   ├── main.scss
│   │   ├── preVersion.jsx
│   │   └── services
│   │       └── service.js
│   ├── SelectedTable
│   │   └── index.js
│   ├── SiderNavToggleBar
│   │   ├── NavSelectPanel.js
│   │   ├── index.js
│   │   └── index.less
│   └── TIcon
│       ├── iconfont.css
│       └── index.js
###  前端设计器
├── core
│   ├── Application.js
│   ├── Application.less
│   ├── designer
│   │   ├── editors
│   │   │   ├── BlockEditor.js
│   │   │   ├── DataSourceEditor.js
│   │   │   ├── ElementEditor.js
│   │   │   ├── FormBlockEditor.js
│   │   │   ├── FormEditor.js
│   │   │   ├── FormItemEditor.js
│   │   │   ├── FuctionEditor.js
│   │   │   ├── PageEditor.js
│   │   │   ├── index.less
│   │   │   └── settings
│   │   │       ├── ActionSetting.js
│   │   │       ├── BlockSetting.js
│   │   │       ├── CustomSetting.js
│   │   │       ├── FilterSetting.js
│   │   │       ├── ScreenSetting.js
│   │   │       ├── TabFilterSetting.js
│   │   │       ├── ToolbarSetting.js
│   │   │       ├── WidgetSetting.js
│   │   │       └── formCommon.less
│   │   └── workbench
│   │       ├── MenuNavigation.js
│   │       ├── NodeNavigation.js
│   │       ├── index.js
│   │       └── index.less
│   ├── framework
│   │   ├── ActionsRender.js
│   │   ├── FilterBar.js
│   │   ├── FilterForm.js
│   │   ├── LinksRender.js
│   │   ├── MixFilterBar.js
│   │   ├── OamAction.js
│   │   ├── OamActionBar.js
│   │   ├── OamContent.js
│   │   ├── OamContentMenuBar.js
│   │   ├── OamContentWithPathPanel.js
│   │   ├── OamCustomActionBar.js
│   │   ├── OamStepAction.js
│   │   ├── OamWidget.js
│   │   ├── OamWidgets.js
│   │   ├── TabToolBar.js
│   │   ├── ToolBar.js
│   │   ├── UserGuideRender.js
│   │   ├── components
│   │   │   ├── ContentDesigner.js
│   │   │   ├── ContentLayout.js
│   │   │   ├── ContentToolbar.js
│   │   │   ├── FluidContentLayout.js
│   │   │   ├── FluidContentLayoutDesigner.js
│   │   │   ├── RowContainerHandler.js
│   │   │   ├── RowSetting.js
│   │   │   ├── WidgetHandleCard.js
│   │   │   ├── WidgetPreviewCard.js
│   │   │   ├── WidgetRepository
│   │   │   │   ├── index.js
│   │   │   │   └── meta
│   │   │   │       ├── action.js
│   │   │   │       ├── block.js
│   │   │   │       ├── filter_bar.js
│   │   │   │       ├── filter_form.js
│   │   │   │       ├── filter_mix.js
│   │   │   │       ├── filter_single.js
│   │   │   │       ├── filter_tab.js
│   │   │   │       ├── grid_card.js
│   │   │   │       ├── icons
│   │   │   │       │   ├── action.svg
│   │   │   │       │   ├── block.svg
│   │   │   │       │   ├── filter-bar.svg
│   │   │   │       │   ├── filter-form.svg
│   │   │   │       │   ├── filter-mix.svg
│   │   │   │       │   ├── filter-single.svg
│   │   │   │       │   ├── filter_tab.svg
│   │   │   │       │   ├── grid_card.svg
│   │   │   │       │   ├── list.svg
│   │   │   │       │   ├── step_action.svg
│   │   │   │       │   ├── step_form.svg
│   │   │   │       │   └── table.svg
│   │   │   │       ├── list.js
│   │   │   │       ├── step_action.js
│   │   │   │       ├── step_form.js
│   │   │   │       └── table.js
│   │   │   ├── WidgetSelector.js
│   │   │   └── index.less
│   │   ├── core
│   │   │   ├── ActionPanel.js
│   │   │   ├── BuiltInWidgets.js
│   │   │   ├── DefaultItemToolbar.js
│   │   │   ├── JSXRender.js
│   │   │   ├── NodeContent.js
│   │   │   ├── PageContent.js
│   │   │   ├── WidgetCard.js
│   │   │   ├── WidgetLoader.js
│   │   │   ├── action
│   │   │   │   └── index.js
│   │   │   ├── block
│   │   │   │   └── index.js
│   │   │   ├── filter
│   │   │   │   └── index.js
│   │   │   ├── index.less
│   │   │   ├── toolbar
│   │   │   │   └── index.js
#### 内置组件区
│   │   │   └── widgets
│   │   │       ├── APITest
│   │   │       │   ├── icon.svg--- 组件icon
│   │   │       │   ├── index.js--- 组件逻辑
│   │   │       │   └── meta.js --- 组件元数据
│   │   │       ├── Alert
│   │   │       │   ├── icon.svg
│   │   │       │   ├── index.js
│   │   │       │   └── meta.js
│   │   │       ├── BChart
│   │   │       │   ├── icon.svg
│   │   │       │   ├── index.js
│   │   │       │   ├── index.less
│   │   │       │   └── meta.js
│   │   │       ├── BizAreaChart
│   │   │       │   ├── icon.svg
│   │   │       │   ├── index.js
│   │   │       │   └── meta.js
│   │   │       ├── BizRadarChart
│   │   │       │   ├── icon.svg
│   │   │       │   ├── index.js
│   │   │       │   └── meta.js
│   │   │       ├── BizStackedAreaChart
│   │   │       │   ├── icon.svg
│   │   │       │   ├── index.js
│   │   │       │   └── meta.js
│   │   │       ├── BizStackedBarChart
│   │   │       │   ├── icon.svg
│   │   │       │   ├── index.js
│   │   │       │   └── meta.js
│   │   │       ├── BizStackedColumnChart
│   │   │       │   ├── icon.svg
│   │   │       │   ├── index.js
│   │   │       │   └── meta.js
│   │   │       ├── BizTreemapChart
│   │   │       │   ├── icon.svg
│   │   │       │   ├── index.js
│   │   │       │   └── meta.js
│   │   │       ├── BuiltInBusiness
│   │   │       │   ├── icon.svg
│   │   │       │   ├── index.js
│   │   │       │   └── meta.js
│   │   │       ├── Bullet
│   │   │       │   ├── icon.svg
│   │   │       │   ├── index.js
│   │   │       │   └── meta.js
│   │   │       ├── Card
│   │   │       │   ├── icon.svg
│   │   │       │   ├── index.js
│   │   │       │   ├── index.less
│   │   │       │   └── meta.js
│   │   │       ├── CarouselComp
│   │   │       │   ├── icon.svg
│   │   │       │   ├── index.js
│   │   │       │   ├── index.less
│   │   │       │   └── meta.js
│   │   │       ├── Collapse
│   │   │       │   ├── icon.svg
│   │   │       │   ├── index.js
│   │   │       │   └── meta.js
│   │   │       ├── CopyRight
│   │   │       │   ├── icon.svg
│   │   │       │   ├── index.js
│   │   │       │   └── meta.js
│   │   │       ├── CustomComp
│   │   │       │   ├── icon.svg
│   │   │       │   ├── index.js
│   │   │       │   └── meta.js
│   │   │       ├── CustomRender
│   │   │       │   ├── icon.svg
│   │   │       │   ├── index.js
│   │   │       │   └── meta.js
│   │   │       ├── Dashboard
│   │   │       │   ├── icon.svg
│   │   │       │   ├── index.js
│   │   │       │   └── meta.js
│   │   │       ├── DescribtionCard
│   │   │       │   ├── icon.svg
│   │   │       │   ├── index.js
│   │   │       │   ├── index.less
│   │   │       │   └── meta.js
│   │   │       ├── Descriptions
│   │   │       │   ├── icon.svg
│   │   │       │   ├── index.js
│   │   │       │   └── meta.js
│   │   │       ├── Desktop
│   │   │       │   ├── icon.svg
│   │   │       │   ├── index.js
│   │   │       │   └── meta.js
│   │   │       ├── DisplayScreens
│   │   │       │   ├── ChartFactory.js
│   │   │       │   ├── ScreenOne
│   │   │       │   │   ├── ScreenHeader.js
│   │   │       │   │   ├── index.js
│   │   │       │   │   ├── index.less
│   │   │       │   │   └── screenHeader.less
│   │   │       │   ├── ScreenTwo
│   │   │       │   │   ├── ScreenHeader.js
│   │   │       │   │   ├── index.js
│   │   │       │   │   ├── index.less
│   │   │       │   │   └── screenHeader.less
│   │   │       │   ├── icon.svg
│   │   │       │   ├── index.js
│   │   │       │   └── meta.js
│   │   │       ├── FluidCharts
│   │   │       │   ├── icon.svg
│   │   │       │   ├── index.js
│   │   │       │   └── meta.js
│   │   │       ├── Grafana
│   │   │       │   ├── icon.svg
│   │   │       │   ├── index.js
│   │   │       │   ├── index.scss
│   │   │       │   └── meta.js
│   │   │       ├── HoverableCard
│   │   │       │   ├── icon.svg
│   │   │       │   ├── index.js
│   │   │       │   ├── index.less
│   │   │       │   └── meta.js
│   │   │       ├── Iframe
│   │   │       │   ├── icon.svg
│   │   │       │   ├── index.js
│   │   │       │   └── meta.js
│   │   │       ├── JsonEditor
│   │   │       │   ├── icon.svg
│   │   │       │   ├── index.js
│   │   │       │   └── meta.js
│   │   │       ├── List
│   │   │       │   ├── icon.svg
│   │   │       │   ├── index.js
│   │   │       │   ├── index.less
│   │   │       │   └── meta.js
│   │   │       ├── MarkdownComp
│   │   │       │   ├── icon.svg
│   │   │       │   ├── index.js
│   │   │       │   ├── index.less
│   │   │       │   └── meta.js
│   │   │       ├── PageHeaderLayout
│   │   │       │   ├── icon.svg
│   │   │       │   ├── index.js
│   │   │       │   ├── index.less
│   │   │       │   └── meta.js
│   │   │       ├── Pie
│   │   │       │   ├── icon.svg
│   │   │       │   ├── index.js
│   │   │       │   └── meta.js
│   │   │       ├── QuickLink
│   │   │       │   ├── icon.svg
│   │   │       │   ├── index.js
│   │   │       │   └── meta.js
│   │   │       ├── RankingList
│   │   │       │   ├── icon.svg
│   │   │       │   ├── index.js
│   │   │       │   ├── index.scss
│   │   │       │   └── meta.js
│   │   │       ├── SREAdmin
│   │   │       │   ├── icon.svg
│   │   │       │   ├── index.js
│   │   │       │   └── meta.js
│   │   │       ├── SRESearch
│   │   │       │   ├── icon.svg
│   │   │       │   ├── index.js
│   │   │       │   ├── index.less
│   │   │       │   └── meta.js
│   │   │       ├── SliderChart
│   │   │       │   ├── icon.svg
│   │   │       │   ├── index.js
│   │   │       │   └── meta.js
│   │   │       ├── StaticCard
│   │   │       │   ├── icon.svg
│   │   │       │   ├── index.js
│   │   │       │   ├── index.less
│   │   │       │   └── meta.js
│   │   │       ├── StaticPage
│   │   │       │   ├── icon.svg
│   │   │       │   ├── index.js
│   │   │       │   ├── index.less
│   │   │       │   └── meta.js
│   │   │       ├── StatisticCard
│   │   │       │   ├── icon.svg
│   │   │       │   ├── index.js
│   │   │       │   └── meta.js
│   │   │       ├── StatusList
│   │   │       │   ├── icon.svg
│   │   │       │   ├── index.js
│   │   │       │   ├── index.scss
│   │   │       │   └── meta.js
│   │   │       ├── StatusSummary
│   │   │       │   ├── icon.svg
│   │   │       │   ├── index.js
│   │   │       │   ├── index.scss
│   │   │       │   └── meta.js
│   │   │       ├── SwitchCard
│   │   │       │   ├── icon.svg
│   │   │       │   ├── index.js
│   │   │       │   ├── index.less
│   │   │       │   └── meta.js
│   │   │       ├── Tabs
│   │   │       │   ├── icon.svg
│   │   │       │   ├── index.js
│   │   │       │   ├── index.less
│   │   │       │   └── meta.js
│   │   │       ├── TaskCards
│   │   │       │   ├── index.js
│   │   │       │   └── index.less
│   │   │       ├── Timeline
│   │   │       │   ├── icon.svg
│   │   │       │   ├── index.js
│   │   │       │   └── meta.js
│   │   │       ├── WebDesignerWorkbench
│   │   │       │   ├── icon.svg
│   │   │       │   ├── index.js
│   │   │       │   └── meta.js
│   │   │       └── WelcomeCard
│   │   │           ├── icon.svg
│   │   │           ├── index.js
│   │   │           ├── index.less
│   │   │           └── meta.js
│   │   ├── index.js
│   │   ├── legacy
│   │   │   └── widgets
│   │   │       ├── ActionForm
│   │   │       │   └── index.js
│   │   │       ├── AsyncAntdAlert
│   │   │       │   └── index.js
│   │   │       ├── CommonKvList
│   │   │       │   ├── index.js
│   │   │       │   └── index.scss
│   │   │       ├── CompositionCard
│   │   │       │   ├── index.js
│   │   │       │   └── index.less
│   │   │       ├── CompositionTabs
│   │   │       │   ├── index.js
│   │   │       │   └── index.less
│   │   │       ├── CompositionWidget
│   │   │       │   └── CompositionWidget.js
│   │   │       ├── FilterBar
│   │   │       │   └── index.js
│   │   │       ├── GridCard
│   │   │       │   ├── index.js
│   │   │       │   └── index.scss
│   │   │       ├── Homepage
│   │   │       │   └── index.js
│   │   │       ├── MixFilterBar
│   │   │       │   └── index.js
│   │   │       ├── SimpleTable
│   │   │       │   ├── PagedTable
│   │   │       │   │   └── index.js
│   │   │       │   ├── index.js
│   │   │       │   └── index.less
│   │   │       ├── TabFilter
│   │   │       │   └── index.js
│   │   │       ├── TaskCards
│   │   │       │   ├── index.js
│   │   │       │   └── index.less
│   │   │       ├── WebDesignerWorkbench
│   │   │       │   └── index.js
│   │   │       ├── WidgetFactory.js
│   │   │       └── flyadmin
│   │   │           ├── add-component
│   │   │           │   └── index.js
│   │   │           ├── component
│   │   │           │   ├── Editable.js
│   │   │           │   └── index.scss
│   │   │           ├── flyadmin-add-container
│   │   │           │   └── index.js
│   │   │           ├── flyadmin-add-package
│   │   │           │   ├── index.js
│   │   │           │   └── index.scss
│   │   │           ├── flyadmin-deploy
│   │   │           │   ├── deploy-component-detail.js
│   │   │           │   └── index.js
│   │   │           ├── global-configuration
│   │   │           │   └── index.js
│   │   │           ├── namespace
│   │   │           │   └── index.js
│   │   │           └── service.js
│   │   ├── model
│   │   │   ├── APIAction.js
│   │   │   ├── Action.js
│   │   │   ├── BaseModel.js
│   │   │   ├── Constants.js
│   │   │   ├── ContainerModel.js
│   │   │   ├── DataSource.js
│   │   │   ├── NodeModel.js
│   │   │   ├── PageModel.js
│   │   │   ├── WidgetMeta.js
│   │   │   └── WidgetModel.js
│   │   └── tabFilter
│   │       ├── index.js
│   │       └── index.less
│   ├── index.js
│   ├── interceptor.js
│   ├── services
│   │   ├── actionService.js
│   │   ├── appMenuTreeService.js
│   │   ├── appService.js
│   │   └── oamTreeService.js
│   └── style.less
├── index.js
├── index.less
### 公共布局
├── layouts
│   ├── BriefLayout
│   │   ├── Header.js
│   │   ├── PageContent.js
│   │   ├── index.js
│   │   └── index.less
│   ├── DefaultHeader.js
│   ├── DefaultLayout.js
│   ├── Home
│   │   ├── AppStore.js
│   │   ├── DesktopLayout.js
│   │   ├── HomeHeader.js
│   │   ├── HomeWorkspace.js
│   │   ├── WorkspaceSetting.js
│   │   ├── index.js
│   │   ├── index.less
│   │   ├── localImglist.js
│   │   └── workspaceSetting.less
│   ├── SearchBar
│   │   ├── index.js
│   │   └── index.less
│   └── common
│       ├── ContentWithMenus.js
│       ├── DropDownUser.js
│       ├── LeftMenus.js
│       ├── LeftSiderMenus.js
│       ├── NoticeBar.js
│       └── TopMenus.js
├── locales
│   ├── en_US.js
│   └── zh_MO.js
├── models
│   ├── global.js
│   ├── home.js
│   └── node.js
├── properties.js
### publicMedia 用作内置app logo 后端读取使用
├── publicMedia 
│   ├── aiops.png
│   ├── application.png
│   ├── cluster.png
│   ├── data.png
│   ├── dataops-framework.png
│   ├── desktop.png
│   ├── ding.jpg
│   ├── healing.png
│   ├── health.png
│   ├── help.png
│   ├── job.png
│   ├── ocenter.png
│   ├── search.png
│   ├── swadmin.png
│   ├── system.png
│   ├── team.png
│   ├── test.png
│   ├── upload.png
│   └── weixin.jpg
├── router.js
### themes 内置主题色
├── themes
│   ├── dark.less
│   ├── index.less
│   ├── light.less
│   └── navyblue.less
└── utils
    ├── BaseEntity.js
    ├── SafeEval.js
    ├── cacheRepository.js
    ├── cookie.js
    ├── eventBus.js
    ├── httpClient.js
    ├── loadChartData.js
    ├── localeHelper.js
    └── utils.js

147 directories, 481 files

