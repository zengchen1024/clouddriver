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

package com.netflix.spinnaker.clouddriver.huaweicloud.provider.agent

import com.huawei.openstack4j.openstack.vpc.v1.domain.Vpc
import com.netflix.spinnaker.cats.agent.AgentDataType
import com.netflix.spinnaker.cats.agent.CacheResult
import com.netflix.spinnaker.cats.provider.ProviderCache
import com.netflix.spinnaker.clouddriver.huaweicloud.cache.CacheResultBuilder
import com.netflix.spinnaker.clouddriver.huaweicloud.cache.Keys
import groovy.transform.InheritConstructors
import groovy.util.logging.Slf4j

import static com.netflix.spinnaker.cats.agent.AgentDataType.Authority.AUTHORITATIVE
import static com.netflix.spinnaker.clouddriver.huaweicloud.cache.Keys.Namespace.NETWORKS

@Slf4j
@InheritConstructors
class HuaweiCloudNetworkCachingAgent extends AbstractHuaweiCloudCachingAgent {

  final Set<AgentDataType> providedDataTypes = [
    AUTHORITATIVE.forType(NETWORKS.ns)
  ] as Set

  String agentType = "${accountName}/${region}/${HuaweiCloudNetworkCachingAgent.simpleName}"

  @Override
  CacheResult loadData(ProviderCache providerCache) {
    List<Vpc> vpcs = cloudClient.listVpcs(region)
    buildCacheResult(vpcs)
  }

  private CacheResult buildCacheResult(List<Vpc> vpcs) {
    log.info("Describing items in ${agentType}")

    def cacheResultBuilder = new CacheResultBuilder()
    def nscache = cacheResultBuilder.namespace(NETWORKS.ns)

    vpcs.each { Vpc vpc ->
      nscache.keep(Keys.getNetworkKey(vpc.id, accountName, region)).with {
        attributes.network = vpc
      }
    }

    log.info("Caching ${nscache.keepSize()} networks in ${agentType}")

    cacheResultBuilder.build()
  }
}
