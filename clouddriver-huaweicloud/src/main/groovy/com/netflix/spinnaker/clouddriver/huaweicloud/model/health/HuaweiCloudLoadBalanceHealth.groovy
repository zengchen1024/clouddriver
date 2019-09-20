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
import com.netflix.spinnaker.clouddriver.model.HealthState

class HuaweiCloudLoadBalanceHealth extends HuaweiCloudHealth {

  final HealthType type = HealthType.LoadBalancer
  final HealthClass healthClass = null

  List<LBHealthSummary> loadBalancers

  @Override
  HealthState getState() {
    (loadBalancers ?: []).every {
      it.state == LBHealthSummary.ServiceStatus.InService
    } ? HealthState.Up : HealthState.Down
  }

  static class LBHealthSummary {
    String name
    ServiceStatus state

    LBHealthSummary(String name, LbOperatingStatus status) {
      this.name = name
      this.state = buildState(status)
    }

    private static ServiceStatus buildState(LbOperatingStatus status) {
      switch(status) {
        case LbOperatingStatus.ONLINE:
          return ServiceStatus.InService

        case LbOperatingStatus.OFFLINE:
          return ServiceStatus.OutOfService

        default:
          return ServiceStatus.InService
      }
    }

    String getDescription() {
      state == ServiceStatus.OutOfService ?
        "Instance has failed at least the Unhealthy Threshold number of health checks consecutively." :
        "Healthy"
    }

    ServiceStatus getHealthState() {
      state
    }

    enum ServiceStatus {
      InService,
      OutOfService
    }
  }
}
