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

import org.omegat.core.data.SourceTextEntry;
import org.omegat.gui.editor.mark.IMarker;
import org.omegat.gui.editor.mark.Mark;
import org.omegat.util.gui.Styles;

import javax.swing.text.AttributeSet;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

class Highlighter implements IMarker {

    private final static Color TAG_FONT_COLOR = new Color(34, 200,220);

    private static final AttributeSet ATTRIBUTES = Styles
            .createAttributeSet(TAG_FONT_COLOR, null, null, null);

    private final static int[] HIGHLIGHT_GROUPS = {1, 3};

    public List<Mark> getMarksForEntry(SourceTextEntry ste, String sourceText,
                                       String translationText, boolean isActive) {

        if (translationText == null || !Util.isPublishrFile()) {
            return null;
        }

        Matcher matcher = Util.EF_PATTERN.matcher(translationText);
        if (!matcher.find()) {
            return null;
        }

        List<Mark> result = new ArrayList<>();

        do {
            for (int g : HIGHLIGHT_GROUPS) {
                Mark mark = new Mark(Mark.ENTRY_PART.TRANSLATION, matcher.start(g), matcher.end(g));
                mark.painter = null;
                mark.attributes = ATTRIBUTES;
                mark.toolTipText = Util.RB.getString("FOOTNOTE_HINT");
                result.add(mark);
            }
        } while (matcher.find());

        return result;
    }
}