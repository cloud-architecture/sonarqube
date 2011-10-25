/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2011 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.persistence.model;

import org.apache.commons.io.IOUtils;
import org.apache.ibatis.session.SqlSession;
import org.dbunit.DataSourceDatabaseTester;
import org.dbunit.IDatabaseTester;
import org.dbunit.database.DatabaseConfig;
import org.dbunit.database.IDatabaseConnection;
import org.dbunit.dataset.CompositeDataSet;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.ReplacementDataSet;
import org.dbunit.dataset.xml.FlatXmlDataSet;
import org.dbunit.operation.DatabaseOperation;
import org.hamcrest.core.Is;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sonar.persistence.InMemoryDatabase;
import org.sonar.persistence.MyBatis;

import java.io.InputStream;
import java.util.List;

import static org.junit.Assert.assertThat;

public class RuleMapperTest {

  protected IDatabaseTester databaseTester;
  private MyBatis myBatis;
  private InMemoryDatabase database;

  @Before
  public final void startDatabase() throws Exception {
    database = new InMemoryDatabase();
    myBatis = new MyBatis(database);
    
    database.start();
    myBatis.start();

    databaseTester = new DataSourceDatabaseTester(database.getDataSource());
  }

  @After
  public final void stopDatabase() throws Exception {
    if (databaseTester != null) {
      databaseTester.onTearDown();
    }
    database.stop();
  }

  @Test
  public void testSelectAll() throws Exception {
    setupData("selectAll");
    SqlSession sqlSession = myBatis.openSession();
    RuleMapper ruleMapper = sqlSession.getMapper(RuleMapper.class);
    List<Rule> rules = ruleMapper.selectAll();
    sqlSession.close();

    assertThat(rules.size(), Is.is(1));
    Rule rule = rules.get(0);
    assertThat(rule.getId(), Is.is(1L));
    assertThat(rule.getName(), Is.is("Avoid Null"));
    assertThat(rule.getDescription(), Is.is("Should avoid NULL"));
    assertThat(rule.isEnabled(), Is.is(true));
    assertThat(rule.getRepositoryKey(), Is.is("checkstyle"));
  }

  protected final void setupData(String... testNames) throws Exception {
    InputStream[] streams = new InputStream[testNames.length];
    try {
      for (int i = 0; i < testNames.length; i++) {
        String className = getClass().getName();
        className = String.format("/%s/%s.xml", className.replace(".", "/"), testNames[i]);
        streams[i] = getClass().getResourceAsStream(className);
        if (streams[i] == null) {
          throw new RuntimeException("Test not found :" + className);
        }
      }

      setupData(streams);

    } finally {
      for (InputStream stream : streams) {
        IOUtils.closeQuietly(stream);
      }
    }
  }

  protected final void setupData(InputStream... dataSetStream) throws Exception {
    IDataSet[] dataSets = new IDataSet[dataSetStream.length];
    for (int i = 0; i < dataSetStream.length; i++) {
      ReplacementDataSet dataSet = new ReplacementDataSet(new FlatXmlDataSet(dataSetStream[i]));
      dataSet.addReplacementObject("[null]", null);
      dataSets[i] = dataSet;
    }
    CompositeDataSet compositeDataSet = new CompositeDataSet(dataSets);

    databaseTester.setDataSet(compositeDataSet);
    IDatabaseConnection connection = databaseTester.getConnection();
    DatabaseConfig config = connection.getConfig();
    //config.setProperty(DatabaseConfig.PROPERTY_DATATYPE_FACTORY, new Db2DataTypeFactory());

    DatabaseOperation.CLEAN_INSERT.execute(connection, databaseTester.getDataSet());

    connection.getConnection().commit();
    connection.close();

  }

}
