package datart.core.data.provider.processor;

import datart.core.base.processor.ExtendProcessor;
import datart.core.base.processor.ProcessorResponse;
import datart.core.data.provider.DataProviderSource;
import datart.core.data.provider.Dataframe;
import datart.core.data.provider.ExecuteParam;
import datart.core.data.provider.QueryScript;

public interface DataProviderPostProcessor extends ExtendProcessor {
    ProcessorResponse postRun(Dataframe frame, DataProviderSource config, QueryScript script, ExecuteParam executeParam);
}
