/**************************************************************************
 OmegaT - Computer Assisted Translation (CAT) tool
          with fuzzy matching, translation memory, keyword search,
          glossaries, and translation leveraging into updated projects.

 Copyright (C) 2017 Lev Abashkin
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

import org.omegat.core.Core;
import org.omegat.core.data.IProject;
import org.omegat.gui.editor.IPopupMenuConstructor;
import org.omegat.gui.editor.SegmentBuilder;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.text.JTextComponent;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * Popup menu constructor.
 */
public class PublishrPopupMenuConstructor implements IPopupMenuConstructor {

    private final ResourceBundle rb;
    private final String extraFootnoteTagPair;

    public PublishrPopupMenuConstructor() {
        ResourceBundle.Control utf8Control = new UTF8Control();
        rb = ResourceBundle.getBundle("Messages", Locale.getDefault(), utf8Control);
        extraFootnoteTagPair = String.format(
                "<%s>%s</%s>",
                PublishrFilter.EXTRA_FOOTNOTE_TAGNAME,
                rb.getString("FOOTNOTE_TEXT_HINT"),
                PublishrFilter.EXTRA_FOOTNOTE_TAGNAME);
    }

    /**
     * Check if current file using PublishR file filter.
     * @return check result
     */
    private static boolean isPublishrFile() {
        String filePath = Core.getEditor().getCurrentFile();
        if (filePath == null) {
            return false;
        }
        for (IProject.FileInfo fi : Core.getProject().getProjectFiles()) {
            if (fi.filePath.equals(filePath)
                    && fi.filterFileFormatName.equals(PublishrFilter.FILTER_NAME)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void addItems(final JPopupMenu menu,
                         final JTextComponent comp,
                         final int mousepos,
                         final boolean isInActiveEntry,
                         final boolean isInActiveTranslation,
                         final SegmentBuilder sb) {

        if (!isPublishrFile()) {
            return;
        }

        JMenuItem item = new JMenuItem();
        item.setText(rb.getString("POPUP_MENU_INSERT_FOOTNOTE"));
        item.addActionListener(e -> Core.getEditor().insertTag(extraFootnoteTagPair));
        menu.addSeparator();
        menu.add(item);
        menu.addSeparator();
    }
}

