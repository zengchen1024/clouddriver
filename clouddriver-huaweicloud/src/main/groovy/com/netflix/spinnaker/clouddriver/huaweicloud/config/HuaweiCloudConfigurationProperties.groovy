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

package com.netflix.spinnaker.clouddriver.huaweicloud.config

import com.netflix.spinnaker.clouddriver.consul.config.ConsulConfig
import groovy.transform.ToString

@ToString(includeNames = true)
class HuaweiCloudConfigurationProperties {

  @ToString(includeNames = true, excludes = "password")
  static class ManagedAccount {
    String name
    String environment
    String accountType
    String username
    String password
    String projectName
    String domainName
    String authUrl
    List<String> regions
    Boolean insecure
    String asgConfigLocation
    ConsulConfig consulConfig
    String userDataFile
  }

  List<ManagedAccount> accounts = []
}
