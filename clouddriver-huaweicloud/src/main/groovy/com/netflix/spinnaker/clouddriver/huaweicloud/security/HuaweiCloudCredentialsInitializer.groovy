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

import com.netflix.spinnaker.clouddriver.huaweicloud.config.HuaweiCloudConfigurationProperties
import com.netflix.spinnaker.clouddriver.security.AccountCredentialsRepository
import com.netflix.spinnaker.clouddriver.security.ProviderUtils
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Component

@Component
@Configuration
class HuaweiCloudCredentialsInitializer {
  private static final Logger log = LoggerFactory.getLogger(this.class)

  @Bean
  List<HuaweiCloudNamedAccountCredentials> huaweiCloudNamedAccountCredentials(
      HuaweiCloudConfigurationProperties huaweiCloudConfigurationProperties,
      AccountCredentialsRepository accountCredentialsRepository) {

    def (ArrayList<HuaweiCloudConfigurationProperties.ManagedAccount> accountsToAdd, List<String> namesOfDeletedAccounts) =
      ProviderUtils.calculateAccountDeltas(accountCredentialsRepository,
                                          HuaweiCloudNamedAccountCredentials,
                                          huaweiCloudConfigurationProperties.accounts)

    accountsToAdd.each { HuaweiCloudConfigurationProperties.ManagedAccount managedAccount ->
      log.info("Found huaweicloud managed account $managedAccount")

      try {
        def account = new HuaweiCloudNamedAccountCredentials(
          managedAccount.name,
          managedAccount.environment ?: managedAccount.name,
          managedAccount.accountType ?: managedAccount.name,
          managedAccount.username,
          managedAccount.password,
          managedAccount.projectName,
          managedAccount.domainName,
          managedAccount.authUrl,
          managedAccount.regions,
          managedAccount.insecure,
          managedAccount.asgConfigLocation,
          managedAccount.consulConfig,
          managedAccount.userDataFile,
          null
        )

        accountCredentialsRepository.save(managedAccount.name, account)
      } catch (e) {
        log.error "Could not load account ${managedAccount.name} for huaweicloud.", e
      }
    }

    ProviderUtils.unscheduleAndDeregisterAgents(namesOfDeletedAccounts, null)

    accountCredentialsRepository.all.findAll {
      it instanceof HuaweiCloudNamedAccountCredentials
    } as List<HuaweiCloudNamedAccountCredentials>
  }
}
