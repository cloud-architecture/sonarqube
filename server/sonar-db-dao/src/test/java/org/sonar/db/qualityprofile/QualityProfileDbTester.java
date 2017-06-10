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
package org.sonar.db.qualityprofile;

import java.util.Arrays;
import java.util.Optional;
import java.util.function.Consumer;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.rule.RuleDefinitionDto;

import static org.sonar.api.rule.Severity.MAJOR;
import static org.sonar.db.qualityprofile.ActiveRuleDto.createFor;

public class QualityProfileDbTester {
  private final DbTester dbTester;
  private final DbClient dbClient;
  private final DbSession dbSession;

  public QualityProfileDbTester(DbTester dbTester) {
    this.dbTester = dbTester;
    this.dbClient = dbTester.getDbClient();
    this.dbSession = dbTester.getSession();
  }

  public Optional<RulesProfileDto> selectByKey(String key) {
    return Optional.ofNullable(dbClient.qualityProfileDao().selectByKey(dbSession, key));
  }

  /**
   * Create a profile with random field values on the specified organization.
   */
  @SafeVarargs
  public final RulesProfileDto insert(OrganizationDto organization, Consumer<RulesProfileDto>... consumers) {
    RulesProfileDto profile = QualityProfileTesting.newQualityProfileDto()
      .setOrganizationUuid(organization.getUuid());
    Arrays.stream(consumers).forEach(c -> c.accept(profile));

    dbClient.qualityProfileDao().insert(dbSession, profile);
    dbSession.commit();
    return profile;
  }

  public void insertQualityProfiles(RulesProfileDto qualityProfile, RulesProfileDto... qualityProfiles) {
    dbClient.qualityProfileDao().insert(dbSession, qualityProfile, qualityProfiles);
    dbSession.commit();
  }

  public RulesProfileDto insertQualityProfile(RulesProfileDto qualityProfile) {
    dbClient.qualityProfileDao().insert(dbSession, qualityProfile);
    dbSession.commit();
    return qualityProfile;
  }

  public void insertProjectWithQualityProfileAssociations(ComponentDto project, RulesProfileDto... qualityProfiles) {
    dbClient.componentDao().insert(dbSession, project);
    for (RulesProfileDto qualityProfile : qualityProfiles) {
      dbClient.qualityProfileDao().insertProjectProfileAssociation(dbSession, project, qualityProfile);
    }
    dbSession.commit();
  }

  public void associateProjectWithQualityProfile(ComponentDto project, RulesProfileDto... qualityProfiles) {
    for (RulesProfileDto qualityProfile : qualityProfiles) {
      dbClient.qualityProfileDao().insertProjectProfileAssociation(dbSession, project, qualityProfile);
    }
    dbSession.commit();
  }

  @SafeVarargs
  public final ActiveRuleDto activateRule(RulesProfileDto profile, RuleDefinitionDto rule, Consumer<ActiveRuleDto>... consumers) {
    ActiveRuleDto activeRule = createFor(profile, rule).setSeverity(MAJOR);
    for (Consumer<ActiveRuleDto> consumer : consumers) {
      consumer.accept(activeRule);
    }
    dbClient.activeRuleDao().insert(dbSession, activeRule);
    dbSession.commit();
    return activeRule;
  }

  public void markAsDefault(RulesProfileDto... profiles) {
    for (RulesProfileDto profile : profiles) {
      DefaultQProfileDto dto = new DefaultQProfileDto()
        .setOrganizationUuid(profile.getOrganizationUuid())
        .setLanguage(profile.getLanguage())
        .setQProfileUuid(profile.getKee());
      dbClient.defaultQProfileDao().insertOrUpdate(dbSession, dto);
    }
    
    dbSession.commit();
  }

  public Optional<String> selectUuidOfDefaultProfile(OrganizationDto org, String language) {
    return dbTester.select("select qprofile_uuid as \"profileUuid\" " +
      " from default_qprofiles " +
      " where organization_uuid='" + org.getUuid() + "' and language='" + language + "'")
      .stream()
      .findFirst()
      .map(m -> (String)m.get("profileUuid"));
  }
}
