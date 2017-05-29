/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.sonar.server.platform.db.migration.version.v65;

import java.sql.SQLException;
import org.sonar.api.utils.System2;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.step.DataChange;
import org.sonar.server.platform.db.migration.step.MassUpdate;

public class PopulateQProfiles  extends DataChange {

  private final System2 system2;

  public PopulateQProfiles(Database db, System2 system2) {
    super(db);
    this.system2 = system2;
  }

  @Override
  protected void execute(Context context) throws SQLException {
    long now = system2.now();

    MassUpdate massUpdate = context.prepareMassUpdate();
    massUpdate.select("select p.kee, p.organization_uuid, p.parent_kee from rules_profiles p " +
      "where not exists ( select qp.uuid from qprofiles qp where qp.uuid = p.kee and qp.organization_uuid = p.organization_uuid )");
    massUpdate.update("insert into qprofiles" +
      " (uuid, organization_uuid, rules_profile_uuid, parent_uuid, created_at, updated_at) values (?, ?, ?, ?, ?, ?)");
    massUpdate.rowPluralName("qprofiles");
    massUpdate.execute((row, update) -> {
      update.setString(1, row.getString(1));
      update.setString(2, row.getString(2));
      update.setString(3, row.getString(1));
      update.setString(4, row.getString(3));
      update.setLong(5, now);
      update.setLong(6, now);
      return true;
    });
  }
}
