package org.fedorahosted.flies.core.model;

import java.util.Set;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.OneToOne;

import org.hibernate.validator.NotNull;

/**
 * A community represents people from different tribes coming together
 * around a common cause, e.g. to translate a set of projects.
 * 
 * A community has goals/targets, priorities
 * 
 * @author asgeirf
 *
 */
@Entity
public class Community extends AbstractFliesEntity{
	
	private Person owner;
	private Set<Person> officers;
	private Set<Person> members;
	/*
	private Set<Community> parentCommunities;
	private Set<Community> childCommunities;
	
	private Set<Project> associatedProjects;
	*/
	
	@NotNull
	@OneToOne(optional = false, fetch = FetchType.EAGER)
	@JoinColumn(name = "ownerId")
	public Person getOwner() {
		return owner;
	}
	
	public void setOwner(Person owner) {
		this.owner = owner;
	}
	
	@ManyToMany
	@JoinTable(name = "Community_Officer", joinColumns = @JoinColumn(name = "communityId"), inverseJoinColumns = @JoinColumn(name = "personId"))
	public Set<Person> getOfficers() {
		return officers;
	}
	
	public void setOfficers(Set<Person> officers) {
		this.officers = officers;
	}
	
	@ManyToMany
	@JoinTable(name = "Community_Member", joinColumns = @JoinColumn(name = "communityId"), inverseJoinColumns = @JoinColumn(name = "personId"))
	public Set<Person> getMembers() {
		return members;
	}
	
	public void setMembers(Set<Person> members) {
		this.members = members;
	}
/*	
	public Set<Community> getParentCommunities() {
		return parentCommunities;
	}
	
	public void setParentCommunities(Set<Community> parentCommunities) {
		this.parentCommunities = parentCommunities;
	}
	
	public Set<Community> getChildCommunities() {
		return childCommunities;
	}
	
	public void setChildCommunities(Set<Community> childCommunities) {
		this.childCommunities = childCommunities;
	}
	
	public Set<Project> getAssociatedProjects() {
		return associatedProjects;
	}
	
	public void setAssociatedProjects(Set<Project> associatedProjects) {
		this.associatedProjects = associatedProjects;
	}
	*/
	
}