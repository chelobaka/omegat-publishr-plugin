/**************************************************************************
 OmegaT - Computer Assisted Translation (CAT) tool
          with fuzzy matching, translation memory, keyword search,
          glossaries, and translation leveraging into updated projects.

 Copyright (C) 2016 Lev Abashkin
               Home page: http://www.omegat.org/
               Support center: http://groups.yahoo.com/group/OmegaT/

 This file is part of OmegaT.

 OmegaT is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 OmegaT is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 **************************************************************************/

package com.pilulerouge.publishr.omegat;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.omegat.core.Core;

import org.omegat.filters2.AbstractFilter;
import org.omegat.filters2.FilterContext;
import org.omegat.filters2.Instance;
import org.omegat.util.LinebreakPreservingReader;

/**
 * PublishR syntax filter.
 *
 * @author Lev Abashkin
 */
public class PublishrFilter extends AbstractFilter {

    private static final String FILTER_NAME = "PublishR";
    private final Map<String, String> tag2token;
    private final Map<String, String[]> tokens2tags;

    private static final String ESCAPED_ASTERISK_TAG = "<$@EA@$>";

    // Non-translatable line patterns (no groups)
    private final Pattern[] skipPatterns = {
        Pattern.compile("^[\\|\\-\\+:= ]+$"), // Table separator line
        Pattern.compile("^\\{\\:.+\\}\\s*$"), // Comment/command line
        Pattern.compile("^\\^\\s*$"),         // EOB marker
    };

    // Block level patterns (2 groups)
    private final Pattern[] blockPatterns = {
        Pattern.compile("^(\\s+)(.+)"),                 // Indentation
        Pattern.compile("^(#+\\**\\s)(.+)"),            // Heading
        Pattern.compile("^((?:\\*|\\d+.)\\s)(.+)"),     // List
        Pattern.compile("^((?:>+\\s*)+)(.*)"),           // Block-quote
        Pattern.compile("^(\\[\\^.+?\\]\\:\\s+)(.+)")   // Footnote
    };

    // In-text control symbols patterns (1 or more groups).
    // Order may be important.
    private final Pattern[] tagPatterns = {
        // Emphasis pairs
        Pattern.compile("(?<!\\*)(\\*{3})(?!\\*)(?:.*?)(?<!\\*)(\\*{3})(?!\\*)"),
        Pattern.compile("(?<!\\*)(\\*{2})(?!\\*)(?:.*?)(?<!\\*)(\\*{2})(?!\\*)"),
        Pattern.compile("(?<!\\*)(\\*{1})(?!\\*)(?:.*?)(?<!\\*)(\\*{1})(?!\\*)"),
        Pattern.compile("(?<!\\*)(\\*{1,3})(?!\\*)"),   // Single emphasis of any type
        Pattern.compile("(~)(?:[^~]+)(~)"),              // Subscript
        Pattern.compile("(\\^)(?:[^\\^]+)(\\^)"),       // Superscript
        Pattern.compile("(\\[\\^).+?(\\])"),            // Footnote
        Pattern.compile("(name\\()(?:[^\\)]+)(\\))"),   // Name wrapper
        Pattern.compile("(title\\()(?:[^\\)]+)(\\))"),  // Title wrapper
        Pattern.compile("(\\|)"),                       // Table column
        Pattern.compile("(!?\\[)(?:[^\\]]*)(\\]\\()(?:[^\\)]+)(\\))") // Image/link
    };

    // Token count in each row should be equal to tag count
    private final String[][][] tagTable = {
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

    /**
     * Constructor.
     */
    public PublishrFilter() {
        tag2token = new HashMap<>();
        tokens2tags = new HashMap<>();
        // Populate tag/token maps
        for (int i = 0; i < tagTable.length; i++) {
            String tokens = "";
            for (int j = 0; j < tagTable[i][0].length; j++) {
                if (j == 0) {
                    tokens = tagTable[i][0][j];
                } else {
                    tokens = tokens + "\t" + tagTable[i][0][j];
                }
                tag2token.put(tagTable[i][1][j], tagTable[i][0][j]);
            }
            tokens2tags.put(tokens, tagTable[i][1]);
        }
    }

    /**
     * Plugin loader.
     */
    public static void loadPlugins() {
        Core.registerFilterClass(PublishrFilter.class);
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
        return true;
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

    /**
     * {@inheritDoc} See syntax at http://kramdown.gettalong.org/syntax.html
     */
    @Override
    public void processFile(final BufferedReader reader, final BufferedWriter outfile,
            final FilterContext fc) throws IOException {
        LinebreakPreservingReader lbpr = new LinebreakPreservingReader(reader);

        String line;
        Matcher matcher;

        while ((line = lbpr.readLine()) != null) {

            String br = lbpr.getLinebreak();

            /* Skip non-translatable lines */
            if (line.trim().isEmpty()) {
                outfile.write(line + br);
                continue;
            }

            boolean skipLine = false;
            for (Pattern p : skipPatterns) {
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
            while (i < blockPatterns.length) {
                matcher = blockPatterns[i].matcher(line);
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

            /* Replace tokens with OmegaT tags */
            for (Pattern p : tagPatterns) {
                line = replaceWithTags(line, p);
            }

            /* Put escaped asterisks back */
            if (line.contains(ESCAPED_ASTERISK_TAG)) {
                line = line.replace(ESCAPED_ASTERISK_TAG, "\\*");
            }

            /* Translate the text */
            line = processEntry(line);

            /* Replace OmegaT tags with tokens */
            line = replaceWithTokens(line);

            /* Write translated text to file */
            outfile.write(line + br);
        }
    }
}
