package com.walmartlabs.concord.server.org.project;

/*-
 * *****
 * Concord
 * -----
 * Copyright (C) 2017 - 2018 Walmart Inc.
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

import com.walmartlabs.concord.common.validation.ConcordKey;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.sonatype.siesta.Resource;
import org.sonatype.siesta.Validate;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Named
@Singleton
@Path("/api/v2/org")
@Tag(name = "Projects")
public class ProjectResourceV2 implements Resource {

    private final ProjectManager projectManager;

    @Inject
    public ProjectResourceV2(ProjectManager projectManager) {

        this.projectManager = projectManager;
    }

    @GET
    @Path("/{orgName}/project/{projectName}")
    @Produces(MediaType.APPLICATION_JSON)
    @Validate
    @Operation(description = "Get an existing project", operationId = "getProject")
    public ProjectEntry get(@PathParam("orgName") @ConcordKey String orgName,
                            @PathParam("projectName") @ConcordKey String projectName) {

        return projectManager.get(orgName, projectName);
    }
}
