package stroom.jdbc.shared;

import stroom.docref.DocRef;
import stroom.util.shared.FetchWithUuid;
import stroom.util.shared.ResourceGeneration;
import stroom.util.shared.ResourcePaths;
import stroom.util.shared.RestResource;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.fusesource.restygwt.client.DirectRestService;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Tag(name = "JDBC Database Config")
@Path("/JDBCDatabaseConfig" + ResourcePaths.V1)
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface JDBCConfigResource extends RestResource, DirectRestService, FetchWithUuid<JDBCConfigDoc> {

    @GET
    @Path("/{uuid}")
    @Operation(
            summary = "Fetch a JDBCConfig doc by its UUID",
            operationId = "fetchJDBCConfig")
    JDBCConfigDoc fetch(@PathParam("uuid") String uuid);

    @PUT
    @Path("/{uuid}")
    @Operation(
            summary = "Update a JDBCConfig doc",
            operationId = "updateJDBCConfig")
    JDBCConfigDoc update(
            @PathParam("uuid") String uuid, @Parameter(description = "doc", required = true) JDBCConfigDoc doc);

    @POST
    @Path("/download")
    @Operation(
            summary = "Download a JDBCConfig doc",
            operationId = "downloadJDBCConfig")
    ResourceGeneration download(@Parameter(description = "docRef", required = true) DocRef docRef);
}
