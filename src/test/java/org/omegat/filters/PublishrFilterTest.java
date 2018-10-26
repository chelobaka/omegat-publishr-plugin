/**************************************************************************
 OmegaT - Computer Assisted Translation (CAT) tool 
          with fuzzy matching, translation memory, keyword search, 
          glossaries, and translation leveraging into updated projects.

 Copyright (C) 2008 Alex Buloichik
               2010 Volker Berlin
               Home page: http://www.omegat.org/
               Support center: http://groups.yahoo.com/group/OmegaT/

 This file is part of OmegaT.

 OmegaT is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 OmegaT is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 **************************************************************************/

package org.omegat.filters;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import com.pilulerouge.publishr.omegat.PublishrFilter;
import com.pilulerouge.publishr.omegat.Util;

import static org.junit.Assert.*;

public class PublishrFilterTest extends TestFilterBase {

    private static final Map<String, String> OLD_OPTIONS;
    static {
        OLD_OPTIONS = new HashMap<>();
        OLD_OPTIONS.put(Util.PLAIN_SHORTCUTS, "true");
    }

    @Test
    public void testTextFilterParsingOld() throws Exception {
        List<String> entries = parse(new PublishrFilter(),
                "/filters/publishr/publishr.txt",
                OLD_OPTIONS);
        int i = 0;
        assertEquals("Simple paragraph.", entries.get(i++));
        assertEquals("Single quote.", entries.get(i++));
        assertEquals("Double quote.", entries.get(i++));
        assertEquals("Another single quote.", entries.get(i++));
        assertEquals("Heading 1", entries.get(i++));
        assertEquals("Heading 2", entries.get(i++));
        assertEquals("Indented heading 1", entries.get(i++));
        assertEquals("Indented ordered list item 1", entries.get(i++));
        assertEquals("indented ordered list item 2", entries.get(i++));
        assertEquals("Unordered list item 1", entries.get(i++));
        assertEquals("Unordered list item 2", entries.get(i++));
        assertEquals("Refer to a wise book<fn1>wise-book</fn1>", entries.get(i++));
        assertEquals("Footnote text", entries.get(i++));
        assertEquals("<n1>Philip K. Dick</n1> wrote <t1>Ubik</t1>.", entries.get(i++));
        assertEquals("Here is an image <id1>Image description</id1><il1>image1.jpg</il1>.", entries.get(i++));
        assertEquals("And here is a link <ld1>Link description</ld1><la1>http://first.url</la1>.", entries.get(i++));
        assertEquals("Second link <ld1>Link description</ld1><la1>http://second.url</la1>.", entries.get(i++));
        assertEquals("Let's <e2>try</e2> some <e1>formatting</e1>.", entries.get(i++));
        assertEquals("Water's chemical formula is H<sub1>2</sub1>O.", entries.get(i++));
        assertEquals("Water is <e2>essential</e2>!", entries.get(i++));
        assertEquals("1<sup1>st</sup1> and 2<sup1>nd</sup1> elements are <e1>H</e1> and <e1>He</e1>.", entries.get(i++));
        assertEquals("Second image <id1>description</id1><il1>image2.jpg</il1>", entries.get(i++));
        assertEquals("Single <e3/>combined, strong<e2/> and ligh<e1/>t emphasis.", entries.get(i++));
        assertEquals("Some escaped asterisks \\* \\*<e1>light</e1>, <e2>strong</e2>\\*, and <e1>more\\*</e1>.", entries.get(i++));
        assertEquals("<s1/> Default aligned <s1/> Left aligned <s1/> Center aligned <s1/> Right aligned", entries.get(i++));
        assertEquals("<s1/> First body part <s1/> Second cell <s1/> Third cell <s1/> fourth cell", entries.get(i++));
        assertEquals("<s1/> Second line <s1/>foo <s1/> <e2>strong</e2> <s1/> baz", entries.get(i++));
        assertEquals("<s1/> Third line <s1/>quux <s1/> baz <s1/> bar", entries.get(i++));
        assertEquals("<s1/> Second body", entries.get(i++));
        assertEquals("<s1/> 2 line", entries.get(i++));
        assertEquals("<s1/> Footer row<fn1>footer-note</fn1>", entries.get(i++));
        assertEquals("Footer footnote", entries.get(i++));
        assertEquals("Numbered line.", entries.get(i++));
    }

