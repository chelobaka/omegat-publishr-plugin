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


import java.awt.Window;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.omegat.core.Core;

import org.omegat.core.CoreEvents;
import org.omegat.core.events.IApplicationEventListener;
import org.omegat.filters2.AbstractFilter;
import org.omegat.filters2.FilterContext;
import org.omegat.filters2.Instance;
import org.omegat.util.LinebreakPreservingReader;
import org.omegat.util.Log;


/**
 * PublishR syntax filter.
 *
 * @author Lev Abashkin
 */
public class PublishrFilter extends AbstractFilter {

    /**
     * Types of PublishR formatting.
     */
    private enum FormattingType {
        BLOCK_QUOTE,
        HEADING,
        LIST_ITEM,
        FOOTNOTE,
        LINE_NUMBER,
        TRANSCRIPT,
        LYRICS,
        EMAIL
    }

    /**
     * Holds formatting metadata.
     */
    private static class FormattingInfo {

        private Pattern pattern;
        private String rbName;
        private boolean hasParam;
        private boolean countChars;

        /**
         * Constructor.
         * @param re regexp string
         * @param rbName format name string for resource bundle
         * @param hasParam is there a parameter?
         * @param countChars count characters in param group instead of taking literal value
         */
        FormattingInfo(final String re,
                       final String rbName,
                       final boolean hasParam,
                       final boolean countChars) {
            this.pattern = Pattern.compile(re);
            this.rbName = rbName;
            this.hasParam = hasParam;
            this.countChars = countChars;
        }

        /**
         * Get format info from string.
         * @param s input string
         * @return description of format or null
         */
        String getInfo(final String s) {
            Matcher matcher = pattern.matcher(s);
            if (matcher.find()) {
                String result = Util.RB.getString(rbName);
                if (hasParam) {
                    String param = matcher.group(1);
                    if (countChars) {
                        param = String.valueOf(param.length());
                    }
                    result += ": " + param;
                }
                return result;
            } else {
                return null;
            }
        }
    }

    private final Map<String, String> tag2token;
    private final Map<String, String[]> tokens2tags;

    private static final String ESCAPED_ASTERISK_TAG = "<$@EA@$>";

    private static final Map<Pattern, Set<FormattingType>> SKIP_PATTERN_MAP;

    static {
        SKIP_PATTERN_MAP = new HashMap<>();

        // Table separator line
        SKIP_PATTERN_MAP.put(
                Pattern.compile("^[|\\-+:= ]+$"),
                Collections.emptySet()
        );

        // EOB marker
        SKIP_PATTERN_MAP.put(
                Pattern.compile("^\\^\\s*$"),
                Collections.emptySet()
        );

        // Comment/command line
        SKIP_PATTERN_MAP.put(
                Pattern.compile("^\\s*\\{:.+}\\s*$"),
                Stream.of(FormattingType.TRANSCRIPT,
                          FormattingType.LYRICS,
                          FormattingType.EMAIL)
                        .collect(Collectors.toCollection(HashSet::new))
        );
    }

    private static final Map<Pattern, Set<FormattingType>> BLOCK_PATTERN_MAP;
    static {
        BLOCK_PATTERN_MAP = new HashMap<>();

        // Indentation
        BLOCK_PATTERN_MAP.put(
                Pattern.compile("^(\\s+)(.+)"),
                Collections.emptySet()
        );

        // Heading
        BLOCK_PATTERN_MAP.put(
                Pattern.compile("^(#+\\**\\s)(.+)"),
                Stream.of(FormattingType.HEADING)
                        .collect(Collectors.toCollection(HashSet::new))
        );

        // List
        BLOCK_PATTERN_MAP.put(
                Pattern.compile("^((?:\\*|\\d+.)\\s)(.+)"),
                Stream.of(FormattingType.LIST_ITEM)
                        .collect(Collectors.toCollection(HashSet::new))
        );

        // Block-quote
        BLOCK_PATTERN_MAP.put(
                Pattern.compile("^((?:>+\\s*)+)(.*)"),
                Stream.of(FormattingType.BLOCK_QUOTE)
                        .collect(Collectors.toCollection(HashSet::new))
        );

        // Footnote
        BLOCK_PATTERN_MAP.put(
                Pattern.compile("^(\\[\\^.+?]:\\s+)(.+)"),
                Stream.of(FormattingType.FOOTNOTE)
                        .collect(Collectors.toCollection(HashSet::new))
        );

        // Line number
        BLOCK_PATTERN_MAP.put(
                Pattern.compile("^(\\{L\\d+?}\\s+)(.+)"),
                Stream.of(FormattingType.LINE_NUMBER)
                        .collect(Collectors.toCollection(HashSet::new))
        );
    }

