package datart.server.service;

import java.net.MalformedURLException;
import java.util.Set;

public interface CustomPluginService {

    Set<String> scanCustomChartPlugins() throws MalformedURLException;

}
