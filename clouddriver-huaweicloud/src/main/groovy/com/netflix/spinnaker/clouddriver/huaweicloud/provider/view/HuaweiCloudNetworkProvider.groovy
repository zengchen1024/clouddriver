/*
 * Copyright 2019 Huawei Technologies Co.,Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netflix.spinnaker.clouddriver.huaweicloud.provider.view

import com.fasterxml.jackson.databind.ObjectMapper
import com.netflix.spinnaker.cats.cache.Cache
import com.netflix.spinnaker.cats.cache.CacheData
import com.netflix.spinnaker.cats.cache.RelationshipCacheFilter
import com.netflix.spinnaker.clouddriver.model.NetworkProvider
import com.netflix.spinnaker.clouddriver.huaweicloud.HuaweiCloudProvider
import com.netflix.spinnaker.clouddriver.huaweicloud.cache.Keys
import com.netflix.spinnaker.clouddriver.huaweicloud.model.HuaweiCloudNetwork
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import static com.netflix.spinnaker.clouddriver.huaweicloud.cache.Keys.Namespace.NETWORKS

@Component
@Slf4j
class HuaweiCloudNetworkProvider implements NetworkProvider<HuaweiCloudNetwork>  {

  final Cache cacheView
  final ObjectMapper objectMapper

  String cloudProvider = HuaweiCloudProvider.ID

  @Autowired
  HuaweiCloudNetworkProvider(Cache cacheView, ObjectMapper objectMapper) {
    this.cacheView = cacheView
    this.objectMapper = objectMapper
  }

  @Override
  Set<HuaweiCloudNetwork> getAll() {
    Collection<String> filters = cacheView.filterIdentifiers(NETWORKS.ns, Keys.getNetworkKey('*', '*', '*'))
    Collection<CacheData> data = cacheView.getAll(NETWORKS.ns, filters, RelationshipCacheFilter.none())
    !data ? Sets.newHashSet() : data.collect(this.&fromCacheData)
  }

  HuaweiCloudNetwork fromCacheData(CacheData cacheData) {
    log.info("build network from cache data=${cacheData.attributes.network}")

    Map network = cacheData.attributes.network
    Map<String, String> parts = Keys.parse(cacheData.id)

    return new HuaweiCloudNetwork(
      cloudProvider: this.cloudProvider,
      id: parts.id,
      region: parts.region,
      account: parts.account,
      name: network.name
    )
  }
}
