package org.fedorahosted.flies.webtrans.shared.model;

import com.google.gwt.user.client.rpc.IsSerializable;

public class ResourceId implements IsSerializable{
	private Long id;

	private ResourceId() {
	}
	
	public ResourceId(long id) {
		this.id = id;
	}
	
	public Long getId() {
		return id;
	}
	
	@Override
	public int hashCode() {
		return id.hashCode();
	}
	
	@Override
	public String toString() {
		return String.valueOf(id);
	}
}
