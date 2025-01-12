package com.walmartlabs.concord.it.server;

/*-
 * *****
 * Concord
 * -----
 * Copyright (C) 2017 - 2022 Walmart Inc.
 * -----
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =====
 */

import com.walmartlabs.concord.client.*;
import org.junit.jupiter.api.Test;

import java.util.*;

import static com.walmartlabs.concord.it.common.ITUtils.archive;
import static com.walmartlabs.concord.it.common.ServerClient.*;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class SecretProjectsIT extends AbstractServerIT {

    @Test
    public void test() throws Exception {

        OrganizationsApi orgApi = new OrganizationsApi(getApiClient());
        String orgName = "org_" + randomString();
        orgApi.createOrUpdate(new OrganizationEntry().setName(orgName));

        String projectName1 = "project_" + randomString();
        String projectName2 = "project_" + randomString();
        String projectName3 = "project_" + randomString();
        ProjectsApi projectsApi = new ProjectsApi(getApiClient());
        projectsApi.createOrUpdate(orgName, new ProjectEntry()
                .setName(projectName1).setRawPayloadMode(ProjectEntry.RawPayloadModeEnum.EVERYONE));
        projectsApi.createOrUpdate(orgName, new ProjectEntry()
                .setName(projectName2).setRawPayloadMode(ProjectEntry.RawPayloadModeEnum.EVERYONE));
        projectsApi.createOrUpdate(orgName, new ProjectEntry()
                .setName(projectName3).setRawPayloadMode(ProjectEntry.RawPayloadModeEnum.EVERYONE));

        String secretName = "secret_" + randomString();
        String storePassword = "St0rePassword1";
        byte[] secret = "C0nC0rD".getBytes();
        addPlainSecretWithProjectNames(orgName, secretName, new HashSet<>(Arrays.asList(projectName1, projectName2)), false, storePassword, secret);

        byte[] payload = archive(SecretProjectsIT.class.getResource("secretProjects").toURI());

        // Creating non admin user

        UsersApi usersApi = new UsersApi(getApiClient());
        TeamsApi teamsApi = new TeamsApi(getApiClient());
        ApiKeysApi apiKeyResource = new ApiKeysApi(getApiClient());

        String userName = "userA_" + randomString();
        CreateUserResponse userResponse = usersApi.createOrUpdate(new CreateUserRequest()
                .setUsername(userName)
                .setType(CreateUserRequest.TypeEnum.LOCAL));
        CreateApiKeyResponse apiKeyResponse = apiKeyResource.create(new CreateApiKeyRequest()
                .setUsername(userName)
                .setUserType(CreateApiKeyRequest.UserTypeEnum.LOCAL));

        String teamName = "team_" + randomString();
        teamsApi.createOrUpdate(orgName, new TeamEntry().setName(teamName));
        teamsApi.addUsers(orgName, teamName, false, Collections.singletonList(new TeamUserEntry()
                .setUsername(userName)
                .setRole(TeamUserEntry.RoleEnum.MEMBER)));

        resetApiKey();
        setApiKey(apiKeyResponse.getKey());

        Map<String, Object> input = new HashMap<>();
        input.put("archive", payload);
        input.put("org", orgName);
        input.put("arguments.secretName", secretName);
        input.put("arguments.storePassword", storePassword);
        input.put("arguments.orgName", orgName);
        input.put("project", projectName3);

        StartProcessResponse spr = start(input);

        ProcessApi processApi = new ProcessApi(getApiClient());
        ProcessEntry pir = waitForCompletion(processApi, spr.getInstanceId());
        assertNotNull(pir.getLogFileName());

        byte[] bytes = getLog(pir.getLogFileName());
        assertLog(".*Project-scoped secrets can only be accessed within the project they belong to.*", 2, bytes);

        input.put("project", projectName2);
        spr = start(input);

        processApi = new ProcessApi(getApiClient());
        pir = waitForCompletion(processApi, spr.getInstanceId());
        assertNotNull(pir.getLogFileName());

        bytes = getLog(pir.getLogFileName());
        assertLog(".*C0nC0rD.*", bytes);

        input.put("project", projectName1);
        spr = start(input);

        processApi = new ProcessApi(getApiClient());
        pir = waitForCompletion(processApi, spr.getInstanceId());
        assertNotNull(pir.getLogFileName());

        bytes = getLog(pir.getLogFileName());
        assertLog(".*C0nC0rD.*", bytes);

        resetApiKey();
        setApiKey(DEFAULT_API_KEY);
        projectsApi.delete(orgName, projectName1);
        projectsApi.delete(orgName, projectName2);
        projectsApi.delete(orgName, projectName3);

        SecretsApi secretsApi = new SecretsApi(getApiClient());
        secretsApi.delete(orgName, secretName);

        usersApi.delete(userResponse.getId());
        teamsApi.delete(orgName, teamName);
        orgApi.delete(orgName, "yes");

    }
}
