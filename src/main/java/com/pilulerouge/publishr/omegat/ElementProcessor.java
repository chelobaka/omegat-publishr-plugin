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

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Swiss army knife for single formatting element.
 */
public class ElementProcessor {

    private final Pattern pattern, reversePattern;
    private final String shortcutName;
    private final boolean useCounter;
    private final int textGroup;
    private final int extraGroup;
    // Original formatting parts
    private final String left;
    private final String right;

    private int counter;

    private Map<String, String> shortcutMap; // Label -> shortcut
    private Map<String, String> labelMap;  // Shortcut -> actual text

    ElementProcessor(final String regexp, final String shortcutName,
                     final int textGroup, final int extraGroup,
                     final String left, final String right) {
        this.pattern = Pattern.compile(regexp);
        this.shortcutName = shortcutName;
        this.textGroup = textGroup;
        this.extraGroup = extraGroup;
        this.left = left;
        this.right = right;

        shortcutMap = new HashMap<>();
        labelMap = new HashMap<>();
        counter = 0;

        // Do not use counter if shortcut name contains a digit
        useCounter = !Pattern.compile("\\d").matcher(shortcutName).find();

        // Build reverse pattern for matching shortcuts
        String rpt;
        if (textGroup > 0) {
            rpt = "(<%s>)(.+?)(</\\1>)";
        } else {
            rpt = "(<%s/>)";
        }

        if (useCounter) {
            rpt = String.format(rpt, shortcutName + "\\d+");
        } else {
            rpt = String.format(rpt, shortcutName);
        }

        reversePattern = Pattern.compile(rpt);
    }

    /**
     * Substitute formatting elements with shortcut tags. Found extra strings
     * to be translated as separate segments go to extras map.
     * @param text      text to be processed
     * @param extras    extra strings container
     * @return          processed text
     */
    public String toShortcuts(final String text, final Map<String, String> extras) {

        Matcher matcher = pattern.matcher(text);

        // Return early if no matches found in text
        if (!matcher.find()) {
            return text;
        }

        StringBuilder result = new StringBuilder();
        int lastMatcherPosition = 0;
        do {
            // Append text between last and this match
            result.append(text.substring(lastMatcherPosition, matcher.start()));
            lastMatcherPosition = matcher.end();

            // Find labels/regions
            StringBuilder firstLabelBuilder = new StringBuilder();
            StringBuilder lastLabelBuilder = new StringBuilder();
            String shortcutText = "";
            String extraText = null;
            boolean isFirstShortcut = true;
            // Iterate over groups and find shortcut regions
            for (int i = 1; i <= matcher.groupCount(); i++) {
                if (i == extraGroup) {
                    extraText = matcher.group(i);
                }
                if (i == textGroup) {
                    isFirstShortcut = false;
                    shortcutText = matcher.group(i);
                    continue;
                }
                if (isFirstShortcut) {
                    firstLabelBuilder.append(matcher.group(i));
                } else {
                    lastLabelBuilder.append(matcher.group(i));
                }
            }

            String firstLabel = firstLabelBuilder.toString();
            String lastLabel = lastLabelBuilder.toString();

            // Check if this shortcut is already registered
            // Assume tabulation cannot be found in any labels
            String searchLabel = firstLabel + "\t" + lastLabel;
            String scName = shortcutMap.get(searchLabel);

            // If not then create a new shortcut
            boolean newShortcutName = false;
            if (scName == null) {
                scName = getShortcutName();
                newShortcutName = true;
            }

            // Save extra text if any
            if (extraText != null) {
                extras.put(scName, extraText);
            }

            // Create actual shortcuts
            String openingSC, closingSC;
            if (textGroup == 0) {
                // Self-closing shortcut
                openingSC = "<" + scName + "/>";
                closingSC = "";
            } else {
                openingSC = "<" + scName + ">";
                closingSC = "</" + scName + ">";
            }

            // Save inverse mapping
            if (newShortcutName) {
                labelMap.put(openingSC, firstLabel);
                if (!closingSC.isEmpty()) {
                    labelMap.put(closingSC, lastLabel);
                }
            }

            // Append shortcuts/text to result
            result.append(openingSC);
            result.append(shortcutText);
            result.append(closingSC);

        } while (matcher.find());

        result.append(text.substring(lastMatcherPosition, text.length()));

        return result.toString();
    }

