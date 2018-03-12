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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Formatting element used for adding original formatting
 * and highlighting it in translation.
 */
class FormattingElement {
    private final String left;
    private final String right;
    private final Pattern pattern;

    FormattingElement(String left, String right, String re) {
        this.left = left;
        this.right = right;
        if (re == null) {
            pattern = null;
        } else {
            pattern = Pattern.compile(re);
        }
    }

    /**
     * Apply formatting to text.
     * @param text source text
     * @return formatted text
     */
    String format(String text) {
        return String.format("%s%s%s", left, text, right);
    }

    /**
     * Return Matcher instance for given text searching
     * for this formatting element.
     * Can return null if no pattern was passed on creation.
     * @param text text to search in
     * @return Matcher instance
     */
    Matcher match(String text) {
        if (pattern == null) {
            return null;
        }
        return pattern.matcher(text);
    }
}

public final class Util {

    public static final String PLAIN_SHORTCUTS = "plainShortcuts";

    static final String EF_TAG_NAME = "ef";
    static final Pattern EF_PATTERN = Pattern.compile(
            String.format("(<%s>)(.+?)(</%s>)", EF_TAG_NAME, EF_TAG_NAME));

    /**
     * Formatting elements data
     * 1. ResourceBundle name for popup menu
     * 2. Left formatting part
     * 3. Right formatting part
     * 4. RegExp for highlighting, can be null
     */
    private final static String[][] FORMAT_ELEMENTS = {
        {"POPUP_MENU_FORMAT_EMPHASIS", "*", "*", null},
        {"POPUP_MENU_FORMAT_STRONG", "**", "**", null},
        {"POPUP_MENU_FORMAT_SUPERSCRIPT", "^", "^", null},
        {"POPUP_MENU_FORMAT_SUBSCRIPT", "~", "~", null},
        {"POPUP_MENU_FORMAT_NAME", "name(", ")", null},
        {"POPUP_MENU_FORMAT_TITLE", "title(", ")", null}
    };


    final static Map<String, FormattingElement> FORMAT_ELEMENT_MAP = new LinkedHashMap<>();
    static {
        for (String[] fe : FORMAT_ELEMENTS) {
            FORMAT_ELEMENT_MAP.put(fe[0], new FormattingElement(fe[1], fe[2], fe[3]));
        }
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
