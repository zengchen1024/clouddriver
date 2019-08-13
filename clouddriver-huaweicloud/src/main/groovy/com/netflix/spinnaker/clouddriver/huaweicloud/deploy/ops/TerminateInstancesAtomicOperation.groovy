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

package com.netflix.spinnaker.clouddriver.huaweicloud.deploy.ops

import com.netflix.spinnaker.clouddriver.huaweicloud.deploy.description.HuaweiCloudInstancesDescription
import com.netflix.spinnaker.clouddriver.orchestration.AtomicOperation
import groovy.util.logging.Slf4j

/**
 * Terminates an HuaweiCloud instance by marking the stack resource unhealthy and doing a stack update. This will
 * recreate instances until the stack reaches the correct size.
 *
 * TODO test upsert load balancer
 */
@Slf4j
class TerminateInstancesAtomicOperation implements AtomicOperation<Void> {
  final String phaseName = "TERMINATE_INSTANCES"

  TerminateInstancesAtomicOperation(HuaweiCloudInstancesDescription description) {
    super(description)
  }

  /*
   * curl -X POST -H "Content-Type: application/json" -d '[ { "terminateInstances": { "instanceIds": ["os-test-v000-beef"], "account": "test", "region": "region1" }} ]' localhost:7002/huaweicloud/ops
   * curl -X GET -H "Accept: application/json" localhost:7002/task/1
   */
  @Override
  Void operate(List priorOutputs) {}
}
