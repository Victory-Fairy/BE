package kr.co.victoryfairy.media.application;

import kr.co.victoryfairy.shared.domain.RefType;
import kr.co.victoryfairy.media.domain.FileDomain;
import kr.co.victoryfairy.media.infrastructure.S3FileUploader;
import kr.co.victoryfairy.media.infrastructure.persistence.entity.FileEntity;
import kr.co.victoryfairy.media.infrastructure.persistence.repository.FileRepository;
import kr.co.victoryfairy.web.response.MessageEnum;
import kr.co.victoryfairy.web.error.CustomException;
import kr.co.victoryfairy.media.infrastructure.FileProperties;
import kr.co.victoryfairy.media.infrastructure.S3PresignedUrlService;
import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFmpegExecutor;
import net.bramp.ffmpeg.FFprobe;
import net.bramp.ffmpeg.builder.FFmpegBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

@Service
public class MediaCommandService {

    private static final Logger logger = LoggerFactory.getLogger(MediaCommandService.class);

    private final FileProperties fileProperties;

    private final FileRepository fileRepository;

    private final Optional<S3FileUploader> s3FileUploader;

    private final S3PresignedUrlService s3PresignedUrlService;

    public MediaCommandService(FileProperties fileProperties, FileRepository fileRepository,
            Optional<S3FileUploader> s3FileUploader, S3PresignedUrlService s3PresignedUrlService) {
        this.fileProperties = fileProperties;
        this.fileRepository = fileRepository;
        this.s3FileUploader = s3FileUploader;
        this.s3PresignedUrlService = s3PresignedUrlService;
    }

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

        return fileEntities.stream()
            .map(entity -> new FileDomain.Response(entity.getId(), entity.getName(), entity.getSaveName(),
                    entity.getPath(), entity.getExt(),
                    s3PresignedUrlService.create(entity.getPath(), entity.getSaveName(), entity.getExt())))
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
            List<Path> savedFiles = saveFile(saveName, path, file);
            s3FileUploader.ifPresent(uploader -> {
                uploader.upload(Path.of(fileProperties.getStoragePath()), savedFiles);
                deleteWorkspaceFiles(savedFiles);
            });

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
        String yearMonth = YearMonth.now().format(DateTimeFormatter.ofPattern("yyyyMM"));
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
    private List<Path> saveFile(String saveName, String path, MultipartFile file) {

        Path savedPath = Path.of(fileProperties.getStoragePath(), path, saveName + "." + getExtension(file));

        try {
            // File savedFile = new File(savedPath, saveName);
            // file.transferTo(savedFile);
            Files.copy(file.getInputStream(), savedPath, StandardCopyOption.REPLACE_EXISTING);
            File savedFile = savedPath.toFile();
            List<Path> savedFiles = new ArrayList<>();
            savedFiles.add(savedPath);

            Image image = ImageIO.read(savedFile);

            String fileType = getFileType(file);

            if ("image".equals(fileType)) {
                Arrays.stream(fileProperties.getImageResizes()).forEach(size -> {
                    savedFiles.add(resizeImage(file, savedFile, image, size));
                });
            }
            if ("video".equals(fileType)) {
                Arrays.stream(fileProperties.getVideoResizes()).forEach(size -> {
                    savedFiles.add(resizeVideo(file, savedFile, size));
                });
            }

            return savedFiles;

        }
        catch (IOException e) {
            throw new CustomException(MessageEnum.File.FAIL_UPLOAD);
        }

    }

    private Path resizeImage(MultipartFile file, File orgFile, Image image, Integer size) {
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
            return savedFile.toPath();
        }
        catch (Exception e) {
            throw new CustomException(MessageEnum.File.FAIL_UPLOAD);
        }
    }

    private Path resizeVideo(MultipartFile file, File orgFile, Integer size) {
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
            return savedFile.toPath();

        }
        catch (Exception e) {
            throw new CustomException(MessageEnum.File.FAIL_UPLOAD);
        }
    }

    private void deleteWorkspaceFiles(List<Path> files) {
        files.forEach(file -> {
            try {
                Files.deleteIfExists(file);
            }
            catch (IOException e) {
                logger.warn("Failed to delete S3 upload workspace file: {}", file, e);
            }
        });
    }

}
