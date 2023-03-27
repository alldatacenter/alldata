import {
  Button,
  ClipboardCopyButton,
  CodeBlock,
  CodeBlockAction,
  CodeBlockCode,
  Tooltip,
} from '@patternfly/react-core';
import {
  EyeIcon,
  EyeSlashIcon,
  FileDownloadIcon,
} from '@patternfly/react-icons';
import * as React from 'react';
import { useTranslation } from 'react-i18next';
import { maskPropertyValues } from 'shared';

export interface IJsonViewerProps {
  propertyValues: object;
}

const getJson = (properties, showHiddenFields) => {
  return showHiddenFields
    ? JSON.stringify(properties, null, 2)
    : JSON.stringify(maskPropertyValues(properties), null, 2);
};

export const JsonViewer: React.FC<IJsonViewerProps> = (props) => {
  let timer;

  const { t } = useTranslation();
  const [copied, setCopied] = React.useState<boolean>(false);
  const [showPassword, setShowPassword] = React.useState<boolean>(false);

  const downloadTooltipRef = React.useRef();
  const showTooltipRef = React.useRef();

  const clipboardCopyFunc = (event, text) => {
    const clipboard = event.currentTarget.parentElement;
    const el = document.createElement('textarea');
    el.value = text.toString();
    clipboard.appendChild(el);
    el.select();
    document.execCommand('copy');
    clipboard.removeChild(el);
  };

  const onClick = (event, text) => {
    if (timer) {
      window.clearTimeout(timer);
      setCopied(false);
    }
    clipboardCopyFunc(event, text);
    setCopied(true);
  };

  const downloadFile = async (event, data) => {
    const downloadJson = event.currentTarget.parentElement;
    const file = 'debeziumConfig.json';
    const json = data;
    const blob = new Blob([json], { type: 'application/json' });
    const href = await URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = href;
    link.download = file;
    downloadJson.appendChild(link);
    link.click();
    downloadJson.removeChild(link);
  };

  React.useEffect(() => {
    if (copied === true) {
      timer = window.setTimeout(() => {
        setCopied(false);
        timer = null;
      }, 1000);
    }
  }, [copied]);

  const actions = (
    <React.Fragment>
      <CodeBlockAction>
        <Button
          variant="plain"
          ref={showTooltipRef}
          aria-label={t('show hidden fields icon')}
          onClick={() => setShowPassword(!showPassword)}
        >
          {showPassword ? <EyeSlashIcon /> : <EyeIcon />}
        </Button>
        <Tooltip
          content={
            <div>{showPassword ? t('Hide password') : t('Show password')}</div>
          }
          reference={showTooltipRef}
        />
      </CodeBlockAction>
      <CodeBlockAction>
        <ClipboardCopyButton
          id="copy-button"
          textId="code-content"
          aria-label="Copy to clipboard"
          onClick={(e) =>
            onClick(e, getJson(props.propertyValues, showPassword))
          }
          exitDelay={600}
          maxWidth="110px"
          variant="plain"
        >
          {copied ? t('Successfully copied to clipboard!') : t('Copy to clipboard')}
        </ClipboardCopyButton>
      </CodeBlockAction>
      <CodeBlockAction>
        <Button
          variant="plain"
          ref={downloadTooltipRef}
          aria-label="Download icon"
          onClick={(e) =>
            downloadFile(e, getJson(props.propertyValues, showPassword))
          }
        >
          <FileDownloadIcon />
        </Button>
        <Tooltip
          content={<div>{t('Download JSON')}</div>}
          reference={downloadTooltipRef}
        />
      </CodeBlockAction>
    </React.Fragment>
  );

  return (
    <>
      <CodeBlock actions={actions}>
        <CodeBlockCode id="code-content">
          {getJson(props.propertyValues, showPassword)}
        </CodeBlockCode>
      </CodeBlock>
    </>
  );
};
