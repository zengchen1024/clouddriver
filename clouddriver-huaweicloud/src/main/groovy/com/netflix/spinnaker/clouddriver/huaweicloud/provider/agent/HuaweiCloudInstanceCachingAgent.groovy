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

import com.huawei.openstack4j.openstack.ecs.v1.domain.CloudServer
import com.netflix.spinnaker.cats.agent.AgentDataType
import com.netflix.spinnaker.cats.agent.CacheResult
import com.netflix.spinnaker.cats.provider.ProviderCache
import com.netflix.spinnaker.clouddriver.consul.provider.ConsulProviderUtils
import com.netflix.spinnaker.clouddriver.huaweicloud.cache.CacheResultBuilder
import com.netflix.spinnaker.clouddriver.huaweicloud.cache.Keys
import groovy.transform.InheritConstructors
import groovy.util.logging.Slf4j

import static com.netflix.spinnaker.cats.agent.AgentDataType.Authority.AUTHORITATIVE
import static com.netflix.spinnaker.clouddriver.huaweicloud.cache.Keys.Namespace.INSTANCES

@Slf4j
@InheritConstructors
class HuaweiCloudInstanceCachingAgent extends AbstractHuaweiCloudCachingAgent {

  final Set<AgentDataType> providedDataTypes = [
    AUTHORITATIVE.forType(INSTANCES.ns)
  ] as Set

  String agentType = "${accountName}/${region}/${HuaweiCloudInstanceCachingAgent.simpleName}"

  @Override
  CacheResult loadData(ProviderCache providerCache) {
    List<CloudServer> instances = cloudClient.getInstances(region)
    buildCacheResult(instances)
  }

  private CacheResult buildCacheResult(List<CloudServer> instances) {
    log.info("Describing items in ${agentType}")

    def cacheResultBuilder = new CacheResultBuilder()
    def nscache = cacheResultBuilder.namespace(INSTANCES.ns)

    instances?.each { CloudServer instance ->
      nscache.keep(Keys.getInstanceKey(instance.id, accountName, region)).with {
        attributes.instance = instance
      }
    }

    log.info("Caching ${nscache.keepSize()} instances in ${agentType}")

    cacheResultBuilder.build()
  }
}
