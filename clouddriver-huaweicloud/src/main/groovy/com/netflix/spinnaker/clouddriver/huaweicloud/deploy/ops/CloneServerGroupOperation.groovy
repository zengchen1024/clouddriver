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

import com.netflix.spinnaker.clouddriver.deploy.DeploymentResult
import com.netflix.spinnaker.clouddriver.huaweicloud.deploy.description.CloneServerGroupDescription
import com.netflix.spinnaker.clouddriver.orchestration.AtomicOperation
import groovy.util.logging.Slf4j

@Slf4j
class CloneServerGroupOperation implements AtomicOperation<DeploymentResult> {
  private final String BASE_PHASE = "CLONE_SERVER_GROUP"

  CloneServerGroupDescription description

  CloneServerGroupOperation(CloneServerGroupDescription description) {
    this.description = description
  }

  /*
   curl -X POST -H "Content-Type: application/json" -d '[{
     "cloneServerGroup": {
       "serverGroupName": "drmaastestapp-drmaasteststack-v000",
       "serverGroupId": "",
       "region": "region",
     }
   }]' localhost:7002/huaweicloud/ops
  */
  @Override
  DeploymentResult operate(List priorOutputs) {
    TaskAware.task.updateStatus BASE_PHASE, "Cloning server group=${description.source.serverGroupName}: ${description.source.serverGroupName} in region=${description.region}..."

    DeploymentResult result = (new DeployServerGroupOperation(description, BASE_PHASE)).operate(priorOutputs)

    TaskAware.task.updateStatus BASE_PHASE, "Finished cloning server group=${description.source.serverGroupName}."

    result
  }
}
