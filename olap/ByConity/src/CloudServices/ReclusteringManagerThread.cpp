#include <CloudServices/ReclusteringManagerThread.h>
#include <Storages/StorageCnchMergeTree.h>

namespace DB
{

ReclusteringManagerThread::ReclusteringManagerThread(ContextPtr context_, const StorageID & id)
    : ICnchBGThread(context_, CnchBGThreadType::Clustering, id)
{
}

ReclusteringManagerThread::~ReclusteringManagerThread()
{
    try
    {
        stop();
    }
    catch(...)
    {
        tryLogCurrentException(log, __PRETTY_FUNCTION__);
    }
}

void ReclusteringManagerThread::runImpl()
{
    try
    {
        auto istorage = getStorageFromCatalog();
        auto & storage = checkAndGetCnchTable(istorage);

        if (istorage->is_dropped)
        {
            LOG_DEBUG(log, "Table was dropped, no clustering job to start");
            return;
        }

        if (storage.isBucketTable())
            recluster_task_status = true;
    }
    catch (...)
    {
        tryLogCurrentException(log, __PRETTY_FUNCTION__);
    }
}

void ReclusteringManagerThread::clearData()
{
    recluster_task_status = false;
}

bool ReclusteringManagerThread::getTableReclusterStatus()
{
    return recluster_task_status;
}

}
