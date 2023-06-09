/*
 * Copyright 2016-2023 ClickHouse, Inc.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


/*
 * This file may have been modified by Bytedance Ltd. and/or its affiliates (“ Bytedance's Modifications”).
 * All Bytedance's Modifications are Copyright (2023) Bytedance Ltd. and/or its affiliates.
 */

#include "DiskDecorator.h"
#include <IO/ReadBufferFromFileBase.h>
#include <IO/WriteBufferFromFileBase.h>

namespace DB
{
DiskDecorator::DiskDecorator(const DiskPtr & delegate_) : delegate(delegate_)
{
}

UInt64 DiskDecorator::getID() const
{
    return delegate->getID();
}

const String & DiskDecorator::getName() const
{
    return delegate->getName();
}

ReservationPtr DiskDecorator::reserve(UInt64 bytes)
{
    return delegate->reserve(bytes);
}

const String & DiskDecorator::getPath() const
{
    return delegate->getPath();
}

UInt64 DiskDecorator::getTotalSpace() const
{
    return delegate->getTotalSpace();
}

UInt64 DiskDecorator::getAvailableSpace() const
{
    return delegate->getAvailableSpace();
}

UInt64 DiskDecorator::getUnreservedSpace() const
{
    return delegate->getUnreservedSpace();
}

UInt64 DiskDecorator::getKeepingFreeSpace() const
{
    return delegate->getKeepingFreeSpace();
}

bool DiskDecorator::exists(const String & path) const
{
    return delegate->exists(path);
}

bool DiskDecorator::isFile(const String & path) const
{
    return delegate->isFile(path);
}

bool DiskDecorator::isDirectory(const String & path) const
{
    return delegate->isDirectory(path);
}

size_t DiskDecorator::getFileSize(const String & path) const
{
    return delegate->getFileSize(path);
}

void DiskDecorator::createDirectory(const String & path)
{
    delegate->createDirectory(path);
}

void DiskDecorator::createDirectories(const String & path)
{
    delegate->createDirectories(path);
}

void DiskDecorator::clearDirectory(const String & path)
{
    delegate->clearDirectory(path);
}

void DiskDecorator::moveDirectory(const String & from_path, const String & to_path)
{
    delegate->moveDirectory(from_path, to_path);
}

DiskDirectoryIteratorPtr DiskDecorator::iterateDirectory(const String & path)
{
    return delegate->iterateDirectory(path);
}

void DiskDecorator::createFile(const String & path)
{
    delegate->createFile(path);
}

void DiskDecorator::moveFile(const String & from_path, const String & to_path)
{
    delegate->moveFile(from_path, to_path);
}

void DiskDecorator::replaceFile(const String & from_path, const String & to_path)
{
    delegate->replaceFile(from_path, to_path);
}

void DiskDecorator::copy(const String & from_path, const std::shared_ptr<IDisk> & to_disk, const String & to_path)
{
    delegate->copy(from_path, to_disk, to_path);
}

void DiskDecorator::listFiles(const String & path, std::vector<String> & file_names)
{
    delegate->listFiles(path, file_names);
}

std::unique_ptr<ReadBufferFromFileBase>
DiskDecorator::readFile(
    const String & path, const ReadSettings& settings) const
{
    return delegate->readFile(path, settings);
}

std::unique_ptr<WriteBufferFromFileBase>
DiskDecorator::writeFile(const String & path, const WriteSettings& settings)
{
    return delegate->writeFile(path, settings);
}

void DiskDecorator::removeFile(const String & path)
{
    delegate->removeFile(path);
}

void DiskDecorator::removeFileIfExists(const String & path)
{
    delegate->removeFileIfExists(path);
}

void DiskDecorator::removeDirectory(const String & path)
{
    delegate->removeDirectory(path);
}

void DiskDecorator::removeRecursive(const String & path)
{
    delegate->removeRecursive(path);
}

void DiskDecorator::removeSharedFile(const String & path, bool keep_s3)
{
    delegate->removeSharedFile(path, keep_s3);
}

void DiskDecorator::removeSharedRecursive(const String & path, bool keep_s3)
{
    delegate->removeSharedRecursive(path, keep_s3);
}

void DiskDecorator::setLastModified(const String & path, const Poco::Timestamp & timestamp)
{
    delegate->setLastModified(path, timestamp);
}

Poco::Timestamp DiskDecorator::getLastModified(const String & path)
{
    return delegate->getLastModified(path);
}

void DiskDecorator::setReadOnly(const String & path)
{
    delegate->setReadOnly(path);
}

void DiskDecorator::createHardLink(const String & src_path, const String & dst_path)
{
    delegate->createHardLink(src_path, dst_path);
}

void DiskDecorator::truncateFile(const String & path, size_t size)
{
    delegate->truncateFile(path, size);
}

Executor & DiskDecorator::getExecutor()
{
    return delegate->getExecutor();
}

SyncGuardPtr DiskDecorator::getDirectorySyncGuard(const String & path) const
{
    return delegate->getDirectorySyncGuard(path);
}

void DiskDecorator::onFreeze(const String & path)
{
    delegate->onFreeze(path);
}

void DiskDecorator::shutdown()
{
    delegate->shutdown();
}

void DiskDecorator::startup()
{
    delegate->startup();
}

void DiskDecorator::applyNewSettings(const Poco::Util::AbstractConfiguration & config, ContextPtr context)
{
    delegate->applyNewSettings(config, context);
}

}
