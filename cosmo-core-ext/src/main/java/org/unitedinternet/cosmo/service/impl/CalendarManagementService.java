package org.unitedinternet.cosmo.service.impl;


import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.Date;
import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.TimeZone;
import net.fortuna.ical4j.model.TimeZoneRegistry;
import net.fortuna.ical4j.model.TimeZoneRegistryFactory;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.property.Clazz;
import net.fortuna.ical4j.model.property.Summary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.unitedinternet.cosmo.dao.CalendarDao;
import org.unitedinternet.cosmo.dao.ContentDao;
import org.unitedinternet.cosmo.dao.external.UuidExternalGenerator;
import org.unitedinternet.cosmo.dao.subscription.UuidSubscriptionGenerator;
import org.unitedinternet.cosmo.model.CollectionItem;
import org.unitedinternet.cosmo.model.ContentItem;
import org.unitedinternet.cosmo.model.EventStamp;
import org.unitedinternet.cosmo.model.ICalendarItem;
import org.unitedinternet.cosmo.model.Item;
import org.unitedinternet.cosmo.model.NoteItem;
import org.unitedinternet.cosmo.model.StampUtils;
import org.unitedinternet.cosmo.model.User;
import org.unitedinternet.cosmo.model.filter.EventStampFilter;
import org.unitedinternet.cosmo.model.filter.NoteItemFilter;
import org.unitedinternet.cosmo.model.hibernate.HibEventStamp;
import org.unitedinternet.cosmo.model.hibernate.HibQName;
import org.unitedinternet.cosmo.service.CalendarService;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


@Service
public class CalendarManagementService implements CalendarService {

    private static final TimeZoneRegistry TIMEZONE_REGISTRY = TimeZoneRegistryFactory.getInstance().createRegistry();

    @Autowired
    private CalendarDao calendarDao;

    @Autowired
    private ContentDao contentDao;

    /**
     * logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(CalendarManagementService.class);


    /**
     * management commands names
     */
    private static final String CMD_HELP = "calendar-manage: help";
    private static final String CMD_SHARED_READ = "calendar-manage: share-read";
    private static final String CMD_SHARED_READ_WRITE = "calendar-manage: share-readwrite";
    private static final String CMD_SHARED_WRITE = "calendar-manage: share-write";
    private static final String CMD_SET_NAME = "calendar-manage: set-name";
    private static final String CMD_SET_DEFAULT_CAL = "calendar-manage: set-default-calendar";
    private static final String CMD_DOM_INV = "calendar-manage: allow-domain-invites";
    private static final String CMD_ADD_CAL = "calendar-manage: add-calendar";

    private static final String CMD_SET_CAL_COLOUR = "calendar-manage: set-calendar-colour";
    private static final String CMD_SET_CAL_DEFAULT_ALERT = "calendar-manage: set-calendar-default-alert";
    private static final String CMD_SET_CAL_DEFAULT_DURATION = "calendar-manage: set-calendar-default-duration";
    private static final String CMD_SET_CAL_DEFAULT_HOME = "calendar-manage: set-calendar-default-home";
    private static final String CMD_SET_CAL_NOTIFICATION_LOCATION = "calendar-manage: set-calendar-notification-location";
    private static final String CMD_SET_CAL_TIMEZONE = "calendar-manage: set-calendar-timezone";
    private static final String CMD_SET_CAL_LOG_AUDIT = "calendar-manage: set-calendar-log-audit";
    private static final String CMD_SET_CAL_NOTIFICATION_DELETES = "calendar-manage: set-calendar-notification-deletes";


    /**
     * list of allowed management commands
     */
    private final List<String> allowedCommands = Arrays.asList(
        CMD_HELP,
        CMD_SHARED_READ,
        CMD_SHARED_READ_WRITE,
        CMD_SHARED_WRITE,
        CMD_SET_NAME,
        CMD_SET_DEFAULT_CAL,
        CMD_DOM_INV,
        CMD_ADD_CAL,
        CMD_SET_CAL_COLOUR,
        CMD_SET_CAL_DEFAULT_ALERT,
        CMD_SET_CAL_DEFAULT_DURATION,
        CMD_SET_CAL_DEFAULT_HOME,
        CMD_SET_CAL_NOTIFICATION_LOCATION,
        CMD_SET_CAL_TIMEZONE,
        CMD_SET_CAL_LOG_AUDIT,
        CMD_SET_CAL_NOTIFICATION_DELETES
    );

    /**
     * method to validate if the event is a valid management event
     */
    public boolean isValidManagementEvent(VEvent event) {
        // Ensure CLASS is PRIVATE
        Clazz classification = event.getClassification();
        if (classification == null) {
            return false;
        }
        if (Clazz.PRIVATE != classification) {
            return false;
        }

        // Check if the SUMMARY matches one of the allowed commands
        Summary summaryProperty = event.getSummary();
        if (summaryProperty == null) {
            return false;
        }
        String summary = summaryProperty.getValue().trim();
        return allowedCommands.contains(summary);
    }

    /**
     * method to process the event if it's valid
     */
    public boolean handleCalendarEvent(CollectionItem collection, ContentItem item) {
        // Get event
        EventStamp eventStamp = StampUtils.getEventStamp(item);
        VEvent masterEvent = eventStamp.getMasterEvent();
        if (isValidManagementEvent(masterEvent)) {
            boolean success = processEvent(masterEvent);
            if (success) {
                HibQName key = new HibQName(
                        "org.unitedinternet.cosmo.model.NoteItem",
                        "body"
                );
                item.setAttribute(
                        key,
                        masterEvent.getDescription().getValue()
                );

                HibEventStamp  hibEventStamp = (HibEventStamp) eventStamp;
                String iCalData = hibEventStamp.getIcaldata();
                String updatedDescription = replaceDescriptionBlock(iCalData, masterEvent.getDescription().toString());
                hibEventStamp.setIcaldata(updatedDescription);
            }
            return success;
        } else {
            return false;
        }
    }

