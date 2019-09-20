/*
 * Copyright 2019 Huawei Technologies Co.,Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netflix.spinnaker.clouddriver.huaweicloud.provider.view

import com.fasterxml.jackson.databind.ObjectMapper
import com.netflix.spinnaker.cats.cache.Cache
import com.netflix.spinnaker.cats.cache.CacheData
import com.netflix.spinnaker.cats.cache.RelationshipCacheFilter
import com.netflix.spinnaker.clouddriver.model.InstanceProvider
import com.netflix.spinnaker.clouddriver.huaweicloud.HuaweiCloudProvider
import com.netflix.spinnaker.clouddriver.huaweicloud.cache.Keys
import com.netflix.spinnaker.clouddriver.huaweicloud.model.HuaweiCloudInstance
import com.netflix.spinnaker.clouddriver.huaweicloud.model.HuaweiCloudServerGroup
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import static com.netflix.spinnaker.clouddriver.huaweicloud.cache.Keys.Namespace.INSTANCES
import static com.netflix.spinnaker.clouddriver.huaweicloud.cache.Keys.Namespace.SERVER_GROUPS

@Component
class HuaweiCloudInstanceProvider implements InstanceProvider<HuaweiCloudInstance.View, String> {
  final String cloudProvider = HuaweiCloudProvider.ID

  final Cache cacheView
  final ObjectMapper objectMapper

  @Autowired
  HuaweiCloudInstanceProvider(Cache cacheView, ObjectMapper objectMapper) {
    this.cacheView = cacheView
    this.objectMapper = objectMapper
  }

  @Override
  HuaweiCloudInstance.View getInstance(String account, String region, String id) {

    CacheData cacheData = cacheView.get(
       INSTANCES.ns,
       Keys.getInstanceKey(id, account, region),
       RelationshipCacheFilter.include(SERVER_GROUPS.ns)
    )
    if (!cacheData) {
      return null
    }

    def serverGroupKeys = cacheData.relationships[SERVER_GROUPS.ns]
    if (!serverGroupKeys) {
      return null
    }

    CacheData serverGroupData = cacheView.get(SERVER_GROUPS.ns, serverGroupKeys[0])
    if (!serverGroupData) {
      return null
    }

    HuaweiCloudServerGroup serverGroup = objectMapper.convertValue(serverGroupData.attributes, HuaweiCloudServerGroup)
    HuaweiCloudInstance instance = serverGroup.instances.get(id)

    instance ? instance.view : null
  }

  @Override
  String getConsoleOutput(String account, String region, String id) {
    ""
  }
}
