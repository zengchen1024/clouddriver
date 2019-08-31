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

package com.netflix.spinnaker.clouddriver.huaweicloud.deploy.ops

import com.netflix.spinnaker.clouddriver.huaweicloud.deploy.description.DeleteSecurityGroupDescription
import com.netflix.spinnaker.clouddriver.orchestration.AtomicOperation
import com.netflix.spinnaker.clouddriver.orchestration.AtomicOperations

class DeleteSecurityGroupOperation implements AtomicOperation<Void> {

  private final String BASE_PHASE = 'DELETE_SECURITY_GROUP'
  DeleteSecurityGroupDescription description

  DeleteSecurityGroupOperation(DeleteSecurityGroupDescription description) {
    this.description = description
  }

  /*
   * Delete:
   * curl -X POST -H "Content-Type: application/json" -d '[ { "deleteSecurityGroup": { "account": "test", "region": "west", "id": "ee411748-88b5-4825-a9d4-ec549d1a1276" } } ]' localhost:7002/huaweicloud/ops
   * Task status:
   * curl -X GET -H "Accept: application/json" localhost:7002/task/1
   */
  @Override
  Void operate(List priorOutputs) {
    TaskAware.task.updateStatus(BASE_PHASE, "Deleting security group ${description.id}")

    def resp = description.credentials.cloudClient.deleteSecurityGroup(description.region, description.id)
    if (!resp.isSuccess()) {
      throw new OperationException(resp, AtomicOperations.UPSERT_SECURITY_GROUP)
    }

    TaskAware.task.updateStatus(BASE_PHASE, "Finished deleting security group ${description.id}")
  }
}
