package com.zeneng.zhixingheyi.service;

import com.zeneng.zhixingheyi.model.request.CreateDocumentRequest;
import com.zeneng.zhixingheyi.model.request.UpdateDocumentRequest;
import com.zeneng.zhixingheyi.model.response.CreateDocumentResponse;
import com.zeneng.zhixingheyi.model.response.GetDocumentsResponse;
import org.springframework.web.multipart.MultipartFile;

public interface DocumentFacadeService {
    GetDocumentsResponse getDocuments();

    GetDocumentsResponse getDocumentsByKbId(String kbId);

    CreateDocumentResponse createDocument(CreateDocumentRequest request);

    CreateDocumentResponse uploadDocument(String kbId, MultipartFile file);

    void deleteDocument(String documentId);

    void updateDocument(String documentId, UpdateDocumentRequest request);
}
