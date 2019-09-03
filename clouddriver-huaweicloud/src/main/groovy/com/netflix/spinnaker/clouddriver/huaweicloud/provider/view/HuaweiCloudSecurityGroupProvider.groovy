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

package com.netflix.spinnaker.clouddriver.huaweicloud.provider.view

import com.fasterxml.jackson.databind.ObjectMapper
import com.netflix.spinnaker.cats.cache.Cache
import com.netflix.spinnaker.cats.cache.CacheData
import com.netflix.spinnaker.cats.cache.RelationshipCacheFilter
import com.netflix.spinnaker.clouddriver.model.AddressableRange
import com.netflix.spinnaker.clouddriver.model.SecurityGroupProvider
import com.netflix.spinnaker.clouddriver.model.securitygroups.IpRangeRule
import com.netflix.spinnaker.clouddriver.model.securitygroups.Rule
import com.netflix.spinnaker.clouddriver.model.securitygroups.SecurityGroupRule
import com.netflix.spinnaker.clouddriver.huaweicloud.HuaweiCloudProvider
import com.netflix.spinnaker.clouddriver.huaweicloud.cache.Keys
import com.netflix.spinnaker.clouddriver.huaweicloud.model.HuaweiCloudSecurityGroup
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import static com.netflix.spinnaker.clouddriver.huaweicloud.cache.Keys.Namespace.SECURITY_GROUPS

@Slf4j
@Component
class HuaweiCloudSecurityGroupProvider implements SecurityGroupProvider<HuaweiCloudSecurityGroup> {

  final String cloudProvider = HuaweiCloudProvider.ID
  final ObjectMapper objectMapper
  final Cache cacheView

  @Autowired
  HuaweiCloudSecurityGroupProvider(Cache cacheView, ObjectMapper objectMapper) {
    this.cacheView = cacheView
    this.objectMapper = objectMapper
  }

  @Override
  Set<HuaweiCloudSecurityGroup> getAll(boolean includeRules) {
    def result = loadResults(Keys.getSecurityGroupKey('*', '*', '*', '*'), includeRules)
    log.info("get all security group, return ${result ? result.size() : 0} groups")
    result
  }

  @Override
  Set<HuaweiCloudSecurityGroup> getAllByRegion(boolean includeRules, String region) {
    def result = loadResults(Keys.getSecurityGroupKey('*', '*', '*', region), includeRules)
    log.info("get all security group by region=${region}, return ${result ? result.size() : 0} groups")
    result
  }

  @Override
  Set<HuaweiCloudSecurityGroup> getAllByAccount(boolean includeRules, String account) {
    def result = loadResults(Keys.getSecurityGroupKey('*', '*', account, '*'), includeRules)
    log.info("get all security group by account=${account}, return ${result ? result.size() : 0} groups")
    result
  }

  @Override
  Set<HuaweiCloudSecurityGroup> getAllByAccountAndName(boolean includeRules, String account, String name) {
    def result = loadResults(Keys.getSecurityGroupKey(name, '*', account, '*'), includeRules)
    log.info("get all security group by account=${account} and group name=${name}, return ${result ? result.size() : 0} groups")
    result
  }

  @Override
  Set<HuaweiCloudSecurityGroup> getAllByAccountAndRegion(boolean includeRules, String account, String region) {
    def result = loadResults(Keys.getSecurityGroupKey('*', '*', account, region), includeRules)
    log.info("get all security group by account=${account} and region=${region}, return ${result ? result.size() : 0} groups")
    result
  }

  @Override
  HuaweiCloudSecurityGroup get(String account, String region, String name, String vpcId) {
    Set<HuaweiCloudSecurityGroup> result = loadResults(Keys.getSecurityGroupKey(name, '*', account, region), true)
    log.info("get all security group by account=${account}, region=${region} and group name=${name}, return ${result ? result.size() : 0} groups")

    if (!result) {
      return null
    }
    return result.find { it.vpcId == vpcId }
  }

  private Set<HuaweiCloudSecurityGroup> loadResults(String pattern, boolean includeRules) {

    Collection<CacheData> data = cacheView.getAll(
      SECURITY_GROUPS.ns,
      cacheView.filterIdentifiers(SECURITY_GROUPS.ns, pattern),
      RelationshipCacheFilter.none())

    if (!data) {
      return [] as Set
    }

    if (!includeRules) {
      return data.collect { this.fromCacheData(it, null) }.findAll()
    }

    return data.collect(this.&buildWithRules).findAll()
  }

  private HuaweiCloudSecurityGroup fromCacheData(CacheData cacheData, List<Rule> inboundRules) {
    Map<String, String> parts = Keys.parse(cacheData.id)
    if (!parts) {
      return null
    }

    return new HuaweiCloudSecurityGroup(
      type: this.cloudProvider,
      cloudProvider: this.cloudProvider,
      id: parts.id,
      name: parts.name,
      region: parts.region,
      accountName: parts.account,
      application: parts.application,
      vpcId: cacheData.attributes.securityGroup.vpc_id,
      inboundRules: inboundRules ? inboundRules as Set : [] as Set
    )
  }

  private HuaweiCloudSecurityGroup buildWithRules(CacheData cacheData) {
    Map allGroupKeys = cacheData.attributes.allGroupKeys

    List<Rule> inboundRules = []
    Map data = cacheData.attributes.securityGroup
    data.security_group_rules.each { Map rule ->
      if (rule.direction == "ingress") {

        if (rule.remote_ip_prefix) {
          def parts = rule.remote_ip_prefix.split('/')
          String ip = parts[0]
          String cidr = ""
          if (parts.size() > 1 && (parts[1] as Integer) != 32) {
            cidr = rule.remote_ip_prefix
            ip = ""
          }

          inboundRules << new IpRangeRule(
            portRanges: [buildPortRange(rule)] as SortedSet,
            protocol: rule.protocol,
            range: new AddressableRange(ip: ip, cidr: cidr)
          )

        } else if (rule.remote_group_id) {
          inboundRules << new SecurityGroupRule(
            portRanges: [buildPortRange(rule)] as SortedSet,
            protocol: rule.protocol,
            securityGroup: buildRemoteSecurityGroup(allGroupKeys.get(rule.remote_group_id, null))
          )
        }
      }
    }

    return fromCacheData(cacheData, inboundRules)
  }

  private Rule.PortRange buildPortRange(Map rule) {
    if (rule.protocol != "icmp") {
      return new Rule.PortRange(
        startPort: rule.port_range_min != null ? rule.port_range_min as Integer : 1,
        endPort: rule.port_range_max != null ? rule.port_range_max  as Integer : 65535
      )
    }

    // there are two cases for icmp: both min and max are null or not.
    def startPort = (rule.port_range_min != null) ? rule.port_range_min as Integer : 0
    def endPort = (rule.port_range_max != null) ? rule.port_range_max  as Integer : 255
    return new Rule.PortRange(
      startPort: startPort <= endPort ? startPort : endPort,
      endPort: startPort <= endPort ? endPort : startPort
    )
  }

  private HuaweiCloudSecurityGroup buildRemoteSecurityGroup(String key) {
    if (!key) {
      return null
    }

    Map<String, String> parts = Keys.parse(key)
    if (!parts) {
      return null
    }

    new HuaweiCloudSecurityGroup(
      type: this.cloudProvider,
      cloudProvider: this.cloudProvider,
      id: parts.id,
      name: parts.name,
      region: parts.region,
      accountName: parts.account,
      application: parts.application,
    )
  }
}
