package com.io7m.roommodel0.tests.mesh;

import com.io7m.jregions.core.unparameterized.areas.AreasI;
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
      AreasI.create(-2048, -2048, 4096, 4096));
  }
}
