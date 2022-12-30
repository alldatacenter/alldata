package datart.data.provider;

import datart.core.data.provider.Column;
import datart.core.data.provider.Dataframe;
import org.apache.http.HttpResponse;

import java.io.IOException;
import java.util.List;

public interface HttpResponseParser {

    Dataframe parseResponse(String targetPropertyName, HttpResponse response, List<Column> columns) throws IOException;

}
