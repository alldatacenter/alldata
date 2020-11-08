import logoImage from "app/assets/flink_squirrel_200_color.png";
import React, { FC, useState, Dispatch, SetStateAction } from "react";
import { Button, ButtonGroup, Col, Navbar, NavbarBrand } from "reactstrap";
import styled from "styled-components/macro";
import { AddRuleModal } from "./AddRuleModal";
import { Rule } from "app/interfaces";

const AppNavbar = styled(Navbar)`
  && {
    z-index: 1;
    justify-content: flex-start;
    padding: 0;
  }
`;

const Logo = styled.img`
  max-height: 40px;
`;

const TransactionsCol = styled(Col)`
  border-right: 1px solid rgba(255, 255, 255, 0.125);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0.5em 15px;
`;

export const Header: FC<Props> = props => {
  const [modalOpen, setModalOpen] = useState(false);
  const openRuleModal = () => setModalOpen(true);
  const closeRuleModal = () => setModalOpen(false);
  const toggleRuleModal = () => setModalOpen(state => !state);

  const startTransactions = () => fetch("/api/startTransactionsGeneration").then();
  const stopTransactions = () => fetch("/api/stopTransactionsGeneration").then();

  const syncRules = () => fetch("/api/syncRules").then();
  const clearState = () => fetch("/api/clearState").then();
  const pushToFlink = () => fetch("/api/rules/pushToFlink").then();

  return (
    <>
      <AppNavbar color="dark" dark={true}>
        <TransactionsCol xs="2">
          <NavbarBrand tag="div">Live Transactions</NavbarBrand>
          <ButtonGroup size="sm">
            <Button color="success" onClick={startTransactions}>
              Start
            </Button>
            <Button color="danger" onClick={stopTransactions}>
              Stop
            </Button>
          </ButtonGroup>
        </TransactionsCol>

        <Col xs={{ size: 5, offset: 1 }}>
          <Button size="sm" color="primary" onClick={openRuleModal}>
            Add Rule
          </Button>

          <Button size="sm" color="warning" onClick={syncRules}>
            Sync Rules
          </Button>

          <Button size="sm" color="warning" onClick={clearState}>
            Clear State
          </Button>

          <Button size="sm" color="warning" onClick={pushToFlink}>
            Push to Flink
          </Button>
        </Col>

        <Col xs={{ size: 3, offset: 1 }} className="justify-content-end d-flex align-items-center">
          <NavbarBrand tag="div">Apache Flink - Fraud Detection Demo</NavbarBrand>
          <Logo src={logoImage} title="Apache Flink" />
        </Col>
      </AppNavbar>
      <AddRuleModal isOpen={modalOpen} toggle={toggleRuleModal} onClosed={closeRuleModal} setRules={props.setRules} />
    </>
  );
};

interface Props {
  setRules: Dispatch<SetStateAction<Rule[]>>;
}
