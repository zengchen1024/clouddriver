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

import com.netflix.spinnaker.clouddriver.huaweicloud.deploy.description.ServerGroupDescription
import com.netflix.spinnaker.clouddriver.orchestration.AtomicOperation
import groovy.util.logging.Slf4j

@Slf4j
class DestroyServerGroupOperation implements AtomicOperation<Void> {
  private final String BASE_PHASE = "DESTROY_SERVER_GROUP"
  ServerGroupDescription description

  DestroyServerGroupOperation(ServerGroupDescription description) {
    this.description = description
  }

  /*
   curl -X POST -H "Content-Type: application/json" -d '[{
     "destroyServerGroup": {
       "serverGroupName": "drmaastestapp-drmaasteststack-v000",
       "serverGroupId": "",
       "region": "region",
     }
   }]' localhost:7002/huaweicloud/ops
  */
  @Override
  Void operate(List priorOutputs) {
    TaskAware.task.updateStatus BASE_PHASE, "Destroying server group=${description.serverGroupName} in region=${description.region}..."

    description.credentials.cloudClient.deleteScalingGroup(
      description.region, description.serverGroupId,
    )

    // TODO wait for deleted
    TaskAware.task.updateStatus BASE_PHASE, "Finished destroying server group=${description.serverGroupName}."
    return
  }
}
