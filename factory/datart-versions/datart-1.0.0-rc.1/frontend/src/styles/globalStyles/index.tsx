import { Base } from './base';
import { Form } from './overwritten/form';
import { GlobalOverlays } from './overwritten/globalOverlays';
import { Hardcoded } from './overwritten/hardcoded';
import { Viz } from './overwritten/viz';
import { ReactSplit } from './reactSplit';

export function GlobalStyles() {
  return (
    <>
      <Base />
      <Hardcoded />
      <GlobalOverlays />
      <Form />
      <Viz />
      <ReactSplit />
    </>
  );
}
