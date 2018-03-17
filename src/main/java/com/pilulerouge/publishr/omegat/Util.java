/**************************************************************************
 PublishR file filter for OmegaT

 Copyright (C) 2018 Lev Abashkin

 This file is NOT a part of OmegaT.

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 **************************************************************************/

package com.pilulerouge.publishr.omegat;

import org.omegat.core.Core;
import org.omegat.core.data.IProject;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.regex.Pattern;


enum Element {
    EMPHASIS,
    STRONG,
    NAME,
    TITLE,
    SUPERSCRIPT,
    SUBSCRIPT,
    IMAGE,
    LINK,
    FOOTNOTE,
    SEPARATOR
}


enum BlockType {
    TEXT,
    SHORTCUT,
    ELEMENT
}


public final class Util {

    public static final Formatter FORMATTER = new Formatter();

    static {
        FORMATTER.addProcessor(
                Element.STRONG,
                "(?<!\\\\)(\\*{2})(?!\\s)(.+?)(?<![\\s\\\\])(\\*{2})",
                "e2",
                2,
                0,
                "**",
                "**");

        FORMATTER.addProcessor(
                Element.EMPHASIS,
                "(?<!\\\\)(\\*{1})(?!\\s)(.+?)(?<![\\s\\\\])(\\*{1})",
                "e1",
                2,
                0,
                "*",
                "*");

        FORMATTER.addProcessor(
                Element.FOOTNOTE,
                "(\\[\\^.+?\\])",
                "f",
                0,
                0,
                null,
                null);

        FORMATTER.addProcessor(
                Element.SEPARATOR,
                "(?<!\\\\)(\\|)",
                "s1",
                0,
                0,
                null,
                null);

        FORMATTER.addProcessor(
                Element.SUPERSCRIPT,
                "(\\^)(.+?)(\\^)",
                "s2",
                2,
                0,
                "^",
                "^");

        FORMATTER.addProcessor(
                Element.SUBSCRIPT,
                "(~)(.+?)(~)",
                "s3",
                2,
                0,
                "~",
                "~");

        FORMATTER.addProcessor(
                Element.NAME,
                "(name\\()(.+?)(\\))",
                "n1",
                2,
                0,
                "name(",
                ")");

        FORMATTER.addProcessor(
                Element.TITLE,
                "(title\\()(.+?)(\\))",
                "t1",
                2,
                0,
                "title(",
                ")");

        FORMATTER.addProcessor(
                Element.IMAGE,
                "(\\!\\[)(.*?)(\\]\\(.+?\\))",
                "i",
                2,
                0,
                null,
                null);

        FORMATTER.addProcessor(
                Element.LINK,
                "(\\[)(.+?)(\\]\\()(.+?)(\\))",
                "a",
                2,
                4,
                null,
                null);
    }

    public static final String PLAIN_SHORTCUTS = "plainShortcuts";

    static final String EF_TAG_NAME = "ef";
    static final Pattern EF_PATTERN = Pattern.compile(
            String.format("(<%s>)(.+?)(</%s>)", EF_TAG_NAME, EF_TAG_NAME));

    final static Map<String, Element> FORMAT_ELEMENT_MAP = new LinkedHashMap<>();
    static {
        FORMAT_ELEMENT_MAP.put("POPUP_MENU_FORMAT_EMPHASIS", Element.EMPHASIS);
        FORMAT_ELEMENT_MAP.put("POPUP_MENU_FORMAT_STRONG", Element.STRONG);
        FORMAT_ELEMENT_MAP.put("POPUP_MENU_FORMAT_SUPERSCRIPT", Element.SUPERSCRIPT);
        FORMAT_ELEMENT_MAP.put("POPUP_MENU_FORMAT_SUBSCRIPT", Element.SUBSCRIPT);
        FORMAT_ELEMENT_MAP.put("POPUP_MENU_FORMAT_NAME", Element.NAME);
        FORMAT_ELEMENT_MAP.put("POPUP_MENU_FORMAT_TITLE", Element.TITLE);
    };

    /**
     * Resource bundle.
     */
    static final ResourceBundle RB;

    static {
        ResourceBundle.Control utf8Control = new UTF8Control();
        RB = ResourceBundle.getBundle("PublishR_strings", Locale.getDefault(), utf8Control);
    }

    static final String FILTER_NAME = RB.getString("FILTER_NAME");

    /**
     * Check if current file using PublishR file filter.
     * @return check result
     */
    static boolean isPublishrFile() {
        String filePath = Core.getEditor().getCurrentFile();
        if (filePath == null) {
            return false;
        }
        for (IProject.FileInfo fi : Core.getProject().getProjectFiles()) {
            if (fi.filePath.equals(filePath)
                    && fi.filterFileFormatName.equals(FILTER_NAME)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Do not allow instances of this class.
     */
    private Util() { }

}
