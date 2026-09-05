package kr.co.victoryfairy.contract;

import kr.co.victoryfairy.common.presentation.CommonController;
import kr.co.victoryfairy.community.presentation.CommunityController;
import kr.co.victoryfairy.diary.presentation.DiaryController;
import kr.co.victoryfairy.game.presentation.MatchController;
import kr.co.victoryfairy.game.presentation.GameCommonController;
import kr.co.victoryfairy.diary.presentation.ViewingStatisticsController;
import kr.co.victoryfairy.media.presentation.FileController;
import kr.co.victoryfairy.member.presentation.AuthController;
import kr.co.victoryfairy.member.presentation.MemberController;
import kr.co.victoryfairy.member.presentation.MyPageController;
import kr.co.victoryfairy.member.presentation.RedirectController;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.RequestMapping;

import static org.assertj.core.api.Assertions.assertThat;

class ApiRouteContractTest {

    @Test
    void keepsPublicRoutesWhenApplicationsAreMerged() {
        assertRoute(AuthController.class, "/api/auth");
        assertRoute(CommonController.class, "/api/common");
        assertRoute(GameCommonController.class, "/api/common");
        assertRoute(CommunityController.class, "/api/community/posts");
        assertRoute(DiaryController.class, "/api/diary");
        assertRoute(MatchController.class, "/api/match");
        assertRoute(MemberController.class, "/api/member");
        assertRoute(MyPageController.class, "/api/my-page");
        assertRoute(ViewingStatisticsController.class, "/api/my-page");
        assertRoute(RedirectController.class, "/api");
        assertRoute(FileController.class, "/file");
    }

    private void assertRoute(Class<?> controller, String expected) {
        assertThat(controller.getAnnotation(RequestMapping.class).value()).containsExactly(expected);
    }

}
