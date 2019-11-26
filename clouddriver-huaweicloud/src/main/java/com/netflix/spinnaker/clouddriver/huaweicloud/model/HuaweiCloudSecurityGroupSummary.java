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

package com.netflix.spinnaker.clouddriver.huaweicloud.model;

import com.netflix.spinnaker.clouddriver.model.SecurityGroupSummary;

public class HuaweiCloudSecurityGroupSummary implements SecurityGroupSummary {
  private String id;
  private String name;
  private String vpcId;

  public HuaweiCloudSecurityGroupSummary(String name, String id, String vpcId) {
    this.id = id;
    this.name = name;
    this.vpcId = vpcId;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public String getId() {
    return id;
  }

  public String getVpcId() {
    return vpcId;
  }
}
