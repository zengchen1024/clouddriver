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

package com.netflix.spinnaker.clouddriver.huaweicloud.security

import com.fasterxml.jackson.annotation.JsonIgnore
import com.huawei.openstack4j.api.OSClient
import com.huawei.openstack4j.core.transport.Config
import com.huawei.openstack4j.model.common.Identifier
import com.huawei.openstack4j.model.identity.v3.Token
import com.huawei.openstack4j.openstack.OSFactory
import com.netflix.spinnaker.clouddriver.huaweicloud.client.AuthorizedClientProvider
import groovy.util.logging.Slf4j

@Slf4j
public class HuaweiCloudCredentials implements AuthorizedClientProvider {
  final String username
  @JsonIgnore
  final String password
  final String projectName
  final String domainName
  final String authUrl
  final Boolean insecure

  Token token = null

  HuaweiCloudCredentials(String username,
                         String password,
                         String projectName,
                         String domainName,
                         String authUrl,
                         Boolean insecure) {
    this.username = username
    this.password = password
    this.projectName = projectName
    this.domainName = domainName
    this.authUrl = authUrl
    this.insecure = insecure
  }

  OSClient getAuthClient() {
    def config = insecure ? Config.newConfig().withSSLVerificationDisabled() : Config.newConfig()
    def client = null
    try {
      if (needRefreshToken()) {
        synchronized (this) {
          if (needRefreshToken()) {
            token = OSFactory.builderV3()
              .withConfig(config)
              .endpoint(authUrl)
              .credentials(username, password, Identifier.byName(domainName))
              .scopeToProject(Identifier.byName(projectName), Identifier.byName(domainName))
              .authenticate()
              .token
          }
        }
      }

      client = OSFactory.clientFromToken(token, config)
    } catch (Exception) {
      log.error("build authorized client error")
    }
    client
  }

  private boolean needRefreshToken() {
    if (!token) {
      return true
    }

    long now = System.currentTimeMillis()
    long expires = token.expires.time
    now >= expires
  }
}
