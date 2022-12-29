import { CommonFormTypes } from 'globalConstants';
import { createContext, useCallback, useState } from 'react';
import { BoardType } from '../../../DashBoardPage/pages/Board/slice/types';
import { VizType } from './slice/types';

export interface SaveFormModel {
  id?: string;
  name: string;
  boardType?: BoardType; //template
  config?: string;
  description?: string;
  parentId?: string | null;
  file?: FormData; //template
  subType?: string; //board
  avatar?: string; //datachart
}

interface SaveFormState {
  vizType: VizType;
  type: CommonFormTypes;
  visible: boolean;
  isSaveAs?: boolean;
  initialValues?: SaveFormModel;
  onSave: (values: SaveFormModel, onClose: () => void) => void;
  onAfterClose?: () => void;
}

interface SaveFormContextValue extends SaveFormState {
  onCancel: () => void;
  showSaveForm: (formState: SaveFormState) => void;
}

const saveFormContextValue: SaveFormContextValue = {
  vizType: 'FOLDER',
  type: CommonFormTypes.Add,
  visible: false,
  isSaveAs: false,
  onSave: () => {},
  onCancel: () => {},
  showSaveForm: () => {},
};
export const SaveFormContext = createContext(saveFormContextValue);
export const useSaveFormContext = (): SaveFormContextValue => {
  const [vizType, setVizType] = useState<VizType>('FOLDER');
  const [type, setType] = useState(CommonFormTypes.Add);
  const [visible, setVisible] = useState(false);
  const [initialValues, setInitialValues] = useState<
    undefined | SaveFormModel
  >();
  const [onSave, setOnSave] = useState(() => () => {});
  const [onAfterClose, setOnAfterClose] = useState(() => () => {});

  const onCancel = useCallback(() => {
    setVisible(false);
  }, [setVisible]);

  const showSaveForm = useCallback(
    ({
      vizType,
      type,
      visible,
      initialValues,
      onSave,
      onAfterClose,
    }: SaveFormState) => {
      setVizType(vizType);
      setType(type);
      setVisible(visible);
      setInitialValues(initialValues);
      setOnSave(() => onSave);
      setOnAfterClose(() => onAfterClose);
    },
    [],
  );

  return {
    vizType,
    type,
    visible,
    initialValues,
    onSave,
    onCancel,
    onAfterClose,
    showSaveForm,
  };
};
