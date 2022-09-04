package com.alibaba.tesla.tkgone.server.domain.dto;

import com.alibaba.tesla.tkgone.server.domain.ChatopsHistory;
import lombok.*;
import org.springframework.beans.BeanUtils;

import java.util.Date;

/**
 * @author yangjinghua
 */
@Data
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class ChatopsHistoryDto extends ChatopsHistory {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    private Long id;

    @Builder.Default
    private Date gmtCreate = new Date();

    @Builder.Default
    private Date gmtModified = new Date();

    @Builder.Default
    private String category = "";

    @Builder.Default
    private String senderNick = "";

    @Builder.Default
    private String senderId = "";

    @Builder.Default
    private String senderEmpid = "";

    @Builder.Default
    private Integer isConversation = -1;

    @Builder.Default
    private String conversationTitle = "";

    @Builder.Default
    private Integer isContentHelp = -1;

    @Builder.Default
    private String sendContent = "";

    @Builder.Default
    private String backContent = "";

    @Builder.Default
    private Integer rate = 10;

    @Builder.Default
    private String suggest = "";

    public ChatopsHistoryDto(ChatopsHistory chatopsHistory) {

        BeanUtils.copyProperties(chatopsHistory, this);

    }

    public ChatopsHistory toChatopsHistory() {

        ChatopsHistory chatopsHistory = new ChatopsHistory();
        BeanUtils.copyProperties(this, chatopsHistory);

        return chatopsHistory;

    }

}