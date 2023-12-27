/*
 * Copyright (C) 2008-2013 Openia S.r.l.
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. 
 */

package it.openia.crm;

import biweekly.Biweekly;
import biweekly.ICalendar;
import biweekly.component.VEvent;
import biweekly.component.ValidationWarnings;
import biweekly.parameter.ParticipationStatus;
import biweekly.parameter.Role;
import biweekly.property.*;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author nicholas
 */
public class ICalMaker {
    
    private String subject;
    private String lang;
    private Date start;
    private Date end;

    public String getLang() {
        return lang;
    }

    public void setLang(String lang) {
        this.lang = lang;
    }

    public Date getStart() {
        return start;
    }

    public void setStart(Date start) {
        this.start = start;
    }
    
    public void setEnd(Date end) {
        this.end = end;
    }
    
    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }
    
    public File Create(String path) throws IOException{
        ICalendar ical = new ICalendar();
        
        VEvent event = new VEvent();

        Summary summary = event.setSummary(this.subject);
        //summary.setLanguage(this.lang);
        event.setDateStart(this.start);
        
        ical.addEvent(event);
        
        File file = new File(path);
        Biweekly.write(ical).go(file);
        
        return file;
    }
    
    
    public File CreateGuest(String path, String email, String guestName, String organizerEmail,String organizerName) throws IOException{
        
        ICalendar ical = new ICalendar();
        
        VEvent event = new VEvent();

        CalendarScale calscale = CalendarScale.gregorian();
        
        Attendee attendee = Attendee.email(email);
        attendee.setCommonName(guestName);
        attendee.setRsvp(true);
        attendee.setRole(Role.REQ_PARTICIPANT);
        attendee.setParticipationStatus(ParticipationStatus.NEEDS_ACTION);

        Method requestMethod = new Method("REQUEST");

        Organizer organizer = Organizer.email(organizerEmail);
        organizer.setCommonName(organizerName);
        
        ical.setProperty(calscale);
        ical.setProperty(requestMethod);
        
        
        event.setDateStart(this.start);
        event.setDateEnd(this.end);
        event.setDateTimeStamp(new Date());
        event.setOrganizer(organizer);
        event.setProperty(attendee);
        event.setCreated(new Date());
        event.setLastModified(new Date());
        event.setSequence(new Integer(1));
        event.setStatus(Status.tentative());
        event.setSummary(this.subject);
        event.setTransparency(Transparency.opaque());
        
        ical.addEvent(event);
        
        ValidationWarnings warnings = ical.validate();
        
        //System.out.println(warnings.toString());
        //Logger.getLogger(ICalMaker.class.getName()).log(Level.WARNING, null, warnings.toString());
        
        File file = new File(path);
        Biweekly.write(ical).go(file);
        
        return file;
        
    }
    
}
