// Copyright (c) 2021 Terminus, Inc.
//
// This program is free software: you can use, redistribute, and/or modify
// it under the terms of the GNU Affero General Public License, version 3
// or later ("AGPL"), as published by the Free Software Foundation.
//
// This program is distributed in the hope that it will be useful, but WITHOUT
// ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
// FITNESS FOR A PARTICULAR PURPOSE.
//
// You should have received a copy of the GNU Affero General Public License
// along with this program. If not, see <http://www.gnu.org/licenses/>.

import { Table } from './table/table';
import ContractiveFilter from './contractive-filter/contractive-filter';
import ConfigurableFilter from './configurable-filter/configurable-filter';
import { Form } from './form/form';
import { FormModal } from './form-modal/form-modal';
import { Container, LRContainer, RowContainer } from './container/container';
import { Card } from './card/card';
import Button from './button/button';
import { Drawer } from './drawer/drawer';
import { ActionForm } from './action-form';
import NotFound from './not-found';
import { FileTree } from './file-tree/file-tree';
import { APIEditor } from './api-editor/api-editor';
import Radio from './radio/radio';
import SplitPage from './split-page/split-page';
import Tabs from './tabs/tabs';
import Title from './title/title';
import { SortDragGroupList } from './sort-drag-group/sort-drag-group';
import Panel from './panel/panel';
import Tags from './tags/tags';
import Popover from './popover/popover';
import EditList from './edit-list/edit-list';
import Breadcrumb from './breadcrumb/breadcrumb';
import SelectPro from './select-pro/select-pro';
import Input from './input/input';
import TreeSelect from './tree-select/tree-select';
import InfoPreview from './info-preview/info-preview';
import InputSelect from './input-select/input-select';
import Alert from './alert/alert';
import List from './list/list';
import ListNew from './list-new/list-new';
import LinearDistribution from './linear-distribution/linear-distribution';
import Text from './text/text';
import Icon from './icon/icon';
import EmptyHolder from './empty-holder/empty-holder';
import Image from './image/image';
import DropdownSelect from './dropdown-select/dropdown-select';
import TableGroup from './table-group/table-group';
import TextGroup from './text-group/text-group';
import Chart from './chart/chart';
import ChartDashboard from './chart-dashboard/chart-dashboard';
import Badge from './badge/badge';
import TiledFilter from './tiled-filter/tiled-filter';
import FileEditor from './file-editor/file-editor';
import Modal from './modal/modal';
import PieChart from './pie-chart/pie-chart';
import Grid from './grid/grid';
import DatePicker from './date-picker/date-picker';
import Dropdown from './dropdown/dropdown';
import MarkdownEditor from './markdown-editor/markdown-editor';
import { CardContainer, ChartContainer } from './card-container/card-container';
import CopyButton from './copy-button/copy-button';
import ComposeTable from './compose-table/compose-table';
import TextBlock from './text-block/text-block';
import TextBlockGroup from './text-block-group/text-block-group';
import BarChart from './bar-chart/bar-chart';
import Gantt from './gantt/gantt';
import SimpleChart from './simple-chart/simple-chart';
import ChartBlock from './chart-block/chart-block';
import Kanban from './kanban/kanban';
import RadioTabs from './radio-tabs/radio-tabs';
import TopN from './top-n';

export const containerMap = {
  Alert,
  Badge,
  Button,
  FormModal,
  Table,
  PieChart,
  Card,
  Container,
  RowContainer,
  LRContainer,
  NotFound,
  Form,
  SplitPage,
  ActionForm,
  ContractiveFilter,
  ConfigurableFilter,
  FileTree,
  Radio,
  Tabs,
  Title,
  Drawer,
  Panel,
  SortGroup: SortDragGroupList,
  Popover,
  APIEditor,
  EditList,
  Breadcrumb,
  SelectPro,
  Input,
  TreeSelect,
  InfoPreview,
  InputSelect,
  List,
  ListNew,
  Text,
  Tags,
  Icon,
  EmptyHolder,
  Image,
  DropdownSelect,
  Dropdown,
  TableGroup,
  TextGroup,
  LinearDistribution,
  Chart,
  ChartDashboard,
  TiledFilter,
  FileEditor,
  Modal,
  Grid,
  DatePicker,
  MarkdownEditor,
  CardContainer,
  ChartContainer,
  CopyButton,
  ComposeTable,
  TextBlock,
  TextBlockGroup,
  BarChart,
  Gantt,
  SimpleChart,
  ChartBlock,
  Kanban,
  RadioTabs,
  TopN,
};
