package com.volunteerhub.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import com.jayway.jsonpath.JsonPath;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.volunteerhub.model.AccountStatus;
import com.volunteerhub.model.Role;
import com.volunteerhub.model.User;
import com.volunteerhub.repository.UserRepository;
import com.volunteerhub.security.JwtService;

/**
 * End-to-end tests through the real security filter chain, real JWT encode/decode, real services,
 * real repositories and an isolated in-memory H2 database. Each test is rolled back.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ApiIntegrationTest {

    private static final String PASSWORD = "Password123!";

    @Autowired MockMvc mvc;
    @Autowired UserRepository userRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired JwtService jwtService;
    @Autowired JwtEncoder jwtEncoder;

    // ---------- helpers ----------

    private User createUser(String email, Role role) {
        return userRepository.saveAndFlush(new User("Test " + role, email, passwordEncoder.encode(PASSWORD), role));
    }

    private String bearer(User u) {
        return "Bearer " + jwtService.issue(u).value();
    }

    private static String json(String... kv) {
        StringBuilder sb = new StringBuilder("{");
        for (int i = 0; i < kv.length; i += 2) {
            if (i > 0) sb.append(',');
            sb.append('"').append(kv[i]).append("\":").append(kv[i + 1]);
        }
        return sb.append('}').toString();
    }

    private static String q(String s) { return "\"" + s + "\""; }

    private String registerBody(String email, String role) {
        return json("name", q("Jane Doe"), "email", q(email), "password", q(PASSWORD), "role", q(role));
    }

    private String opportunityBody(String title) {
        return json("title", q(title), "description", q("Help out"), "location", q("Kurnool"), "eventDate", q("2030-01-15"));
    }

    // ---------- registration ----------

    @Test
    void register_validRequest_returns201WithSafeProfile_andStoresBcryptHash() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody("Jane@Example.com", "Volunteer")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.email").value("jane@example.com"))
                .andExpect(jsonPath("$.role").value("VOLUNTEER"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(content().string(not(containsString(PASSWORD))));

        User stored = userRepository.findByEmail("jane@example.com").orElseThrow();
        assertThat(stored.getPasswordHash()).startsWith("$2").isNotEqualTo(PASSWORD);
        assertThat(passwordEncoder.matches(PASSWORD, stored.getPasswordHash())).isTrue();
    }

    @Test
    void register_invalidData_returns400WithFieldErrors() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(json("name", q(""), "email", q("not-an-email"), "password", q("short"), "role", q("VOLUNTEER"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.name").exists())
                .andExpect(jsonPath("$.fieldErrors.email").exists())
                .andExpect(jsonPath("$.fieldErrors.password").exists());
    }

    @Test
    void register_malformedJsonOrUnknownRole_returns400() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content("{not json"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody("x@example.com", "SUPERUSER")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_duplicateEmail_returns409() throws Exception {
        createUser("dup@example.com", Role.VOLUNTEER);
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody("DUP@example.com", "VOLUNTEER")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void register_asAdmin_isRejected_andNothingIsStored() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody("evil@example.com", "ADMIN")))
                .andExpect(status().isForbidden());
        assertThat(userRepository.existsByEmail("evil@example.com")).isFalse();
    }

    // ---------- login ----------

    @Test
    void login_validCredentials_returnsUsableToken() throws Exception {
        createUser("login@example.com", Role.ORGANIZER);

        MvcResult result = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json("email", q("LOGIN@example.com"), "password", q(PASSWORD))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.user.role").value("ORGANIZER"))
                .andExpect(jsonPath("$.user.password").doesNotExist())
                .andReturn();

        String token = JsonPath.read(result.getResponse().getContentAsString(), "$.token");
        mvc.perform(get("/api/users/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("login@example.com"));
    }

    @Test
    void login_wrongPasswordAndUnknownEmail_looksIdentical() throws Exception {
        createUser("known@example.com", Role.VOLUNTEER);

        String wrongPassword = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json("email", q("known@example.com"), "password", q("WrongPass123"))))
                .andExpect(status().isUnauthorized()).andReturn().getResponse().getContentAsString();
        String unknownEmail = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json("email", q("ghost@example.com"), "password", q(PASSWORD))))
                .andExpect(status().isUnauthorized()).andReturn().getResponse().getContentAsString();

        assertThat((String) JsonPath.read(wrongPassword, "$.message"))
                .isEqualTo(JsonPath.read(unknownEmail, "$.message"));
    }

    @Test
    void login_suspendedAccount_isRejected() throws Exception {
        User u = createUser("suspended@example.com", Role.VOLUNTEER);
        u.setStatus(AccountStatus.SUSPENDED);
        userRepository.saveAndFlush(u);

        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json("email", q("suspended@example.com"), "password", q(PASSWORD))))
                .andExpect(status().isUnauthorized());
    }

    // ---------- token validation ----------

    @Test
    void protectedEndpoint_withoutToken_returns401() throws Exception {
        mvc.perform(get("/api/users/me")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/opportunities")).andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpoint_withValidToken_returns200() throws Exception {
        User u = createUser("me@example.com", Role.VOLUNTEER);
        mvc.perform(get("/api/users/me").header("Authorization", bearer(u)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(u.getId()))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void malformedToken_returns401() throws Exception {
        mvc.perform(get("/api/users/me").header("Authorization", "Bearer not.a.jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void expiredToken_returns401() throws Exception {
        User u = createUser("expired@example.com", Role.VOLUNTEER);
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder().issuer(JwtService.ISSUER).subject(String.valueOf(u.getId()))
                .issuedAt(now.minusSeconds(7200)).expiresAt(now.minusSeconds(3600)).claim("role", "VOLUNTEER").build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();

        mvc.perform(get("/api/users/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenSignedWithAnotherSecret_returns401() throws Exception {
        User u = createUser("forged@example.com", Role.VOLUNTEER);
        byte[] otherSecret = "a-completely-different-secret-value-9876543210".getBytes(StandardCharsets.UTF_8);
        JwtEncoder attacker = new NimbusJwtEncoder(new ImmutableSecret<>(new SecretKeySpec(otherSecret, "HmacSHA256")));
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder().issuer(JwtService.ISSUER).subject(String.valueOf(u.getId()))
                .issuedAt(now).expiresAt(now.plusSeconds(3600)).claim("role", "ADMIN").build();
        String forged = attacker.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();

        mvc.perform(get("/api/users/me").header("Authorization", "Bearer " + forged))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenOfSuspendedOrDeletedUser_isRejectedImmediately() throws Exception {
        User suspended = createUser("later-suspended@example.com", Role.VOLUNTEER);
        String suspendedToken = bearer(suspended);
        suspended.setStatus(AccountStatus.SUSPENDED);
        userRepository.saveAndFlush(suspended);
        mvc.perform(get("/api/users/me").header("Authorization", suspendedToken)).andExpect(status().isUnauthorized());

        User deleted = createUser("later-deleted@example.com", Role.VOLUNTEER);
        String deletedToken = bearer(deleted);
        userRepository.delete(deleted);
        userRepository.flush();
        mvc.perform(get("/api/users/me").header("Authorization", deletedToken)).andExpect(status().isUnauthorized());
    }

    // ---------- RBAC ----------

    @Test
    void adminEndpoints_volunteerAndOrganizerGet403_adminGets200_anonymousGets401() throws Exception {
        User volunteer = createUser("v@example.com", Role.VOLUNTEER);
        User organizer = createUser("o@example.com", Role.ORGANIZER);
        User admin = createUser("a@example.com", Role.ADMIN);

        mvc.perform(get("/api/admin/users")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/admin/users").header("Authorization", bearer(volunteer))).andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/users").header("Authorization", bearer(organizer))).andExpect(status().isForbidden());
        mvc.perform(patch("/api/admin/users/" + volunteer.getId() + "/role").header("Authorization", bearer(organizer))
                        .contentType(MediaType.APPLICATION_JSON).content(json("role", q("ADMIN"))))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/users").header("Authorization", bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].password").doesNotExist());
        assertThat(userRepository.findById(volunteer.getId()).orElseThrow().getRole()).isEqualTo(Role.VOLUNTEER);
    }

    @Test
    void admin_canChangeRoleAndStatus_andRoleChangeAppliesToExistingToken() throws Exception {
        User admin = createUser("a2@example.com", Role.ADMIN);
        User volunteer = createUser("v2@example.com", Role.VOLUNTEER);
        String volunteerToken = bearer(volunteer);

        mvc.perform(post("/api/opportunities").header("Authorization", volunteerToken)
                        .contentType(MediaType.APPLICATION_JSON).content(opportunityBody("Beach cleanup")))
                .andExpect(status().isForbidden());

        mvc.perform(patch("/api/admin/users/" + volunteer.getId() + "/role").header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON).content(json("role", q("ORGANIZER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ORGANIZER"));

        // same token as before: authorities come from the DB, so the new role is effective at once
        mvc.perform(post("/api/opportunities").header("Authorization", volunteerToken)
                        .contentType(MediaType.APPLICATION_JSON).content(opportunityBody("Beach cleanup")))
                .andExpect(status().isCreated());

        mvc.perform(patch("/api/admin/users/" + volunteer.getId() + "/status").header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON).content(json("status", q("SUSPENDED"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUSPENDED"));
        mvc.perform(get("/api/users/me").header("Authorization", volunteerToken)).andExpect(status().isUnauthorized());
    }

    @Test
    void admin_cannotDemoteSuspendOrDeleteTheLastActiveAdmin() throws Exception {
        User admin = createUser("only-admin@example.com", Role.ADMIN);
        String token = bearer(admin);

        mvc.perform(patch("/api/admin/users/" + admin.getId() + "/role").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content(json("role", q("VOLUNTEER"))))
                .andExpect(status().isConflict());
        mvc.perform(patch("/api/admin/users/" + admin.getId() + "/status").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content(json("status", q("SUSPENDED"))))
                .andExpect(status().isConflict());
        mvc.perform(delete("/api/admin/users/" + admin.getId()).header("Authorization", token))
                .andExpect(status().isConflict());
    }

    @Test
    void admin_missingUserAndBadIdAreHandledSafely() throws Exception {
        User admin = createUser("a3@example.com", Role.ADMIN);
        mvc.perform(get("/api/admin/users/999999").header("Authorization", bearer(admin)))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/admin/users/abc").header("Authorization", bearer(admin)))
                .andExpect(status().isBadRequest());
        mvc.perform(patch("/api/admin/users/" + admin.getId() + "/role").header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON).content(json("role", q("KING"))))
                .andExpect(status().isBadRequest());
    }

    // ---------- profile ----------

    @Test
    void updateProfile_changesName_butIgnoresRoleStatusEmailAndIdInBody() throws Exception {
        User victim = createUser("victim@example.com", Role.VOLUNTEER);
        User attacker = createUser("attacker@example.com", Role.VOLUNTEER);

        mvc.perform(put("/api/users/me").header("Authorization", bearer(attacker))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("name", q("Renamed"), "role", q("ADMIN"), "status", q("ACTIVE"),
                                "email", q("hijack@example.com"), "id", String.valueOf(victim.getId()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Renamed"))
                .andExpect(jsonPath("$.role").value("VOLUNTEER"))
                .andExpect(jsonPath("$.email").value("attacker@example.com"));

        User a = userRepository.findById(attacker.getId()).orElseThrow();
        User v = userRepository.findById(victim.getId()).orElseThrow();
        assertThat(a.getRole()).isEqualTo(Role.VOLUNTEER);
        assertThat(a.getName()).isEqualTo("Renamed");
        assertThat(v.getName()).isEqualTo("Test VOLUNTEER"); // someone else's profile untouched
        assertThat(v.getEmail()).isEqualTo("victim@example.com");
    }

    @Test
    void updateProfile_blankName_returns400() throws Exception {
        User u = createUser("blank@example.com", Role.VOLUNTEER);
        mvc.perform(put("/api/users/me").header("Authorization", bearer(u))
                        .contentType(MediaType.APPLICATION_JSON).content(json("name", q("  "))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void volunteerCannotReadAnotherUsersProfile() throws Exception {
        User v = createUser("reader@example.com", Role.VOLUNTEER);
        User other = createUser("other@example.com", Role.VOLUNTEER);
        mvc.perform(get("/api/admin/users/" + other.getId()).header("Authorization", bearer(v)))
                .andExpect(status().isForbidden());
    }

    // ---------- organizer ownership ----------

    @Test
    void organizer_canManageOnlyOwnOpportunities_volunteerCanOnlyRead() throws Exception {
        User owner = createUser("owner@example.com", Role.ORGANIZER);
        User otherOrganizer = createUser("other-org@example.com", Role.ORGANIZER);
        User volunteer = createUser("vol@example.com", Role.VOLUNTEER);
        User admin = createUser("mod@example.com", Role.ADMIN);

        MvcResult created = mvc.perform(post("/api/opportunities").header("Authorization", bearer(owner))
                        .contentType(MediaType.APPLICATION_JSON).content(opportunityBody("Food drive")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.organizerId").value(owner.getId()))
                .andReturn();
        int id = JsonPath.read(created.getResponse().getContentAsString(), "$.id");

        mvc.perform(get("/api/opportunities").header("Authorization", bearer(volunteer)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Food drive"));

        mvc.perform(put("/api/opportunities/" + id).header("Authorization", bearer(otherOrganizer))
                        .contentType(MediaType.APPLICATION_JSON).content(opportunityBody("Hijacked")))
                .andExpect(status().isForbidden());
        mvc.perform(put("/api/opportunities/" + id).header("Authorization", bearer(volunteer))
                        .contentType(MediaType.APPLICATION_JSON).content(opportunityBody("Hijacked")))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/api/opportunities/" + id).header("Authorization", bearer(otherOrganizer)))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/api/opportunities/" + id).header("Authorization", bearer(volunteer)))
                .andExpect(status().isForbidden());

        mvc.perform(put("/api/opportunities/" + id).header("Authorization", bearer(owner))
                        .contentType(MediaType.APPLICATION_JSON).content(opportunityBody("Food drive 2")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Food drive 2"));

        mvc.perform(delete("/api/opportunities/" + id).header("Authorization", bearer(admin)))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/opportunities/" + id).header("Authorization", bearer(owner)))
                .andExpect(status().isNotFound());
    }

    @Test
    void opportunity_validationErrorsReturn400() throws Exception {
        User organizer = createUser("val-org@example.com", Role.ORGANIZER);
        mvc.perform(post("/api/opportunities").header("Authorization", bearer(organizer))
                        .contentType(MediaType.APPLICATION_JSON).content(json("title", q(""))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.title").exists());
    }

    // ---------- CORS / unknown routes ----------

    @Test
    void corsPreflight_allowsConfiguredOriginOnly() throws Exception {
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options("/api/auth/login")
                        .header("Origin", "http://localhost:3000")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "authorization,content-type"))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .header().string("Access-Control-Allow-Origin", "http://localhost:3000"));
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options("/api/auth/login")
                        .header("Origin", "http://evil.example")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isForbidden());
    }
}