    @Test
    public void testTextFilterParsingNew() throws Exception {
        List<String> entries = parse(new PublishrFilter(), "/filters/publishr/publishr.txt");
        int i = 0;
        assertEquals("Simple paragraph.", entries.get(i++));
        assertEquals("Single quote.", entries.get(i++));
        assertEquals("Double quote.", entries.get(i++));
        assertEquals("Another single quote.", entries.get(i++));
        assertEquals("Heading 1", entries.get(i++));
        assertEquals("Heading 2", entries.get(i++));
        assertEquals("Indented heading 1", entries.get(i++));
        assertEquals("Indented ordered list item 1", entries.get(i++));
        assertEquals("indented ordered list item 2", entries.get(i++));
        assertEquals("Unordered list item 1", entries.get(i++));
        assertEquals("Unordered list item 2", entries.get(i++));
        assertEquals("Refer to a wise book<f1/>", entries.get(i++));
        assertEquals("Footnote text", entries.get(i++));
        assertEquals("<n1>Philip K. Dick</n1> wrote <t1>Ubik</t1>.", entries.get(i++));
        assertEquals("Here is an image <i1>Image description</i1>.", entries.get(i++));
        assertEquals("And here is a link <a1>Link description</a1>.", entries.get(i++));
        assertEquals("http://first.url", entries.get(i++));
        assertEquals("Second link <a2>Link description</a2>.", entries.get(i++));
        assertEquals("http://second.url", entries.get(i++));
        assertEquals("Let's <e2>try</e2> some <e1>formatting</e1>.", entries.get(i++));
        assertEquals("Water's chemical formula is H<s3>2</s3>O.", entries.get(i++));
        assertEquals("Water is <e2>essential</e2>!", entries.get(i++));
        assertEquals("1<s2>st</s2> and 2<s2>nd</s2> elements are <e1>H</e1> and <e1>He</e1>.", entries.get(i++));
        assertEquals("Second image <i2>description</i2>", entries.get(i++));
        assertEquals("Single <e2><e1>combined, strong</e2> and ligh</e1>t emphasis.", entries.get(i++));
        assertEquals("Some escaped asterisks \\* \\*<e1>light</e1>, <e2>strong</e2>\\*, and <e1>more\\*</e1>.", entries.get(i++));
        assertEquals("<s1/> Default aligned <s1/> Left aligned <s1/> Center aligned <s1/> Right aligned", entries.get(i++));
        assertEquals("<s1/> First body part <s1/> Second cell <s1/> Third cell <s1/> fourth cell", entries.get(i++));
        assertEquals("<s1/> Second line <s1/>foo <s1/> <e2>strong</e2> <s1/> baz", entries.get(i++));
        assertEquals("<s1/> Third line <s1/>quux <s1/> baz <s1/> bar", entries.get(i++));
        assertEquals("<s1/> Second body", entries.get(i++));
        assertEquals("<s1/> 2 line", entries.get(i++));
        assertEquals("<s1/> Footer row<f2/>", entries.get(i++));
        assertEquals("Footer footnote", entries.get(i++));
        assertEquals("Numbered line.", entries.get(i++));
    }

    @Test
    public void testTranslateOld() throws Exception {
        translateText(new PublishrFilter(), "/filters/publishr/publishr.txt", OLD_OPTIONS);
    }

    @Test
    public void testTranslateNew() throws Exception {
        translateText(new PublishrFilter(), "/filters/publishr/publishr.txt");
    }

}
