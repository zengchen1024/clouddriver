/*
 * Copyright 2019 Huawei Technologies Co.,Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netflix.spinnaker.clouddriver.huaweicloud.deploy.validators

import com.netflix.spinnaker.clouddriver.deploy.DescriptionValidator
import com.netflix.spinnaker.clouddriver.huaweicloud.deploy.description.AbstractHuaweiCloudCredentialsDescription
import com.netflix.spinnaker.clouddriver.huaweicloud.security.HuaweiCloudNamedAccountCredentials
import com.netflix.spinnaker.clouddriver.security.AccountCredentialsProvider
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.validation.Errors

abstract class AbstractHuaweiCloudDescriptionValidator<T extends AbstractHuaweiCloudCredentialsDescription> extends DescriptionValidator<T> {

  @Autowired
  AccountCredentialsProvider accountCredentialsProvider

  @Override
  void validate(List priorDescriptions, T description, Errors errors) {
    def helper = getValidateHelper(errors)

    if (validateCredentials(description.accountName, errors)) {
      return
    }

    validateMore(priorDescriptions, description, errors)
  }

  abstract String getContext()

  abstract void validateMore(List priorDescriptions, T description, Errors errors)

  HuaweiCloudDiscriptionValidateHelper getValidateHelper(Errors errors) {
    return new HuaweiCloudDiscriptionValidateHelper(context, errors)
  }

  private boolean validateCredentials(String accountName, Errors errors) {
    def helper = getValidateHelper(errors)
    def result = helper.validateNotEmpty(accountName, "accountName")
    if (!result) {
      return result
    }

    def credentials = accountCredentialsProvider.getCredentials(accountName)
    if (!(credentials instanceof HuaweiCloudNamedAccountCredentials)) {
      errors.rejectValue("credentials", "${context}.credentials.invalid")
      return false
    }
    return true
  }
}
