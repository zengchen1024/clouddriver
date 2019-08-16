/*
 * Copyright 2019 Huawei Technologies Co.,Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
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

package com.netflix.spinnaker.clouddriver.huaweicloud.security

import com.netflix.spinnaker.clouddriver.consul.config.ConsulConfig
import com.netflix.spinnaker.clouddriver.huaweicloud.client.HuaweiCloudClient
import com.netflix.spinnaker.clouddriver.huaweicloud.client.HuaweiCloudClientImpl
import com.netflix.spinnaker.clouddriver.security.AccountCredentials

class HuaweiCloudNamedAccountCredentials implements AccountCredentials<HuaweiCloudCredentials> {
  static final String CLOUD_PROVIDER = "huaweicloud"

  final String accountName
  final String environment
  final String accountType
  List<String> regions
  final String asgConfigLocation
  final ConsulConfig consulConfig
  final String userDataFile
  Map<String, List<String>> regionToZones
  final List<String> requiredGroupMembership
  final HuaweiCloudCredentials credentials
  final HuaweiCloudClient cloudClient

  HuaweiCloudNamedAccountCredentials(String accountName,
                                     String environment,
                                     String accountType,
                                     String username,
                                     String password,
                                     String projectName,
                                     String domainName,
                                     String authUrl,
                                     Boolean insecure,
                                     List<String> regions,
                                     String asgConfigLocation,
                                     ConsulConfig consulConfig,
                                     String userDataFile,
                                     List<String> requiredGroupMembership) {
    this.accountName = accountName
    this.environment = environment
    this.accountType = accountType
    this.regions = regions
    this.asgConfigLocation = asgConfigLocation
    this.consulConfig = consulConfig
    this.userDataFile = userDataFile
    if (this.consulConfig?.enabled) {
      this.consulConfig.applyDefaults()
    }
    this.credentials = new HuaweiCloudCredentials(
      username,
      password,
      projectName,
      domainName,
      authUrl,
      insecure
    )
    this.cloudClient = new HuaweiCloudClientImpl(this.credentials)
    this.requiredGroupMembership = requiredGroupMembership ?: [] as List<String>
  }

  @Override
  String getCloudProvider() {
    CLOUD_PROVIDER
  }

  @Override
  String getName() {
    accountName
  }
}
