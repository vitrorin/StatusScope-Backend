package com.itesm.interfaces.rest;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.notNullValue;

@QuarkusTest
class AuthRbacResourceTest {

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
}
