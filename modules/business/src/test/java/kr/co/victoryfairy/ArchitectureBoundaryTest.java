package kr.co.victoryfairy;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

class ArchitectureBoundaryTest {

    private static final List<String> SOURCE_ROOTS = List.of("modules/business/src/main/java",
            "applications/api/src/main/java", "applications/crawler/src/main/java");

    private static final Pattern DOMAIN_FORBIDDEN = Pattern.compile(
            "\\b(?:jakarta\\.persistence|org\\.hibernate|org\\.springframework|io\\.swagger|[^\\s;]+\\.(?:application|presentation|infrastructure|web)(?:\\.|;))");

    private static final Pattern OUTER_FORBIDDEN = Pattern.compile(
            "\\b(?:jakarta\\.persistence|com\\.querydsl|org\\.springframework\\.data\\.jpa|[^\\s;]+\\.persistence\\.(?:entity|repository|model)(?:\\.|;))");

    private static final Pattern DOMAIN_TYPE = Pattern.compile(
            "\\bkr\\.co\\.victoryfairy\\.(admin|diary|game|media|member)\\.domain\\.([A-Z][A-Za-z0-9_]*)");

    private static final Set<String> SHARED_DOMAIN_VALUES = Set.of("diary.DiaryEnum", "game.MatchEnum");

    @Test
    void sourceDependenciesPointInward() throws IOException {
        Path root = repositoryRoot();
        List<Path> sources = new ArrayList<>();
        for (String sourceRoot : SOURCE_ROOTS) {
            List<Path> moduleSources;
            try (var files = Files.walk(root.resolve(sourceRoot))) {
                moduleSources = files.filter(path -> path.toString().endsWith(".java")).toList();
            }
            assertThat(moduleSources).as("main Java sources under %s", sourceRoot).isNotEmpty();
            sources.addAll(moduleSources);
        }

        List<String> violations = new ArrayList<>();
        for (Path source : sources) {
            String path = source.toString().replace('\\', '/');
            String code = codeOnly(Files.readString(source));
            Pattern forbidden = path.contains("/domain/") ? DOMAIN_FORBIDDEN
                    : path.contains("/application/") || path.contains("/presentation/") ? OUTER_FORBIDDEN : null;
            if (forbidden != null && forbidden.matcher(code).find()) {
                violations.add(root.relativize(source).toString());
            }
            if (path.contains("/domain/")) {
                String owner = path.substring(0, path.indexOf("/domain/")).replaceFirst(".*/", "");
                Matcher types = DOMAIN_TYPE.matcher(code);
                while (types.find()) {
                    String dependency = types.group(1) + "." + types.group(2);
                    if (!owner.equals(types.group(1)) && !SHARED_DOMAIN_VALUES.contains(dependency)) {
                        violations.add(root.relativize(source) + " -> " + dependency);
                    }
                }
            }
        }
        assertThat(violations).as("forbidden outward dependencies").isEmpty();
    }

    @Test
    void businessContainsOnlySharedFeatures() {
        Path root = repositoryRoot().resolve("modules/business/src/main/java/kr/co/victoryfairy");

        assertThat(List.of("admin", "media", "member", "game/infrastructure/persistence/GameUserPersistenceAdapter.java"))
                .allSatisfy(path -> assertThat(root.resolve(path)).doesNotExist());
    }

    @Test
    void featureOwnedSourcesLiveWithTheirOwners() {
        Path root = repositoryRoot();
        assertThat(List.of(
                "modules/business/src/main/java/kr/co/victoryfairy/diary/domain/DiaryModel.java",
                "modules/business/src/main/java/kr/co/victoryfairy/diary/domain/ViewingRecordReader.java",
                "applications/api/src/main/java/kr/co/victoryfairy/diary/application/admin/AdminDiaryQueryStore.java",
                "applications/api/src/main/java/kr/co/victoryfairy/member/application/admin/AdminMemberQueryService.java",
                "applications/api/src/main/java/kr/co/victoryfairy/game/application/CommonQueryService.java"))
                .allSatisfy(path -> assertThat(root.resolve(path)).exists());
        assertThat(List.of(
                "applications/api/src/main/java/kr/co/victoryfairy/member/domain/MemberGameReader.java",
                "applications/api/src/main/java/kr/co/victoryfairy/admin/application/AdminDiaryQueryService.java",
                "applications/api/src/main/java/kr/co/victoryfairy/common/application/CommonQueryService.java",
                "modules/business/src/main/java/kr/co/victoryfairy/shared/application/model/CommonDto.java"))
                .allSatisfy(path -> assertThat(root.resolve(path)).doesNotExist());
    }

    private static Path repositoryRoot() {
        Path current = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        while (current != null && !Files.exists(current.resolve("settings.gradle.kts"))) {
            current = current.getParent();
        }
        assertThat(current).as("repository root").isNotNull();
        return current;
    }

    private static String codeOnly(String source) {
        StringBuilder result = new StringBuilder(source.length());
        boolean lineComment = false;
        boolean blockComment = false;
        boolean string = false;
        boolean character = false;
        boolean escaped = false;
        for (int i = 0; i < source.length(); i++) {
            char current = source.charAt(i);
            char next = i + 1 < source.length() ? source.charAt(i + 1) : '\0';
            if (lineComment) {
                if (current == '\n') {
                    lineComment = false;
                    result.append(current);
                }
            }
            else if (blockComment) {
                if (current == '*' && next == '/') {
                    blockComment = false;
                    i++;
                }
            }
            else if (string || character) {
                if (!escaped && current == (string ? '"' : '\'')) {
                    string = false;
                    character = false;
                }
                escaped = !escaped && current == '\\';
            }
            else if (current == '/' && next == '/') {
                lineComment = true;
                i++;
            }
            else if (current == '/' && next == '*') {
                blockComment = true;
                i++;
            }
            else if (current == '"' || current == '\'') {
                string = current == '"';
                character = !string;
                escaped = false;
            }
            else {
                result.append(current);
            }
        }
        return result.toString();
    }

}
