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

package com.netflix.spinnaker.clouddriver.huaweicloud.model.health

import com.huawei.openstack4j.model.scaling.ScalingGroup.ScalingGroupStatus
import com.huawei.openstack4j.model.scaling.ScalingGroupInstance.HealthStatus
import com.netflix.spinnaker.clouddriver.model.HealthState

class HuaweiCloudInstanceHealth extends HuaweiCloudHealth {

  final HealthType type = HealthType.HuaweiCloud
  final HealthClass healthClass = HealthClass.platform

  HealthState state

  HuaweiCloudInstanceHealth(HealthStatus status, ScalingGroupStatus sgStatus) {
    this.state = buildState(status, sgStatus)
  }

  private static HealthState buildState(HealthStatus status, ScalingGroupStatus sgStatus) {
    // TODO maybe there is some other way to turn down the instance
    // at present, if scaling group is paused, then all the instances are down as well.
    if (sgStatus == ScalingGroupStatus.PAUSED) {
      return HealthState.Down
    }

    switch(status) {
      case HealthStatus.NORMAL:
        return HealthState.Up

      case HealthStatus.INITIALIZING:
        return HealthState.Starting

      case HealthStatus.ERROR:
        return HealthState.Down

      default:
        return HealthState.Unknown
    }
  }
}
