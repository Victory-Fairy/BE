package kr.co.victoryfairy.core.file.service.impl;

import io.dodn.springboot.core.enums.RefType;
import kr.co.victoryfairy.core.file.domain.FileDomain;
import kr.co.victoryfairy.core.file.service.FileService;
import kr.co.victoryfairy.storage.db.core.entity.FileEntity;
import kr.co.victoryfairy.storage.db.core.repository.FileRefRepository;
import kr.co.victoryfairy.storage.db.core.repository.FileRepository;
import kr.co.victoryfairy.support.constant.MessageEnum;
import kr.co.victoryfairy.support.exception.CustomException;
import kr.co.victoryfairy.support.properties.FileProperties;
import kr.co.victoryfairy.support.utils.DateUtils;
import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFmpegExecutor;
import net.bramp.ffmpeg.FFprobe;
import net.bramp.ffmpeg.builder.FFmpegBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.List;

@Service
public class FileServiceImpl implements FileService {

    private final FileProperties fileProperties;

    private final FileRepository fileRepository;

    private final FileRefRepository fileRefRepository;

    public FileServiceImpl(FileProperties fileProperties, FileRepository fileRepository,
            FileRefRepository fileRefRepository) {
        this.fileProperties = fileProperties;
        this.fileRepository = fileRepository;
        this.fileRefRepository = fileRefRepository;
    }

    @Override
    @Transactional
    public List<FileDomain.Response> createFile(FileDomain.CreateRequest request) {
        if (request.file().isEmpty())
            return null;

        var fileDomains = this.convertFile(request.fileRefType(), request.file());

        if (fileDomains.isEmpty())
            return null;

        var fileEntities = fileDomains.stream()
            .map(file -> FileEntity.builder()
                .name(file.name())
                .saveName(file.saveName())
                .path(file.path())
                .ext(file.ext())
                .size(file.size())
                .build())
            .toList();
        fileRepository.saveAll(fileEntities);

        List<FileDomain.Response> response = new ArrayList<>();

        return fileEntities.stream()
            .map(entity -> new FileDomain.Response(entity.getId(), entity.getName(), entity.getSaveName(),
                    entity.getPath(), entity.getExt()))
            .toList();
    }

    private List<FileDomain.File> convertFile(RefType refType, List<MultipartFile> multipartFiles) {
        List<FileDomain.File> files = new ArrayList<>();

        // saveName 만들기
        multipartFiles.forEach(file -> {
            String saveName = makeFileSaveName(file);
            // path 만들기
            String path = makePath(file, refType);

            // 만들어진 경로에 새로운 이름으로 저장
            saveFile(saveName, path, file);

            // 윈도우 시스템 기반 경로 rule 에 대한 대응 (저장 시 역슬래시 '\' 기호를 unix 시스템 호환을 위해 슬래시 '/' 로 변환)
            if (path.contains("\\")) {
                path = path.replaceAll("\\\\", "/");
            }

            FileDomain.File fileDomain = new FileDomain.File(refType, file.getOriginalFilename(), saveName, path,
                    getExtension(file), file.getSize());
            files.add(fileDomain);
        });

        return files;
    }

    private String makeFileSaveName(MultipartFile file) {
        String uuid = UUID.randomUUID().toString();
        return uuid;
    }

