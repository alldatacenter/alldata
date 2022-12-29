import React from 'react';

let doc;
let win;
if (typeof document !== 'undefined') {
  doc = document;
}
if (typeof window !== 'undefined') {
  win = window;
}

export const FrameContext = React.createContext({ document: doc, window: win });

export const useFrame = () => React.useContext(FrameContext);

export const {
  Provider: FrameContextProvider,
  Consumer: FrameContextConsumer,
} = FrameContext;
