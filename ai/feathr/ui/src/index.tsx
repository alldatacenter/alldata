import React from 'react'

import dayjs from 'dayjs'
import utc from 'dayjs/plugin/utc'
import ReactDOM from 'react-dom'
import { BrowserRouter as Router } from 'react-router-dom'

import App from './app'
import './site.css'

dayjs.extend(utc)

ReactDOM.render(
  <React.StrictMode>
    <Router>
      <App />
    </Router>
  </React.StrictMode>,
  document.getElementById('root')
)
