package org.fedorahosted.flies.rest.dto.resource;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import org.codehaus.jackson.annotate.JsonValue;
import org.fedorahosted.flies.common.Namespaces;
import org.fedorahosted.flies.rest.dto.HasSample;

/**
 * 
 * This class is only used for generating the schema,
 * as List<ResourceMeta> serializes better across Json and XML.
 * 
 * @author asgeirf
 *
 */
@XmlType(name="resourcesListType", namespace=Namespaces.FLIES, propOrder={"resources"})
@XmlRootElement(name="resources", namespace=Namespaces.FLIES)
public class ResourceMetaList implements Serializable, HasSample<ResourceMetaList> {
	
	private List<ResourceMeta> resources;
	
	@XmlElement(name="resource", namespace=Namespaces.FLIES, required=true)
	@JsonValue
	public List<ResourceMeta> getResources() {
		if(resources == null) {
			resources = new ArrayList<ResourceMeta>();
		}
		return resources;
	}
	
	@Override
	public ResourceMetaList createSample() {
		ResourceMetaList entity = new ResourceMetaList();
		entity.getResources().addAll(new ResourceMeta().createSamples());
		return entity;
	}
}
