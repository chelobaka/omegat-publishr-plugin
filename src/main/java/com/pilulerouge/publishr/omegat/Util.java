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

import java.util.Locale;
import java.util.ResourceBundle;
import java.util.regex.Pattern;

public final class Util {

    public static final String PLAIN_SHORTCUTS = "plainShortcuts";

    static final String EF_TAG_NAME = "ef";
    static final Pattern EF_PATTERN = Pattern.compile(
            String.format("(<%s>)(.+?)(</%s>)", EF_TAG_NAME, EF_TAG_NAME));

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
