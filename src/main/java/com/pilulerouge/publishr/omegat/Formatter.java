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


/**
 * Format conversion manager.
 */
public class Formatter {

    private static final String CORK = "@@";

    private final Map<Element, ElementProcessor> processorMap;

    /**
     * Constructor.
     */
    public Formatter() {
        processorMap = new LinkedHashMap<>();

        addProcessor(
                Element.STRONG,
                "(?<!\\\\)(\\*{2})(?!\\s)(.+?)(?<![\\s\\\\])(\\*{2})",
                "e2",
                2,
                0,
                "**",
                "**");

        addProcessor(
                Element.EMPHASIS,
                "(?<!\\\\)(\\*{1})(?!\\s)(.+?)(?<![\\s\\\\])(\\*{1})",
                "e1",
                2,
                0,
                "*",
                "*");

        addProcessor(
                Element.FOOTNOTE,
                "(\\[\\^.+?\\])",
                "f",
                0,
                0,
                null,
                null);

        addProcessor(
                Element.SEPARATOR,
                "(?<!\\\\)(\\|)",
                "s1",
                0,
                0,
                null,
                null);

        addProcessor(
                Element.SUPERSCRIPT,
                "(\\^)(.+?)(\\^)",
                "s2",
                2,
                0,
                "^",
                "^");

        addProcessor(
                Element.SUBSCRIPT,
                "(~)(.+?)(~)",
                "s3",
                2,
                0,
                "~",
                "~");

        addProcessor(
                Element.NAME,
                "(name\\()(.+?)(\\))",
                "n1",
                2,
                0,
                "name(",
                ")");

        addProcessor(
                Element.TITLE,
                "(title\\()(.+?)(\\))",
                "t1",
                2,
                0,
                "title(",
                ")");

        addProcessor(
                Element.IMAGE,
                "(\\!\\[)(.*?)(\\]\\(.+?\\))",
                "i",
                2,
                0,
                null,
                null);

        addProcessor(
                Element.LINK,
                "(\\[)(.+?)(\\]\\()(.+?)(\\))",
                "a",
                2,
                4,
                null,
                null);
    }

    private void addProcessor(final Element element, final String re, final String shortcutName,
                             final int textGroup, final int extraGroup,
                             final String left, final String right) {

        ElementProcessor processor = new ElementProcessor(re, shortcutName, textGroup, extraGroup,
                left, right);
        processorMap.put(element, processor);
    }

    /**
     * Reset all converters.
     */
    void resetConverters() {
        processorMap.values().forEach(ElementProcessor::reset);
    }

    /**
     * Substitute original formatting with shortcuts.
     * @param text text with original formatting
     * @param extras element specific extra strings
     * @return text with shortcuts
     */
    String toShortcuts(final String text, final Map<String, String> extras) {
        String result = text;
        for (ElementProcessor converter : processorMap.values()) {
            result = converter.toShortcuts(result, extras);
        }
        return result;
    }

    /**
     * Substitute shortcuts with original formatting.
     * @param text text containing shortcuts
     * @param extras element specific extra strings (like URL)
     * @return text with original formatting
     */
    String toOriginal(final String text, final Map<String, String> extras) {
        String result = text;
        for (ElementProcessor converter : processorMap.values()) {
            result = converter.toOriginal(result, extras);
        }
        return result;
    }

    /**
     * Wrap text with formatting element.
     * @param text unformatted text
     * @param element element to wrap with
     * @return formatted text
     */
    String applyElement(final String text, final Element element) {
        ElementProcessor processor = processorMap.get(element);
        if (processor == null) {
            return text;
        }
        return processor.applyOriginalFormatting(text);
    }

    /**
     * Parse text structure. Can be used for highlighting.
     * @param input input text
     * @param findOriginal look for original elements?
     * @param findShortcuts look for shortcuts?
     * @return list of text structure elements
     */
    public List<FormatSpan> parseStructure(final String input,
                                           final boolean findOriginal,
                                           final boolean findShortcuts) {

        String text = input;

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
                span.getSignature().setElement(element); // Elements are unknown to converters

                for (int i = span.getBegin(); i < span.getEnd(); i++) {
                    // Overwrite only text cells
                    if (layout[i] == null || layout[i].getType() == BlockType.TEXT) {
                        layout[i] = span.getSignature();
                    }
                }
            }

            // Overwrite sensitive symbols in text
            if (element == Element.STRONG) {
                StringBuilder sb = new StringBuilder();
                int lastPosition = 0;
                for (FormatSpan span : hits) {
                    sb.append(text.substring(lastPosition, span.getBegin()));
                    if (span.getSignature().getType() == BlockType.ELEMENT) {
                        sb.append(CORK); // Simplified because we know element length
                    } else {
                        sb.append(text.substring(span.getBegin(), span.getEnd()));
                    }
                    lastPosition = span.getEnd();
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
