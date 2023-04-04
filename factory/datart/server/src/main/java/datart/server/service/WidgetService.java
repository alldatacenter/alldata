package datart.server.service;

import datart.core.entity.Widget;
import datart.core.mappers.WidgetMapper;
import datart.server.base.params.WidgetCreateParam;
import datart.server.base.params.WidgetUpdateParam;

import java.util.List;

public interface WidgetService extends BaseCRUDService<Widget, WidgetMapper> {

    List<Widget> createWidgets(List<WidgetCreateParam> createParams);

    boolean updateWidgets(List<WidgetUpdateParam> updateParams);

    boolean deleteWidgets(List<String> widgetIds);

}
