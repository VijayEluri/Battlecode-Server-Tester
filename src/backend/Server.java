package backend;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

import networking.CometCmd;
import networking.CometMessage;
import networking.Packet;

import common.BattlecodeMap;
import common.Config;
import common.Match;

import db.Database;

/**
 * Handles distribution of load to connected Clients
 * @author stevearc
 *
 */
public class Server {
	private Config config;
	private Database db;
	private Logger _log;
	private NetworkHandler handler;
	private HashSet<ClientRepr> clients = new HashSet<ClientRepr>();
	private HashSet<BattlecodeMap> maps = new HashSet<BattlecodeMap>();
	private WebPollHandler wph;

	public Server() throws Exception {
		config = Config.getConfig();
		db = Config.getDB();
		wph = Config.getWebPollHandler();
		_log = config.getLogger();
		handler = new NetworkHandler();
	}

	/**
	 * Start running the server
	 */
	public synchronized void start() {
		try {
			new Thread(handler).start();
			startRun();
		} catch (SQLException e) {
		}
	}

	/**
	 * Add a run to the queue
	 * @param run
	 * @param seeds
	 * @param mapNames
	 */
	public synchronized void queueRun(String team_a, String team_b, String[] seeds, String[] mapNames) {
		try {
			PreparedStatement st = db.prepare("INSERT INTO runs (team_a, team_b) VALUES (?, ?)");
			st.setString(1, team_a);
			st.setString(2, team_b);
			db.update(st, true);
			ResultSet rs = db.query("SELECT MAX(id) as newest FROM runs"); 
			rs.next();
			int id = rs.getInt("newest");

			HashSet<BattlecodeMap> runMaps = getMapsByName(mapNames);
			for (String seed: seeds) {
				int seed_int = Integer.parseInt(seed);
				for (BattlecodeMap map: runMaps) {
					PreparedStatement stmt = db.prepare("INSERT INTO matches (run_id, map, height, width, rounds, points, seed)" +
							" VALUES (?, ?, ?, ?, ?, ?, ?)");
					stmt.setInt(1, id);
					stmt.setString(2, map.map);
					stmt.setInt(3, map.height);
					stmt.setInt(4, map.width);
					stmt.setInt(5, map.rounds);
					stmt.setInt(6, map.points);
					stmt.setInt(7, seed_int);
					db.update(stmt, true);
				}
			}
			wph.broadcastMsg("matches", new CometMessage(CometCmd.INSERT_TABLE_ROW, new String[] {""+id, 
					getTeamNameOrAlias(team_a), getTeamNameOrAlias(team_b)}));
			startRun();
		} catch (SQLException e) {
		}
	}
	
	private String getTeamNameOrAlias(String team) throws SQLException {
		PreparedStatement st = db.prepare("SELECT * FROM tags WHERE tag LIKE ?");
		st.setString(1, team);
		ResultSet r = db.query(st);
		r.next();
		String alias = r.getString("alias");
		if (alias != null)
			return alias;
		return team;
	}

	/**
	 * Delete run data
	 * @param runId
	 */
	public synchronized void deleteRun(int runId) {
		try {
			if (runId == getCurrentId()) {
				stopCurrentRun(Config.STATUS_CANCELED);
				startRun();
			} else {
				db.update("DELETE FROM matches WHERE run_id = " + runId, true);
				db.update("DELETE FROM runs WHERE id = " + runId, true);
				wph.broadcastMsg("matches", new CometMessage(CometCmd.DELETE_TABLE_ROW, new String[] {""+runId}));
			}
		} catch (SQLException e) {
		}
	}

	/**
	 * Process a client connecting
	 * @param client
	 */
	public synchronized void clientConnect(ClientRepr client) {
		_log.info("Client connected: " + client);
		clients.add(client);
		wph.broadcastMsg("connections", new CometMessage(CometCmd.INSERT_TABLE_ROW, new String[] {client.toHTML()}));
		sendClientMatches(client);
	}

	/**
	 * Process a client disconnecting
	 * @param client
	 */
	public synchronized void clientDisconnect(ClientRepr client) {
		_log.info("Client disconnected: " + client);
		wph.broadcastMsg("connections", new CometMessage(CometCmd.DELETE_TABLE_ROW, new String[] {client.toHTML()}));
		clients.remove(client);
	}

