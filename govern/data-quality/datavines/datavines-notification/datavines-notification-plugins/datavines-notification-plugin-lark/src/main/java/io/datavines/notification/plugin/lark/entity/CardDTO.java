/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.datavines.notification.plugin.lark.entity;

import io.datavines.notification.plugin.lark.entity.card.*;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public abstract class CardDTO {
    private HeaderDTO header;
    private List<ElementDTO> elements;

    public CardDTO() {
    }

    public CardDTO(String headerContent, String headerColor, List<MessageDTO> messageList) {
        TitleDTO title = new TitleDTO(headerContent, "plain_text");
        HeaderDTO headerDTO = new HeaderDTO(headerColor,title);
        this.header = headerDTO;

        List<ElementDTO> elementsDtoList = new ArrayList<>();
        for (int i = 0; i < messageList.size(); i++) {
            MessageDTO messageDTO = messageList.get(i);
            TextDTO textDTO = new TextDTO("**" + messageDTO.getTitle() + "：**\n" + messageDTO.getContent(),"lark_md");
            elementsDtoList.add(new ElementDTO(textDTO,"div"));

        }
        this.elements = elementsDtoList;

    }

}
