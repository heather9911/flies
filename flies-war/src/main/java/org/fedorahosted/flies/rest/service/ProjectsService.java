package org.fedorahosted.flies.rest.service;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.fedorahosted.flies.rest.MediaTypes;
import org.fedorahosted.flies.rest.dto.ProjectRefs;
import org.jboss.seam.Component;
import org.jboss.seam.annotations.Name;

@Name("projectsService")
@Path("/projects")
public class ProjectsService {

	@GET
	@Produces({ MediaTypes.APPLICATION_FLIES_PROJECTS_XML, MediaType.APPLICATION_JSON })
	public ProjectRefs get() {

		// Delegate to hot deployed component
		ProjectsServiceAction impl = (ProjectsServiceAction) Component.getInstance("projectsServiceAction");
		return impl.get();
	}
}