	/**
	 * Save data from a finished match
	 * @param client
	 * @param p
	 */
	public synchronized void matchFinished(ClientRepr client, Packet p) {
		Match m = (Match) p.get(0);
		String status = (String) p.get(1);
		int winner = (Integer) p.get(2);
		int win_condition = (Integer) p.get(3);
		int a_points = (Integer) p.get(4);
		int b_points = (Integer) p.get(5);
		byte[] data = (byte[]) p.get(6);
		wph.broadcastMsg("connections", new CometMessage(CometCmd.REMOVE_MAP, new String[] {client.toHTML(), m.toMapString()}));
		try {
			if ("ok".equals(status) && m.run_id == getCurrentId()) {
				if (!getMatchesLeft().contains(m)) {
					_log.info("Received duplicate match: " + m);
					sendClientMatches(client);
					return;
				}
				_log.info("Match finished: " + m + " winner: " + (winner == 1 ? "A" : "B"));
				PreparedStatement stmt = null;
				stmt = db.prepare("UPDATE matches SET win = ?, win_condition = ?, a_points = ?, b_points = ?, data = ? WHERE id = ?");
				stmt.setInt(1, winner);
				stmt.setInt(2, win_condition);
				stmt.setInt(3, a_points);
				stmt.setInt(4, b_points);
				stmt.setBinaryStream(5, new ByteArrayInputStream(data), data.length);
				stmt.setInt(6, m.id);
				db.update(stmt, true);
				wph.broadcastMsg("matches", new CometMessage(CometCmd.MATCH_FINISHED, new String[] {""+m.run_id, ""+winner}));

				// If finished, start next run
				if (getMatchesLeft().isEmpty()) {
					stopCurrentRun(Config.STATUS_COMPLETE);
					startRun();
				}

			} else {
				_log.warning("Match " + m + " on client " + client + " failed");
			}

			if (getMatchesLeft().isEmpty()) {
				stopCurrentRun(Config.STATUS_COMPLETE);
				startRun();
			} else {
				sendClientMatches(client);
			}
		} catch (SQLException e) {
		}
	}

	/**
	 * Send matches to a client until they are saturated
	 * @param client
	 */
	public synchronized void sendClientMatches(ClientRepr client) {
		try {
			HashSet<Match> matches = getMatchesLeftAndNotRunning();
			for (Match m: matches) {
				if (!client.isFree())
					break;
				_log.info("Sending match " + m + " to client " + client);
				wph.broadcastMsg("connections", new CometMessage(CometCmd.ADD_MAP, new String[] {client.toHTML(), m.toMapString()}));
				client.runMatch(m);
			}

			// If we are currently running all necessary maps, add some redundancy by
			// Sending this client random maps that other clients are currently running
			Match[] matchIndex = getMatchesLeft().toArray(new Match[0]);
			Random r = new Random();
			if (client.isFree()) {
				client.runMatch(matchIndex[r.nextInt(matchIndex.length)]);
			}
		} catch (SQLException e) {
		}
	}

	private void startRun() throws SQLException {
		updateMaps();
		if (getCurrentId() != -1)
			return;

		ResultSet rs = db.query("SELECT * FROM runs WHERE status = " + Config.STATUS_QUEUED + " ORDER BY id");
		if (!rs.next()) {
			return;
		}
		String team_a = rs.getString("team_a");
		String team_b = rs.getString("team_b");
		db.update("UPDATE runs SET status = " + Config.STATUS_RUNNING + ", started = NOW() WHERE id = " + rs.getInt("id"), true);
		if (!validateTeams(team_a, team_b)) {
			PreparedStatement stmt = db.prepare("UPDATE runs SET status = " + Config.STATUS_ERROR + " WHERE team_a LIKE ? AND team_b LIKE ?");
			stmt.setString(1, team_a);
			stmt.setString(2, team_b);
			db.update(stmt, true);
			wph.broadcastMsg("matches", new CometMessage(CometCmd.RUN_ERROR, new String[] {""+rs.getInt("id")}));
			startRun();
		} else {
			int runid = rs.getInt("id");
			ResultSet r = db.query("SELECT COUNT(*) AS num_matches FROM matches WHERE run_id = " + runid);
			r.next();
			wph.broadcastMsg("matches", new CometMessage(CometCmd.START_RUN, new String[] {""+runid, ""+r.getInt("num_matches")}));
			for (ClientRepr c: clients) {
				sendClientMatches(c);
			}
		}
	}

	private int getCurrentId() throws SQLException {
		ResultSet rs = db.query("SELECT id FROM runs WHERE status = " + Config.STATUS_RUNNING);
		if (rs.next()) {
			return rs.getInt("id");
		} else {
			return -1;
		}
	}

