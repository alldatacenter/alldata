/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.manager.pojo.transform;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.hibernate.validator.constraints.Length;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

/**
 * Delete request of transform
 */
@Data
@ApiModel("Delete request of stream transform")
public class DeleteTransformRequest {

    @ApiModelProperty("Inlong group id")
    @NotBlank(message = "inlongGroupId cannot be blank")
    @Length(min = 4, max = 100, message = "length must be between 4 and 100")
    @Pattern(regexp = "^[a-z0-9_.-]{4,100}$", message = "only supports lowercase letters, numbers, '-', or '_'")
    private String inlongGroupId;

    @ApiModelProperty("Inlong stream id")
    @NotBlank(message = "inlongStreamId cannot be blank")
    @Length(min = 1, max = 100, message = "inlongStreamId length must be between 1 and 100")
    @Pattern(regexp = "^[a-z0-9_.-]{1,100}$", message = "inlongStreamId only supports lowercase letters, numbers, '-', or '_'")
    private String inlongStreamId;

    @ApiModelProperty("Transform name, unique in one stream")
    @NotBlank(message = "transformName cannot be blank")
    @Length(min = 1, max = 100, message = "transformName length must be between 1 and 100")
    @Pattern(regexp = "^[a-z0-9_-]{1,100}$", message = "transformName only supports lowercase letters, numbers, '-', or '_'")
    private String transformName;

}
