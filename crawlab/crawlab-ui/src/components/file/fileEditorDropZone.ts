import {useDropzone} from 'crawlab-vue3-dropzone';

const useFileEditorDropZone = () => {
  const {
    getRootProps,
    getInputProps,
    open,
  } = useDropzone({});

  return {
    getRootProps,
    getInputProps,
    open,
  };
};

export default useFileEditorDropZone;
