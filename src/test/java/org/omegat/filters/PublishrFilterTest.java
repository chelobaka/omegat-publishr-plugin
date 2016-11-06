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

import java.util.List;

import org.junit.Test;
import com.pilulerouge.publishr.omegat.PublishrFilter;

import static org.junit.Assert.*;

public class PublishrFilterTest extends TestFilterBase {

    @Test
    public void testTextFilterParsing() throws Exception {
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
        assertEquals("Refer to a wise book<fn1>wise-book</fn1>", entries.get(i++));
        assertEquals("Footnote text", entries.get(i++));
        assertEquals("<n1>Philip K. Dick</n1> wrote <t1>Ubik</t1>.", entries.get(i++));
        assertEquals("Here is an image <id1>Image description</id1><il1>image.jpg</il1>.", entries.get(i++));
        assertEquals("And here is a link <ld1>Link description</ld1><la1>http://foo.bar</la1>.", entries.get(i++));
        assertEquals("Let's <e3>try</e3> some <e1>formating</e1>.", entries.get(i++));
        assertEquals("Water's chemical formula is H<sub1>2</sub1>O.", entries.get(i++));
        assertEquals("Water is <e2>essential</e2>!", entries.get(i++));
        assertEquals("1<sup1>st</sup1> and 2<sup1>nd</sup1> elements are <e1>H</e1> and <e1>He</e1>.", entries.get(i++));
        assertEquals("Single <e3/>combined, strong<e2/> and ligh<e1/>t emphasis.", entries.get(i++));
        assertEquals("Some escaped asterisks \\* \\*<e1>light</e1>, <e2>strong</e2>\\*, and <e1>more\\*</e1>.", entries.get(i++));
        assertEquals("<s1/> Default aligned <s1/> Left aligned <s1/> Center aligned <s1/> Right aligned", entries.get(i++));
        assertEquals("<s1/> First body part <s1/> Second cell <s1/> Third cell <s1/> fourth cell", entries.get(i++));
        assertEquals("<s1/> Second line <s1/>foo <s1/> <e2>strong</e2> <s1/> baz", entries.get(i++));
        assertEquals("<s1/> Third line <s1/>quux <s1/> baz <s1/> bar", entries.get(i++));
        assertEquals("<s1/> Second body", entries.get(i++));
        assertEquals("<s1/> 2 line", entries.get(i++));
        assertEquals("<s1/> Footer row", entries.get(i++));
    }

    @Test
    public void testTranslate() throws Exception {
        translateText(new PublishrFilter(), "/filters/publishr/publishr.txt");
    }
}
