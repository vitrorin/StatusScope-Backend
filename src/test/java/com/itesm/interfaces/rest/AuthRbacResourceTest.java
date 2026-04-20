package com.itesm.interfaces.rest;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.notNullValue;

@QuarkusTest
class AuthRbacResourceTest {

    @Test
    void registerShouldAssignDefaultRoleAndReturnCreated() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "fullName": "Dr. Register",
                          "email": "register@statusscope.local",
                          "externalAuthId": "register-ext"
                        }
                        """)
                .when()
                .post("/auth/register")
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("roles[0].code", org.hamcrest.Matchers.equalTo("DOCTOR"));
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
    void shouldReturn403WithoutPrivilegeAnd200WithPrivilege() {
        String doctorToken = given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "email": "doctor@statusscope.local"
                        }
                        """)
                .when()
                .post("/auth/login")
                .then()
                .statusCode(200)
                .extract()
                .path("token");

        given()
                .header("Authorization", "Bearer " + doctorToken)
                .when()
                .get("/admin/roles")
                .then()
                .statusCode(403);

        String adminToken = given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "email": "admin@statusscope.local"
                        }
                        """)
                .when()
                .post("/auth/login")
                .then()
                .statusCode(200)
                .extract()
                .path("token");

        given()
                .header("Authorization", "Bearer " + adminToken)
                .when()
                .get("/admin/roles")
                .then()
                .statusCode(200);

        String roleCode = given()
                .header("Authorization", "Bearer " + adminToken)
                .when()
                .get("/auth/me")
                .then()
                .statusCode(200)
                .extract()
                .path("roles[0]");

        Assertions.assertNotNull(roleCode);
    }
}