    /**
     * Remove shortcuts and restore original formatting.
     * @param text      piece of text where shortcuts should be removed
     * @param extras    mapping of source extra string to translated ones
     * @return          restored text
     */
    public String toOriginal (final String text, final Map<String, String> extras) {
        String result = text;
        // Replace shortcuts with actual formatting
        for (Map.Entry<String, String> scEntry : labelMap.entrySet()) {
            String shortcut = scEntry.getKey();
            if (result.contains(shortcut)) {

                // Replace extra strings first
                String replacement = scEntry.getValue();
                if (extraGroup > 0) {
                    for (Map.Entry<String, String> egEntry : extras.entrySet()) {
                        replacement = replacement.replaceAll(egEntry.getKey(),
                                egEntry.getValue());
                    }
                }

                result = result.replaceAll(shortcut, replacement);
            }
        }
        return result;
    }

    /**
     * Reset internal state.
     */
    public void reset() {
        counter = 0;
        shortcutMap.clear();
        labelMap.clear();
    }

    /**
     * Apply original formatting to text if converter has
     * left and right parts defined.
     * @param text text to format
     * @return formatted text or original text
     */
    public String applyOriginalFormatting(String text) {
        if (left != null && right != null) {
            return left + text + right;
        }
        return text;
    }

    public List<FormatSpan> getFormatStructure(String text, boolean findOriginal,
                                                    boolean findShortcuts) {

        List<FormatSpan> result = new ArrayList<>();

        if (findOriginal) {
            Matcher matcher = pattern.matcher(text);
            while (matcher.find()) {
                if (textGroup > 0) {
                    result.add(new FormatSpan(
                            BlockType.FORMATTING_ELEMENT,
                            null,
                            matcher.start(1),
                            matcher.end(textGroup - 1)));
                    result.add(new FormatSpan(
                            BlockType.TEXT,
                            null,
                            matcher.start(textGroup),
                            matcher.end(textGroup)));
                    result.add(new FormatSpan(
                            BlockType.FORMATTING_ELEMENT,
                            null,
                            matcher.start(textGroup + 1),
                            matcher.end(matcher.groupCount())));
                } else {
                    result.add(new FormatSpan(
                            BlockType.FORMATTING_ELEMENT,
                            null,
                            matcher.start(1),
                            matcher.end(matcher.groupCount())));
                }
            }
        }

        if (findShortcuts) {
            Matcher matcher = reversePattern.matcher(text);
            while (matcher.find()) {
                // 3 groups
                if (textGroup > 0) {
                    result.add(new FormatSpan(
                            BlockType.SHORTCUT,
                            null,
                            matcher.start(1),
                            matcher.end(1)));
                    result.add(new FormatSpan(
                            BlockType.TEXT,
                            null,
                            matcher.start(2),
                            matcher.end(2)));
                    result.add(new FormatSpan(
                            BlockType.SHORTCUT,
                            null,
                            matcher.start(3),
                            matcher.end(3)));
                } else { // 1 group
                    result.add(new FormatSpan(
                            BlockType.SHORTCUT,
                            null,
                            matcher.start(1),
                            matcher.end(1)));
                }
            }
        }

        if (findOriginal && findShortcuts) {
            Collections.sort(result);
        }

        return result;
    }

    /**
     * Generate next shortcut name.
     */
    private String getShortcutName() {
        if (useCounter) {
            return String.format("%s%d", shortcutName, ++counter);
        } else {
            return shortcutName;
        }
    }
}
