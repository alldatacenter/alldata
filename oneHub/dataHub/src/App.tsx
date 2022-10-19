import BaseRoutes from "@router/routers";
import { BrowserRouter } from "react-router-dom";
const MenuRouter: React.FC = () => {
  return (
    <BrowserRouter>
      <BaseRoutes />
    </BrowserRouter>
  );
};

export default MenuRouter;
