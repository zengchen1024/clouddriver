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

import com.fasterxml.jackson.annotation.JsonIgnore
import com.huawei.openstack4j.api.OSClient
import com.netflix.spinnaker.clouddriver.consul.config.ConsulConfig
import com.netflix.spinnaker.clouddriver.huaweicloud.client.AuthorizedClientProvider
import com.netflix.spinnaker.clouddriver.huaweicloud.client.HuaweiCloudClient
import com.netflix.spinnaker.clouddriver.huaweicloud.client.HuaweiCloudClientImpl
import com.netflix.spinnaker.clouddriver.security.AccountCredentials

class HuaweiCloudNamedAccountCredentials implements AccountCredentials<HuaweiCloudCredentials>, AuthorizedClientProvider {
  static final String CLOUD_PROVIDER = "huaweicloud"

  final String accountName
  final String environment
  final String accountType
  final String username
  @JsonIgnore
  final String password
  final String projectName
  final String domainName
  final String authUrl
  List<String> regions
  final Boolean insecure
  final String asgConfigLocation
  final ConsulConfig consulConfig
  final String userDataFile
  Map<String, List<String>> regionToZones
  final List<String> requiredGroupMembership
  final HuaweiCloudCredentials credentials
  final HuaweiCloudClient cloudClient = new HuaweiCloudClientImpl(this)

  HuaweiCloudNamedAccountCredentials(String accountName,
                                     String environment,
                                     String accountType,
                                     String username,
                                     String password,
                                     String projectName,
                                     String domainName,
                                     String authUrl,
                                     List<String> regions,
                                     Boolean insecure,
                                     String asgConfigLocation,
                                     ConsulConfig consulConfig,
                                     String userDataFile,
                                     List<String> requiredGroupMembership) {
    this.accountName = accountName
    this.environment = environment
    this.accountType = accountType
    this.username = username
    this.password = password
    this.projectName = projectName
    this.domainName = domainName
    this.authUrl = authUrl
    this.regions = regions
    this.insecure = insecure
    this.asgConfigLocation = asgConfigLocation
    this.consulConfig = consulConfig
    this.userDataFile = userDataFile
    if (this.consulConfig?.enabled) {
      this.consulConfig.applyDefaults()
    }
    this.credentials = new HuaweiCloudCredentials()
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

  OSClient getAuthClient() {
    credentials.buildClient(username, password, projectName, domainName, authUrl, insecure)
  }
}
