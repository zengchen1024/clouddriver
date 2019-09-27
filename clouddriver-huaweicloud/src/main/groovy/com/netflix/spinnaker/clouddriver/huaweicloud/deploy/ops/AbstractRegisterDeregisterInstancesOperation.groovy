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

package com.netflix.spinnaker.clouddriver.huaweicloud.deploy.ops

import com.netflix.spinnaker.clouddriver.huaweicloud.deploy.description.InstancesRegistrationDescription
import com.netflix.spinnaker.clouddriver.orchestration.AtomicOperation

abstract class AbstractRegisterDeregisterInstancesOperation implements AtomicOperation<Void> {

  abstract String getBasePhase() // Either 'REGISTER' or 'DEREGISTER'.
  abstract Boolean getAction() // Either 'true' or 'false', for Register and Deregister respectively.
  abstract String getVerb() // Either 'registering' or 'deregistering'.
  abstract String getPreposition() // Either 'with' or 'from'

  InstancesRegistrationDescription description

  AbstractRegisterDeregisterInstancesOperation(InstancesRegistrationDescription description) {
    this.description = description
  }

  //TODO we should be able to get all the instance ips once, instead of refetching for each load balancer
  //TODO we should also not refetch listeners for each instance, that should only happen once per balancer
  @Override
  Void operate(List priorOutputs) {}
}