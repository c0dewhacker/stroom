package stroom.security.server;

import stroom.security.impl.UserAppPermissionService;
import stroom.security.impl.UserService;
import stroom.security.shared.User;
import stroom.util.shared.RestResource;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Set;


/**
 * NB: This class has been back-ported from Stroom's master branch. This was necessary if we wanted to use
 * stroom-auth 7.0 with stroom 6.0.
 */
@Path("/appPermissions/v1")
@Produces(MediaType.APPLICATION_JSON)
public class UserAppPermissionResource implements RestResource {
    private final UserAppPermissionService userAppPermissionService;
    private UserService userService;

    @Inject
    public UserAppPermissionResource(final UserAppPermissionService userAppPermissionService, UserService userService) {
        this.userAppPermissionService = userAppPermissionService;
        this.userService = userService;
    }

    @GET
    @Path("/byName/{userName}")
    public Response getPermissionNamesForUserName(@PathParam("userName") final String userName) {
        User userRef = userService.getUserByName(userName);
        final Set<String> permissions = userAppPermissionService.getPermissionsForUser(userRef).getUserPermissons();
        return Response.ok(permissions).build();
    }
}