    private static final Map<FormattingType, FormattingInfo> FORMATTING_TYPE_MAP;
    static {
        FORMATTING_TYPE_MAP = new HashMap<>();
        FORMATTING_TYPE_MAP.put(
                FormattingType.BLOCK_QUOTE,
                new FormattingInfo("(>+)", "FMT_BLOCK_QUOTE", true, true)
        );
        FORMATTING_TYPE_MAP.put(
                FormattingType.FOOTNOTE,
                new FormattingInfo("\\[\\^(.+)?]:", "FMT_FOOTNOTE", true, false)
        );
        FORMATTING_TYPE_MAP.put(
                FormattingType.HEADING,
                new FormattingInfo("(#+)", "FMT_HEADING", true, true)
        );
        FORMATTING_TYPE_MAP.put(
                FormattingType.LIST_ITEM,
                new FormattingInfo(".", "FMT_LIST_ITEM", false, false)
        );
        FORMATTING_TYPE_MAP.put(
                FormattingType.LINE_NUMBER,
                new FormattingInfo("\\{L(\\d+)?}", "FMT_LINE_NUMBER", true, false)
        );
        FORMATTING_TYPE_MAP.put(
                FormattingType.TRANSCRIPT,
                new FormattingInfo("\\.transcript", "FMT_TRANSCRIPT", false, false)
        );
        FORMATTING_TYPE_MAP.put(
                FormattingType.LYRICS,
                new FormattingInfo("\\.lyrics", "FMT_LYRICS", false, false)
        );
        FORMATTING_TYPE_MAP.put(
                FormattingType.EMAIL,
                new FormattingInfo("\\.email", "FMT_EMAIL", false, false)
        );
    }

    /*
     In-text control symbols patterns (1 or more groups). Order may be important.
     This structure is used in plain parsing mode.
     */
    private static final Pattern[] TAG_PATTERNS = {
        // Emphasis pairs
        Pattern.compile("(?<!\\*)(\\*{3})(?!\\*)(?:.*?)(?<!\\*)(\\*{3})(?!\\*)"),
        Pattern.compile("(?<!\\*)(\\*{2})(?!\\*)(?:.*?)(?<!\\*)(\\*{2})(?!\\*)"),
        Pattern.compile("(?<!\\*)(\\*{1})(?!\\*)(?:.*?)(?<!\\*)(\\*{1})(?!\\*)"),
        Pattern.compile("(?<!\\*)(\\*{1,3})(?!\\*)"),   // Single emphasis of any type
        Pattern.compile("(\\[\\^).+?(])"),          // Footnote
        Pattern.compile("(~).+?(~)"),               // Subscript
        Pattern.compile("(\\^).+?(\\^)"),           // Superscript
        Pattern.compile("(name\\().+?(\\))"),       // Name wrapper
        Pattern.compile("(title\\().+?(\\))"),      // Title wrapper
        Pattern.compile("(\\|)"),                   // Table column
        Pattern.compile("(!?\\[).*?(]\\().+?(\\))") // Image/link
    };

