/*
VisAD system for interactive analysis and visualization of numerical
data.  Copyright (C) 1996 - 2002 Bill Hibbard, Curtis Rueden, Tom
Rink, Dave Glowacki, Steve Emmerson, Tom Whittaker, Don Murray, and
Tommy Jasmin.

This library is free software; you can redistribute it and/or
modify it under the terms of the GNU Library General Public
License as published by the Free Software Foundation; either
version 2 of the License, or (at your option) any later version.

This library is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
Library General Public License for more details.

You should have received a copy of the GNU Library General Public
License along with this library; if not, write to the Free
Software Foundation, Inc., 59 Temple Place - Suite 330, Boston,
MA 02111-1307, USA
*/

/* Generated By:JavaCC: Do not edit this line. UnitParserConstants.java */
package visad.data.units;

public interface UnitParserConstants {

  int EOF = 0;
  int SIGN = 1;
  int DIGIT = 2;
  int INT = 3;
  int INTEGER = 4;
  int EXP = 5;
  int DECIMAL = 6;
  int REAL = 7;
  int WHITESPACE = 8;
  int SINCE = 9;
  int FROM = 10;
  int SHIFT = 11;
  int DIVIDE = 12;
  int LETTER = 13;
  int NAME = 14;
  int YEAR = 15;
  int MONTH = 16;
  int DAY = 17;
  int DATE = 18;
  int HOUR = 19;
  int MINUTE = 20;
  int SECOND = 21;
  int TIME = 22;

  int DEFAULT = 0;

  String[] tokenImage = {
    "<EOF>",
    "<SIGN>",
    "<DIGIT>",
    "<INT>",
    "<INTEGER>",
    "<EXP>",
    "<DECIMAL>",
    "<REAL>",
    "<WHITESPACE>",
    "<SINCE>",
    "<FROM>",
    "<SHIFT>",
    "<DIVIDE>",
    "<LETTER>",
    "<NAME>",
    "<YEAR>",
    "<MONTH>",
    "<DAY>",
    "<DATE>",
    "<HOUR>",
    "<MINUTE>",
    "<SECOND>",
    "<TIME>",
    "\".\"",
    "\"*\"",
    "\"(\"",
    "\")\"",
    "\"^\"",
  };

}
