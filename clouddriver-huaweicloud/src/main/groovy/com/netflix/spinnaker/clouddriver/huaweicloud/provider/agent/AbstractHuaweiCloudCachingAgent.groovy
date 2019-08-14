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

package com.netflix.spinnaker.clouddriver.huaweicloud.provider.agent

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.netflix.spinnaker.cats.agent.AccountAware
import com.netflix.spinnaker.cats.agent.CachingAgent
import com.netflix.spinnaker.clouddriver.huaweicloud.client.HuaweiCloudClient
import com.netflix.spinnaker.clouddriver.huaweicloud.provider.HuaweiCloudInfrastructureProvider
import com.netflix.spinnaker.clouddriver.huaweicloud.security.HuaweiCloudNamedAccountCredentials

abstract class AbstractHuaweiCloudCachingAgent implements CachingAgent, AccountAware {

  final TypeReference<Map<String, Object>> ATTRIBUTES = new TypeReference<Map<String, Object>>() {}
  final String providerName = HuaweiCloudInfrastructureProvider.PROVIDER_NAME

  final HuaweiCloudNamedAccountCredentials credentials
  final ObjectMapper objectMapper
  final String region

  AbstractHuaweiCloudCachingAgent(HuaweiCloudNamedAccountCredentials credentials, ObjectMapper objectMapper, String region) {
    this.credentials = credentials
    this.objectMapper = objectMapper
    this.region = region
  }

  @Override
  String getAccountName() {
    credentials.accountName
  }

  HuaweiCloudClient getCloudClient() {
    credentials.cloudClient
  }
}
