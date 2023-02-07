export declare global {
  interface Track {
    code: string;
    type?: TrackEventType;
    params?: TrackEventParams;
    events?: (keyof HTMLElementEventMap)[];
  }

  type TrackEventType = 'CLK' | 'EXP';

  type TrackEventParamsWrapped = { [key: string]: any };

  type TrackEventParams = { [key: string]: string | number | boolean | Function | undefined };

  type TrackEventListenerDict = { [key: string]: EventListener };

  interface TrackSendEventPayload {
    eventCode: string;
    eventParams?: TrackEventParams;
    eventType?: TrackEventType;
  }
}
