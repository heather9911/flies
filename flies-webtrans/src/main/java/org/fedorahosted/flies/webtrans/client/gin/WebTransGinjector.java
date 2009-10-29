package org.fedorahosted.flies.webtrans.client.gin;


import net.customware.gwt.presenter.client.EventBus;
import net.customware.gwt.presenter.client.place.PlaceManager;

import org.fedorahosted.flies.webtrans.client.AppPresenter;
import org.fedorahosted.flies.webtrans.client.WorkspaceContext;
import org.fedorahosted.flies.webtrans.client.auth.Identity;
import org.fedorahosted.flies.webtrans.client.auth.IdentityImpl;

import com.google.gwt.inject.client.GinModules;
import com.google.gwt.inject.client.Ginjector;

@GinModules({ WebTransClientModule.class })
public interface WebTransGinjector extends Ginjector {

	AppPresenter getAppPresenter();

	PlaceManager getPlaceManager();
	
	EventBus getEventBus();

	WorkspaceContext getWorkspaceContext();
	
	Identity getIdentity();

}