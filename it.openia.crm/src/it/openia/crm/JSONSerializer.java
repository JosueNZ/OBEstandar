/*
 * Copyright (C) 2008-2013 Openia S.r.l.
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. 
 */

package it.openia.crm;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.io.json.JsonHierarchicalStreamDriver;
import com.thoughtworks.xstream.io.json.JsonWriter;
import java.io.Writer;

/**
 *
 * @author nicholas
 */
public class JSONSerializer {
    public static String getJSON(GridDataObject obj) {
        String result = null;

        XStream xstream = new XStream(new JsonHierarchicalStreamDriver() {

            @Override
            public HierarchicalStreamWriter createWriter(Writer writer) {
                return new JsonWriter(writer, JsonWriter.DROP_ROOT_MODE);
            }
        });

        xstream.setMode(XStream.NO_REFERENCES);
        result = xstream.toXML(obj); //it makes JSON, not XML

        return result;
    }
}