	private void stopCurrentRun(int status) throws SQLException {
		_log.info("Stopping current run");
		int id = getCurrentId();
		db.update("UPDATE runs SET status = " + status + ", ended = NOW() WHERE id = " + id, true);
		wph.broadcastMsg("matches", new CometMessage(CometCmd.FINISH_RUN, new String[] {""+id, ""+status}));
		wph.broadcastMsg("connections", new CometMessage(CometCmd.FINISH_RUN, new String[] {}));
		for (ClientRepr c: clients) {
			c.stopAllMatches();
		}
	}

	private HashSet<Match> getMatchesLeft() throws SQLException {
		ResultSet rs = db.query("SELECT id, team_a, team_b FROM runs WHERE status = " + Config.STATUS_RUNNING);
		rs.next();
		int run = rs.getInt("id");
		String team_a = rs.getString("team_a");
		String team_b = rs.getString("team_b");
		rs.close();
		PreparedStatement st = db.prepare("SELECT * FROM matches WHERE run_id = ? AND win is NULL");
		st.setInt(1, run);
		rs = db.query(st);
		HashSet<Match> unfinishedMatches = new HashSet<Match>();
		while (rs.next()) {
			unfinishedMatches.add(new Match(run, rs.getInt("id"), team_a, team_b, new BattlecodeMap(rs.getString("map"), 0, 0, 0, 0), rs.getInt("seed")));
		}
		return unfinishedMatches;
	}

	private HashSet<Match> getMatchesLeftAndNotRunning() throws SQLException {
		HashSet<Match> matchesLeft = getMatchesLeft();
		for (ClientRepr c: clients) {
			for (Match m: c.getRunningMatches()) {
				matchesLeft.remove(m);
			}
		}
		return matchesLeft;
	}

	private boolean validateTeams(String team_a, String team_b) throws SQLException {
		updateRepo();

		PreparedStatement st = db.prepare("SELECT * FROM tags WHERE tag LIKE ? OR alias LIKE ?");
		st.setString(1, team_a);
		st.setString(2, team_a);
		ResultSet rs = db.query(st);

		if (!rs.next()) {
			st.close();
			return false;
		}
		st.close();
		PreparedStatement stmt = db.prepare("SELECT * FROM tags WHERE tag LIKE ? OR alias LIKE ?");
		stmt.setString(1, team_b);
		stmt.setString(2, team_b);
		rs = db.query(stmt);
		if (!rs.next()) {
			stmt.close();
			return false;
		}
		stmt.close();
		return true;
	}

	/**
	 * Update the repository
	 * @throws SQLException
	 */
	public synchronized void updateRepo() throws SQLException {
		try {
			Process p = Runtime.getRuntime().exec(new String[] {config.cmd_update, "server"});

			p.waitFor();
			BufferedReader read = new BufferedReader(new InputStreamReader(p.getInputStream()));
			for (String line = read.readLine(); line != null; line = read.readLine()) {
				PreparedStatement st = db.prepare("INSERT INTO tags (tag) VALUES (?)");
				st.setString(1, line);
				db.update(st, false);
			}
			wph.broadcastMsg("teams_update", new CometMessage(CometCmd.RELOAD, new String[] {}));
		} catch (InterruptedException e){
			_log.log(Level.WARNING, "Error updating repo", e);
		} catch (IOException e) {
			_log.log(Level.WARNING, "Error updating repo", e);
		}
	}

	/**
	 * Update the list of available maps
	 */
	public synchronized void updateMaps() {
		File file = new File(config.repo + "/maps");
		File[] mapFiles = file.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return name.toLowerCase().endsWith(".xml");
			}
		});
		maps.clear();
		for (File m: mapFiles) {
			try {
				maps.add(new BattlecodeMap(m));
			} catch (Exception e) {
				_log.log(Level.WARNING, "Error parsing map", e);
			}
		}
	}

	/**
	 * 
	 * @return Currently connected clients
	 */
	public HashSet<ClientRepr> getConnections() {
		return clients;
	}

	/**
	 * 
	 * @return All known maps
	 */
	@SuppressWarnings("unchecked")
	public HashSet<BattlecodeMap> getMaps() {
		return (HashSet<BattlecodeMap>) maps.clone();
	}

	private HashSet<BattlecodeMap> getMapsByName(String[] mapNames) {
		HashSet<BattlecodeMap> result = new HashSet<BattlecodeMap>();
		HashSet<String> mapNameSet = new HashSet<String>();
		for (String m: mapNames)
			mapNameSet.add(m);
		for (BattlecodeMap m: maps)
			if (mapNameSet.contains(m.map))
				result.add(m);
		return result;
	}

}