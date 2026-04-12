package thinkunderstar.puretalk.puretalkbackend.common;

import lombok.Data;

@Data
public class DoSendPost {
    public DoSendPost(String title, String content, Long userId) {
        this.title = title;
        this.content = content;
        this.userId = userId;
    }

    public DoSendPost() {
    }

    /**
     * 帖子标题
     */
    private String title;

    /**
     * 帖子内容
     */
    private String content;

    /**
     * 发布用户ID
     */
    private Long userId;
}
