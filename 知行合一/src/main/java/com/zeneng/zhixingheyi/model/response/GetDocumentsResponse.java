package com.zeneng.zhixingheyi.model.response;

import com.zeneng.zhixingheyi.model.vo.DocumentVO;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GetDocumentsResponse {
    private DocumentVO[] documents;
}

