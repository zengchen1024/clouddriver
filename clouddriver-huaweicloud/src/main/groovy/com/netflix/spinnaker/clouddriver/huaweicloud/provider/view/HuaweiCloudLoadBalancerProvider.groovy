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
 *  See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netflix.spinnaker.clouddriver.huaweicloud.provider.view

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import com.netflix.spinnaker.cats.cache.Cache
import com.netflix.spinnaker.cats.cache.CacheData
import com.netflix.spinnaker.cats.cache.RelationshipCacheFilter
import com.netflix.spinnaker.clouddriver.model.LoadBalancerInstance
import com.netflix.spinnaker.clouddriver.model.LoadBalancerProvider
import com.netflix.spinnaker.clouddriver.model.LoadBalancerServerGroup
import com.netflix.spinnaker.clouddriver.model.ServerGroup
import com.netflix.spinnaker.clouddriver.huaweicloud.HuaweiCloudProvider
import com.netflix.spinnaker.clouddriver.huaweicloud.cache.Keys
import com.netflix.spinnaker.clouddriver.huaweicloud.model.HuaweiCloudLoadBalancer
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import static com.netflix.spinnaker.clouddriver.huaweicloud.cache.Keys.Namespace.LOAD_BALANCERS
import static com.netflix.spinnaker.clouddriver.huaweicloud.cache.Keys.Namespace.SERVER_GROUPS

@Slf4j
@Component
class HuaweiCloudLoadBalancerProvider implements LoadBalancerProvider<HuaweiCloudLoadBalancer.View> {

  final String cloudProvider = HuaweiCloudProvider.ID

  final Cache cacheView
  final ObjectMapper objectMapper
  final HuaweiCloudClusterProvider clusterProvider

  @Autowired
  HuaweiCloudLoadBalancerProvider(Cache cacheView,
                                  ObjectMapper objectMapper,
                                  HuaweiCloudClusterProvider clusterProvider) {
    this.cacheView = cacheView
    this.objectMapper = objectMapper
    this.clusterProvider = clusterProvider
  }

  @Override
  List<HuaweiCloudLoadBalancerItem> list() {
    getLoadBalancerItem(Keys.getLoadBalancerKey('*', '*', '*', '*'))
  }

  @Override
  LoadBalancerProvider.Item get(String name) {
    def result = getLoadBalancerItem(Keys.getLoadBalancerKey(name, '*', '*', '*'))
    result ? result[0] : null
  }

  @Override
  List<HuaweiCloudLoadBalancer.Details> byAccountAndRegionAndName(String account, String region, String name) {

    Collection<String> identifiers = cacheView.filterIdentifiers(
      LOAD_BALANCERS.ns, Keys.getLoadBalancerKey(name, '*', account, region)
    )
    Collection<CacheData> data = cacheView.getAll(
      LOAD_BALANCERS.ns, identifiers,
      RelationshipCacheFilter.include(SERVER_GROUPS.ns)
    )

    if (!data) {
      return []
    }

    Map<String, LoadBalancerServerGroup> serverGroups = buildServerGroup(
      getRelationshipsFromCache(data, SERVER_GROUPS.ns)
    )

    data.collect {CacheData cacheData ->
      buildDetailsFromCacheData(cacheData, serverGroups)
    }
  }

  @Override
  Set<HuaweiCloudLoadBalancer.View> getApplicationLoadBalancers(String application) {

    Collection<String> identifiers = cacheView.filterIdentifiers(
      LOAD_BALANCERS.ns,
      Keys.getLoadBalancerKey("$application-*", '*', '*', '*')
    )

    Collection<CacheData> data = cacheView.getAll(
      LOAD_BALANCERS.ns, identifiers,
      RelationshipCacheFilter.include(SERVER_GROUPS.ns)
    )

    if (!data) {
      return [] as Set
    }

    Map<String, LoadBalancerServerGroup> serverGroups = buildServerGroup(
      getRelationshipsFromCache(data, SERVER_GROUPS.ns)
    )

    data.collect {CacheData cacheData ->
      buildViewFromCacheData(cacheData, serverGroups)
    }?.toSet()
  }

  private Set<String> getRelationshipsFromCache(Collection<CacheData> data, String namespace) {
    data.collect {CacheData cacheData ->
      cacheData.relationships[namespace]
    }.flatten().findAll().toSet()
  }

