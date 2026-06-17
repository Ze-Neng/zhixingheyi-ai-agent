package com.zeneng.zhixingheyi.model.response;

import com.zeneng.zhixingheyi.model.vo.AgentVO;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GetAgentsResponse {
    private AgentVO[] agents;
}
