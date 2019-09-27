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

import com.netflix.spinnaker.clouddriver.huaweicloud.deploy.description.EnableDisableServerGroupDescription
import com.netflix.spinnaker.clouddriver.orchestration.AtomicOperation
import com.netflix.spinnaker.clouddriver.orchestration.AtomicOperations

class EnableServerGroupOperation implements AtomicOperation<Void> {
  final String BASE_PHASE = "ENABLE_SERVER_GROUP"
  final String operation = AtomicOperations.ENABLE_SERVER_GROUP

  EnableDisableServerGroupDescription description

  EnableServerGroupOperation(EnableDisableServerGroupDescription description) {
    this.description = description
  }

  /*
   curl -X POST -H "Content-Type: application/json" -d '[{
     "enableServerGroup": {
       "serverGroupName": "myapp-teststack-v006",
       "serverGroupId": "",
       "region": "RegionOne",
       "account": "test" }
   }]' localhost:7002/huaweicloud/ops
  */
  @Override
  Void operate(List priorOutputs) {
    TaskAware.task.updateStatus BASE_PHASE, "Enabling server group=${description.serverGroupName} in region=${description.region}..."

    description.credentials.cloudClient.enableScalingGroup(
      description.region, description.serverGroupId,
    )

    TaskAware.task.updateStatus BASE_PHASE, "Finished enabling server group=${description.serverGroupName}."
    return
  }
}