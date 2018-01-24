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

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ShortcutConverter {

    private final Pattern pattern;
    private final String shortcutName;
    private final boolean useCounter;
    private final int textGroup;
    private final int extraGroup;

    private int counter;

    private Map<String, String> shortcutMap; // Label -> shortcut
    private Map<String, String> labelMap;  // Shortcut -> actual text

    ShortcutConverter(String regexp, String shortcutName, int textGroup, int extraGroup) {
        this.pattern = Pattern.compile(regexp);
        this.shortcutName = shortcutName;
        this.textGroup = textGroup;
        this.extraGroup = extraGroup;

        shortcutMap = new HashMap<>();
        labelMap = new HashMap<>();
        counter = 0;

        // Do not use counter if shortcut name contains a digit
        useCounter = !Pattern.compile("\\d").matcher(shortcutName).find();
    }

    /**
     * Substitute formatting elements with shortcut tags. Found extra strings
     * to be translated as separate segments go to extras map.
     * @param text      text to be processed
     * @param extras    extra strings container
     * @return          processed text
     */
    public String makeShortcuts(String text, Map<String, String> extras) {

        Matcher matcher = pattern.matcher(text);

        // Return early if no matches found in text
        if (!matcher.find()) return text;

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
                }
                else {
                    lastLabelBuilder.append(matcher.group(i));
                }
            }

            String firstLabel = firstLabelBuilder.toString();
            String lastLabel = lastLabelBuilder.toString();

            // Check if this shortcut is already registered
            // Assume tabulation cannot be found in any labels
            String searchLabel = firstLabel + "\t" + lastLabel;
            String shortcutName = shortcutMap.get(searchLabel);

            // If not then create a new shortcut
            boolean newShortcutName = false;
            if (shortcutName == null) {
                shortcutName = getShortcutName();
                newShortcutName = true;
            }

            // Save extra text if any
            if (extraText != null) {
                extras.put(shortcutName, extraText);
            }

            // Create actual shortcuts
            String openingSC, closingSC;
            if (textGroup == 0) {
                // Self-closing shortcut
                openingSC = "<" + shortcutName + "/>";
                closingSC = "";
            }
            else {
                openingSC = "<" + shortcutName + ">";
                closingSC = "</" + shortcutName + ">";
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
    public String removeShortcuts(String text, Map<String, String> extras) {

        // Replace shortcuts with actual formatting
        for (String shortcut : labelMap.keySet()) {
            if (text.contains(shortcut)) {

                // Replace extra strings first
                String replacement = labelMap.get(shortcut);
                if (extraGroup > 0) {
                    for (String k : extras.keySet()) {
                        replacement = replacement.replaceAll(k, extras.get(k));
                    }
                }

                text = text.replaceAll(shortcut, replacement);
            }
        }

        return text;
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
     * Generate next shortcut name.
     */
    private String getShortcutName() {
        if (useCounter) {
            return String.format("%s%d", shortcutName, ++counter);
        }
        else {
            return shortcutName;
        }
    }
}
