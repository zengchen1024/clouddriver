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

package com.netflix.spinnaker.clouddriver.huaweicloud.deploy.ops

import groovy.transform.InheritConstructors
import com.huawei.openstack4j.model.common.ActionResponse

@InheritConstructors
class OperationException extends RuntimeException {
  OperationException(String operation, String message) {
    super("$operation failed: ${message}")
  }

  OperationException(String operation, Exception e) {
    super("$operation failed: ${e.message}", e)
  }

  OperationException(ActionResponse actionResponse, String operation) {
    super("$operation failed: fault $actionResponse.fault with code $actionResponse.code")
  }

  OperationException(String account, String operation, Exception e) {
    super("$operation for account $account failed: ${e.message}", e)
  }
}
