import React from 'react'
import ReactDOM from 'react-dom/client'
import { BrowserRouter, HashRouter, Routes, Route } from 'react-router-dom'
import './index.css'
import App from './App'
import reportWebVitals from './reportWebVitals'
import PageSql from './page-sql'
import PageTaskManage from './page-task-manage'

const root = ReactDOM.createRoot(document.getElementById('root'));
root.render(
  <React.StrictMode>
    <HashRouter>
      <Routes>
        <Route path="/" element={<App />}>
          {/* 默认为后端 overview */}
          <Route index element={<PageSql />} />
          <Route path="sql" element={<PageSql />} />
          <Route path="task-manage" element={<PageTaskManage />} />
          {/* <Route path={`/backend`} element={<Index />}>
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
          </Route> */}
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
  </React.StrictMode>
);

// If you want to start measuring performance in your app, pass a function
// to log results (for example: reportWebVitals(console.log))
// or send to an analytics endpoint. Learn more: https://bit.ly/CRA-vitals
reportWebVitals();
