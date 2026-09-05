package kr.co.victoryfairy.admin.presentation;

import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.RequestMapping;

import static org.assertj.core.api.Assertions.assertThat;

class AdminRouteContractTest {

    @Test
    void keepsAdminRoutesWhenApplicationsAreMerged() throws Exception {
        assertRoute("AdminAuthController", "/admin/auth");
        assertRoute("kr.co.victoryfairy.diary.presentation.admin.AdminDiaryController", "/admin/diary");
        assertRoute("kr.co.victoryfairy.member.presentation.admin.AdminMemberController", "/admin/member");
    }

    private void assertRoute(String className, String expected) throws ClassNotFoundException {
        var controller = Class.forName(className.contains(".") ? className : getClass().getPackageName() + "." + className);
        assertThat(controller.getAnnotation(RequestMapping.class).value()).containsExactly(expected);
    }

}