    /*
     Token count in each row should be equal to tag count.
     This structure is used in plain parsing mode.
     */
    private static final String[][][] TAG_TABLE = {
        {{"*"}, {"<e1/>"}},  // single light emphasis
        {{"**"}, {"<e2/>"}}, // single strong emphasis
        {{"***"}, {"<e3/>"}}, // single combined emphasis
        {{"*", "*"}, {"<e1>", "</e1>"}}, // light emphasis pair
        {{"**", "**"}, {"<e2>", "</e2>"}}, // strong emphasis pair
        {{"***", "***"}, {"<e3>", "</e3>"}}, // combined emphasis pair
        {{"|"}, {"<s1/>"}}, // table column separator
        {{"^", "^"}, {"<sup1>", "</sup1>"}}, // superscript
        {{"~", "~"}, {"<sub1>", "</sub1>"}}, // subscript
        {{"[^", "]"}, {"<fn1>", "</fn1>"}},  // footnote
        {{"name(", ")"}, {"<n1>", "</n1>"}}, // name wrapper
        {{"title(", ")"}, {"<t1>", "</t1>"}}, // name wrapper
        {{"![", "](", ")"}, {"<id1>", "</id1><il1>", "</il1>"}}, // image
        {{"[", "](", ")"}, {"<ld1>", "</ld1><la1>", "</la1>"}} // link
    };

    private static final String EXTRA_FOOTNOTE_MARKER = "[^omegat-%d]";

    private static final Highlighter HIGHLIGHTER = new Highlighter();
    static {
        Core.registerMarker(HIGHLIGHTER);
    }


    /**
     * Constructor.
     */
    public PublishrFilter() {

        // Init objects for plain parsing mode
        tag2token = new HashMap<>();
        tokens2tags = new HashMap<>();
        // Populate tag/token maps
        for (int i = 0; i < TAG_TABLE.length; i++) {
            String tokens = "";
            for (int j = 0; j < TAG_TABLE[i][0].length; j++) {
                if (j == 0) {
                    tokens = TAG_TABLE[i][0][j];
                } else {
                    tokens = tokens + "\t" + TAG_TABLE[i][0][j];
                }
                tag2token.put(TAG_TABLE[i][1][j], TAG_TABLE[i][0][j]);
            }
            tokens2tags.put(tokens, TAG_TABLE[i][1]);
        }
    }

    private static IApplicationEventListener generateIApplicationEventListener() {
        return new IApplicationEventListener() {

            @Override
            public void onApplicationStartup() {
                Core.getEditor().registerPopupMenuConstructors(0,
                        new PopupMenuConstructor());
            }

            @Override
            public void onApplicationShutdown() {
            }
        };
    }

    /**
     * Plugin loader.
     */
    public static void loadPlugins() {
        Core.registerFilterClass(PublishrFilter.class);
        CoreEvents.registerApplicationEventListener(generateIApplicationEventListener());
    }

    /**
     * Plugin unloader.
     */
    public static void unloadPlugins() {
    }

    @Override
    public String getFileFormatName() {
        return Util.FILTER_NAME;
    }

    @Override
    public boolean isSourceEncodingVariable() {
        return false;
    }

    @Override
    public boolean isTargetEncodingVariable() {
        return false;
    }

    @Override
    public Instance[] getDefaultInstances() {
        return new Instance[] {
                new Instance("*.page", "UTF-8", "UTF-8"),
        };
    }

    @Override
    protected boolean requirePrevNextFields() {
        return true;
    }

    @Override
    protected boolean isFileSupported(final BufferedReader reader) {
        return true;
    }

    /**
     * Replace shortcuts with original formatting.
     * Used in plain parsing mode.
     * @param input text with shortcuts
     * @return text with original formatting
     */
    private String replaceWithFormatting(final String input) {
        String result = input;
        for (Entry<String, String> tagEntry : tag2token.entrySet()) {
            result = result.replace(tagEntry.getKey(), tagEntry.getValue());
        }
        return result;
    }

