package com.itesm.interfaces.rest;

import com.itesm.domain.repository.UserRepository;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;

@QuarkusTest
class AuthRbacResourceTest {

    @Inject
    UserRepository userRepository;

    @Test
    void registerShouldCreateUserWithInviteCode() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "fullName": "Dr. Register",
                          "email": "register-test@statusscope.local",
                          "password": "Password123!",
                          "inviteCode": "INVITE-HGZ21"
                        }
                        """)
                .when()
                .post("/auth/register")
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("email", org.hamcrest.Matchers.equalTo("register-test@statusscope.local"));
    }

    @Test
    void protectedRouteShouldReturn401WithoutToken() {
        given()
                .when()
                .get("/auth/me")
                .then()
                .statusCode(401);
    }

    @Test
    void missingRouteShouldReturn404() {
        given()
                .when()
                .get("/does-not-exist")
                .then()
                .statusCode(404)
                .body("code", org.hamcrest.Matchers.equalTo("NOT_FOUND"));
    }

    @Test
    void doctorShouldBeForbiddenFromAdminRoles() {
        given()
                .header("Authorization", "Bearer test-token")
                .header("X-Test-User", "doctor1@statusscope.local")
                .when()
                .get("/admin/roles")
                .then()
                .statusCode(403);
    }

    @Test
    void adminShouldAccessRolesEndpoint() {
        given()
                .header("Authorization", "Bearer test-token")
                .header("X-Test-User", "admin@statusscope.local")
                .when()
                .get("/admin/roles")
                .then()
                .statusCode(200);
    }

    @Test
    void systemAdminProfileShouldExposeSystemPrivilegeOnly() {
        given()
                .header("Authorization", "Bearer test-token")
                .header("X-Test-User", "admin@statusscope.local")
                .when()
                .get("/auth/me")
                .then()
                .statusCode(200)
                .body("roles", hasItem("SYSTEM_ADMIN"))
                .body("privileges", hasItem("isSystemAdmin"))
                .body("privileges", not(hasItem("admin.operations")));
    }

    @Test
    void hospitalAdminProfileShouldExposeHospitalOperationsPrivilegeOnly() {
        given()
                .header("Authorization", "Bearer test-token")
                .header("X-Test-User", "admin.hgz21@statusscope.local")
                .when()
                .get("/auth/me")
                .then()
                .statusCode(200)
                .body("roles", hasItem("HOSPITAL_ADMIN"))
                .body("privileges", hasItem("admin.operations"))
                .body("privileges", not(hasItem("isSystemAdmin")));
    }

    @Test
    void systemAdminShouldAccessSystemDashboard() {
        given()
                .header("Authorization", "Bearer test-token")
                .header("X-Test-User", "admin@statusscope.local")
                .when()
                .get("/system/dashboard/summary")
                .then()
                .statusCode(200);
    }

    @Test
    void hospitalAdminShouldBeForbiddenFromSystemDashboard() {
        given()
                .header("Authorization", "Bearer test-token")
                .header("X-Test-User", "admin.hgz21@statusscope.local")
                .when()
                .get("/system/dashboard/summary")
                .then()
                .statusCode(403);
    }

    @Test
    void systemAdminShouldBeForbiddenFromHospitalAdminOperations() {
        given()
                .header("Authorization", "Bearer test-token")
                .header("X-Test-User", "admin@statusscope.local")
                .when()
                .get("/admin/epidemiology/summary")
                .then()
                .statusCode(403);
    }

    @Test
    void meShouldReturnProfileForAuthenticatedUser() {
        given()
                .header("Authorization", "Bearer test-token")
                .header("X-Test-User", "admin@statusscope.local")
                .when()
                .get("/auth/me")
                .then()
                .statusCode(200)
                .body("email", org.hamcrest.Matchers.equalTo("admin@statusscope.local"))
                .body("roles", notNullValue());
    }

    @Test
    void meShouldTrackDoctorLogin() {
        LocalDateTime before = LocalDateTime.now().minusSeconds(1);

        given()
                .header("Authorization", "Bearer test-token")
                .header("X-Test-User", "doctor1@statusscope.local")
                .when()
                .get("/auth/me")
                .then()
                .statusCode(200)
                .body("email", org.hamcrest.Matchers.equalTo("doctor1@statusscope.local"));

        LocalDateTime lastLoginAt = userRepository.findByEmail("doctor1@statusscope.local")
                .orElseThrow()
                .getLastLoginAt();
        Assertions.assertNotNull(lastLoginAt);
        Assertions.assertFalse(lastLoginAt.isBefore(before));
    }

    @Test
    void meShouldNotTrackSystemAdminLogin() {
        LocalDateTime beforeLogin = userRepository.findByEmail("admin@statusscope.local")
                .orElseThrow()
                .getLastLoginAt();

        given()
                .header("Authorization", "Bearer test-token")
                .header("X-Test-User", "admin@statusscope.local")
                .when()
                .get("/auth/me")
                .then()
                .statusCode(200)
                .body("email", org.hamcrest.Matchers.equalTo("admin@statusscope.local"));

        LocalDateTime afterLogin = userRepository.findByEmail("admin@statusscope.local")
                .orElseThrow()
                .getLastLoginAt();
        Assertions.assertEquals(beforeLogin, afterLogin);
    }
}
