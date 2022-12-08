import { Alert, Rule } from "app/interfaces";
import LeaderLine from "leader-line";
import { flattenDeep } from "lodash/fp";
import { RefObject, useCallback, useEffect, useState } from "react";

export const useLines: UseLines = (transactionsRef, rules, alerts) => {
  const [lines, setLines] = useState<Line[]>([]);

  const updateLines = useCallback(() => {
    lines.forEach(line => {
      try {
        line.line.position();
      } catch {
        // nothing
      }
    });
  }, [lines]);

  useEffect(() => {
    const newLines = flattenDeep<Line>(
      rules.map(rule => {
        const hasAlert = alerts.some(alert => alert.ruleId === rule.id);

        const inputLine = new LeaderLine(transactionsRef.current, rule.ref.current, {
          color: hasAlert ? "#dc3545" : undefined,
          dash: { animation: true },
          endSocket: "left",
          startSocket: "right",
        }) as Line;

        const outputLines = alerts.reduce<Line[]>((acc, alert) => {
          if (alert.ruleId === rule.id) {
            return [
              ...acc,
              new LeaderLine(rule.ref.current, alert.ref.current, {
                color: "#fff",
                endPlugOutline: true,
                endSocket: "left",
                outline: true,
                outlineColor: "#dc3545",
                startSocket: "right",
              }) as Line,
            ];
          }
          return acc;
        }, []);

        return [inputLine, ...outputLines];
      })
    );

    setLines(newLines);

    return () => {
      newLines.forEach(line => line.line.remove());
    };
  }, [transactionsRef, rules, alerts]);

  return { lines, handleScroll: updateLines };
};

type UseLines = (
  transactionsRef: RefObject<HTMLDivElement>,
  rules: Rule[],
  alerts: Alert[]
) => {
  lines: Line[];
  handleScroll: () => void;
};

export interface Line {
  line: {
    color: string;
    position: () => void;
    remove: () => void;
  };
  ruleId: number;
}
