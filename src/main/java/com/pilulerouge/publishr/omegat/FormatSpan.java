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


import java.util.Objects;

/**
 * Combination of Element and BlockType.
 */
class FormatSignature {
    private final BlockType type;
    private Element element;

    FormatSignature(final BlockType type, final Element element) {
        this.type = type;
        if (type == BlockType.TEXT) {
            this.element = null;
        } else {
            this.element = element;
        }
    }

    BlockType getType() {
        return type;
    }

    Element getElement() {
        return element;
    }

    void setElement(final Element e) {
        element = e;
    }

    @Override
    public boolean equals(final Object o) {
        if (o == null || o.getClass() != this.getClass()) {
            return false;
        }
        return ((FormatSignature) o).type == type && ((FormatSignature) o).element == element;
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, element);
    }
}

/**
 * Full metadata on text block.
 */
public class FormatSpan implements Comparable<FormatSpan> {
    private final int begin;
    private final int end;
    private final FormatSignature signature;

    FormatSpan(final BlockType type, final Element element, final int begin, final int end) {
        this.begin = begin;
        this.end = end;
        signature = new FormatSignature(type, element);
    }

    FormatSpan(final FormatSignature signature, final int begin, final int end) {
        this.begin = begin;
        this.end = end;
        this.signature = signature;
    }

    int getBegin() {
        return begin;
    }

    int getEnd() {
        return end;
    }

    FormatSignature getSignature() {
        return signature;
    }

    @Override
    public int compareTo(final FormatSpan that) {
        return Integer.compare(this.begin, that.begin);
    }
}