    /**
     * Find extra footnotes declarations, replace them with PublishR formatting
     * and add footnotes to external list.
     * @param text input text
     * @param extraFootnotes list of footnotes in PublishR format
     * @return processed text
     */
    private String makeExtraFootnotes(final String text, final List<String> extraFootnotes) {
        Matcher matcher = Util.EF_PATTERN.matcher(text);
        String result = text;
        while (matcher.find()) {
            int fnCounter = extraFootnotes.size() + 1;
            String fnLabel = String.format(EXTRA_FOOTNOTE_MARKER, fnCounter);
            extraFootnotes.add(fnLabel + ": " + matcher.group(2));
            result = result.replace(matcher.group(0), fnLabel);
        }
        return result;
    }

    /**
     * Replace formatting with shortcuts.
     * Used in plain parsing mode.
     * @param input text with original formatting
     * @param pattern formatting search pattern
     * @return text with shortcuts for a given pattern
     */
    private String replaceWithShortcuts(final String input, final Pattern pattern) {
        Matcher m = pattern.matcher(input);
        int lastPosition = 0;
        StringBuilder builder = new StringBuilder();
        while (m.find()) {
            String[] tags = getMatchedTags(m);
            for (int i = 1; i <= m.groupCount(); i++) {
                builder.append(input.substring(lastPosition, m.start(i)));
                builder.append(tags[i - 1]);
                lastPosition = m.end(i);
            }
        }
        if (lastPosition > 0) {
            builder.append(input.substring(lastPosition));
            return builder.toString();
        } else {
            return input;
        }
    }

    /**
     * Search for shortcut strings in static map.
     * Used in plain parsing mode.
     * @param matcher Matcher instance
     * @return array of shortcuts
     */
    private String[] getMatchedTags(final Matcher matcher) {
        StringBuilder tokenChain = new StringBuilder();
        tokenChain.append(matcher.group(1));
        for (int i = 2; i <= matcher.groupCount(); i++) {
            tokenChain.append("\t");
            tokenChain.append(matcher.group(i));
        }
        return tokens2tags.get(tokenChain.toString());
    }

    @Override
    public Map<String, String> changeOptions(final Window parent,
                                             final Map<String, String> config) {
        try {
            SettingsDialog dialog = new SettingsDialog(parent, config);
            dialog.setVisible(true);
            Map<String, String> newOptions = dialog.getOptions();
            if (newOptions != null) {
                HIGHLIGHTER.setupStyles(newOptions, true);
            }
            return newOptions;
        } catch (Exception e) {
            Log.log(e);
            return null;
        }
    }

    @Override
    public boolean hasOptions() {
        return true;
    }

