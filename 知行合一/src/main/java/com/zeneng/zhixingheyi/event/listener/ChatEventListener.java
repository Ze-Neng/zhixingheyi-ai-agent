package com.zeneng.zhixingheyi.event.listener;

import com.zeneng.zhixingheyi.agent.ZhiXingHeYi;
import com.zeneng.zhixingheyi.agent.ZhiXingHeYiFactory;
import com.zeneng.zhixingheyi.event.ChatEvent;
import lombok.AllArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class ChatEventListener {

    private final ZhiXingHeYiFactory zhiXingHeYiFactory;

    @Async
    @EventListener
    public void handle(ChatEvent event) {
        // 创建一个 Agent 实例处理聊天事件
        ZhiXingHeYi zhiXingHeYi = zhiXingHeYiFactory.create(event.getAgentId(), event.getSessionId());
        zhiXingHeYi.run();
    }
}
