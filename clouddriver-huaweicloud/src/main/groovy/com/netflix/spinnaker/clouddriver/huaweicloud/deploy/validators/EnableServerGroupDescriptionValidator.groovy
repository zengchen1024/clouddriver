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

import com.netflix.spinnaker.clouddriver.huaweicloud.deploy.description.EnableDisableServerGroupDescription
import com.netflix.spinnaker.clouddriver.huaweicloud.HuaweiCloudOperation
import com.netflix.spinnaker.clouddriver.orchestration.AtomicOperations
import org.springframework.validation.Errors

@HuaweiCloudOperation(AtomicOperations.ENABLE_SERVER_GROUP)
class EnableServerGroupDescriptionValidator extends AbstractDescriptionValidator<EnableDisableServerGroupDescription> {
  String context = 'enableServerGroupDescriptionValidator'

  @Override
  void validateMore(List priorDescriptions, EnableDisableServerGroupDescription description, Errors errors) {
  }
}
