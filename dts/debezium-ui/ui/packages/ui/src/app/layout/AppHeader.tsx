import {
  AboutModal,
  Brand,
  PageHeader,
  PageHeaderTools,
  TextContent,
  TextList,
  TextListItem,
} from '@patternfly/react-core';
import { OutlinedQuestionCircleIcon } from '@patternfly/react-icons';
import BrandLogo from 'assets/images/debezium_logo_300px.png';
import { KafkaConnectCluster } from 'components';
import * as React from 'react';
import { Link } from 'react-router-dom';

export interface IAppHeader {
  handleClusterChange: (clusterId: number) => void;
}

export const AppHeader: React.FC<IAppHeader> = (props) => {
  const [isModalOpen, setIsModalOpen] = React.useState(false);
  const handleModalToggle = () => setIsModalOpen(!isModalOpen);
  const BuildModal = () => (
    <AboutModal
      isOpen={isModalOpen}
      onClose={handleModalToggle}
      brandImageSrc={BrandLogo}
      brandImageAlt="Debezium"
      productName="Debezium UI"
    >
      <TextContent>
        {typeof process.env.COMMIT_HASH !== 'undefined' && (
          <TextList component="dl">
            <TextListItem component="dt">Build</TextListItem>
            <TextListItem component="dd">
              <a
                href={`https://github.com/debezium/debezium-ui/commit/${process.env.COMMIT_HASH}`}
                target="_blank"
              >
                {process.env.COMMIT_HASH}
              </a>
            </TextListItem>
          </TextList>
        )}
      </TextContent>
    </AboutModal>
  );
  const logoComponent = (
    <>
      <Link to="./">
        <Brand className="brandLogo" src={BrandLogo} alt="Debezium" />
      </Link>
      <KafkaConnectCluster handleChange={props.handleClusterChange} />
    </>
  );
  const headerTools = (
    <PageHeaderTools>
      {typeof process.env.COMMIT_HASH !== 'undefined' && (
        <>
          <OutlinedQuestionCircleIcon onClick={handleModalToggle} />
          <BuildModal />
        </>
      )}
    </PageHeaderTools>
  );

  return (
    <PageHeader
      logo={logoComponent}
      logoComponent={'div'}
      headerTools={headerTools}
    />
  );
};
