/*
 * Copyright 2016 Target, Inc.
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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.netflix.spinnaker.clouddriver.huaweicloud.provider.agent

import com.fasterxml.jackson.databind.ObjectMapper
import com.huawei.openstack4j.openstack.vpc.v1.domain.SecurityGroup
import com.netflix.spectator.api.Registry
import com.netflix.spinnaker.cats.agent.AgentDataType
import com.netflix.spinnaker.cats.cache.CacheData
import com.netflix.spinnaker.cats.agent.CacheResult
import com.netflix.spinnaker.cats.provider.ProviderCache
import com.netflix.spinnaker.clouddriver.cache.OnDemandAgent
import com.netflix.spinnaker.clouddriver.cache.OnDemandMetricsSupport
import com.netflix.spinnaker.clouddriver.huaweicloud.cache.CacheResultBuilder
import com.netflix.spinnaker.clouddriver.huaweicloud.cache.Keys
import com.netflix.spinnaker.clouddriver.huaweicloud.security.HuaweiCloudNamedAccountCredentials
import groovy.util.logging.Slf4j

import static com.netflix.spinnaker.cats.agent.AgentDataType.Authority.AUTHORITATIVE
import static com.netflix.spinnaker.clouddriver.huaweicloud.cache.Keys.Namespace.ON_DEMAND
import static com.netflix.spinnaker.clouddriver.huaweicloud.cache.Keys.Namespace.SECURITY_GROUPS
import static com.netflix.spinnaker.clouddriver.huaweicloud.HuaweiCloudProvider.ID

@Slf4j
class HuaweiCloudSecurityGroupCachingAgent extends AbstractHuaweiCloudCachingAgent implements OnDemandAgent {

  final Set<AgentDataType> providedDataTypes = [
    AUTHORITATIVE.forType(SECURITY_GROUPS.ns)
  ] as Set

  String agentType = "${accountName}/${region}/${HuaweiCloudSecurityGroupCachingAgent.simpleName}"
  String onDemandAgentType = "${agentType}-OnDemand"

  final OnDemandMetricsSupport metricsSupport

  HuaweiCloudSecurityGroupCachingAgent(HuaweiCloudNamedAccountCredentials credentials,
                                       ObjectMapper objectMapper,
                                       String region,
                                       Registry registry) {
    super(credentials, objectMapper, region)
    this.metricsSupport = new OnDemandMetricsSupport(registry, this, "${ID}:${OnDemandAgent.OnDemandType.SecurityGroup}")
  }

  @Override
  boolean handles(OnDemandAgent.OnDemandType type, String cloudProvider) {
    type == OnDemandAgent.OnDemandType.SecurityGroup && cloudProvider == ID
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

  @Override
  CacheResult loadData(ProviderCache providerCache) {
    List<SecurityGroup> securityGroups = cloudClient.getSecurityGroups(region)
    List<String> keys = securityGroups.collect{ Keys.getSecurityGroupKey(it.name, it.id, accountName, region) }

    OnDemandCacheUtils.buildLoadDataCache(providerCache, keys) { CacheResultBuilder cacheResultBuilder ->
      buildCacheResult(cacheResultBuilder, securityGroups)
    }
  }

  @Override
  OnDemandAgent.OnDemandResult handle(ProviderCache providerCache, Map<String, ? extends Object> data) {
    log.debug("Handling security group on-demand cache update; account=${accountName}, region=${region}, data=${data}")

    if (!(data.containsKey('securityGroupName') && data.account == accountName && data.region == region)) {
      return null
    }

    String name = data.securityGroupName as String

    SecurityGroup securityGroup = metricsSupport.readData {
      try {
        List<SecurityGroup> groups = cloudClient.getSecurityGroups(region).findAll { it.name == name }
        if (groups.size() == 1) {
          return groups.first()
        }

        log.warn("There is not only one security group with name=${name} in region=${region}")
      } catch (Exception e) {
        log.error("Failed to find security group with name=${name} in region=${region}", e)
      }
      return null
    }

    CacheResult cacheResult = metricsSupport.transformData {
      buildCacheResult(
        new CacheResultBuilder(startTime: Long.MAX_VALUE),
        securityGroup ? [securityGroup] : []
      )
    }

    def result = new OnDemandAgent.OnDemandResult()
    result.sourceAgentType = onDemandAgentType
    result.cacheResult = cacheResult

    String key = Keys.getSecurityGroupKey(name, securityGroup ? securityGroup.id : '*', accountName, region)

    Collection<String> identifiers = []
    if (!securityGroup) {
      identifiers = providerCache.filterIdentifiers(SECURITY_GROUPS.ns, key)

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

    log.info("On demand cache refresh succeeded. Data=${data}. Add ${securityGroup ? 1 : 0} security group to cache.")

    if (!securityGroup) {
      result.evictions[SECURITY_GROUPS.ns] = identifiers
    }
    return result
  }

  protected CacheResult buildCacheResult(CacheResultBuilder cacheResultBuilder, List<SecurityGroup> securityGroups) {
    log.info("Describing items in ${agentType}")

    def nscache = cacheResultBuilder.namespace(SECURITY_GROUPS.ns)

    Map<String, String> allGroupKeys = securityGroups.collectEntries {
      [(it.id): Keys.getSecurityGroupKey(it.name, it.id, accountName, region)]
    }

    securityGroups.each { item ->
      log.debug("Caching security group for account ${accountName} in region ${region}: ${item}")

      String key = allGroupKeys.get(item.id)

      if (OnDemandCacheUtils.shouldUseOnDemandData(cacheResultBuilder, key)) {
        OnDemandCacheUtils.moveOnDemandDataToNamespace(objectMapper, cacheResultBuilder, key)
      } else {
        nscache.keep(key).with {
          attributes.securityGroup = item
          attributes.allGroupKeys = allGroupKeys
        }
      }
    }

    log.info("Caching ${nscache.keepSize()} security groups in ${agentType}")
    log.info("Caching ${cacheResultBuilder.onDemand.toKeep.size()} onDemand entries in ${agentType}")
    log.info("Evicting ${cacheResultBuilder.onDemand.toEvict.size()} onDemand entries in ${agentType}")

    cacheResultBuilder.build()
  }
}
