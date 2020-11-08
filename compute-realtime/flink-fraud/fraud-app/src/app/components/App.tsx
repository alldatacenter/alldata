import { Header, Alerts, Rules, Transactions } from "app/components";
import { Rule, Alert } from "app/interfaces";
// import { useLines } from "app/utils/useLines";
import Axios from "axios";
import React, { createRef, FC, useEffect, useRef, useState } from "react";
import { Col, Container, Row } from "reactstrap";
import styled from "styled-components/macro";
import SockJsClient from "react-stomp";
import uuid from "uuid/v4";
import LeaderLine from "leader-line";
import { intersectionWith, find } from "lodash/fp";

import "../assets/app.scss";
import { Line } from "app/utils/useLines";

// edit for rule timeouts. (s * ms)
const RULE_TIMEOUT = 5 * 1000;

const LayoutContainer = styled.div`
  display: flex;
  flex-direction: column;
  width: 100%;
  max-height: 100vh;
  height: 100vh;
  overflow: hidden;
`;

export const ScrollingCol = styled(Col)`
  overflow-y: scroll;
  max-height: 100%;
  display: flex;
  flex-direction: column;
`;

export const App: FC = () => {
  const [rules, setRules] = useState<Rule[]>([]);
  const [alerts, setAlerts] = useState<Alert[]>([]);
  const [ruleLines, setRuleLines] = useState<Line[]>([]);
  const [alertLines, setAlertLines] = useState<Line[]>([]);

  const transactionsRef = useRef<HTMLDivElement>(null);
  // const { handleScroll } = useLines(transactionsRef, rules, alerts);

  useEffect(() => {
    Axios.get<Rule[]>("/api/rules").then(response =>
      setRules(response.data.map(rule => ({ ...rule, ref: createRef<HTMLDivElement>() })))
    );
  }, []);

  useEffect(() => {
    const newLines = rules.map(rule => {
      try {
        return {
          line: new LeaderLine(transactionsRef.current, rule.ref.current, {
            dash: { animation: true },
            endSocket: "left",
            startSocket: "right",
          }),
          ruleId: rule.id,
        };
      } catch (e) {
        return {
          line: {
            position: () => {},
            remove: () => {},
          },
          ruleId: rule.id,
        };
      }
    });

    setRuleLines(newLines);

    return () => newLines.forEach(line => line.line.remove());
  }, [rules]);

  useEffect(() => {
    const alertingRules = intersectionWith((rule, alert) => rule.id === alert.ruleId, rules, alerts).map(
      rule => rule.id
    );
    ruleLines.forEach(line => {
      try {
        line.line.color = alertingRules.includes(line.ruleId) ? "#dc3545" : "#ff7f50";
      } catch (e) {
        // nothing
      }
    });
  }, [rules, alerts, ruleLines]);

  useEffect(() => {
    const newLines = alerts.map(alert => {
      const rule = find(r => r.id === alert.ruleId, rules);

      return {
        line: new LeaderLine(rule!.ref.current, alert.ref.current, {
          color: "#fff",
          endPlugOutline: true,
          endSocket: "left",
          outline: true,
          outlineColor: "#dc3545",
          startSocket: "right",
        }),
        ruleId: rule!.id,
      };
    });

    setAlertLines(newLines);

    return () => newLines.forEach(line => line.line.remove());
  }, [alerts, rules]);

  const clearRule = (id: number) => () => setRules(rules.filter(rule => id !== rule.id));

  const clearAlert = (id: number) => () => {
    setAlerts(state => {
      const newAlerts = [...state];
      newAlerts.splice(id, 1);
      return newAlerts;
    });
  };

  const handleMessage = (alert: Alert) => {
    const alertId = uuid();
    const newAlert = {
      ...alert,
      alertId,
      ref: createRef<HTMLDivElement>(),
      timeout: setTimeout(() => setAlerts(state => state.filter(a => a.alertId !== alertId)), RULE_TIMEOUT),
    };

    setAlerts((state: Alert[]) => {
      const filteredState = state.filter(a => a.ruleId !== alert.ruleId);
      return [...filteredState, newAlert].sort((a, b) => (a.ruleId > b.ruleId ? 1 : -1));
    });
  };

  const handleLatencyMessage = (latency: string) => {
    // tslint:disable-next-line: no-console
    console.info(latency);
  };

  return (
    <>
      <SockJsClient url="/ws/backend" topics={["/topic/alerts"]} onMessage={handleMessage} />
      <SockJsClient url="/ws/backend" topics={["/topic/latency"]} onMessage={handleLatencyMessage} />
      <LayoutContainer>
        <Header setRules={setRules} />
        <Container fluid={true} className="flex-grow-1 d-flex w-100 flex-column overflow-hidden">
          <Row className="flex-grow-1 overflow-hidden">
            <Transactions ref={transactionsRef} />
            <Rules clearRule={clearRule} rules={rules} alerts={alerts} ruleLines={ruleLines} alertLines={alertLines} />
            <Alerts alerts={alerts} clearAlert={clearAlert} lines={alertLines} />
          </Row>
        </Container>
      </LayoutContainer>
    </>
  );
};
