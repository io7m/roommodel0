package com.io7m.roommodel0.tests;

import com.io7m.jregions.core.unparameterized.areas.AreasL;
import com.io7m.roommodel0.RoomModel;
import com.io7m.roommodel0.RoomModelType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class RoomModelTest extends RoomModelContract
{
  private static final Logger LOG;

  static {
    LOG = LoggerFactory.getLogger(RoomModelTest.class);
  }

  @Override
  protected Logger log()
  {
    return LOG;
  }

  @Override
  protected RoomModelType emptyModel()
  {
    return RoomModel.create(
      AreasL.create(-2048L, -2048L, 4096L, 4096L));
  }
}
