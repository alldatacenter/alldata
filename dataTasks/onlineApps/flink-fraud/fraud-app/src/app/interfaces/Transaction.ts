// import { RefObject } from "react";

// MSG
// beneficiaryId: 42694
// eventTime: 1565965071385
// payeeId: 20908
// paymentAmount: 13.54
// paymentType: "CRD"
// transactionId: 5954524216210268000

export interface Transaction {
  beneficiaryId: number;
  eventTime: number;
  payeeId: number;
  paymentAmount: number;
  paymentType: string;
  transactionId: number;
}