    /**
     * {@inheritDoc} See syntax at http://kramdown.gettalong.org/syntax.html
     */
    @Override
    public void processFile(final BufferedReader reader, final BufferedWriter outfile,
            final FilterContext fc) throws IOException {

        // Setup HIGHLIGHTER styles. Unfortunately it can be only here.
        HIGHLIGHTER.setupStyles(processOptions, false);

        LinebreakPreservingReader lbpr = new LinebreakPreservingReader(reader);

        List<String> extraFootnotes = new ArrayList<>();

        // Do we use plain shortcuts? (pre 1.0 format)
        boolean usePlainShortcuts = Boolean.valueOf(processOptions.get(Util.PLAIN_SHORTCUTS));

        // Reset shortcut converters
        Util.FORMATTER.resetConverters();

        String line;
        Matcher matcher;

        Map<String, String> sourceExtras = new HashMap<>();
        Map<String, String> translatedExtras = new HashMap<>();

        // Collected formatting comments
        Map<FormattingType, String> formattingComments = new TreeMap<>();
        // Create a list of block patterns so it can be reiterated
        List<Pattern> blockPatterns = new ArrayList<>(BLOCK_PATTERN_MAP.keySet());

        while ((line = lbpr.readLine()) != null) {

            // Clear extra strings maps
            sourceExtras.clear();
            translatedExtras.clear();

            String br = lbpr.getLinebreak();

            /* Skip empty lines */
            if (line.trim().isEmpty()) {
                outfile.write(line + br);
                formattingComments.clear(); // Clear formatting comments
                continue;
            }

            /* Skip lines matched by skip patterns, collect format metadata */
            boolean skipLine = false;
            for (Map.Entry<Pattern, Set<FormattingType>> e : SKIP_PATTERN_MAP.entrySet()) {
                matcher = e.getKey().matcher(line);
                if (matcher.matches()) {
                    outfile.write(line + br);
                    // Check for format signature
                    for (FormattingType fType: e.getValue()) {
                        FormattingInfo fInfo = FORMATTING_TYPE_MAP.get(fType);
                        String fComment = fInfo.getInfo(line);
                        if (fComment != null) {
                            formattingComments.put(fType, fComment);
                            break;
                        }
                    }
                    skipLine = true;
                    break;
                }
            }
            if (skipLine) {
                continue;
            }

            /* Trim block-level tokens, collect format metadata */
            int i = 0;
            while (i < blockPatterns.size()) {
                Pattern pattern = blockPatterns.get(i);
                matcher = pattern.matcher(line);
                i++;
                if (matcher.matches()) {
                    outfile.write(matcher.group(1));
                    line = matcher.group(2);
                    // Check for format signature
                    for (FormattingType fType: BLOCK_PATTERN_MAP.get(pattern)) {
                        FormattingInfo fInfo = FORMATTING_TYPE_MAP.get(fType);
                        String fComment = fInfo.getInfo(matcher.group(1));
                        if (fComment != null) {
                            formattingComments.put(fType, fComment);
                            break;
                        }
                    }
                    i = 0;
                }
            }

            /* Skip empty lines after trim */
            if (line.trim().isEmpty()) {
                outfile.write(line + br);
                continue;
            }

            /* Temporary replace escaped asterisks to reduce regexp madness */
            if (line.contains("\\*")) {
                line = line.replace("\\*", ESCAPED_ASTERISK_TAG);
            }

            /* Replace formatting with OmegaT shortcuts */
            if (usePlainShortcuts) {
                for (Pattern p : TAG_PATTERNS) {
                    line = replaceWithShortcuts(line, p);
                }
            } else {
                line = Util.FORMATTER.toShortcuts(line, sourceExtras);
            }

            /* Put escaped asterisks back */
            if (line.contains(ESCAPED_ASTERISK_TAG)) {
                line = line.replace(ESCAPED_ASTERISK_TAG, "\\*");
            }

            /* Create a comment for translation */
            StringBuilder cb = new StringBuilder();
            String comment = null;

            if (!formattingComments.isEmpty()) {
                String fComment = formattingComments.values().stream()
                        .collect(Collectors.joining(" / "));
                cb.append(fComment);
                cb.append("\n");
            }

            if (!sourceExtras.isEmpty()) {
                for (Map.Entry<String, String> e : sourceExtras.entrySet()) {
                    cb.append("<");
                    cb.append(e.getKey());
                    cb.append(">: ");
                    cb.append(e.getValue());
                    cb.append("\n");
                }
            }

            if (cb.length() > 0) {
                comment = cb.toString();
            }

            /* Translate the text */
            line = processEntry(line, comment);

            /* Translate extra strings */
            if (!usePlainShortcuts) {
                for (Map.Entry<String, String> e : sourceExtras.entrySet()) {
                    String translatedExtra = processEntry(e.getValue(),
                            String.format("<%s>", e.getKey()));
                    translatedExtras.put(e.getValue(), translatedExtra);
                }
            }

            /* Replace OmegaT shortcuts with original formatting */
            if (usePlainShortcuts) {
                line = replaceWithFormatting(line);
            } else {
                line = Util.FORMATTER.toOriginal(line, translatedExtras);
            }

            /* Check for extra footnotes */
            line = makeExtraFootnotes(line, extraFootnotes);

            /* Write translated text to file */
            outfile.write(line + br);
        }

        // Finally write extra footnotes created during translation
        for (String fn : extraFootnotes) {
            outfile.write("\n\n");
            outfile.write(fn);
        }
    }
}
