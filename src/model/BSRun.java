package model;

import java.util.Date;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.annotations.IndexColumn;

import common.Util;

@Entity
public class BSRun {
	public static enum STATUS {QUEUED, RUNNING, COMPLETE, ERROR, CANCELED};
	
	private Long id;
	private BSPlayer teamA;
	private BSPlayer teamB;
	private STATUS status;
	private Date started;
	private Date ended;
	private List<BSMatch> matches;

	@Id
	@GeneratedValue(strategy=GenerationType.SEQUENCE)
	public Long getId() {
		return id;
	}
	
	@Temporal(TemporalType.TIMESTAMP)
	public Date getStarted() {
		return started;
	}
	
	@Temporal(TemporalType.TIMESTAMP)
	public Date getEnded() {
		return ended;
	}

	@ManyToOne
	public BSPlayer getTeamA() {
		return teamA;
	}

	@ManyToOne
	public BSPlayer getTeamB() {
		return teamB;
	}

	@Enumerated(EnumType.STRING)
	public STATUS getStatus() {
		return status;
	}
	
	@OneToMany(fetch=FetchType.LAZY, orphanRemoval=true)
	@IndexColumn(name="id")
	@JoinColumn(name="run")
	public List<BSMatch> getMatches() {
		return matches;
	}

	public void setTeamA(BSPlayer teamA) {
		this.teamA = teamA;
	}

	public void setTeamB(BSPlayer teamB) {
		this.teamB = teamB;
	}

	public void setStatus(STATUS status) {
		this.status = status;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public void setStarted(Date started) {
		this.started = started;
	}

	public void setEnded(Date ended) {
		this.ended = ended;
	}
	
	public void setMatches(List<BSMatch> matches) {
		this.matches = matches;
	}
	
	public String printTimeTaken() {
		return Util.formatTime(calculateTimeTaken());
	}
	
	public long calculateTimeTaken() {
		Date end = (ended == null ? new Date() : ended);
		return (end.getTime() - started.getTime());
	}
}