    /**
     * <li>파일 확장자 구하기</li>
     * @param file
     * @return
     */
    private String getExtension(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        String extension = "";

        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf(".") + 1);
        }
        else {
            throw new CustomException(MessageEnum.File.WRONG_FILE);
        }

        return extension;
    }

    /**
     * <li>저장될 경로 생성 및 가져오기</li>
     * @param file
     * @param type
     * @return path
     */
    private String makePath(MultipartFile file, RefType type) {
        String yearMonth = DateUtils.now(DateUtils.Format.DATETIME_FORMAT_MONTH_TRIM.getPattern());
        String fileType = getFileType(file);
        String path = Path.of(fileType, type.name().toLowerCase(), yearMonth).toString();
        if (!new File(fileProperties.getStoragePath(), path).exists()) {
            new File(fileProperties.getStoragePath(), path).mkdirs();
        }
        return path;
    }

    /**
     * <li>경로 분기를 위한 파일 종류 정해주기</li>
     * @param file
     * @return fileType
     */
    private String getFileType(MultipartFile file) {
        String contentType = file.getContentType();
        String fileType = "etc";

        if (!Objects.isNull(contentType)) {
            if (contentType.contains("image")) {
                fileType = "image";
            }
            if (contentType.contains("video")) {
                fileType = "video";
            }
            if (contentType.contains("audio")) {
                fileType = "audio";
            }
        }

        return fileType;
    }

    /**
     * <li>파일 저장</li>
     * @param saveName
     * @param path
     * @param file
     */
    private void saveFile(String saveName, String path, MultipartFile file) {

        Path savedPath = Path.of(fileProperties.getStoragePath(), path, saveName + "." + getExtension(file));

        try {
            // File savedFile = new File(savedPath, saveName);
            // file.transferTo(savedFile);
            Files.copy(file.getInputStream(), savedPath, StandardCopyOption.REPLACE_EXISTING);
            File savedFile = savedPath.toFile();

            Image image = ImageIO.read(savedFile);

            String fileType = getFileType(file);

            if ("image".contains(fileType)) {
                Arrays.stream(fileProperties.getImageResizes()).forEach(size -> {
                    resizeImage(file, savedFile, image, size);
                });
            }
            if ("video".contains(fileType)) {
                Arrays.stream(fileProperties.getVideoResizes()).forEach(size -> {
                    resizeVideo(file, savedFile, size);
                });
            }

        }
        catch (IOException e) {
            throw new CustomException(MessageEnum.File.FAIL_UPLOAD);
        }

    }

    private void resizeImage(MultipartFile file, File orgFile, Image image, Integer size) {
        try {
            String ext = getExtension(file);
            String filePath = orgFile.getParent();
            String newFileName = orgFile.getName().replace("." + ext, "") + "_" + size + "." + ext;

            int height = (int) Math.round(((double) size / (double) image.getWidth(null)) * image.getHeight(null));
            int width = size;

            Image resizeImage = image.getScaledInstance(width, height, Image.SCALE_FAST);

            BufferedImage newImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            Graphics newImageGraphics = newImage.getGraphics();
            newImageGraphics.drawImage(resizeImage, 0, 0, null);
            newImageGraphics.dispose();

            File savedFile = new File(filePath, newFileName);
            ImageIO.write(newImage, ext, savedFile);
        }
        catch (Exception e) {
            throw new CustomException(MessageEnum.File.FAIL_UPLOAD);
        }
    }

    private void resizeVideo(MultipartFile file, File orgFile, Integer size) {
        try {
            int width = 0;
            int height = 0;

            if (size == 1920) {
                width = 1920;
                height = 1080;
            }
            else if (size == 1280) {
                width = 1280;
                height = 720;
            }
            else if (size == 960) {
                width = 960;
                height = 540;
            }
            else if (size == 640) {
                width = 640;
                height = 360;
            }
            else if (size == 320) {
                width = 320;
                height = 180;
            }

            String ext = getExtension(file);
            String filePath = orgFile.getParent();
            String newFileName = orgFile.getName() + "_" + size + "." + ext;

            File savedFile = new File(filePath, newFileName);

            FFmpeg ffmpeg = new FFmpeg(fileProperties.getStoragePath());
            FFprobe ffprobe = new FFprobe(fileProperties.getStoragePath());
            FFmpegBuilder builder = new FFmpegBuilder().overrideOutputFiles(true) // 오버라이드
                                                                                  // 여부
                .setInput(orgFile.getAbsolutePath()) // 생성대상 파일
                .addOutput(savedFile.getAbsolutePath()) // 생성 파일의 Path
                .setFormat("mp4")
                .setVideoCodec("libx264") // 비디오 코덱
                .setVideoFrameRate(30, 1) // 비디오 프레임
                .setVideoResolution(width, height) // 비디오 해상도
                .setStrict(FFmpegBuilder.Strict.EXPERIMENTAL) // x264 사용
                .addExtraArgs("-crf", "28") // 화질
                .addExtraArgs("-movflags", "use_metadata_tags") // 메타데이터 복사
                .done();
            FFmpegExecutor executor = new FFmpegExecutor(ffmpeg, ffprobe);
            executor.createJob(builder).run();

        }
        catch (Exception e) {
            throw new CustomException(MessageEnum.File.FAIL_UPLOAD);
        }
    }

}
