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
import com.netflix.spinnaker.config.HuaweiCloudConfiguration
import com.netflix.spinnaker.clouddriver.huaweicloud.provider.HuaweiCloudInfrastructureProvider
import com.netflix.spinnaker.clouddriver.security.AccountCredentialsRepository
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
      HuaweiCloudInfrastructureProvider huaweiCloudInfastructureProvider,
      AccountCredentialsRepository accountCredentialsRepository,
      ObjectMapper objectMapper,
      Registry registry) {
  }
}
