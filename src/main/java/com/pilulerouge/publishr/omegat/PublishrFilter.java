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

import java.awt.*;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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

    static final String FILTER_NAME = Util.RB.getString("FILTER_NAME");
    private final Map<String, String> tag2token;
    private final Map<String, String[]> tokens2tags;

    private static final String ESCAPED_ASTERISK_TAG = "<$@EA@$>";

    // Non-translatable line patterns (no groups)
    private static final Pattern[] SKIP_PATTERNS = {
        Pattern.compile("^[\\|\\-\\+:= ]+$"), // Table separator line
        Pattern.compile("^\\s*\\{\\:.+\\}\\s*$"), // Comment/command line
        Pattern.compile("^\\^\\s*$"),         // EOB marker
    };

    // Block level patterns (2 groups)
    private static final Pattern[] BLOCK_PATTERNS = {
        Pattern.compile("^(\\s+)(.+)"),                 // Indentation
        Pattern.compile("^(#+\\**\\s)(.+)"),            // Heading
        Pattern.compile("^((?:\\*|\\d+.)\\s)(.+)"),     // List
        Pattern.compile("^((?:>+\\s*)+)(.*)"),           // Block-quote
        Pattern.compile("^(\\[\\^.+?\\]\\:\\s+)(.+)"),  // Footnote
        Pattern.compile("^(\\{L\\d+?\\}\\s+)(.+)")      // Line number
    };

    // In-text control symbols patterns (1 or more groups).
    // Order may be important.
    private static final Pattern[] TAG_PATTERNS = {
        // Emphasis pairs
        Pattern.compile("(?<!\\*)(\\*{3})(?!\\*)(?:.*?)(?<!\\*)(\\*{3})(?!\\*)"),
        Pattern.compile("(?<!\\*)(\\*{2})(?!\\*)(?:.*?)(?<!\\*)(\\*{2})(?!\\*)"),
        Pattern.compile("(?<!\\*)(\\*{1})(?!\\*)(?:.*?)(?<!\\*)(\\*{1})(?!\\*)"),
        Pattern.compile("(?<!\\*)(\\*{1,3})(?!\\*)"),   // Single emphasis of any type
        Pattern.compile("(\\[\\^).+?(\\])"),            // Footnote
        Pattern.compile("(~)(?:[^~]+)(~)"),              // Subscript
        Pattern.compile("(\\^)(?:[^\\^]+)(\\^)"),       // Superscript
        Pattern.compile("(name\\()(?:[^\\)]+)(\\))"),   // Name wrapper
        Pattern.compile("(title\\()(?:[^\\)]+)(\\))"),  // Title wrapper
        Pattern.compile("(\\|)"),                       // Table column
        Pattern.compile("(!?\\[)(?:[^\\]]*)(\\]\\()(?:[^\\)]+)(\\))") // Image/link
    };

    // Token count in each row should be equal to tag count
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

    private static final String EXTRA_FOOTNOTE_STRING = "[^omegat-%d]";
    static final String EXTRA_FOOTNOTE_TAGNAME = "ef";
    private static final Pattern EXTRA_FOOTNOTE_PATTERN = Pattern.compile(
            String.format("<%s>(.+?)</%s>",
                    EXTRA_FOOTNOTE_TAGNAME,
                    EXTRA_FOOTNOTE_TAGNAME)
    );

    // Shortcut converters (v.1.0).
    private ShortcutConverter[] shortcutConverters;

    /**
     * Constructor.
     */
    public PublishrFilter() {
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

        // Initialize shortcut converters. Order is important!
        shortcutConverters = new ShortcutConverter[]{
            // Triple asterisk pair
            new ShortcutConverter("(\\*{3})(.*?)(\\*{3})","e3",2,0),
            // Double asterisk pair
            new ShortcutConverter("(\\*{2})(.*?)(\\*{2})","e2",2,0),
            // Single asterisk pair
            new ShortcutConverter("(\\*)(.*?)(\\*)","e1",2,0),
            // Triple asterisk
            new ShortcutConverter("(\\*{3})", "e3", 0,0),
            // Double asterisk
            new ShortcutConverter("(\\*{2})", "e2", 0,0),
            // Single asterisk
            new ShortcutConverter("(\\*)", "e1", 0,0),
            // Footnote reference
            new ShortcutConverter("(\\[\\^.+?\\])", "f",0,0),
            // Subscript
            new ShortcutConverter("(~)(.+?)(~)", "s1", 2, 0),
            // Superscript
            new ShortcutConverter("(\\^)(.+?)(\\^)","s2", 2, 0),
            // Table column separator
            new ShortcutConverter("(\\|)", "s3", 0, 0),
            // Name wrapper
            new ShortcutConverter("(name\\()(.+?)(\\))", "n1", 2, 0),
            // Title wrapper
            new ShortcutConverter("(title\\()(.+?)(\\))", "t1", 2, 0),
            // Image
            new ShortcutConverter("(\\!\\[)(.+?)(\\]\\(.+?\\))", "i", 2, 0),
            // Link
            new ShortcutConverter("(\\[)(.+?)(\\]\\()(.+?)(\\))", "a", 2, 4)
        };
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
        return FILTER_NAME;
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
        return false;
    }

    @Override
    protected boolean isFileSupported(final BufferedReader reader) {
        return true;
    }

    private String replaceWithTokens(final String input) {
        String result = input;
        for (Entry<String, String> tagEntry : tag2token.entrySet()) {
            result = result.replace(tagEntry.getKey(), tagEntry.getValue());
        }
        return result;
    }

    /**
     * Find extra footnotes declarations, replace them with PublishR formatting
     * and add footnotes to external list.
     * @param text
     * @param extraFootnotes
     * @return
     */
    private String makeExtraFootnotes(String text, List<String> extraFootnotes) {
        Matcher matcher = EXTRA_FOOTNOTE_PATTERN.matcher(text);
        while (matcher.find()) {
            int fnCounter = extraFootnotes.size() + 1;
            String fnLabel = String.format(EXTRA_FOOTNOTE_STRING, fnCounter);
            extraFootnotes.add(fnLabel + ": " + matcher.group(1));
            text = text.replace(matcher.group(0), fnLabel);
        }
        return text;
    }

    private String replaceWithTags(final String input, final Pattern pattern) {
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
    public Map<String, String> changeOptions(Window parent, Map<String, String> config) {
        try {
            SettingsDialog dialog = new SettingsDialog(parent, config);
            dialog.setVisible(true);
            return dialog.getOptions();
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
        LinebreakPreservingReader lbpr = new LinebreakPreservingReader(reader);

        List<String> extraFootnotes = new ArrayList<>();

        // Do we use plain shortcuts? (pre 1.0 format)
        boolean usePlainShortcuts = Boolean.valueOf(processOptions.get(Util.PLAIN_SHORTCUTS));

        // Reset shortcut converters
        for (ShortcutConverter sc : shortcutConverters) {
            sc.reset();
        }

        String line;
        Matcher matcher;

        Map<String, String> sourceExtras = new HashMap<>();
        Map<String, String> translatedExtras = new HashMap<>();

        while ((line = lbpr.readLine()) != null) {

            // Clear extra strings maps
            sourceExtras.clear();
            translatedExtras.clear();

            String br = lbpr.getLinebreak();

            /* Skip non-translatable lines */
            if (line.trim().isEmpty()) {
                outfile.write(line + br);
                continue;
            }

            boolean skipLine = false;
            for (Pattern p : SKIP_PATTERNS) {
                matcher = p.matcher(line);
                if (matcher.matches()) {
                    outfile.write(line + br);
                    skipLine = true;
                    break;
                }
            }
            if (skipLine) {
                continue;
            }

            /* Trim block-level tokens */
            int i = 0;
            while (i < BLOCK_PATTERNS.length) {
                matcher = BLOCK_PATTERNS[i].matcher(line);
                i++;
                if (matcher.matches()) {
                    outfile.write(matcher.group(1));
                    line = matcher.group(2);
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
                    line = replaceWithTags(line, p);
                }
            }
            else {
                for (ShortcutConverter sc : shortcutConverters) {
                    line = sc.makeShortcuts(line, sourceExtras);
                }
            }

            /* Put escaped asterisks back */
            if (line.contains(ESCAPED_ASTERISK_TAG)) {
                line = line.replace(ESCAPED_ASTERISK_TAG, "\\*");
            }

            /* Translate the text */
            line = processEntry(line);

            /* Translate extra strings */
            if (!usePlainShortcuts) {
                for (String key : sourceExtras.keySet()) {
                    String translatedExtra = processEntry(sourceExtras.get(key), key);
                    translatedExtras.put(sourceExtras.get(key), translatedExtra);
                }
            }

            /* Replace OmegaT shortcuts with original formatting */
            if (usePlainShortcuts) {
                line = replaceWithTokens(line);
            }
            else {
                for (ShortcutConverter sc : shortcutConverters) {
                    line = sc.removeShortcuts(line, translatedExtras);
                }
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
