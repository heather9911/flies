package org.fedorahosted.flies.webtrans.client;

import net.customware.gwt.dispatch.client.DispatchAsync;
import net.customware.gwt.presenter.client.EventBus;
import net.customware.gwt.presenter.client.place.Place;
import net.customware.gwt.presenter.client.place.PlaceRequest;
import net.customware.gwt.presenter.client.widget.WidgetDisplay;
import net.customware.gwt.presenter.client.widget.WidgetPresenter;

import org.fedorahosted.flies.gwt.model.Person;
import org.fedorahosted.flies.gwt.model.PersonId;
import org.fedorahosted.flies.gwt.rpc.GetTranslatorList;
import org.fedorahosted.flies.gwt.rpc.GetTranslatorListResult;
import org.fedorahosted.flies.webtrans.client.events.EnterWorkspaceEvent;
import org.fedorahosted.flies.webtrans.client.events.EnterWorkspaceEventHandler;
import org.fedorahosted.flies.webtrans.client.events.ExitWorkspaceEvent;
import org.fedorahosted.flies.webtrans.client.events.ExitWorkspaceEventHandler;
import org.fedorahosted.flies.webtrans.client.rpc.CachingDispatchAsync;
import org.fedorahosted.flies.webtrans.client.ui.HasChildTreeNodes;
import org.fedorahosted.flies.webtrans.client.ui.HasFilter;
import org.fedorahosted.flies.webtrans.client.ui.HasNodeMouseOutHandlers;
import org.fedorahosted.flies.webtrans.client.ui.HasNodeMouseOverHandlers;
import org.fedorahosted.flies.webtrans.client.ui.TreeNodeImpl;

import com.allen_sauer.gwt.log.client.Log;
import com.google.gwt.event.dom.client.MouseOutEvent;
import com.google.gwt.event.dom.client.MouseOutHandler;
import com.google.gwt.event.dom.client.MouseOverEvent;
import com.google.gwt.event.dom.client.MouseOverHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.DecoratedPopupPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.inject.Inject;

public class WorkspaceUsersPresenter extends WidgetPresenter<WorkspaceUsersPresenter.Display> {

	private final DispatchAsync dispatcher;
	private final WorkspaceContext workspaceContext;
	private int latestStatusCountOffset = -1;
		
	public interface Display extends WidgetDisplay{
		HasChildTreeNodes<Person> getTree();
		HasFilter<Person> getFilter();
		HasNodeMouseOverHandlers getNodeMouseOver();
		HasNodeMouseOutHandlers getNodeMouseOut();
	}
	
	@Inject
	public WorkspaceUsersPresenter(final Display display, final EventBus eventBus,
			CachingDispatchAsync dispatcher,
		   WorkspaceContext workspaceContext) {
		super(display, eventBus);
		this.workspaceContext = workspaceContext;
		this.dispatcher = dispatcher;
		//loadTranslatorList();
	}
	
	
	@Override
	public Place getPlace() {
		return null;
	}

	@Override
	protected void onBind() {
		
		final DecoratedPopupPanel userPopupPanel = new DecoratedPopupPanel(true);
		
		getDisplay().getNodeMouseOver().addNodeMouseOverHandler(new MouseOverHandler() {

			@Override
			public void onMouseOver(MouseOverEvent event) {
				if (event.getSource() instanceof TreeNodeImpl<?, ?>) {
					TreeNodeImpl<PersonId, Person> source = (TreeNodeImpl<PersonId, Person>) event.getSource();	
					
					VerticalPanel popupMainPanel = new VerticalPanel();
					Person overPerson = source.getObject();
					Label popupTitle = new Label ("User Profile");
					Label userID = new Label("User ID: " + overPerson.getId().toString());
					Label userName = new Label ("User Name: " + overPerson.getName().toString());
					
					popupMainPanel.add(popupTitle);
					popupMainPanel.add(userID);
					popupMainPanel.add(userName);
					
					userPopupPanel.setWidget(popupMainPanel);
					userPopupPanel.setPopupPosition(source.getAbsoluteLeft() + 125, source.getAbsoluteTop() - 5);
					userPopupPanel.show();
				}
			}
			
		});
		
		getDisplay().getNodeMouseOut().addNodeMouseOutHandler(new MouseOutHandler() {
		
			@Override
			public void onMouseOut(MouseOutEvent event) {
				userPopupPanel.clear();
				userPopupPanel.hide();
			}
			
		});
		
		registerHandler(eventBus.addHandler(ExitWorkspaceEvent.getType(), new ExitWorkspaceEventHandler() {
			@Override
			public void onExitWorkspace(ExitWorkspaceEvent event) {
				if( event.getOffset() < latestStatusCountOffset){
					return;
				}
				loadTranslatorList();
			}
		}));
		
		registerHandler(eventBus.addHandler(EnterWorkspaceEvent.getType(), new EnterWorkspaceEventHandler() {
			@Override
			public void onEnterWorkspace(EnterWorkspaceEvent event) {
				if( event.getOffset() < latestStatusCountOffset){
					return;
				}
				loadTranslatorList();
			}
		}));
	}

	private void loadTranslatorList() {
		dispatcher.execute(new GetTranslatorList(workspaceContext.getProjectContainerId(), workspaceContext.getLocaleId()), new AsyncCallback<GetTranslatorListResult>() {
			@Override
			public void onFailure(Throwable caught) {
				Log.error("error");
			}
			@Override
			public void onSuccess(GetTranslatorListResult result) {
				getDisplay().getFilter().setList(result.getTranslatorList());
			}
	});		
	}


	@Override
	protected void onPlaceRequest(PlaceRequest request) {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void onUnbind() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void refreshDisplay() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void revealDisplay() {
		// TODO Auto-generated method stub
		
	}

}
