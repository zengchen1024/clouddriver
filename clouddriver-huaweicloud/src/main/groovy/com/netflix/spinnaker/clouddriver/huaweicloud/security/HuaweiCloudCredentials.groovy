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

import com.huawei.openstack4j.api.OSClient
import com.huawei.openstack4j.core.transport.Config
import com.huawei.openstack4j.model.common.Identifier
import com.huawei.openstack4j.model.identity.v3.Token
import com.huawei.openstack4j.openstack.OSFactory

public class HuaweiCloudCredentials {

  Token token = null

  OSClient buildClient(
      String username,
      String password,
      String projectName,
      String domainName,
      String authUrl,
      Boolean insecure) {
    def config = insecure ? Config.newConfig().withSSLVerificationDisabled() : Config.newConfig()

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
    OSFactory.clientFromToken(token, config)
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
