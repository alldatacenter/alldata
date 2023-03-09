import React, { lazy, ReactNode, Suspense } from 'react'

import type { RouteObject } from 'react-router-dom'

import Loading from '@/components/Loading'
import AppLayout from '@/layouts/AppLayout'

const Home = lazy(() => import('@/pages/Home'))

const Projects = lazy(() => import('@/pages/Projects'))

const ProjectLineage = lazy(() => import('@/pages/ProjectLineage'))

const DataSources = lazy(() => import('@/pages/DataSources'))

const NewSource = lazy(() => import('@/pages/NewSource'))

const DataSourceDetails = lazy(() => import('@/pages/DataSourceDetails'))

const Features = lazy(() => import('@/pages/Features'))

const NewFeature = lazy(() => import('@/pages/NewFeature'))

const FeatureDetails = lazy(() => import('@/pages/FeatureDetails'))

const Jobs = lazy(() => import('@/pages/Jobs'))

const Monitoring = lazy(() => import('@/pages/Monitoring'))

const Management = lazy(() => import('@/pages/Management'))

const RoleManagement = lazy(() => import('@/pages/RoleManagement'))

const Page403 = lazy(() => import('@/pages/403'))

const Page404 = lazy(() => import('@/pages/404'))

const lazyLoad = (children: ReactNode): ReactNode => {
  return <Suspense fallback={<Loading />}>{children}</Suspense>
}

export const routers: RouteObject[] = [
  {
    path: '/',
    element: <AppLayout />,
    children: [
      {
        index: true,
        path: '/',
        element: lazyLoad(<Home />)
      },
      {
        path: '/403',
        element: lazyLoad(<Page403 />)
      },
      {
        path: '/404',
        element: lazyLoad(<Page404 />)
      },
      {
        path: '/projects',
        element: lazyLoad(<Projects />)
      },
      {
        path: '/management',
        children: [
          {
            path: '',
            element: lazyLoad(<Management />)
          },
          {
            path: 'role',
            element: lazyLoad(<RoleManagement />)
          }
        ]
      },
      {
        path: ':project',
        children: [
          {
            path: '',
            element: lazyLoad(<Home />)
          },
          {
            path: 'lineage',
            element: lazyLoad(<ProjectLineage />)
          },
          {
            path: 'dataSources',
            children: [
              {
                path: '',
                element: lazyLoad(<DataSources />)
              },
              {
                path: 'new',
                element: lazyLoad(<NewSource />)
              },
              {
                path: ':id',
                element: lazyLoad(<DataSourceDetails />)
              }
            ]
          },
          {
            path: 'features',
            children: [
              {
                path: '',
                element: lazyLoad(<Features />)
              },
              {
                path: 'new',
                element: lazyLoad(<NewFeature />)
              },
              {
                path: ':id',
                element: lazyLoad(<FeatureDetails />)
              }
            ]
          },
          {
            path: 'jobs',
            element: lazyLoad(<Jobs />)
          },
          {
            path: 'monitoring',
            element: lazyLoad(<Monitoring />)
          }
        ]
      },
      {
        path: '*',
        element: lazyLoad(<Page404 />)
      }
    ]
  }
]
