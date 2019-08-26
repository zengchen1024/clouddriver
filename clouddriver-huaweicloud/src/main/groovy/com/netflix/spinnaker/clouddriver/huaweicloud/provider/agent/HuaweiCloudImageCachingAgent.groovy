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

package com.netflix.spinnaker.clouddriver.huaweicloud.provider.agent

import com.huawei.openstack4j.openstack.ims.v2.domain.Image
import com.netflix.spinnaker.cats.agent.AgentDataType
import com.netflix.spinnaker.cats.agent.CacheResult
import com.netflix.spinnaker.cats.provider.ProviderCache
import com.netflix.spinnaker.clouddriver.huaweicloud.cache.CacheResultBuilder
import com.netflix.spinnaker.clouddriver.huaweicloud.cache.Keys
import groovy.transform.InheritConstructors
import groovy.util.logging.Slf4j

import static com.netflix.spinnaker.cats.agent.AgentDataType.Authority.AUTHORITATIVE
import static com.netflix.spinnaker.clouddriver.huaweicloud.cache.Keys.Namespace.IMAGES

@Slf4j
@InheritConstructors
class HuaweiCloudImageCachingAgent extends AbstractHuaweiCloudCachingAgent {

  final Set<AgentDataType> providedDataTypes = [
    AUTHORITATIVE.forType(IMAGES.ns)
  ] as Set

  String agentType = "${accountName}/${region}/${HuaweiCloudImageCachingAgent.simpleName}"

  @Override
  CacheResult loadData(ProviderCache providerCache) {
    List<Image> images = cloudClient.getImages(region)

    buildCacheResult(images)
  }

  private CacheResult buildCacheResult(List<Image> images) {
    log.info("Describing items in ${agentType}")

    def cacheResultBuilder = new CacheResultBuilder()
    def nscache = cacheResultBuilder.namespace(IMAGES.ns)

    images?.each { Image image ->
      nscache.keep(Keys.getImageKey(image.id, accountName, region)).with {
        attributes.image = image
      }
    }

    log.info("Caching ${nscache.keepSize()} images in ${agentType}")

    cacheResultBuilder.build()
  }
}
