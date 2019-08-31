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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.netflix.spinnaker.clouddriver.huaweicloud.deploy.validators

import com.netflix.spinnaker.clouddriver.huaweicloud.deploy.description.UpsertSecurityGroupDescription
import com.netflix.spinnaker.clouddriver.huaweicloud.HuaweiCloudOperation
import com.netflix.spinnaker.clouddriver.orchestration.AtomicOperations
import org.springframework.stereotype.Component
import org.springframework.validation.Errors

@HuaweiCloudOperation(AtomicOperations.UPSERT_SECURITY_GROUP)
@Component
class UpsertSecurityGroupDescriptionValidator extends AbstractDescriptionValidator<UpsertSecurityGroupDescription> {

  static final int MIN_PORT = -1
  static final int MAX_PORT = (1 << 16) - 1

  String context = "upsertSecurityGroupDescription"

  @Override
  void validateMore(List priorDescriptions, UpsertSecurityGroupDescription description, Errors errors) {
  }
}
