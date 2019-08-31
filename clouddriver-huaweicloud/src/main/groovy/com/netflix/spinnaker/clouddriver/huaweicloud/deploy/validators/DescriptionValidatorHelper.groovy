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

import com.netflix.spinnaker.clouddriver.huaweicloud.deploy.description.AbstractHuaweiCloudCredentialsDescription
import com.netflix.spinnaker.clouddriver.huaweicloud.deploy.description.InstancesDescription
import com.netflix.spinnaker.clouddriver.huaweicloud.deploy.description.InstancesRegistrationDescription
import org.springframework.validation.Errors

class DiscriptionValidatorHelper {

  static public void validateInstancesDescription(InstancesDescription description, String context, Errors errors) {
    def helper = new ValidateHelper(context, errors)

    helper.validateNotEmpty(description.instanceIds, 'instanceIds')

    def region = description.region
    helper.validateNotEmpty(region, 'region')

    if (!(region in description.credentials.regions)) {
      errors.rejectValue("region", "${context}.region.invalid")
    }
  }

  static public void validateInstancesRegistrationDescription(InstancesRegistrationDescription description, String context, Errors errors) {
    def helper = new ValidateHelper(context, errors)

    helper.validateNotEmpty(description.instanceIds, "instanceIds")
    helper.validateNotEmpty(description.loadBalancerIds, "loadBalancerIds")

    def region = description.region
    helper.validateNotEmpty(region, 'region')
    if (!(region in description.credentials.regions)) {
      errors.rejectValue("region", "${context}.region.invalid")
    }
    // validate weight
  }

  static private boolean validateRegion(ValidateHelper helper, Class <? extends AbstractHuaweiCloudCredentialsDescription> description, String region) {
    if (!helper.validateNotEmpty(region, 'region')) {
      return false
    }

    if (!description.credentials.regions.contains(region)) {
      //errors.rejectValue("region", "${context}.region.invalid")
      return false
    }
    return true
  }
}
