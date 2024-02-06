// Decompiled by DJ v3.11.11.95 Copyright 2009 Atanas Neshkov  Date: 27/12/2011 14:02:53
// Home Page: http://members.fortunecity.com/neshkov/dj.html  http://www.neshkov.com/dj.html - Check often for new version!
// Decompiler options: packimports(3) 
// Source File Name:   FechaUtil.java

package ec.cusoft.facturaec.utils;

import java.util.Calendar;
import javax.xml.datatype.*;

public class FechaUtils
{

    public static XMLGregorianCalendar CalendarToXml(Calendar fecha)
        throws DatatypeConfigurationException
    {
        XMLGregorianCalendar xmlGregorianCalendar = DatatypeFactory.newInstance().newXMLGregorianCalendar();
        xmlGregorianCalendar.setDay(fecha.get(5));
        xmlGregorianCalendar.setMonth(fecha.get(2) + 1);
        xmlGregorianCalendar.setYear(fecha.get(1));
        xmlGregorianCalendar.setTime(fecha.get(11), fecha.get(12), fecha.get(13));
        return xmlGregorianCalendar;
    }
}
