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

import com.fasterxml.jackson.databind.ObjectMapper
import com.huawei.openstack4j.model.network.ext.LoadBalancerV2
import com.huawei.openstack4j.model.network.ext.LoadBalancerV2StatusTree
import com.huawei.openstack4j.model.network.ext.status.LbPoolV2Status
import com.huawei.openstack4j.model.network.ext.status.MemberV2Status
import com.huawei.openstack4j.openstack.networking.domain.ext.NeutronLoadBalancerV2
import com.netflix.spectator.api.Registry
import com.netflix.spinnaker.cats.agent.AgentDataType
import com.netflix.spinnaker.cats.agent.CacheResult
import com.netflix.spinnaker.cats.cache.CacheData
import com.netflix.spinnaker.cats.provider.ProviderCache
import com.netflix.spinnaker.clouddriver.cache.OnDemandAgent
import com.netflix.spinnaker.clouddriver.cache.OnDemandAgent.OnDemandResult
import com.netflix.spinnaker.clouddriver.cache.OnDemandMetricsSupport
import com.netflix.spinnaker.clouddriver.huaweicloud.cache.CacheResultBuilder
import com.netflix.spinnaker.clouddriver.huaweicloud.cache.Keys
import com.netflix.spinnaker.clouddriver.huaweicloud.model.HuaweiCloudLoadBalancer
import com.netflix.spinnaker.clouddriver.huaweicloud.security.HuaweiCloudNamedAccountCredentials
import groovy.util.logging.Slf4j
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Future

import static com.netflix.spinnaker.cats.agent.AgentDataType.Authority.AUTHORITATIVE
import static com.netflix.spinnaker.cats.agent.AgentDataType.Authority.INFORMATIVE
import static com.netflix.spinnaker.clouddriver.cache.OnDemandAgent.OnDemandType.LoadBalancer
import static com.netflix.spinnaker.clouddriver.huaweicloud.HuaweiCloudProvider.ID
import static com.netflix.spinnaker.clouddriver.huaweicloud.cache.Keys.Namespace.INSTANCES
import static com.netflix.spinnaker.clouddriver.huaweicloud.cache.Keys.Namespace.LOAD_BALANCERS

@Slf4j
class HuaweiCloudLoadBalancerCachingAgent extends AbstractHuaweiCloudCachingAgent implements OnDemandAgent {

  Collection<AgentDataType> providedDataTypes = [
    AUTHORITATIVE.forType(LOAD_BALANCERS.ns),
    INFORMATIVE.forType(INSTANCES.ns)
  ] as Set

  String agentType = "${accountName}/${region}/${HuaweiCloudLoadBalancerCachingAgent.simpleName}"
  String onDemandAgentType = "${agentType}-OnDemand"

  final OnDemandMetricsSupport metricsSupport

  HuaweiCloudLoadBalancerCachingAgent(HuaweiCloudNamedAccountCredentials credentials,
                                      ObjectMapper objectMapper,
                                      String region,
                                      Registry registry) {
    super(credentials, objectMapper, region)
    this.metricsSupport = new OnDemandMetricsSupport(
      registry, this, "${ID}:${LoadBalancer}")
  }

  @Override
  CacheResult loadData(ProviderCache providerCache) {
    log.info("Describing items in ${agentType}")

    //Get all data in parallel to cut down on processing time
    Future<Set<? extends LoadBalancerV2>> loadBalancers = CompletableFuture.supplyAsync {
      cloudClient.getLoadBalancers(region)?.toSet()
    }

    Future<Map<String, ? extends LoadBalancerV2StatusTree>> statusTrees = loadBalancers.thenApplyAsync { lbs ->
      lbs.collectEntries { lb ->
        [(lb.id): cloudClient.getLoadBalancerStatusTree(region, lb.id)]
      }
    }

    CompletableFuture.allOf(loadBalancers, statusTrees).join()

    List<String> loadBalancerKeys = loadBalancers.get().collect {
      Keys.getLoadBalancerKey(it.name, it.id, accountName, region)
    }
    
    OnDemandCacheUtils.buildLoadDataCache(providerCache, loadBalancerKeys) { CacheResultBuilder cacheResultBuilder ->
      buildCacheResult(providerCache, loadBalancers.get(), statusTrees.get(), cacheResultBuilder)
    }
  }

