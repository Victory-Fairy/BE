package kr.co.victoryfairy.core.file.domain;

import io.dodn.springboot.core.enums.RefType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface FileDomain {

    @Schema(name = "File.Response")
    record Response(@Schema(description = "file id") Long id, @Schema(description = "원본 파일명") String name,
            @Schema(description = "저장된 파일명") String saveName, @Schema(description = "경로") String path,
            @Schema(description = "확장자") String ext) {
    }

    @Schema(name = "File.CreateRequest")
    record CreateRequest(@RequestPart("file") List<MultipartFile> file,

            @NotNull @Schema(description = "참조 타입", example = "PROFILE",
                    implementation = RefType.class) RefType fileRefType) {
    }

    record File(RefType refType, String name, String saveName, String path, String ext, Long size) {
    }

}
