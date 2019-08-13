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

package com.netflix.spinnaker.clouddriver.huaweicloud.deploy.converters

import com.netflix.spinnaker.clouddriver.huaweicloud.HuaweiCloudOperation
import com.netflix.spinnaker.clouddriver.huaweicloud.deploy.description.InstancesRegistrationDescription
import com.netflix.spinnaker.clouddriver.huaweicloud.deploy.ops.DeregisterInstancesFromLBOperation
import com.netflix.spinnaker.clouddriver.orchestration.AtomicOperation
import com.netflix.spinnaker.clouddriver.orchestration.AtomicOperations
import com.netflix.spinnaker.clouddriver.security.AbstractAtomicOperationsCredentialsSupport

@HuaweiCloudOperation(AtomicOperations.DEREGISTER_INSTANCES_FROM_LOAD_BALANCER)
class DeregisterInstancesFromLBConverter extends AbstractAtomicOperationsCredentialsSupport {
  @Override
  AtomicOperation convertOperation(Map input) {
    new DeregisterInstancesFromLBOperation(convertDescription(input))
  }

  @Override
  InstancesRegistrationDescription convertDescription(Map input) {
    AtomicOperationConverterHelper.convertDescription(input, this, InstancesRegistrationDescription)
  }
}
