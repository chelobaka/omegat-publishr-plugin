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

import java.util.Locale;
import java.util.ResourceBundle;

public final class Util {

    /**
     * Resource bundle.
     */
    public static final ResourceBundle RB;

    static {
        ResourceBundle.Control utf8Control = new UTF8Control();
        RB = ResourceBundle.getBundle("PublishR_strings", Locale.getDefault(), utf8Control);
    }

    /**
     * Do not allow instances of this class.
     */
    private Util() { }
}
