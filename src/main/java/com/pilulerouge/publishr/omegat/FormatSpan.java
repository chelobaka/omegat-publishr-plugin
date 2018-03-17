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


class FormatSignature {
    final BlockType type;
    Element element;

    public FormatSignature(BlockType type, Element element) {
        this.type = type;
        if (type == BlockType.TEXT) {
            this.element = null;
        } else {
            this.element = element;
        }
    }

    @Override
    public boolean equals(Object o) {
        return ((FormatSignature) o).type == type && ((FormatSignature) o).element == element;
    }
}


public class FormatSpan implements Comparable<FormatSpan> {
    final int begin;
    final int end;
    final FormatSignature signature;

    public FormatSpan(BlockType type, Element element, int begin, int end) {
        this.begin = begin;
        this.end = end;
        signature = new FormatSignature(type, element);
    }

    public FormatSpan(FormatSignature signature, int begin, int end) {
        this.begin = begin;
        this.end = end;
        this.signature = signature;
    }

    @Override
    public int compareTo(FormatSpan that) {
        return Integer.compare(this.begin, that.begin);
    }
}
