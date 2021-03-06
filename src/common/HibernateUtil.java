package common;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import model.BSMap;
import model.BSMatch;
import model.BSMetadata;
import model.BSPlayer;
import model.BSRun;
import model.BSScrimmageSet;
import model.BSUser;
import model.MatchResultImpl;
import model.ScrimmageMatchResult;
import model.TeamMatchResult;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.ejb.Ejb3Configuration;
import org.hibernate.tool.hbm2ddl.SchemaUpdate;


public class HibernateUtil {
	private static final String[][] properties = {
		{"hibernate.id.new_generator_mappings", "true"},
		{"hibernate.dialect", "org.hibernate.dialect.HSQLDialect"},
		{"hibernate.connection.driver_class", "org.hsqldb.jdbcDriver"},
		{"hibernate.connection.username", "sa"},
		{"hibernate.connection.password", ""},
		{"hibernate.connection.url", "jdbc:hsqldb:file:BSTesterDB"},
		{"hibernate.hbm2ddl.auto", "validate"},
		{"hibernate.search.autoregister_listeners", "false"},
	};
	private static final Configuration config = buildConfiguration();
	private static final SessionFactory sessionFactory = buildSessionFactory();
	private static final EntityManagerFactory entityManagerFactory = buildEntityManagerFactory();

	private static Configuration buildConfiguration() {
		try {
			Configuration cfg = new Configuration();
			cfg.addAnnotatedClass(BSRun.class);
			cfg.addAnnotatedClass(BSMap.class);
			cfg.addAnnotatedClass(BSUser.class);
			cfg.addAnnotatedClass(BSMatch.class);
			cfg.addAnnotatedClass(BSPlayer.class);
			cfg.addAnnotatedClass(MatchResultImpl.class);
			cfg.addAnnotatedClass(ScrimmageMatchResult.class);
			cfg.addAnnotatedClass(TeamMatchResult.class);
			cfg.addAnnotatedClass(BSScrimmageSet.class);
			cfg.addAnnotatedClass(BSMetadata.class);
			for (String[] keyVal: properties) {
				cfg.setProperty(keyVal[0], keyVal[1]);
			}
			if (Config.DEBUG) {
				cfg.setProperty("hibernate.show_sql", Config.SHOW_SQL ? "true" : "false");
			}
			return cfg;
		}
		catch (Throwable ex) {
			// Make sure you log the exception, as it might be swallowed
			System.err.println("SEVERE: Initial Hibernate configuration creation failed." + ex);
			ex.printStackTrace();
			throw new ExceptionInInitializerError(ex);
		}
	}

	private static SessionFactory buildSessionFactory() {
		try {
			SchemaUpdate su = new SchemaUpdate(config);
			su.execute(Config.DEBUG, true);
			SessionFactory sf = config.buildSessionFactory();
			return sf;
		} catch (Throwable ex) {
			System.err.println("SEVERE: SessionFactory creation failed: " + ex);
			ex.printStackTrace();
			throw new ExceptionInInitializerError(ex);
		}
	}

	private static EntityManagerFactory buildEntityManagerFactory() {
		Ejb3Configuration cfg = new Ejb3Configuration();
		cfg.addAnnotatedClass(BSRun.class);
		cfg.addAnnotatedClass(BSMap.class);
		cfg.addAnnotatedClass(BSUser.class);
		cfg.addAnnotatedClass(BSMatch.class);
		cfg.addAnnotatedClass(BSPlayer.class);
		cfg.addAnnotatedClass(MatchResultImpl.class);
		cfg.addAnnotatedClass(ScrimmageMatchResult.class);
		cfg.addAnnotatedClass(TeamMatchResult.class);
		cfg.addAnnotatedClass(BSScrimmageSet.class);
		cfg.addAnnotatedClass(BSMetadata.class);
		for (String[] keyVal: properties) {
			cfg.setProperty(keyVal[0], keyVal[1]);
		}
		if (Config.DEBUG) {
			cfg.setProperty("hibernate.show_sql", Config.SHOW_SQL ? "true" : "false");
		}
		return cfg.buildEntityManagerFactory();
	}

	public static SessionFactory getSessionFactory() {
		return sessionFactory;
	}

	public static EntityManager getEntityManager() {
		return entityManagerFactory.createEntityManager();
	}

}
