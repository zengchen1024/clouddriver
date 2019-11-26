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

package com.netflix.spinnaker.clouddriver.huaweicloud.provider.view;

import static com.netflix.spinnaker.clouddriver.huaweicloud.cache.Keys.Namespace.SECURITY_GROUPS;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.spinnaker.cats.cache.Cache;
import com.netflix.spinnaker.cats.cache.CacheData;
import com.netflix.spinnaker.clouddriver.huaweicloud.HuaweiCloudProvider;
import com.netflix.spinnaker.clouddriver.huaweicloud.cache.Keys;
import com.netflix.spinnaker.clouddriver.huaweicloud.model.HuaweiCloudSecurityGroup;
import com.netflix.spinnaker.clouddriver.huaweicloud.model.HuaweiCloudSecurityGroupCacheData;
import com.netflix.spinnaker.clouddriver.model.AddressableRange;
import com.netflix.spinnaker.clouddriver.model.SecurityGroupProvider;
import com.netflix.spinnaker.clouddriver.model.securitygroups.IpRangeRule;
import com.netflix.spinnaker.clouddriver.model.securitygroups.Rule;
import com.netflix.spinnaker.clouddriver.model.securitygroups.SecurityGroupRule;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class HuaweiCloudSecurityGroupProvider
    implements SecurityGroupProvider<HuaweiCloudSecurityGroup> {

  private static final Logger log = LoggerFactory.getLogger(HuaweiCloudSecurityGroupProvider.class);

  private final ObjectMapper objectMapper;
  private final Cache cacheView;

  @Autowired
  public HuaweiCloudSecurityGroupProvider(Cache cacheView, ObjectMapper objectMapper) {
    this.cacheView = cacheView;
    this.objectMapper = objectMapper;
  }

  @Override
  public String getCloudProvider() {
    return HuaweiCloudProvider.ID;
  }

  @Override
  public Set<HuaweiCloudSecurityGroup> getAll(boolean includeRules) {
    Set<HuaweiCloudSecurityGroup> result =
        loadResults(Keys.getSecurityGroupKey("*", "*", "*", "*"), includeRules);

    log.debug("get all security group, return {} groups", result.size());
    return result;
  }

  @Override
  public Set<HuaweiCloudSecurityGroup> getAllByRegion(boolean includeRules, String region) {
    Set<HuaweiCloudSecurityGroup> result =
        loadResults(Keys.getSecurityGroupKey("*", "*", "*", region), includeRules);

    log.debug("get all security group by region={}, return {} groups", region, result.size());
    return result;
  }

  @Override
  public Set<HuaweiCloudSecurityGroup> getAllByAccount(boolean includeRules, String account) {
    Set<HuaweiCloudSecurityGroup> result =
        loadResults(Keys.getSecurityGroupKey("*", "*", account, "*"), includeRules);

    log.debug("get all security group by account={}, return {} groups", account, result.size());
    return result;
  }

  @Override
  public Set<HuaweiCloudSecurityGroup> getAllByAccountAndName(
      boolean includeRules, String account, String name) {
    Set<HuaweiCloudSecurityGroup> result =
        loadResults(Keys.getSecurityGroupKey(name, "*", account, "*"), includeRules);

    log.debug(
        "get all security group by account={} and group name={}, return {} groups",
        account,
        name,
        result.size());
    return result;
  }

  @Override
  public Set<HuaweiCloudSecurityGroup> getAllByAccountAndRegion(
      boolean includeRules, String account, String region) {
    Set<HuaweiCloudSecurityGroup> result =
        loadResults(Keys.getSecurityGroupKey("*", "*", account, region), includeRules);

    log.debug(
        "get all security group by account={} and region={}, return {} groups",
        account,
        region,
        result.size());
    return result;
  }

  @Override
  public HuaweiCloudSecurityGroup get(String account, String region, String name, String vpcId) {
    Set<HuaweiCloudSecurityGroup> result =
        loadResults(Keys.getSecurityGroupKey(name, "*", account, region), true);

    log.debug(
        "get all security group by account={}, region={}, group name={} and vpc id={}, return {} groups",
        account,
        region,
        name,
        vpcId,
        result.size());
    return result.stream().filter(it -> it.getVpcId() == vpcId).findFirst().orElse(null);
  }

  private Set<HuaweiCloudSecurityGroup> loadResults(String pattern, boolean includeRules) {
    Collection<CacheData> data =
        cacheView.getAll(
            SECURITY_GROUPS.ns, cacheView.filterIdentifiers(SECURITY_GROUPS.ns, pattern));

    if (data == null || data.isEmpty()) {
      return Collections.emptySet();
    }

    return data.stream()
        .map(cacheData -> this.fromCacheData(cacheData, includeRules))
        .filter(it -> it != null)
        .collect(Collectors.toSet());
  }

  private HuaweiCloudSecurityGroup fromCacheData(CacheData cacheData, boolean includeRules) {
    Map<String, String> parts = Keys.parse(cacheData.getId());
    if (parts == null) {
      return null;
    }

    HuaweiCloudSecurityGroupCacheData segCacheData =
        objectMapper.convertValue(
            cacheData.getAttributes(), HuaweiCloudSecurityGroupCacheData.class);

    return new HuaweiCloudSecurityGroup(
        parts.get("id"),
        parts.get("name"),
        parts.get("region"),
        parts.get("account"),
        parts.get("application"),
        segCacheData.getSecurityGroup().getVpcId(),
        includeRules ? buildInboundRules(segCacheData) : Collections.emptySet());
  }

  private Set<Rule> buildInboundRules(HuaweiCloudSecurityGroupCacheData segCacheData) {
    return segCacheData.getSecurityGroup().getSecurityGroupRules().stream()
        .filter(rule -> rule.getDirection() == "ingress")
        .map(
            rule -> {
              SortedSet portRanges = new TreeSet();
              portRanges.add(buildPortRange(rule));

              if (!StringUtils.isEmpty(rule.getRemoteIpPrefix())) {
                String[] parts = rule.getRemoteIpPrefix().split("/");
                String ip = parts[0];
                String cidr = "";
                if (parts.length > 1 && Integer.parseInt(parts[1]) != 32) {
                  cidr = rule.getRemoteIpPrefix();
                  ip = "";
                }

                return new IpRangeRule(
                    new AddressableRange(ip, cidr), rule.getProtocol(), portRanges);

              } else if (!StringUtils.isEmpty(rule.getRemoteGroupId())) {
                return SecurityGroupRule.builder()
                    .portRanges(portRanges)
                    .protocol(rule.getProtocol())
                    .securityGroup(
                        buildRemoteSecurityGroup(
                            segCacheData.getRelevantSecurityGroups().get(rule.getRemoteGroupId())))
                    .build();
              }

              return null;
            })
        .filter(it -> it != null)
        .collect(Collectors.toSet());
  }

  private Rule.PortRange buildPortRange(
      com.huawei.openstack4j.openstack.vpc.v1.domain.SecurityGroupRule rule) {
    Rule.PortRange portRange = new Rule.PortRange();

    if (rule.getProtocol() != "icmp") {
      portRange.setStartPort(
          rule.getPortRangeMin() != null ? rule.getPortRangeMin() : new Integer(1));

      portRange.setEndPort(
          rule.getPortRangeMax() != null ? rule.getPortRangeMax() : new Integer(65535));

      return portRange;
    }

    // there are two cases for icmp: both min and max are null or not.
    Integer startPort = rule.getPortRangeMin() != null ? rule.getPortRangeMin() : new Integer(0);
    Integer endPort = rule.getPortRangeMax() != null ? rule.getPortRangeMax() : new Integer(255);

    portRange.setStartPort(startPort <= endPort ? startPort : endPort);
    portRange.setEndPort(startPort <= endPort ? endPort : startPort);

    return portRange;
  }

  private HuaweiCloudSecurityGroup buildRemoteSecurityGroup(String cacheDataId) {
    if (StringUtils.isEmpty(cacheDataId)) {
      return null;
    }

    Map<String, String> parts = Keys.parse(cacheDataId);
    if (parts == null) {
      return null;
    }

    return new HuaweiCloudSecurityGroup(
        parts.get("id"),
        parts.get("name"),
        parts.get("region"),
        parts.get("account"),
        parts.get("application"),
        "",
        null);
  }
}
