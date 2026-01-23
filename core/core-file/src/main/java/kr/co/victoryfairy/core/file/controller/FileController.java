package kr.co.victoryfairy.core.file.controller;

import io.swagger.v3.oas.annotations.Operation;
import kr.co.victoryfairy.core.file.domain.FileDomain;
import kr.co.victoryfairy.core.file.service.FileService;
import kr.co.victoryfairy.support.constant.MessageEnum;
import kr.co.victoryfairy.support.model.CustomResponse;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class FileController {

    private final FileService fileService;

    public FileController(FileService fileService) {
        this.fileService = fileService;
    }

    @Operation(summary = "파일 등록")
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public CustomResponse<List<FileDomain.Response>> createFile(@ModelAttribute FileDomain.CreateRequest request) {
        var response = fileService.createFile(request);
        return CustomResponse.ok(response);
    }

}