  CacheResult buildCacheResult(ProviderCache providerCache,
                               Set<? extends LoadBalancerV2> loadBalancers,
                               Map<String, LoadBalancerV2StatusTree> statusTreeMap,
                               CacheResultBuilder cacheResultBuilder) {

    loadBalancers?.each { loadBalancer ->
      String loadBalancerKey = Keys.getLoadBalancerKey(loadBalancer.name, loadBalancer.id, accountName, region)

      Set<String> instanceIds = [] as Set
      statusTreeMap?.get(loadBalancer.id)?.loadBalancerV2Status?.pools?.each { LbPoolV2Status poolStatus ->
        poolStatus.memberStatuses?.each { MemberV2Status memberStatus ->
          instanceIds.add(memberStatus.id)
        }
      }

      instanceIds.each {
        String key = Keys.getInstanceKey(it, accountName, region)
        cacheResultBuilder.namespace(INSTANCES.ns).keep(key).with {
          relationships[LOAD_BALANCERS.ns].add(loadBalancerKey)
        }
      }

      if (OnDemandCacheUtils.shouldUseOnDemandData(cacheResultBuilder, loadBalancerKey)) {
        OnDemandCacheUtils.moveOnDemandDataToNamespace(objectMapper, cacheResultBuilder, loadBalancerKey)
      } else {

        HuaweiCloudLoadBalancer lb = new HuaweiCloudLoadBalancer(
          account: accountName,
          region: region,
          loadbalancer: loadBalancer as NeutronLoadBalancerV2
        )

        cacheResultBuilder.namespace(LOAD_BALANCERS.ns).keep(loadBalancerKey).with {
          attributes = objectMapper.convertValue(lb, ATTRIBUTES)
        }
      }
    }

    log.info("Caching ${cacheResultBuilder.namespace(LOAD_BALANCERS.ns).keepSize()} load balancers in ${agentType}")
    log.info("Caching ${cacheResultBuilder.namespace(INSTANCES.ns).keepSize()} instance relationships in ${agentType}")
    log.info("Caching ${cacheResultBuilder.onDemand.toKeep.size()} onDemand entries in ${agentType}")
    log.info("Evicting ${cacheResultBuilder.onDemand.toEvict.size()} onDemand entries in ${agentType}")

    cacheResultBuilder.build()
  }

  @Override
  boolean handles(OnDemandAgent.OnDemandType type, String cloudProvider) {
    type == LoadBalancer && cloudProvider == ID
  }

  @Override
  OnDemandResult handle(ProviderCache providerCache, Map<String, ? extends Object> data) {
    if (!(data.containsKey("loadBalancerName") && data.account == accountName && data.region == region)) {
      return null
    }

    String name = data.loadBalancerName.toString()

    LoadBalancerV2 loadbalancer = metricsSupport.readData {
      try {
        List<? extends LoadBalancerV2> loadbalancers = cloudClient.getLoadBalancers(region)?.findAll { it.name == name }
        if (loadbalancers.size() == 1) {
          return loadbalancers.first()
        }

        log.warn("There is not only one load balancer with name=${name} in region=${region}")
      } catch (Exception e) {
        log.error("Failed to find load balancer with name=${name} in region=${region}", e)
      }
      return null
    }

    Map<String, ? extends LoadBalancerV2StatusTree> statusMap = [:]
    if (loadbalancer) {
       statusMap[loadbalancer.id] = cloudClient.getLoadBalancerStatusTree(region, loadbalancer.id)
    }

    CacheResult cacheResult = metricsSupport.transformData {
      buildCacheResult(
        providerCache,
        loadbalancer ? [loadbalancer] : [],
        statusMap,
        new CacheResultBuilder(startTime: Long.MAX_VALUE)
      )
    }

    def result = new OnDemandResult()
    result.sourceAgentType = onDemandAgentType
    result.cacheResult = cacheResult

    String key = Keys.getSecurityGroupKey(name, loadbalancer ? loadbalancer.id : '*', accountName, region)

    Collection<String> identifiers = []
    if (!loadbalancer) {
      identifiers = providerCache.filterIdentifiers(LOAD_BALANCERS.ns, key)

      if (!(identifiers && identifiers.size() == 1)) {
        log.error("There is not only one identifier for key=${key}")
        return result
      }
    }

    if (cacheResult.cacheResults.values().flatten().isEmpty()) {
      providerCache.evictDeletedItems(ON_DEMAND.ns, identifiers)
    } else {
      OnDemandCacheUtils.saveOnDemandCache(cacheResult, objectMapper, metricsSupport, providerCache, key)
    }

    log.info("On demand cache refresh succeeded. Data=${data}. Add ${loadbalancer ? 1 : 0} security group to cache.")

    if (!loadbalancer) {
      result.evictions[LOAD_BALANCERS.ns] = identifiers
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
