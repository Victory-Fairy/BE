package kr.co.victoryfairy.core.file.service;

import kr.co.victoryfairy.core.file.domain.FileDomain;

import java.util.List;

public interface FileService {

    List<FileDomain.Response> createFile(FileDomain.CreateRequest request);

}
