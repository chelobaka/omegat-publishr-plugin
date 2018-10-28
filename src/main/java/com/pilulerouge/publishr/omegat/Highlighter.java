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
import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;


/**
 * Formatting marker.
 */
class Highlighter implements IMarker {

    private AttributeSet tagAttributes;
    private AttributeSet textAttributes;
    private final Map<Integer, AttributeSet> highlightAttrs;
    private final Map<Element, String> tooltipElementMap;
    private boolean stylesAreSet;

    /**
     * Constructor.
     */
    Highlighter() {

        stylesAreSet = false;

        highlightAttrs = new HashMap<>();

        // Build reverse element/text map for tooltips
        tooltipElementMap = new HashMap<>();
        Util.FORMAT_ELEMENT_MAP.entrySet().forEach(
                e -> tooltipElementMap.put(e.getValue(), e.getKey())
        );
    }

    /**
     * Setup highlight styles.
     * This cannot be done in constructor since options become available to plugin only
     * after loading project files.
     * @param options plugin options
     * @param force force styles updating (when changed through settings dialog)
     */
    void setupStyles(final Map<String, String> options, final boolean force) {

        if (options == null || options.isEmpty()) {
            return;
        }

        // Avoid redundant work.
        if (stylesAreSet && !force) {
            return;
        }

        // Get colors from options of take default ones.
        Color tagColor = Color.decode(
                options.getOrDefault(Util.EXTRA_TAG_COLOR, Util.DEFAULT_EXTRA_TAG_COLOR));
        Color textColor = Color.decode(
                options.getOrDefault(Util.EXTRA_TEXT_COLOR, Util.DEFAULT_EXTRA_TEXT_COLOR));

        // Create and store text styles
        tagAttributes = Styles.createAttributeSet(tagColor, null, null, null);
        textAttributes = Styles.createAttributeSet(textColor, null, null, null);
        highlightAttrs.put(1, tagAttributes);
        highlightAttrs.put(2, textAttributes);
        highlightAttrs.put(3, tagAttributes);

        stylesAreSet = true;
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
                    Mark mark = new Mark(Mark.ENTRY_PART.TRANSLATION, matcher.start(g),
                            matcher.end(g));
                    mark.painter = null;
                    mark.attributes = highlightAttrs.get(g);
                    mark.toolTipText = Util.RB.getString("FOOTNOTE_HINT");
                    result.add(mark);
                }
            } while (matcher.find());
        }

        // Original formatting
        List<FormatSpan> spans = Util.FORMATTER.parseStructure(translationText, true, false);
        for (FormatSpan span : spans) {
            if (span.getSignature().getType() == BlockType.ELEMENT) {
                Mark mark = new Mark(Mark.ENTRY_PART.TRANSLATION, span.getBegin(), span.getEnd());
                mark.painter = null;
                mark.attributes = tagAttributes;
                mark.toolTipText = Util.RB.getString(
                        tooltipElementMap.get(span.getSignature().getElement()));
                result.add(mark);
            }
        }

        return result;
    }
}
