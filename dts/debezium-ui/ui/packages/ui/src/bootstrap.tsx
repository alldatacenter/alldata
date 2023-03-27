import App from './app/app';
import './app/app.css';
import '@patternfly/patternfly/patternfly.css';
import '@patternfly/patternfly/utilities/Accessibility/accessibility.css';
import '@patternfly/patternfly/utilities/BackgroundColor/BackgroundColor.css';
import '@patternfly/patternfly/utilities/Display/display.css';
import '@patternfly/patternfly/utilities/Flex/flex.css';
import '@patternfly/patternfly/utilities/Sizing/sizing.css';
import '@patternfly/patternfly/utilities/Spacing/spacing.css';
import '@patternfly/patternfly/utilities/Text/text.css';
import '@patternfly/react-core/dist/styles/base.css';
import React from 'react';
import ReactDOM from 'react-dom';

ReactDOM.render(<App />, document.getElementById('root') as HTMLElement);
