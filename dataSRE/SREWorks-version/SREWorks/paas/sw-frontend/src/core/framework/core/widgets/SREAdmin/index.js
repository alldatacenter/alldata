import React from "react";
import OamWidget from "../../../OamWidget";

function SREAdmin(props) {
    let { widgetConfig = {}, widgetModel = {}, ...otherProps } = props;
    let { id } = widgetModel;
    let { businessConfig } = widgetConfig;
    return <OamWidget {...otherProps} widget={{ type: id, config: businessConfig }} />;

}

export default SREAdmin;