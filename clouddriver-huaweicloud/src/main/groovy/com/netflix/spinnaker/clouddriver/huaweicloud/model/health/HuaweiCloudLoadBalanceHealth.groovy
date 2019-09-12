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

import com.huawei.openstack4j.model.network.ext.LbOperatingStatus
import com.huawei.openstack4j.openstack.networking.domain.ext.NeutronMemberV2
import com.netflix.spinnaker.clouddriver.model.HealthState

class HuaweiCloudLoadBalanceHealth extends HuaweiCloudHealth {

  final HealthType type = HealthType.LoadBalancer

  final HealthState state

  HuaweiCloudLoadBalanceHealth(NeutronMemberV2 instance) {
    this.state = buildState(instance.operatingStatus)
  }

  private static HealthState buildState(LbOperatingStatus status) {
    switch(status) {
      case LbOperatingStatus.ONLINE:
        return HealthState.Up

      case LbOperatingStatus.OFFLINE:
        return HealthState.Down

      default:
        return HealthState.Unknown
    }
  }
}
