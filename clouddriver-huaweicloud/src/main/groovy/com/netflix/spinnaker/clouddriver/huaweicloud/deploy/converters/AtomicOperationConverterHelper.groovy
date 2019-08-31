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

package com.netflix.spinnaker.clouddriver.huaweicloud.deploy.converters

import com.fasterxml.jackson.databind.DeserializationFeature
import com.netflix.spinnaker.clouddriver.huaweicloud.security.HuaweiCloudNamedAccountCredentials
import com.netflix.spinnaker.clouddriver.security.AbstractAtomicOperationsCredentialsSupport
import com.netflix.spinnaker.clouddriver.security.resources.CredentialsNameable
import groovy.util.logging.Slf4j

@Slf4j
class AtomicOperationConverterHelper {

  static <T extends CredentialsNameable> T convertDescription(
      Map input,
      AbstractAtomicOperationsCredentialsSupport credentialsSupport,
      Class<T> targetDescriptionType) {

    if (!input.account) {
      input.account = input.credentials
    }

    def credentials = null
    if (input.account) {
      credentials = credentialsSupport.getCredentialsObject(input.account as String)
    }

    input.remove('credentials')

    T converted = credentialsSupport.getObjectMapper()
      .copy()
      .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
      .convertValue(input, targetDescriptionType)

    converted.credentials = credentials in HuaweiCloudNamedAccountCredentials ? credentials : null
    if (!converted.credentials) {
      log.error("Convert input=${input} to description=${targetDescriptionType.name} failed, the credentials is null.")
    }

    converted
  }
}
