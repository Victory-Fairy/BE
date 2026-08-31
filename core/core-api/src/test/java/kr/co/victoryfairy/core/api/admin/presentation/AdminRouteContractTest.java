package kr.co.victoryfairy.core.api.admin.presentation;

import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.RequestMapping;

import static org.assertj.core.api.Assertions.assertThat;

class AdminRouteContractTest {

    @Test
    void keepsAdminRoutesWhenApplicationsAreMerged() throws Exception {
        assertRoute("AdminAuthController", "/admin/auth");
        assertRoute("AdminDiaryController", "/admin/diary");
        assertRoute("AdminMemberController", "/admin/member");
    }

    private void assertRoute(String className, String expected) throws ClassNotFoundException {
        var controller = Class.forName(getClass().getPackageName() + "." + className);
        assertThat(controller.getAnnotation(RequestMapping.class).value()).containsExactly(expected);
    }

}
