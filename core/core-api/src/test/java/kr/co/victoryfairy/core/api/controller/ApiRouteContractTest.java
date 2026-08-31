package kr.co.victoryfairy.core.api.controller;

import kr.co.victoryfairy.core.api.media.presentation.FileController;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.RequestMapping;

import static org.assertj.core.api.Assertions.assertThat;

class ApiRouteContractTest {

    @Test
    void keepsPublicRoutesWhenApplicationsAreMerged() {
        assertRoute(AuthController.class, "/api/auth");
        assertRoute(CommonController.class, "/api/common");
        assertRoute(DiaryController.class, "/api/diary");
        assertRoute(MatchController.class, "/api/match");
        assertRoute(MemberController.class, "/api/member");
        assertRoute(MyPageController.class, "/api/my-page");
        assertRoute(RedirectController.class, "/api");
        assertRoute(FileController.class, "/file");
    }

    private void assertRoute(Class<?> controller, String expected) {
        assertThat(controller.getAnnotation(RequestMapping.class).value()).containsExactly(expected);
    }

}
