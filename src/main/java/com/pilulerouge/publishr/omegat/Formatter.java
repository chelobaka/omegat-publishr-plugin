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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Formatter {

    private static final String CORK = "@@";

    private final Map<Element, ElementProcessor> processorMap;

    Formatter() {
        processorMap = new LinkedHashMap<>();
    }

    public void addProcessor(final Element element, final String re, final String shortcutName,
                             final int textGroup, final int extraGroup,
                             final String left, final String right) {

        ElementProcessor processor = new ElementProcessor(re, shortcutName, textGroup, extraGroup, left, right);
        processorMap.put(element, processor);
    }

    /**
     * Reset all converters.
     */
    public void resetConverters() {
        processorMap.values().stream().forEach(ElementProcessor::reset);
    }

    /**
     * Substitute original formatting with shortcuts.
     * @param text
     * @return
     */
    String toShortcuts(String text, Map<String, String> extras) {
        for (ElementProcessor converter : processorMap.values()) {
            text = converter.toShortcuts(text, extras);
        }
        return text;
    }

    /**
     * Substitute shortcuts with original formatting.
     * @param text
     * @return
     */
    String toOriginal(String text, Map<String, String> extras) {
        for (ElementProcessor converter : processorMap.values()) {
            text = converter.toOriginal(text, extras);
        }
        return text;
    }

    /**
     * Wrap text with formatting element.
     * @param text
     * @param element
     * @return
     */
    String applyElement(String text, Element element) {

        ElementProcessor processor = processorMap.get(element);
        if (processor == null) {
            return text;
        }
        return processor.applyOriginalFormatting(text);
    }

    /**
     * Parse text signature. Used by highlighter.
     * @param text
     */
    List<FormatSpan> parseStructure(String text, boolean findOriginal, boolean findShortcuts) {

        if (!findOriginal && !findShortcuts) {
            return null;
        }

        List<FormatSpan> result = new ArrayList<>();
        FormatSignature[] layout = new FormatSignature[text.length()];

        for (Map.Entry<Element, ElementProcessor> entry : processorMap.entrySet()) {
            Element element = entry.getKey();
            ElementProcessor converter = entry.getValue();
            List<FormatSpan> hits = converter.getFormatStructure(text, findOriginal, findShortcuts);

            for (FormatSpan span : hits) {
                span.signature.element = element; // Elements are unknown to converters

                for (int i = span.begin; i < span.end; i++) {
                    // Overwrite only text cells
                    if (layout[i] == null || layout[i].type == BlockType.TEXT) {
                        layout[i] = span.signature;
                    }
                }
            }

            // Overwrite sensitive symbols in text
            if (element == Element.STRONG) {
                StringBuilder sb = new StringBuilder();
                int lastPosition = 0;
                for (FormatSpan span : hits) {
                    sb.append(text.substring(lastPosition, span.begin));
                    if (span.signature.type == BlockType.FORMATTING_ELEMENT) {
                        sb.append(CORK); // Simplified because we know element length
                    } else {
                        sb.append(text.substring(span.begin, span.end));
                    }
                    lastPosition = span.end;
                }
                sb.append(text.substring(lastPosition, text.length()));
                text = sb.toString();
            }
        }

        // Build result
        FormatSignature lastSig = null;
        int lastBegin = 0;
        for (int i = 0; i < layout.length; i++) {
            FormatSignature thisSig = layout[i];
            // == should work here because each span uses the same signature instance
            if (thisSig == lastSig && i != layout.length - 1) {
                continue;
            }

            if (lastSig != thisSig) {
                // Create new FormatSpan for previous span
                if (lastSig != null) {
                    result.add(new FormatSpan(lastSig, lastBegin, i));
                }
                lastSig = thisSig;
                lastBegin = i;
            }

            // Handle last symbol in layout case
            if (i == layout.length - 1 && lastSig != null) {
                result.add(new FormatSpan(lastSig, lastBegin, i + 1));
            }
        }

        return result;
    }
}
