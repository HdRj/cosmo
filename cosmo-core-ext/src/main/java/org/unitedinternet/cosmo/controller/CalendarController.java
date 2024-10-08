package org.unitedinternet.cosmo.controller;

import org.unitedinternet.cosmo.service.impl.CalendarManagementService;
import net.fortuna.ical4j.model.component.VEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("")
public class CalendarController {

    private final CalendarManagementService calendarManagementService;

    @Autowired
    public CalendarController(CalendarManagementService calendarManagementService) {
        this.calendarManagementService = calendarManagementService;
    }

    @PostMapping("/manage-event")
    public String manageEvent(@RequestBody VEvent event) {
        calendarManagementService.handleCalendarEvent(event);
        return "Event processed";
    }

}
