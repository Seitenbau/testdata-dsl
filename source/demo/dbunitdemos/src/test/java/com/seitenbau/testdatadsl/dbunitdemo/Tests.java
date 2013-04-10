package com.seitenbau.testdatadsl.dbunitdemo;

import java.sql.Connection;
import java.sql.DriverManager;

import junit.framework.Assert;

import org.dbunit.database.DatabaseConnection;
import org.dbunit.database.IDatabaseConnection;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.ITable;
import org.dbunit.operation.DatabaseOperation;
import org.junit.Before;
import org.junit.Test;

import com.seitenbau.testdatadsl.dbunitdemo.sbtesting.OldSBTestingDataSetProvider;

public class Tests {
	
	//private static final IDataSetProvider PROVIDER = new XmlDataSetProvider(); 
	//private static final IDataSetProvider PROVIDER = new JavaDataSetProvider(); 
	private static final IDataSetProvider PROVIDER = new OldSBTestingDataSetProvider(); 
	//private static final IDataSetProvider PROVIDER = new SBTestingDataSetProvider(); 
	//private static final IDataSetProvider PROVIDER = new GroovyDataSetProvider();

	protected IDatabaseConnection getConnection() throws Exception {
		Class.forName("com.mysql.jdbc.Driver");
		Connection jdbcConnection = DriverManager.getConnection(
				"jdbc:mysql://localhost:3306/tests", "root", "");
		return new DatabaseConnection(jdbcConnection);
	}


	@Before
	public void setUp() throws Exception {
		final IDatabaseConnection connection = getConnection();
		final IDataSet dataSet = PROVIDER.getDataSet();
		try {
			DatabaseOperation.CLEAN_INSERT.execute(connection, dataSet);
		} finally {
			connection.close();
		}
	}

	@Test
	public void testVerteilteSysteme() throws Exception {
		final String query = "SELECT professor.name "
				+ "FROM professor, lehrveranstaltung "
				+ "WHERE professor.id = lehrveranstaltung.professorID "
				+ "  AND lehrveranstaltung.name = 'Verteilte Systeme'";

		ITable actualJoinData = getConnection().createQueryTable("RESULT",
				query);
		Assert.assertEquals(1, actualJoinData.getRowCount());
		Assert.assertEquals("Oliver Haase", actualJoinData.getValue(0, "name").toString());
	}

	@Test
	public void testProfMitMeistenLehrveranstaltungen() throws Exception {
		final String query = "SELECT professor.name, if(temp.Count >= 1,temp.Count,0) as lvCnt "
				+ "FROM professor "
				+ "LEFT JOIN "
				+ "(SELECT professor.id, COUNT(lehrveranstaltung.id) AS Count "
				+ "FROM professor "
				+ "LEFT JOIN lehrveranstaltung ON lehrveranstaltung.professorID = professor.id "
				+ "GROUP BY professor.id) temp ON temp.id = professor.id "
				+ "ORDER BY lvCnt DESC LIMIT 1; ";
		ITable actualJoinData = getConnection().createQueryTable("RESULT",
				query);
		Assert.assertEquals(1, actualJoinData.getRowCount());
		Assert.assertEquals("Oliver Haase", actualJoinData.getValue(0, "name").toString());
	}

	@Test
	public void testProfMitWenigstenLehrveranstaltungen() throws Exception {
		final String query = "SELECT professor.name, if(temp.Count >= 1,temp.Count,0) as lvCnt "
				+ "FROM professor "
				+ "LEFT JOIN "
				+ "(SELECT professor.id, COUNT(lehrveranstaltung.id) AS Count "
				+ "FROM professor "
				+ "LEFT JOIN lehrveranstaltung ON lehrveranstaltung.professorID = professor.id "
				+ "GROUP BY professor.id) temp ON temp.id = professor.id "
				+ "ORDER BY lvCnt ASC LIMIT 1; ";

		ITable actualJoinData = getConnection().createQueryTable("RESULT",
				query);
		Assert.assertEquals(1, actualJoinData.getRowCount());
		Assert.assertEquals("J�rgen W�sch", actualJoinData.getValue(0, "name").toString());
	}

	@Test
	public void testWelcherProfHatDieMeistenStudenten() throws Exception {
		final String query = "select professor.name from professor, ("
				+ "select s.professorid, SUM(s.studCount) as studenten "
				+ "from (select lehrveranstaltung.professorid, ("
				+ "select count(*) from besucht "
				+ "where besucht.lehrveranstaltungid = lehrveranstaltung.id) as studCount "
				+ "from lehrveranstaltung) s group by s.professorid "
				+ "order by studenten desc limit 1) t "
				+ "where t.professorid = professor.id";
		ITable actualJoinData = getConnection().createQueryTable("RESULT",
				query);
		Assert.assertEquals(1, actualJoinData.getRowCount());
		Assert.assertEquals("Oliver Haase", actualJoinData.getValue(0, "name").toString());
	}

	@Test
	public void testWelcherStudentHatDieMeistenPruefungen() throws Exception {
		final String query = "select *, ("
				+ "select count(*) from schreibt "
				+ "where schreibt.studentid = student.matrikelnummer) as total "
				+ "from student order by total desc limit 1";
		ITable actualJoinData = getConnection().createQueryTable("RESULT",
				query);
		Assert.assertEquals(1, actualJoinData.getRowCount());
		Assert.assertEquals("Nikolaus Moll", actualJoinData.getValue(0, "name").toString());
	}

	@Test
	public void testWelcherStudentIstTutorUndSchreibtPruefung()
			throws Exception {
		final String query = "select student.name from student, besucht, schreibt, ("
				+ "select pruefung.id as pruefungid, lehrveranstaltungid "
				+ "from lehrveranstaltung, pruefung "
				+ "where pruefung.lehrveranstaltungid = lehrveranstaltung.id) t "
				+ "where besucht.studentid = schreibt.studentid "
				+ "and besucht.lehrveranstaltungid = t.lehrveranstaltungid "
				+ "and schreibt.pruefungid = t.pruefungid "
				+ "and student.matrikelnummer = schreibt.studentid";
		ITable actualJoinData = getConnection().createQueryTable("RESULT",
				query);
		Assert.assertEquals(1, actualJoinData.getRowCount());
		Assert.assertEquals("Nikolaus Moll", actualJoinData.getValue(0, "name").toString());
	}

}