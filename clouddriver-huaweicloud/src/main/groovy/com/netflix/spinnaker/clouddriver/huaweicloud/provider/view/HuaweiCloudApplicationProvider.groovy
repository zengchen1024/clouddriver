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
import com.netflix.spinnaker.clouddriver.model.ApplicationProvider
import com.netflix.spinnaker.clouddriver.huaweicloud.HuaweiCloudProvider
import com.netflix.spinnaker.clouddriver.huaweicloud.cache.Keys
import com.netflix.spinnaker.clouddriver.huaweicloud.model.HuaweiCloudApplication
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import static com.netflix.spinnaker.clouddriver.huaweicloud.cache.Keys.Namespace.APPLICATIONS
import static com.netflix.spinnaker.clouddriver.huaweicloud.cache.Keys.Namespace.CLUSTERS

@Component
class HuaweiCloudApplicationProvider implements ApplicationProvider {
  final Cache cacheView
  final ObjectMapper objectMapper

  @Autowired
  HuaweiCloudApplicationProvider(final Cache cacheView, final ObjectMapper objectMapper) {
    this.cacheView = cacheView
    this.objectMapper = objectMapper
  }

  @Override
  Set<HuaweiCloudApplication> getApplications(boolean expand) {
    RelationshipCacheFilter filter =
      expand ? RelationshipCacheFilter.include(CLUSTERS.ns) : RelationshipCacheFilter.none()

    Collection<CacheData> data = cacheView.getAll(
      APPLICATIONS.ns,
      cacheView.filterIdentifiers(APPLICATIONS.ns, "${HuaweiCloudProvider.ID}:*"),
      filter
    )
    data.collect(this.&fromCacheData)
  }

  @Override
  HuaweiCloudApplication getApplication(String name) {
    CacheData data = cacheView.get(APPLICATIONS.ns, Keys.getApplicationKey(name))
    fromCacheData(data)
  }

  HuaweiCloudApplication fromCacheData(CacheData cacheData) {
    if (!cacheData) {
      return null
    }

    Map<String, Set<String>> clusterNames = [:].withDefault { [] as Set }

    for (String clusterId : cacheData.relationships[CLUSTERS.ns]) {
      Map<String, String> cluster = Keys.parse(clusterId)

      if (cluster.account && cluster.name) {
        clusterNames[cluster.account].add(cluster.name)
      }
    }

    return new HuaweiCloudApplication(
      name: Keys.parse(cacheData.id).application,
      attributes: [:],
      clusterNames:clusterNames
    )
  }
}
