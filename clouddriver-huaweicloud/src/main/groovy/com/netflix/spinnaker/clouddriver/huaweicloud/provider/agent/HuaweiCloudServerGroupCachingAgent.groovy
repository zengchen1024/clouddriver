/*
 * Copyright 2016 Target Inc.
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

import com.fasterxml.jackson.databind.ObjectMapper
import com.netflix.frigga.Names
import com.netflix.spectator.api.Registry
import com.netflix.spinnaker.cats.agent.AgentDataType
import com.netflix.spinnaker.cats.agent.CacheResult
import com.netflix.spinnaker.cats.cache.CacheData
import com.netflix.spinnaker.cats.provider.ProviderCache
import com.netflix.spinnaker.clouddriver.cache.OnDemandAgent
import com.netflix.spinnaker.clouddriver.cache.OnDemandMetricsSupport
import com.netflix.spinnaker.clouddriver.huaweicloud.cache.CacheResultBuilder
import com.netflix.spinnaker.clouddriver.huaweicloud.cache.Keys
import com.netflix.spinnaker.clouddriver.huaweicloud.security.HuaweiCloudNamedAccountCredentials
import com.huawei.openstack4j.model.scaling.ScalingGroup
import com.huawei.openstack4j.model.scaling.ScalingGroupInstance
import groovy.util.logging.Slf4j

import static com.netflix.spinnaker.cats.agent.AgentDataType.Authority.AUTHORITATIVE
import static com.netflix.spinnaker.cats.agent.AgentDataType.Authority.INFORMATIVE
import static com.netflix.spinnaker.clouddriver.cache.OnDemandAgent.OnDemandType.ServerGroup
import static com.netflix.spinnaker.clouddriver.huaweicloud.HuaweiCloudProvider.ID
import static com.netflix.spinnaker.clouddriver.huaweicloud.cache.Keys.Namespace.APPLICATIONS
import static com.netflix.spinnaker.clouddriver.huaweicloud.cache.Keys.Namespace.CLUSTERS
import static com.netflix.spinnaker.clouddriver.huaweicloud.cache.Keys.Namespace.INSTANCES
import static com.netflix.spinnaker.clouddriver.huaweicloud.cache.Keys.Namespace.LOAD_BALANCERS
import static com.netflix.spinnaker.clouddriver.huaweicloud.cache.Keys.Namespace.SERVER_GROUPS

@Slf4j
class HuaweiCloudServerGroupCachingAgent extends AbstractHuaweiCloudCachingAgent implements OnDemandAgent {

  final Set<AgentDataType> providedDataTypes = [
    AUTHORITATIVE.forType(SERVER_GROUPS.ns),
    INFORMATIVE.forType(APPLICATIONS.ns),
    INFORMATIVE.forType(CLUSTERS.ns),
    INFORMATIVE.forType(INSTANCES.ns),
    INFORMATIVE.forType(LOAD_BALANCERS.ns),
  ] as Set

  String agentType = "${accountName}/${region}/${HuaweiCloudServerGroupCachingAgent.simpleName}"
  String onDemandAgentType = "${agentType}-OnDemand"

  final OnDemandMetricsSupport metricsSupport

  HuaweiCloudServerGroupCachingAgent(HuaweiCloudNamedAccountCredentials credentials,
                                     ObjectMapper objectMapper,
                                     String region,
                                     Registry registry) {
    super(credentials, objectMapper, region)
    this.metricsSupport = new OnDemandMetricsSupport(registry, this, "${ID}:${ServerGroup}")
  }

  @Override
  CacheResult loadData(ProviderCache providerCache) {
    List<? extends ScalingGroup> groups = cloudClient.getScalingGroups(region)
    List<String> serverGroupKeys = groups.collect { Keys.getServerGroupKey(it.groupName, accountName, region) }

    OnDemandCacheUtils.buildLoadDataCache(providerCache, serverGroupKeys) { CacheResultBuilder cacheResultBuilder ->
      buildCacheResult(providerCache, cacheResultBuilder, groups)
    }
  }

  protected CacheResult buildCacheResult(ProviderCache providerCache, CacheResultBuilder cacheResultBuilder, List<? extends ScalingGroup> groups) {
    log.info("Describing items in ${agentType}")

    groups?.each { ScalingGroup item ->
      String serverGroupName = item.groupName
      Names names = Names.parseName(serverGroupName)
      if (!(names && names.app && names.cluster)) {
        log.info("Skipping server group ${serverGroupName}")
        return
      }

      String applicationName = names.app
      String clusterName = names.cluster

      String serverGroupKey = Keys.getServerGroupKey(serverGroupName, accountName, region)
      String clusterKey = Keys.getClusterKey(accountName, applicationName, clusterName)
      String appKey = Keys.getApplicationKey(applicationName)

      cacheResultBuilder.namespace(APPLICATIONS.ns).keep(appKey).with {
        attributes.name = applicationName
        relationships[CLUSTERS.ns].add(clusterKey)
      }

      cacheResultBuilder.namespace(CLUSTERS.ns).keep(clusterKey).with {
        attributes.name = clusterName
        attributes.accountName = accountName
        relationships[APPLICATIONS.ns].add(appKey)
        relationships[SERVER_GROUPS.ns].add(serverGroupKey)
      }

      // TODO add loadbalance

      List<String> instanceKeys = []
      List<? extends ScalingGroupInstance> instances = cloudClient.getScalingGroupInstances(region, item.groupId)
      instances?.each { ScalingGroupInstance instance ->
        String instanceKey = Keys.getInstanceKey(instance.instanceId, accountName, region)
        cacheResultBuilder.namespace(INSTANCES.ns).keep(instanceKey).relationships[SERVER_GROUPS.ns].add(serverGroupKey)
        instanceKeys.add(instanceKey)
      }

      if (OnDemandCacheUtils.shouldUseOnDemandData(cacheResultBuilder, serverGroupKey)) {
        OnDemandCacheUtils.moveOnDemandDataToNamespace(objectMapper, cacheResultBuilder, serverGroupKey)
      } else {
        cacheResultBuilder.namespace(SERVER_GROUPS.ns).keep(serverGroupKey).with {
          attributes.serverGroup = item 
          relationships[APPLICATIONS.ns].add(appKey)
          relationships[CLUSTERS.ns].add(clusterKey)
          // relationships[LOAD_BALANCERS.ns].addAll(loadBalancerKeys)
          relationships[INSTANCES.ns].addAll(instanceKeys)
        }
      }
    }

    cacheResultBuilder.namespaceBuilders.keySet().each { String namespace ->
      log.info("Caching ${cacheResultBuilder.namespace(namespace).keepSize()} ${namespace} in ${agentType}")
    }

    log.info("Caching ${cacheResultBuilder.onDemand.toKeep.size()} onDemand entries in ${agentType}")
    log.info("Evicting ${cacheResultBuilder.onDemand.toEvict.size()} onDemand entries in ${agentType}")

    cacheResultBuilder.build()
  }

  @Override
  boolean handles(OnDemandAgent.OnDemandType type, String cloudProvider) {
    type == ServerGroup && cloudProvider == ID
  }

  @Override
  OnDemandAgent.OnDemandResult handle(ProviderCache providerCache, Map<String, ? extends Object> data) {
    log.debug("Handling server group on-demand cache update; account=${accountName}, region=${region}, data=${data}")

    if (!(data.containsKey("serverGroupName") && data.account == accountName && data.region == region)) {
      return null
    }

    String name = data.serverGroupName.toString()

    ScalingGroup group = metricsSupport.readData {
      try {
        List<? extends ScalingGroup> groups = cloudClient.getScalingGroups(region).findAll { it.name == name }
        if (groups.size() == 1) {
          return groups.first()
        }

        log.warn("There is not only one scaling group with name=${name} in region=${region}")
      } catch (Exception e) {
        log.error("Failed to find scaling group with name=${name} in region=${region}", e)
      }
      return null
    }

    CacheResult cacheResult = metricsSupport.transformData {
      buildCacheResult(
        providerCache,
        new CacheResultBuilder(startTime: Long.MAX_VALUE),
        group ? [group] : [])
    }

    String key = Keys.getServerGroupKey(name, accountName, region)

    if (cacheResult.cacheResults.values().flatten().isEmpty()) {
      providerCache.evictDeletedItems(ON_DEMAND.ns, [key])
    } else {
      OnDemandCacheUtils.saveOnDemandCache(cacheResult, objectMapper, metricsSupport, providerCache, key)
    }

    log.info("On demand cache refresh succeeded. Data=${data}. Added ${group ? 1 : 0} server group to the cache.")

    def result = OnDemandAgent.OnDemandResult()
    result.sourceAgentType = onDemandAgentType
    result.cacheResult = cacheResult
    if (!group) {
      result.evictions[SERVER_GROUPS.ns] = [key]
    }

    return result
  }

  @Override
  Collection<Map> pendingOnDemandRequests(ProviderCache providerCache) {

    Collection<String> keys = providerCache.getIdentifiers(ON_DEMAND.ns).findAll { String key ->
      Map<String, String> parsedKey = Keys.parse(key)
      parsedKey && parsedKey.account == accountName && parsedKey.region == region
    }

    providerCache.getAll(ON_DEMAND.ns, keys).collect { CacheData cacheData ->
      def details = Keys.parse(cacheData.id)

      [
        details       : details,
        moniker       : convertOnDemandDetails(details),
        cacheTime     : cacheData.attributes.cacheTime,
        processedCount: cacheData.attributes.processedCount,
        processedTime : cacheData.attributes.processedTime
      ]
    }
  }
}
