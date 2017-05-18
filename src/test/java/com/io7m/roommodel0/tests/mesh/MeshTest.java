package com.io7m.roommodel0.tests.mesh;

import com.io7m.jfunctional.Unit;
import com.io7m.jregions.core.unparameterized.areas.AreasL;
import com.io7m.roommodel0.RoomModel;
import com.io7m.roommodel0.RoomModelType;
import com.io7m.roommodel0.mesh.Mesh;
import com.io7m.roommodel0.mesh.MeshType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class MeshTest extends MeshContract
{
  private static final Logger LOG;

  static {
    LOG = LoggerFactory.getLogger(MeshTest.class);
  }

  @Override
  protected Logger log()
  {
    return LOG;
  }

  @Override
  protected MeshType emptyMesh()
  {
    return Mesh.create(
      AreasL.create(-2048L, -2048L, 4096L, 4096L));
  }
}
