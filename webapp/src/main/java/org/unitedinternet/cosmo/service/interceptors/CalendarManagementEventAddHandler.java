package org.unitedinternet.cosmo.service.interceptors;

import org.unitedinternet.cosmo.service.impl.CalendarManagementService;

import org.springframework.beans.factory.annotation.Autowired;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.unitedinternet.cosmo.model.CollectionItem;
import org.unitedinternet.cosmo.model.ContentItem;
import org.unitedinternet.cosmo.model.EventStamp;
import org.unitedinternet.cosmo.model.StampUtils;

import net.fortuna.ical4j.model.component.VEvent;

/**
 * Simple <code>EventAddHandler</code> that only logs messages when events are added.
 * 
 * @author daniel grigore
 *
 */
@Order(1)
@Component
public class CalendarManagementEventAddHandler implements EventAddHandler {

    private static final Logger LOG = LoggerFactory.getLogger(CalendarManagementEventAddHandler.class);

    @Autowired
    private CalendarManagementService management;


    /**
     * Default constructor.
     */
    public CalendarManagementEventAddHandler() {}

    @Override
    public void beforeAdd(CollectionItem parent, Set<ContentItem> contentItems) {
        // Handle event
        for (ContentItem item : contentItems) {
            // Get event
            EventStamp eventStamp = StampUtils.getEventStamp(item);
            VEvent masterEvent = eventStamp.getMasterEvent();
            management.handleCalendarEvent(masterEvent);
        }
    }

    @Override
    public void afterAdd(CollectionItem parent, Set<ContentItem> contentItems) {}

}

