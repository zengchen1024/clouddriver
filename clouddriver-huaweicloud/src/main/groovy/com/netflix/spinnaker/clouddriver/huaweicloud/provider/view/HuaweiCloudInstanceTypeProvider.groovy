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
import com.netflix.spinnaker.clouddriver.model.InstanceTypeProvider
import com.netflix.spinnaker.clouddriver.huaweicloud.cache.Keys
import com.netflix.spinnaker.clouddriver.huaweicloud.model.HuaweiCloudInstanceType
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
@Slf4j
class HuaweiCloudInstanceTypeProvider implements InstanceTypeProvider<HuaweiCloudInstanceType> {

  final Cache cacheView
  final ObjectMapper objectMapper

  @Autowired
  HuaweiCloudInstanceTypeProvider(Cache cacheView, ObjectMapper objectMapper) {
    this.cacheView = cacheView
    this.objectMapper = objectMapper
  }

  @Override
  Set<HuaweiCloudInstanceType> getAll() {
    Collection<CacheData> data = cacheView.getAll(
      Keys.Namespace.INSTANCE_TYPES.ns,
      cacheView.filterIdentifiers(Keys.Namespace.INSTANCE_TYPES.ns, Keys.getInstanceTypeKey('*', '*', '*')),
      RelationshipCacheFilter.none())

    log.info("build instance type from cache, has cache data?={}", data ? "yes" : "no")
    data ? data.collect(this.&fromCacheData) : [] as Set
  }

  HuaweiCloudInstanceType fromCacheData(CacheData cacheData) {
    Map flavor = cacheData.attributes.flavor
    Map<String, String> parts = Keys.parse(cacheData.id)

    return new HuaweiCloudInstanceType(
      region: parts.region,
      account: parts.account,
      name: flavor.name
    )
  }
}
