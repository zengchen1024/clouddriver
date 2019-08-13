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

class DeregisterInstancesFromLBOperation extends AbstractRegisterDeregisterInstancesOperation {
  String basePhase = 'DEREGISTER'
  Boolean action = Boolean.FALSE
  String verb = 'deregistering'
  String preposition = 'from'

  /*
   * curl -X POST -H "Content-Type: application/json" -d '[ { "deregisterInstancesFromLoadBalancer": { "loadBalancerIds": ["2112e340-4714-492c-b9db-e45e1b1102c5"], "instanceIds": ["155e68a7-a7dd-433a-b2c1-c8d6d38fb89a"], "account": "test", "region": "region" }} ]' localhost:7002/huaweicloud/ops
   * curl -X GET -H "Accept: application/json" localhost:7002/task/1
   */
  DeregisterInstancesFromLBOperation(InstancesRegistrationDescription description) {
    super(description)
  }
}
