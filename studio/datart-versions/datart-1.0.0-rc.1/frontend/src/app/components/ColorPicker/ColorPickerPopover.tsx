import { Popover, PopoverProps } from 'antd';
import { FC, useCallback, useMemo, useState } from 'react';
import { SketchPickerProps } from 'react-color';
import { ColorPicker } from './ColorTag';
import SingleColorSelection from './SingleColorSelection';

interface ColorPickerPopoverProps {
  popoverProps?: PopoverProps;
  defaultValue?: string;
  size?: number;
  onSubmit?: (color) => void;
  onChange?: (color) => void;
  colorPickerClass?: string;
  colors?: SketchPickerProps['presetColors'];
}
export const ColorPickerPopover: FC<ColorPickerPopoverProps> = ({
  children,
  defaultValue,
  size,
  popoverProps,
  onSubmit,
  onChange,
  colorPickerClass,
}) => {
  const [visible, setVisible] = useState(false);
  const [color] = useState<string | undefined>(defaultValue);

  const onCancel = useCallback(() => {
    setVisible(false);
  }, []);
  const onColorChange = useCallback(
    color => {
      onSubmit?.(color);
      onChange?.(color);
      onCancel();
    },
    [onSubmit, onCancel, onChange],
  );
  const _popoverProps = useMemo(() => {
    return typeof popoverProps === 'object' ? popoverProps : {};
  }, [popoverProps]);
  return (
    <Popover
      {..._popoverProps}
      visible={visible}
      onVisibleChange={setVisible}
      content={<SingleColorSelection color={color} onChange={onColorChange} />}
      trigger="click"
      placement="right"
      className="color-picker"
    >
      {children || (
        <ColorPicker
          color={defaultValue}
          size={size}
          className={colorPickerClass}
        />
      )}
    </Popover>
  );
};
