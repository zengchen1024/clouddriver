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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.netflix.spinnaker.clouddriver.huaweicloud.provider.agent;

import static com.netflix.spinnaker.cats.agent.AgentDataType.Authority.AUTHORITATIVE;
import static com.netflix.spinnaker.clouddriver.huaweicloud.HuaweiCloudProvider.ID;
import static com.netflix.spinnaker.clouddriver.huaweicloud.cache.Keys.Namespace.SECURITY_GROUPS;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.huawei.openstack4j.openstack.vpc.v1.domain.SecurityGroup;
import com.huawei.openstack4j.openstack.vpc.v1.domain.SecurityGroupRule;
import com.netflix.spectator.api.Registry;
import com.netflix.spinnaker.cats.agent.AgentDataType;
import com.netflix.spinnaker.cats.provider.ProviderCache;
import com.netflix.spinnaker.clouddriver.cache.OnDemandAgent;
import com.netflix.spinnaker.clouddriver.cache.OnDemandMetricsSupport;
import com.netflix.spinnaker.clouddriver.huaweicloud.cache.CacheResultBuilder;
import com.netflix.spinnaker.clouddriver.huaweicloud.cache.CacheResultBuilder.NamespaceCache;
import com.netflix.spinnaker.clouddriver.huaweicloud.cache.Keys;
import com.netflix.spinnaker.clouddriver.huaweicloud.exception.HuaweiCloudException;
import com.netflix.spinnaker.clouddriver.huaweicloud.model.HuaweiCloudSecurityGroupCacheData;
import com.netflix.spinnaker.clouddriver.huaweicloud.security.HuaweiCloudNamedAccountCredentials;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

public class HuaweiCloudSecurityGroupCachingAgent extends AbstractOnDemandCachingAgent {

  private static final Logger log =
      LoggerFactory.getLogger(HuaweiCloudSecurityGroupCachingAgent.class);

  private final OnDemandMetricsSupport onDemandMetricsSupport;

  public HuaweiCloudSecurityGroupCachingAgent(
      HuaweiCloudNamedAccountCredentials credentials,
      ObjectMapper objectMapper,
      Registry registry,
      String region) {

    super(credentials, objectMapper, SECURITY_GROUPS.ns, region);

    this.onDemandMetricsSupport =
        new OnDemandMetricsSupport(
            registry, this, ID + ":" + OnDemandAgent.OnDemandType.SecurityGroup);
  }

  @Override
  public OnDemandMetricsSupport getMetricsSupport() {
    return this.onDemandMetricsSupport;
  }

  @Override
  public Collection<AgentDataType> getProvidedDataTypes() {
    return Collections.unmodifiableCollection(
        new ArrayList<AgentDataType>() {
          {
            add(AUTHORITATIVE.forType(SECURITY_GROUPS.ns));
          }
        });
  }

  @Override
  public String getAgentType() {
    return getAccountName() + "/" + region + "/" + this.getClass().getSimpleName();
  }

  @Override
  public String getOnDemandAgentType() {
    return getAgentType() + "-OnDemand";
  }

  @Override
  public boolean handles(OnDemandAgent.OnDemandType type, String cloudProvider) {
    return type == OnDemandAgent.OnDemandType.SecurityGroup && cloudProvider == ID;
  }

  @Override
  public OnDemandAgent.OnDemandResult handle(
      ProviderCache providerCache, Map<String, ? extends Object> data) {

    if (!(data.get("securityGroupName") != null
        && data.get("account") == this.getAccountName()
        && data.get("region") == region)) {
      return null;
    }

    return handle(providerCache, (String) data.get("securityGroupName"));
  }

  @Override
  Optional<Object> getResourceByName(String name) {
    List<SecurityGroup> groups = null;
    try {
      groups =
          getCloudClient().getSecurityGroups(region).stream()
              .filter(it -> it.getName() == name)
              .collect(Collectors.toList());
    } catch (Exception e) {
      throw new HuaweiCloudException(
          String.format(
              "Error loading security group with name=%s in region=%s, error=%s",
              name, region, e.getMessage()));
    }

    if (groups.size() == 1) {
      return Optional.of(groups.get(0));
    }

    log.warn(
        "There is no or more than one security groups with name={} in region={}", name, region);

    return Optional.empty();
  }

  @Override
  String getResourceCacheDataId(Object resource) {
    SecurityGroup seg = (SecurityGroup) resource;
    return Keys.getSecurityGroupKey(seg.getName(), seg.getId(), getAccountName(), region);
  }

  @Override
  Collection<String> getOnDemandKeysToEvict(ProviderCache providerCache, String name) {
    return providerCache.filterIdentifiers(
        SECURITY_GROUPS.ns, Keys.getSecurityGroupKey(name, "*", getAccountName(), region));
  }

  @Override
  void buildCurrentNamespaceCacheData(CacheResultBuilder cacheResultBuilder) {
    List<? extends SecurityGroup> securityGroups = getAllSecurityGroups();
    buildNamespaceCacheData(cacheResultBuilder, securityGroups, securityGroups);
  }

  @Override
  void buildSingleResourceCacheData(CacheResultBuilder cacheResultBuilder, Object resource) {
    List<SecurityGroup> securityGroups = new ArrayList(1);
    securityGroups.add((SecurityGroup) resource);

    buildNamespaceCacheData(cacheResultBuilder, securityGroups, getAllSecurityGroups());
  }

  private List<? extends SecurityGroup> getAllSecurityGroups() {
    try {
      return getCloudClient().getSecurityGroups(region);
    } catch (Exception e) {
      throw new HuaweiCloudException(
          String.format(
              "Error loading security groups in region=%s, error=%s", region, e.getMessage()));
    }
  }

  private void buildNamespaceCacheData(
      CacheResultBuilder cacheResultBuilder,
      List<? extends SecurityGroup> securityGroups,
      List<? extends SecurityGroup> allSecurityGroups) {

    NamespaceCache nsCache = cacheResultBuilder.getNamespaceCache(SECURITY_GROUPS.ns);

    TypeReference<Map<String, Object>> typeRef = new TypeReference<Map<String, Object>>() {};

    Map<String, String> groupId2CacheIds =
        allSecurityGroups.stream()
            .collect(Collectors.toMap(it -> it.getId(), it -> getResourceCacheDataId(it)));

    securityGroups.forEach(
        item -> {
          if (!groupId2CacheIds.containsKey(item.getId())) {
            throw new HuaweiCloudException(
                String.format(
                    "Can't find the security group(id=%s) in current security groups",
                    item.getId()));
          }

          Map<String, String> relevantSecurityGroups = new HashMap();

          List<SecurityGroupRule> rules = item.getSecurityGroupRules();
          if (rules != null && !rules.isEmpty()) {
            rules.forEach(
                rule -> {
                  String remoteGroupId = rule.getRemoteGroupId();

                  if (!StringUtils.isEmpty(remoteGroupId)) {

                    if (!groupId2CacheIds.containsKey(remoteGroupId)) {
                      throw new HuaweiCloudException(
                          String.format(
                              "Can't find the remote security group(id=%s) for rule of security group(id=%s)",
                              remoteGroupId, item.getId()));
                    }
                    relevantSecurityGroups.put(remoteGroupId, groupId2CacheIds.get(remoteGroupId));
                  }
                });
          }

          nsCache
              .getCacheDataBuilder(groupId2CacheIds.get(item.getId()))
              .setAttributes(
                  objectMapper.convertValue(
                      new HuaweiCloudSecurityGroupCacheData(item, relevantSecurityGroups),
                      typeRef));
        });
  }
}
