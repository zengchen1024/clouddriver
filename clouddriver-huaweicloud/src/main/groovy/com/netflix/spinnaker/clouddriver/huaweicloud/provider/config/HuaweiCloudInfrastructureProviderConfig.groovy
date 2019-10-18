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

package com.netflix.spinnaker.clouddriver.huaweicloud.provider.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.netflix.spectator.api.Registry
import com.netflix.spinnaker.cats.agent.Agent
import com.netflix.spinnaker.cats.agent.CachingAgent
import com.netflix.spinnaker.clouddriver.huaweicloud.provider.agent.HuaweiCloudElasticIPCachingAgent
import com.netflix.spinnaker.clouddriver.huaweicloud.provider.agent.HuaweiCloudImageCachingAgent
import com.netflix.spinnaker.clouddriver.huaweicloud.provider.agent.HuaweiCloudInstanceCachingAgent
import com.netflix.spinnaker.clouddriver.huaweicloud.provider.agent.HuaweiCloudInstanceTypeCachingAgent
import com.netflix.spinnaker.clouddriver.huaweicloud.provider.agent.HuaweiCloudLoadBalancerCachingAgent
import com.netflix.spinnaker.clouddriver.huaweicloud.provider.agent.HuaweiCloudNetworkCachingAgent
import com.netflix.spinnaker.clouddriver.huaweicloud.provider.agent.HuaweiCloudSecurityGroupCachingAgent
import com.netflix.spinnaker.clouddriver.huaweicloud.provider.agent.HuaweiCloudServerGroupCachingAgent
import com.netflix.spinnaker.clouddriver.huaweicloud.provider.agent.HuaweiCloudServerGroupConfigCachingAgent
import com.netflix.spinnaker.clouddriver.huaweicloud.provider.agent.HuaweiCloudSubnetCachingAgent
import com.netflix.spinnaker.clouddriver.huaweicloud.provider.HuaweiCloudInfrastructureProvider
import com.netflix.spinnaker.clouddriver.huaweicloud.security.HuaweiCloudNamedAccountCredentials
import com.netflix.spinnaker.clouddriver.security.AccountCredentialsRepository
import com.netflix.spinnaker.clouddriver.security.ProviderUtils
import com.netflix.spinnaker.config.HuaweiCloudConfiguration
import java.util.concurrent.ConcurrentHashMap
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.DependsOn
import org.springframework.context.annotation.Import

@Configuration
@Import(HuaweiCloudConfiguration)
@EnableConfigurationProperties
class HuaweiCloudInfrastructureProviderConfig {

  @Bean
  @DependsOn('huaweiCloudNamedAccountCredentials')
  HuaweiCloudInfrastructureProvider huaweiCloudInfastructureProvider(
      AccountCredentialsRepository accountCredentialsRepository,
      ObjectMapper objectMapper,
      Registry registry) {

    HuaweiCloudInfrastructureProvider provider = new HuaweiCloudInfrastructureProvider(
      Collections.newSetFromMap(new ConcurrentHashMap<Agent, Boolean>()))

    synchronizeHuaweiCloudProvider(provider, accountCredentialsRepository, objectMapper, registry)

    provider
  }

  private static void synchronizeHuaweiCloudProvider(
      HuaweiCloudInfrastructureProvider infastructureProvider,
      AccountCredentialsRepository accountCredentialsRepository,
      ObjectMapper objectMapper,
      Registry registry) {

    def scheduledAccounts = ProviderUtils.getScheduledAccounts(infastructureProvider)
    def allAccounts = ProviderUtils.buildThreadSafeSetOfAccounts(
      accountCredentialsRepository, HuaweiCloudNamedAccountCredentials)

    List<CachingAgent> newlyAddedAgents = []

    allAccounts.each { HuaweiCloudNamedAccountCredentials credentials ->
      if (!scheduledAccounts.contains(credentials.name)) {
        credentials.regions.each { String region ->
          newlyAddedAgents << new HuaweiCloudSubnetCachingAgent(credentials, objectMapper, region)
          newlyAddedAgents << new HuaweiCloudNetworkCachingAgent(credentials, objectMapper, region)
          newlyAddedAgents << new HuaweiCloudElasticIPCachingAgent(credentials, objectMapper, region)
          newlyAddedAgents << new HuaweiCloudImageCachingAgent(credentials, objectMapper, region)
          newlyAddedAgents << new HuaweiCloudInstanceCachingAgent(credentials, objectMapper, region)
          newlyAddedAgents << new HuaweiCloudInstanceTypeCachingAgent(credentials, objectMapper, region)
          newlyAddedAgents << new HuaweiCloudSecurityGroupCachingAgent(credentials, objectMapper, region, registry)
          newlyAddedAgents << new HuaweiCloudServerGroupCachingAgent(credentials, objectMapper, region, registry)
          newlyAddedAgents << new HuaweiCloudLoadBalancerCachingAgent(credentials, objectMapper, region, registry)
          newlyAddedAgents << new HuaweiCloudServerGroupConfigCachingAgent(credentials, objectMapper, region)
        }
      }
    }

    if (infastructureProvider.agentScheduler) {
      ProviderUtils.rescheduleAgents(infastructureProvider, newlyAddedAgents)
    }

    if (newlyAddedAgents) {
      infastructureProvider.agents.addAll(newlyAddedAgents)
    }
  }
}