  private Map<String, LoadBalancerServerGroup> buildServerGroup(Set<String> keys) {
    keys?.collectEntries { key ->
      Map<String, String> parts = Keys.parse(key)
      ServerGroup serverGroup = clusterProvider.getServerGroup(
        parts.account, parts.region, parts.name
      )

      if (!serverGroup) {
        return [(key): null]
      }

      LoadBalancerServerGroup loadBalancerServerGroup = new LoadBalancerServerGroup(
        name: serverGroup.name,
        account: parts.account,
        region: parts.region,
        isDisabled: serverGroup.isDisabled(),
        cloudProvider: HuaweiCloudProvider.ID
      )

      loadBalancerServerGroup.instances = serverGroup.instances?.collect { instance ->
        new LoadBalancerInstance(
          id: instance.name,
          name: instance.humanReadableName,
          zone: instance.zone,
          health: [state: instance.healthState?.toString()]
        )
      }?.toSet()

      return [(key): loadBalancerServerGroup]
    }
  }

  private HuaweiCloudLoadBalancer.View buildViewFromCacheData(CacheData cacheData, Map<String, LoadBalancerServerGroup> serverGroupMap) {

    Set<LoadBalancerServerGroup> serverGroups = cacheData.relationships[SERVER_GROUPS.ns]?.findResults { key ->
      serverGroupMap.get(key)
    }?.toSet()

    HuaweiCloudLoadBalancer loadBalancer = objectMapper.convertValue(cacheData.attributes, HuaweiCloudLoadBalancer)
    HuaweiCloudLoadBalancer.View view = loadBalancer.view
    view.serverGroups = serverGroups ?: [].toSet()

    view
  }

  private HuaweiCloudLoadBalancer.Details buildDetailsFromCacheData(CacheData cacheData, Map<String, LoadBalancerServerGroup> serverGroupMap) {

    Set<LoadBalancerServerGroup> serverGroups = cacheData.relationships[SERVER_GROUPS.ns]?.findResults { key ->
      serverGroupMap.get(key)
    }?.toSet()

    HuaweiCloudLoadBalancer loadBalancer = objectMapper.convertValue(cacheData.attributes, HuaweiCloudLoadBalancer)
    HuaweiCloudLoadBalancer.Details details = loadBalancer.details
    details.serverGroups = serverGroups ?: [].toSet()

    details
  }

  private List<HuaweiCloudLoadBalancerItem> getLoadBalancerItem(String pattern) {

    Collection<CacheData> data = cacheView.getAll(
      LOAD_BALANCERS.ns,
      cacheView.filterIdentifiers(LOAD_BALANCERS.ns, pattern),
      RelationshipCacheFilter.include(SERVER_GROUPS.ns)
    )

    if (!data) {
      return []
    }

    Map<String, LoadBalancerServerGroup> serverGroups = buildServerGroup(
      getRelationshipsFromCache(data, SERVER_GROUPS.ns)
    )

    Map<String, HuaweiCloudLoadBalancerItem> result = [:].withDefault {
      name -> new HuaweiCloudLoadBalancerItem(name: name)
    }

    data.each { CacheData cacheData ->
      def details = buildDetailsFromCacheData(cacheData, serverGroups)
      result.get(details.name)
        .getByAccount(details.account)
        .getByRegion(details.region).loadBalancers << details
    }

    result.values() as List
  }

  static class HuaweiCloudLoadBalancerItem implements LoadBalancerProvider.Item {
    Map<String, HuaweiCloudLoadBalancerAccount> loadBalacerByAccount = [:].withDefault {
      account -> new HuaweiCloudLoadBalancerAccount(name: account)
    }
    String name

    @JsonProperty("accounts")
    List<HuaweiCloudLoadBalancerAccount> getByAccounts() {
      loadBalacerByAccount.values() as List
    }

    HuaweiCloudLoadBalancerAccount getByAccount(String account) {
      loadBalacerByAccount.get(account)
    }
  }

  static class HuaweiCloudLoadBalancerAccount implements LoadBalancerProvider.ByAccount {
    Map<String, HuaweiCloudLoadBalancerByRegion> loadBalancerByRegion = [:].withDefault {
      region -> new HuaweiCloudLoadBalancerByRegion(name: region)
    }
    String name

    @JsonProperty("regions")
    List<HuaweiCloudLoadBalancerByRegion> getByRegions() {
      loadBalancerByRegion.values() as List
    }

    HuaweiCloudLoadBalancerByRegion getByRegion(String region) {
      loadBalancerByRegion.get(region)
    }
  }

  static class HuaweiCloudLoadBalancerByRegion implements LoadBalancerProvider.ByRegion {
    String name
    List<HuaweiCloudLoadBalancer.Details> loadBalancers = []
  }
}
