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

package com.netflix.spinnaker.clouddriver.huaweicloud.model

import com.fasterxml.jackson.annotation.JsonIgnore
import com.huawei.openstack4j.openstack.networking.domain.ext.NeutronLoadBalancerV2
import com.netflix.spinnaker.clouddriver.huaweicloud.HuaweiCloudProvider
import com.netflix.spinnaker.clouddriver.model.LoadBalancer
import com.netflix.spinnaker.clouddriver.model.LoadBalancerProvider
import com.netflix.spinnaker.clouddriver.model.LoadBalancerServerGroup
import com.netflix.spinnaker.moniker.Moniker
import groovy.transform.Canonical

@Canonical
class HuaweiCloudLoadBalancer {

  String account
  String region

  NeutronLoadBalancerV2 loadbalancer
  List<HuaweiCloudLoadBalancerPool> pools

  @JsonIgnore
  View getView() {
    new View()
  }

  @Canonical
  class View implements LoadBalancer {
    final String type = HuaweiCloudProvider.ID
    final String cloudProvider = HuaweiCloudProvider.ID

    String id = HuaweiCloudLoadBalancer.this.loadbalancer.id
    String name = HuaweiCloudLoadBalancer.this.loadbalancer.name
    String account = HuaweiCloudLoadBalancer.this.account
    String region = HuaweiCloudLoadBalancer.this.region
    List<HuaweiCloudLoadBalancerPool> pools = HuaweiCloudLoadBalancer.this.pools

    Set<LoadBalancerServerGroup> serverGroups = [] as Set

    void setMoniker(Moniker _ignored) {}
  }

  @JsonIgnore
  Details getDetails() {
    new Details()
  }

  @Canonical
  class Details extends View implements LoadBalancerProvider.Details {
  }
}
