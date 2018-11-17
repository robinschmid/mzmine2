/*
 * Copyright 2006-2018 The MZmine 2 Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with MZmine 2; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package net.sf.mzmine.modules.visualization.peaklisttable.table;

import net.sf.mzmine.datamodel.PeakIdentity;
import net.sf.mzmine.datamodel.PeakListRow;

public enum CommonColumnType {

  ROWID("ID", Integer.class), //
  AVERAGEMZ("m/z", Double.class), //
  AVERAGERT("RT", Double.class), //
  IDENTITY("Identity", PeakIdentity.class, true), //
  COMMENT("Comment", String.class, true), //
  GROUPID("GID", Integer.class), //
  NETID("NetID", Integer.class), //
  IONTYPE("Ion", String.class, true), //
  NEUTRAL_MASS("Neutral mass", Double.class), //
  NEUTRAL_FORMULA("Formula (M)", String.class, true), //
  ION_FORMULA("Formula (ion)", String.class, true), //
  ION_FORMULA_MASS("Mass", Double.class), //
  ION_FORMULA_PPM("ppm", Double.class), //
  ION_FORMULA_ISOTOPE_SCORE("isotope score", Double.class), //
  ION_FORMULA_MSM_SCORE("MS/MS score", Double.class), //
  PEAKSHAPE("Peak shape", PeakListRow.class);

  private final String columnName;
  private final Class<?> columnClass;
  private final boolean isEditable;

  CommonColumnType(String columnName, Class<?> columnClass) {
    this(columnName, columnClass, false);
  }

  CommonColumnType(String columnName, Class<?> columnClass, boolean isEditable) {
    this.columnName = columnName;
    this.columnClass = columnClass;
    this.isEditable = isEditable;
  }

  public String getColumnName() {
    return columnName;
  }

  public Class<?> getColumnClass() {
    return columnClass;
  }

  @Override
  public String toString() {
    return columnName;
  }

  public boolean isEditable() {
    return isEditable;
  }
}
