import React, {useState, useEffect, useRef} from 'react';
import ReactDOM from 'react-dom';
import ReactButterfly from 'butterfly-react';
import { BrowserRouter, HashRouter, Routes, Route } from 'react-router-dom';
import Related from './related'
import App from './app'
import Index from './index'
import Overview from './module-backend/page-overview'
import DependenceMap from './module-backend/page-dependence-map'
import DependencePackage from './module-backend/page-dependence-package'
import DependenceRequireMap from './module-backend/page-dependence-require-map'
import DependenceStartMap from './module-backend/page-dependence-start-map'
import DependencyStatics from './module-backend/page-statics'
import ProjectStatics from "./module-backend/page-project-statics";

import './index.less';
import './common/flexbox.css'
import './common/common.css'
import 'antd/dist/antd.less'

const rootElement = document.getElementById('bf-layout')
const ReactSample = () => {
  return (
    <HashRouter>
    <Routes>
      <Route path="/" element={<App />}>
        {/* 默认为后端 overview */}
        <Route index element={<Overview />} />
        <Route path={`/backend`} element={<Index />}>
          <Route index element={<Overview />} />
          <Route path="overview" element={<Overview />} />
          <Route path="dependence-map" element={<DependenceMap />} />
          <Route path="dependence-package" element={<DependencePackage />}>
            <Route path=":code" element={<DependencePackage />} />
          </Route>
          <Route path="dependence-require-map" element={<DependenceRequireMap />} />
          <Route path="dependence-start-map" element={<DependenceStartMap />} />
          <Route path="dependencyStatics" element={<DependencyStatics />} />
          <Route path="projectStatics" element={<ProjectStatics />} />
        </Route>
        <Route path="frontend" element={<Index />}>
          <Route index element={<Overview />} />
          <Route path="overview" element={<Overview />} />
          <Route path="dependence-map" element={<DependenceMap />} />
          <Route path="dependence-package" element={<DependencePackage />}>
            <Route path=":code" element={<DependencePackage />} />
          </Route>
          <Route path="dependencyStatics" element={<DependencyStatics />} />
          <Route path="projectStatics" element={<ProjectStatics />} />
        </Route>
        <Route
          path="*"
          element={
            <main style={{ padding: '1rem' }}>
              <p className="fc45">正在开发建设中，敬请期待～</p>
            </main>
          }
        />
      </Route>
    </Routes>
  </HashRouter>
   
  );
};

ReactDOM.render(<ReactSample />, rootElement)
