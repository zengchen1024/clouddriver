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
import com.netflix.spinnaker.clouddriver.model.Cluster
import com.netflix.spinnaker.clouddriver.model.ClusterProvider
import com.netflix.spinnaker.clouddriver.model.ServerGroup
import com.netflix.spinnaker.clouddriver.huaweicloud.HuaweiCloudProvider
import com.netflix.spinnaker.clouddriver.huaweicloud.cache.Keys
import com.netflix.spinnaker.clouddriver.huaweicloud.model.HuaweiCloudCluster
import com.netflix.spinnaker.clouddriver.huaweicloud.model.HuaweiCloudLoadBalancer
import com.netflix.spinnaker.clouddriver.huaweicloud.model.HuaweiCloudServerGroup
import java.util.stream.Collectors
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import static com.netflix.spinnaker.clouddriver.core.provider.agent.Namespace.APPLICATIONS
import static com.netflix.spinnaker.clouddriver.core.provider.agent.Namespace.CLUSTERS
import static com.netflix.spinnaker.clouddriver.core.provider.agent.Namespace.INSTANCES
import static com.netflix.spinnaker.clouddriver.core.provider.agent.Namespace.LOAD_BALANCERS
import static com.netflix.spinnaker.clouddriver.core.provider.agent.Namespace.SERVER_GROUPS

@Component
class HuaweiCloudClusterProvider implements ClusterProvider<HuaweiCloudCluster> {

  final Cache cacheView
  final ObjectMapper objectMapper
  final Closure<String> clusterAccountMapper = { Cluster it -> it.accountName }

  @Autowired
  HuaweiCloudClusterProvider(Cache cacheView, ObjectMapper objectMapper) {
    this.cacheView = cacheView
    this.objectMapper = objectMapper
  }

  @Override
  Map<String, Set<HuaweiCloudCluster>> getClusters() {

    Collection<CacheData> cacheResults = cacheView.getAll(CLUSTERS.ns)

    if (!cacheResults) {
      return null
    }

    cacheResults.stream()
      .map { CacheData cacheData ->
        new HuaweiCloudCluster(
          accountName: cacheData.attributes.accountName,
          name: cacheData.attributes.name,
          type: HuaweiCloudProvider.ID
        )
      }
      .collect(Collectors.groupingBy(this.&clusterAccountMapper, Collectors.toSet()))
  }

  @Override
  Map<String, Set<HuaweiCloudCluster>> getClusterSummaries(String application) {
    getClustersInternal(application, false)
  }

  @Override
  Map<String, Set<HuaweiCloudCluster>> getClusterDetails(String application) {
    getClustersInternal(application, true)
  }

  @Override
  Set<HuaweiCloudCluster> getClusters(final String application, final String account) {
    getClusterDetails(application)?.get(account)
  }

  @Override
  HuaweiCloudCluster getCluster(String application, String account, String name, boolean includeDetails) {
    getClusters(application, account)?.find { it.name == name }
  }

  @Override
  HuaweiCloudCluster getCluster(String application, String account, String name) {
    return getCluster(application, account, name, true)
  }

  @Override
  ServerGroup getServerGroup(String account, String region, String name, boolean includeDetails) {
    CacheData cacheData = cacheView.get(
      SERVER_GROUPS.ns, Keys.getServerGroupKey(name, account, region),
      RelationshipCacheFilter.include(INSTANCES.ns, LOAD_BALANCERS.ns)
    )

    if (!cacheData) {
      return null
    }

    serverGroupFromCacheData(cacheData)
  }

  @Override
  ServerGroup getServerGroup(String account, String region, String name) {
    return getServerGroup(account, region, name, true)
  }

  @Override
  String getCloudProviderId() {
    return HuaweiCloudProvider.ID
  }

  @Override
  boolean supportsMinimalClusters() {
    return false
  }

  protected Map<String, Set<HuaweiCloudCluster>> getClustersInternal(String applicationName, boolean includeInstanceDetails) {

    CacheData application = cacheView.get(APPLICATIONS.ns, Keys.getApplicationKey(applicationName))

    if (!application) {
      return null
    }

    Collection<CacheData> clusters = cacheView.getAll(
      CLUSTERS.ns, application.relationships[CLUSTERS.ns],
      RelationshipCacheFilter.include(SERVER_GROUPS.ns))

    if (!clusters) {
      return null
    }

    clusters.stream()
      .map { this.clusterFromCacheData(it, includeInstanceDetails) }
      .collect(Collectors.groupingBy(this.&clusterAccountMapper, Collectors.toSet()))
  }

  protected HuaweiCloudCluster clusterFromCacheData(CacheData cacheData, boolean includeDetails) {

    HuaweiCloudCluster cluster = new HuaweiCloudCluster(
      accountName: cacheData.attributes.accountName,
      name: cacheData.attributes.name,
      type: HuaweiCloudProvider.ID
    )

    Collection<String> serverGroupKeys = cacheData.relationships[SERVER_GROUPS.ns]
    if (serverGroupKeys) {

      RelationshipCacheFilter filter = includeDetails ?
        RelationshipCacheFilter.include(LOAD_BALANCERS.ns, INSTANCES.ns) :
        RelationshipCacheFilter.include(LOAD_BALANCERS.ns)

      cacheView.getAll(SERVER_GROUPS.ns, serverGroupKeys, filter).each { CacheData serverGroupCacheData ->

        def serverGroup = serverGroupFromCacheData(serverGroupCacheData)
        if (serverGroup) {
          cluster.serverGroups.add(serverGroup)
        }

	def loadBalancer = loadBalancersFromCacheData(serverGroupCacheData)
        if (loadBalancer) {
      	  cluster.loadBalancers.addAll(loadBalancer)
        }
      }
    }

    cluster
  }

  protected ServerGroup serverGroupFromCacheData(final CacheData cacheData) {
    objectMapper.convertValue(cacheData.attributes, HuaweiCloudServerGroup)?.view
  }

  protected Set<HuaweiCloudLoadBalancer> loadBalancersFromCacheData(CacheData cacheData) {
    Collection<String> loadBalancerKeys = cacheData.relationships[LOAD_BALANCERS.ns]
    if (loadBalancerKeys) {
      return null
    }
    return null

    cacheView.getAll(LOAD_BALANCERS.ns, loadBalancerKeys).collect {
      //objectMapper.convertValue(it.attributes, HuaweiCloudLoadBalancer)
    }
  }
}
