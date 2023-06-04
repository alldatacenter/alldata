package datart.server.service;

import datart.core.data.provider.Dataframe;
import datart.core.data.provider.StdSqlOperator;
import datart.core.entity.Download;
import datart.core.entity.Share;
import datart.core.mappers.ext.ShareMapperExt;
import datart.server.base.dto.ShareInfo;
import datart.server.base.params.*;

import java.util.List;
import java.util.Set;

public interface ShareService extends BaseCRUDService<Share, ShareMapperExt> {

    ShareToken createShare(ShareCreateParam createParam);

    ShareToken createShare(String shareUser, ShareCreateParam createParam);

    ShareInfo updateShare(ShareUpdateParam updateParam);

    List<ShareInfo> listShare(String vizId);

    ShareVizDetail getShareViz(ShareToken shareToken);

    Dataframe execute(ShareToken shareToken, ViewExecuteParam executeParam) throws Exception;

    Download createDownload(String clientId, ShareDownloadParam downloadCreateParams);

    List<Download> listDownloadTask(ShareToken shareToken, String clientId);

    Download download(ShareToken shareToken, String downloadId);

    Set<StdSqlOperator> supportedStdFunctions(ShareToken shareToken, String sourceId);

}