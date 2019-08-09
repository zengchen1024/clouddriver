/*
 * Copyright 2016 Target, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
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

abstract class AbstractEnableDisableInstancesInDiscoveryOperation implements AtomicOperation<Void> {

  HuaweiCloudInstancesDescription description

  AbstractEnableDisableInstancesInDiscoveryOperation(HuaweiCloudInstancesDescription description) {
    this.description = description
  }

  @Override
  Void operate(List priorOutputs) {
    return null
  }

  /**
   * Operations must indicate if they are disabling the instance from service discovery.
   * @return
   */
  abstract boolean isDisable()

  /**
   * Phase name associated to operation.
   * @return
   */
  abstract String getPhaseName()
}
