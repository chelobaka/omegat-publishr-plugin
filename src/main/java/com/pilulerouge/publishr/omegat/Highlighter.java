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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;


/**
 * Formatting marker.
 */
class Highlighter implements IMarker {

    private static final AttributeSet TAG_ATTRIBUTES = Styles
            .createAttributeSet(new Color(0, 165, 23), null, null, null);
    private static final AttributeSet EF_ATTRIBUTES = Styles
            .createAttributeSet(new Color(56, 155, 205), null, null, null);

    private static final Map<Integer, AttributeSet> HIGHLIGHT_ATTRS;
    static {
        HIGHLIGHT_ATTRS = new HashMap<>();
        HIGHLIGHT_ATTRS.put(1, TAG_ATTRIBUTES);
        HIGHLIGHT_ATTRS.put(2, EF_ATTRIBUTES);
        HIGHLIGHT_ATTRS.put(3, TAG_ATTRIBUTES);
    }

    // Build reverse element/text map for tooltips
    private static final Map<Element, String> TOOLTIP_ELEMENT_MAP = new HashMap<>();
    static {
        Util.FORMAT_ELEMENT_MAP.entrySet().forEach(
                e -> TOOLTIP_ELEMENT_MAP.put(e.getValue(), e.getKey())
        );
    }

    public List<Mark> getMarksForEntry(final SourceTextEntry ste, final String sourceText,
                                       final String translationText, final boolean isActive) {

        if (translationText == null || !Util.isPublishrFile()) {
            return null;
        }

        List<Mark> result = new ArrayList<>();

        // Extra footnotes
        Matcher matcher = Util.EF_PATTERN.matcher(translationText);
        if (matcher.find()) {
            do {
                for (int g = 1; g <= matcher.groupCount(); g++) {
                    Mark mark = new Mark(Mark.ENTRY_PART.TRANSLATION, matcher.start(g), matcher.end(g));
                    mark.painter = null;
                    mark.attributes = HIGHLIGHT_ATTRS.get(g);
                    mark.toolTipText = Util.RB.getString("FOOTNOTE_HINT");
                    result.add(mark);
                }
            } while (matcher.find());
        }

        // Original formatting
        List<FormatSpan> spans = Util.FORMATTER.parseStructure(translationText, true, false);
        for (FormatSpan span : spans) {
            if (span.signature.type == BlockType.ELEMENT) {
                Mark mark = new Mark(Mark.ENTRY_PART.TRANSLATION, span.begin, span.end);
                mark.painter = null;
                mark.attributes = TAG_ATTRIBUTES;
                mark.toolTipText = Util.RB.getString(TOOLTIP_ELEMENT_MAP.get(span.signature.element));
                result.add(mark);
            }
        }

        return result;
    }
}
