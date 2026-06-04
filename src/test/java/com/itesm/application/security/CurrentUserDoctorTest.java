package com.itesm.application.security;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

class CurrentUserDoctorTest {

    private static final UUID USER_ID = UUID.fromString("11000000-0000-0000-0000-000000000001");
    private static final UUID HOSPITAL_ID = UUID.fromString("30000000-0000-0000-0000-000000000001");

    private CurrentUser makeUser(Set<String> roles, Set<String> privileges) {
        return new CurrentUser(USER_ID, "ext-001", "doctor@test.local", "Dr. Test",
                HOSPITAL_ID, roles, privileges);
    }

    // ── hasRole ───────────────────────────────────────────────────────────────

    @Test
    void shouldReturnTrueWhenUserHasRole() {
        CurrentUser user = makeUser(Set.of("DOCTOR"), Set.of());
        Assertions.assertTrue(user.hasRole("DOCTOR"));
    }

    @Test
    void shouldReturnFalseWhenUserDoesNotHaveRole() {
        CurrentUser user = makeUser(Set.of("DOCTOR"), Set.of());
        Assertions.assertFalse(user.hasRole("HOSPITAL_ADMIN"));
    }

    @Test
    void shouldReturnFalseForEmptyRoles() {
        CurrentUser user = makeUser(Set.of(), Set.of());
        Assertions.assertFalse(user.hasRole("DOCTOR"));
    }

    @Test
    void shouldHandleMultipleRoles() {
        CurrentUser user = makeUser(Set.of("DOCTOR", "HOSPITAL_ADMIN"), Set.of());
        Assertions.assertTrue(user.hasRole("DOCTOR"));
        Assertions.assertTrue(user.hasRole("HOSPITAL_ADMIN"));
        Assertions.assertFalse(user.hasRole("SYSTEM_ADMIN"));
    }

    // ── hasPrivilege ──────────────────────────────────────────────────────────

    @Test
    void shouldReturnTrueWhenUserHasPrivilege() {
        CurrentUser user = makeUser(Set.of(), Set.of("outbreaks.read", "diagnosis.write"));
        Assertions.assertTrue(user.hasPrivilege("outbreaks.read"));
        Assertions.assertTrue(user.hasPrivilege("diagnosis.write"));
    }

    @Test
    void shouldReturnFalseWhenPrivilegeAbsent() {
        CurrentUser user = makeUser(Set.of(), Set.of("outbreaks.read"));
        Assertions.assertFalse(user.hasPrivilege("admin.write"));
    }

    @Test
    void shouldReturnFalseForEmptyPrivileges() {
        CurrentUser user = makeUser(Set.of(), Set.of());
        Assertions.assertFalse(user.hasPrivilege("any.privilege"));
    }

    // ── isSystemAdmin ─────────────────────────────────────────────────────────

    @Test
    void shouldReturnTrueWhenSystemAdmin() {
        CurrentUser user = makeUser(Set.of("SYSTEM_ADMIN"), Set.of());
        Assertions.assertTrue(user.isSystemAdmin());
    }

    @Test
    void shouldReturnFalseWhenNotSystemAdmin() {
        CurrentUser user = makeUser(Set.of("DOCTOR"), Set.of());
        Assertions.assertFalse(user.isSystemAdmin());
    }

    @Test
    void shouldReturnFalseForEmptyRolesSystemAdmin() {
        CurrentUser user = makeUser(Set.of(), Set.of());
        Assertions.assertFalse(user.isSystemAdmin());
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    @Test
    void shouldReturnCorrectUserId() {
        CurrentUser user = makeUser(Set.of(), Set.of());
        Assertions.assertEquals(USER_ID, user.getUserId());
    }

    @Test
    void shouldReturnCorrectHospitalId() {
        CurrentUser user = makeUser(Set.of(), Set.of());
        Assertions.assertEquals(HOSPITAL_ID, user.getHospitalId());
    }

    @Test
    void shouldReturnNullHospitalIdWhenNotAssigned() {
        CurrentUser user = new CurrentUser(USER_ID, "ext-001", "doctor@test.local", "Dr. Test",
                null, Set.of("DOCTOR"), Set.of());
        Assertions.assertNull(user.getHospitalId());
    }

    @Test
    void shouldReturnCorrectEmail() {
        CurrentUser user = makeUser(Set.of(), Set.of());
        Assertions.assertEquals("doctor@test.local", user.getEmail());
    }

    @Test
    void shouldReturnCorrectFullName() {
        CurrentUser user = makeUser(Set.of(), Set.of());
        Assertions.assertEquals("Dr. Test", user.getFullName());
    }

    @Test
    void shouldReturnCorrectExternalAuthId() {
        CurrentUser user = makeUser(Set.of(), Set.of());
        Assertions.assertEquals("ext-001", user.getExternalAuthId());
    }

    @Test
    void shouldReturnCorrectRoles() {
        CurrentUser user = makeUser(Set.of("DOCTOR"), Set.of());
        Assertions.assertTrue(user.getRoles().contains("DOCTOR"));
    }

    @Test
    void shouldReturnCorrectPrivileges() {
        CurrentUser user = makeUser(Set.of(), Set.of("outbreaks.read"));
        Assertions.assertTrue(user.getPrivileges().contains("outbreaks.read"));
    }

    // ── Doctor role convenience check ─────────────────────────────────────────

    @Test
    void shouldBeAbleToCombineDoctorRoleWithPrivileges() {
        CurrentUser user = new CurrentUser(USER_ID, "ext-doctor", "doc@hospital.local", "Dr. House",
                HOSPITAL_ID, Set.of("DOCTOR"), Set.of("outbreaks.read", "diagnosis.assist", "diagnosis.write"));
        Assertions.assertTrue(user.hasRole("DOCTOR"));
        Assertions.assertTrue(user.hasPrivilege("outbreaks.read"));
        Assertions.assertTrue(user.hasPrivilege("diagnosis.assist"));
        Assertions.assertTrue(user.hasPrivilege("diagnosis.write"));
        Assertions.assertFalse(user.isSystemAdmin());
    }
}
