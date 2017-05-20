package com.io7m.roommodel0;

import com.io7m.roommodel0.mesh.MeshReadableType;
import com.io7m.roommodel0.mesh.PolygonID;
import com.io7m.roommodel0.mesh.PolygonType;

import java.util.Map;
import java.util.Set;

public interface RoomLiquidCellsType
{
  MeshReadableType mesh();

  Set<CellType> cellRoots();

  Map<PolygonID, CellType> cellsAll();

  interface CellType
  {
    PolygonType polygon();

    Set<CellType> cellsAbove();

    Set<CellType> cellsBelow();
  }
}
