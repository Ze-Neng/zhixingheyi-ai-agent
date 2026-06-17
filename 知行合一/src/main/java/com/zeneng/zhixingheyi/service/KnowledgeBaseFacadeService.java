package com.zeneng.zhixingheyi.service;

import com.zeneng.zhixingheyi.model.request.CreateKnowledgeBaseRequest;
import com.zeneng.zhixingheyi.model.request.UpdateKnowledgeBaseRequest;
import com.zeneng.zhixingheyi.model.response.CreateKnowledgeBaseResponse;
import com.zeneng.zhixingheyi.model.response.GetKnowledgeBasesResponse;

public interface KnowledgeBaseFacadeService {
    GetKnowledgeBasesResponse getKnowledgeBases();

    CreateKnowledgeBaseResponse createKnowledgeBase(CreateKnowledgeBaseRequest request);

    void deleteKnowledgeBase(String knowledgeBaseId);

    void updateKnowledgeBase(String knowledgeBaseId, UpdateKnowledgeBaseRequest request);
}

