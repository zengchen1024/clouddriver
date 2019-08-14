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

package com.netflix.spinnaker.clouddriver.huaweicloud.client

import com.huawei.openstack4j.openstack.vpc.v1.domain.Subnet
import com.huawei.openstack4j.openstack.vpc.v1.domain.Vpc

interface HuaweiCloudClient {
  /**
   * Reboot an instance ... Default to SOFT reboot.
   * @param region
   * @param instanceId
   */
  void rebootInstance(String region, String instanceId)

  /**
   * List the available vpcs in a region.
   * @param region
   * @return
   */
  List<Vpc> listVpcs(String region)

  /**
   * List the available subnets in a region.
   * @param region
   * @return
   */
  List<Subnet> listSubnets(String region)
}

