package org.fedorahosted.flies.webtrans;

import java.util.concurrent.ConcurrentHashMap;

import org.fedorahosted.flies.FliesInit;
import org.fedorahosted.flies.core.model.FliesLocale;
import org.fedorahosted.flies.core.model.ProjectIteration;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Destroy;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Observer;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.log.Log;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;

@Scope(ScopeType.APPLICATION)
@Name("translationWorkspaceManager")
public class TranslationWorkspaceManager {

	@Logger
	private Log log;

	private final ConcurrentHashMap<WorkspaceKey, TranslationWorkspace> workspaceMap;
	private final Multimap<ProjectIteration, FliesLocale> projectIterationLocaleMap;
	private final Multimap<FliesLocale, TranslationWorkspace> localeWorkspaceMap;

	public TranslationWorkspaceManager() {
		this.workspaceMap = new ConcurrentHashMap<WorkspaceKey, TranslationWorkspace>();

		Multimap<ProjectIteration, FliesLocale> projectIterationLocaleMap = HashMultimap.create();
		this.projectIterationLocaleMap = Multimaps.synchronizedMultimap(projectIterationLocaleMap);

		Multimap<FliesLocale, TranslationWorkspace> localeWorkspaceMap = HashMultimap.create();
		this.localeWorkspaceMap = Multimaps.synchronizedMultimap(localeWorkspaceMap);
		
	}
	
	@Observer(FliesInit.EVENT_Flies_Startup)
	public void start(){
		log.info("starting...");
	}
	
	@Destroy
	public void stop(){
		log.info("stopping...");
		log.info("closing down {0} workspaces: ", workspaceMap.size());
	}
	
	public ImmutableSet<FliesLocale> getLocales(ProjectIteration projectIteration){
		return ImmutableSet.copyOf(projectIterationLocaleMap.get(projectIteration));
	}

	public ImmutableSet<FliesLocale> getLocales(){
		return ImmutableSet.copyOf(localeWorkspaceMap.keySet());
	}
	
	public int getWorkspaceCount(){
		return workspaceMap.size();
	}

	public TranslationWorkspace getOrRegisterWorkspace(ProjectIteration projectIteration, FliesLocale locale){
		WorkspaceKey key = new WorkspaceKey(projectIteration, locale);
		TranslationWorkspace workspace = workspaceMap.get(key);
		if(workspace == null){
			workspace = new TranslationWorkspace(projectIteration, locale);
			TranslationWorkspace prev = workspaceMap.putIfAbsent(key, workspace);
			
			if(prev == null){
				projectIterationLocaleMap.put(projectIteration, locale);
				localeWorkspaceMap.put(locale, workspace);
			}
			
			return prev == null ? workspace : prev;
		}
		return workspace;
	}
	
	public ImmutableSet<TranslationWorkspace> getWorkspaces(FliesLocale locale){
		return ImmutableSet.copyOf(localeWorkspaceMap.get(locale));
	}
	
	public ImmutableSet<ProjectIteration> getProjectIterations(){
		return ImmutableSet.copyOf(projectIterationLocaleMap.keySet());
	}
	
	
}