    private static String ensureLineLength(String input) {
        final int MAX_LINE_LENGTH = 75;
        StringBuilder result = new StringBuilder();
        String[] lines = input.split("\n");

        for (String line : lines) {
            if (line.length() <= MAX_LINE_LENGTH) {
                result.append(line).append("\n");
            } else {
                int start = 0;
                int shift = 0;
                while (start < line.length()) {
                    if (start > 0) {
                        shift = 1;
                    }
                    int end = Math.min(start + MAX_LINE_LENGTH - shift, line.length());
                    result.append(line, start, end).append("\n");
                    start = end;
                    if (start < line.length()) {
                        result.append(" ");
                    }
                }
            }
        }
        return result.toString();
    }

    private static String replaceDescriptionBlock(String original, String newDescription) {
        String regex = "(?m)^DESCRIPTION;";
        String[] blocks = original.split(regex);
        if (blocks.length > 1) {
            String prep = blocks[0];
            String post = blocks[1];
            String[] lines = post.split("(?m)\n");

            StringBuilder updated = new StringBuilder();

            for (int i = 1; i < lines.length; i++) {
                if (lines[i].indexOf(" ") != 0) {
                    updated.append(lines[i]).append("\n");
                }
            }
            return prep + ensureLineLength(newDescription) + updated;
        }

        return original;
    }

    /**
     * implement the logic for processing management events here
     */
    private boolean processEvent(VEvent event) {
        // Based on the event's SUMMARY, process the management command
        String summary = event.getSummary().getValue().trim();
        switch (summary) {
            case CMD_HELP:
                LOG.info("Processing help");
                updateDescription(event,"Available commands\n\n"+String.join("\n",allowedCommands));
                break;
            case CMD_SHARED_READ:
                // Process sharing read permissions
                LOG.info("Processing share-read");
                updateDescription(event,"testing");
                break;
            case CMD_SET_NAME:
                // Process setting calendar name
                LOG.info("Processing set-name");
                break;
            case CMD_ADD_CAL:
                // Process adding a new calendar
                LOG.info("Processing add-calendar");
                break;
            // Add other cases for different management commands
            default:
                LOG.error("Unsupported management command: " + summary);
                return false;
        }
        return true;
    }

    /**
     * method to update events description with result
     */
    public void updateDescription(VEvent event, String text) {
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        SimpleDateFormat format = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
        try {
            Property description = event.getProperty(Property.DESCRIPTION);
            description.setValue(description.getValue() + "\n\n======= update (" + format.format(calendar.getTime()) + ") =======\n");
            description.setValue(description.getValue() + text);
        } catch (Exception e) {
            LOG.error(e.toString());
            //return false;
        }
    }

    @Override
    public Set<Item> findEvents(CollectionItem collection, Date rangeStart, Date rangeEnd, String timeZoneId,
            boolean expandRecurringEvents) {
        Set<Item> resultSet = new HashSet<>();
        String uid = collection.getUid();
        if (UuidExternalGenerator.get().containsUuid(uid) || UuidSubscriptionGenerator.get().containsUuid(uid)) {
            NoteItemFilter filter = new NoteItemFilter();
            filter.setParent(collection);
            EventStampFilter eventFilter = new EventStampFilter();
            eventFilter.setTimeRange(rangeStart, rangeEnd);
            if (timeZoneId != null) {
                TimeZone timezone = TIMEZONE_REGISTRY.getTimeZone(timeZoneId);
                eventFilter.setTimezone(timezone);
            }
            filter.getStampFilters().add(eventFilter);
            Set<Item> externalItems = this.contentDao.findItems(filter);
            if (externalItems != null) {
                resultSet.addAll(externalItems);
            }
        } else {
            Set<Item> internalItems = calendarDao.findEvents(collection, rangeStart, rangeEnd, timeZoneId,
                    expandRecurringEvents);
            resultSet.addAll(internalItems);
        }
        return resultSet;
    }

    @Override
    public ContentItem findEventByIcalUid(String uid, CollectionItem collection) {
        return calendarDao.findEventByIcalUid(uid, collection);
    }

    @Override
    public Map<String, List<NoteItem>> findNoteItemsByIcalUid(Item collection, List<String> uid) {
        Map<String, List<NoteItem>> itemsMap = new HashMap<>();

        if (!(collection instanceof CollectionItem)) {
            return itemsMap;
        }
        CollectionItem collectionItem = (CollectionItem) collection;
        List<NoteItem> items = new ArrayList<>();
        for (String id : uid) {
            ContentItem item = calendarDao.findEventByIcalUid(id, collectionItem);
            if (item != null) {
                items.add((NoteItem) item);
            }
        }
        itemsMap.put(collection.getName(), items);
        return itemsMap;
    }

    /**
     * splits the Calendar Object received into different calendar Components>
     * 
     * @param calendar Calendar
     * @param User cosmoUser
     * @return Set<ContentItem>
     */
    @Override
    public Set<ICalendarItem> splitCalendarItems(Calendar calendar, User cosmoUser) {
        return calendarDao.findCalendarEvents(calendar, cosmoUser);
    }


    @Override
    public void init() {
        if (calendarDao == null || contentDao == null) {
            throw new IllegalStateException("calendarDao and contentDao properties must not be null");
        }
    }

    @Override
    public void destroy() {

    }

}
