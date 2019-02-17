package com.github.admin.addon.model;

import javax.persistence.Id;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Column;
import javax.persistence.Entity;
import java.io.Serializable;
import javax.persistence.Version;
import java.util.Calendar;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import com.github.admin.addon.model.Speaker;
import javax.persistence.ManyToOne;
import com.github.adminfaces.persistence.model.PersistenceEntity;
import java.util.Date;

@Entity
public class Talk implements Serializable, PersistenceEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "id", updatable = false, nullable = false)
	private Long id;
	private static final long serialVersionUID = 1L;
	@Version
	@Column(name = "version")
	private int version;

	@Column
	@NotNull
	private String title;

	@Column(length = 2000)
	@Size(max = 2000)
	private String description;

	@Column
	@NotNull
	private String room;

	@Column
	@Temporal(TemporalType.DATE)
	private Date date;

	@ManyToOne
	private Speaker speaker;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof Talk)) {
			return false;
		}
		Talk other = (Talk) obj;
		if (id != null) {
			if (!id.equals(other.id)) {
				return false;
			}
		}
		return true;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		return result;
	}

	public int getVersion() {
		return version;
	}

	public void setVersion(int version) {
		this.version = version;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getRoom() {
		return room;
	}

	public void setRoom(String room) {
		this.room = room;
	}

	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
	}

	@Override
	public String toString() {
		String result = getClass().getSimpleName() + " ";
		if (title != null && !title.trim().isEmpty())
			result += "title: " + title;
		if (description != null && !description.trim().isEmpty())
			result += ", description: " + description;
		if (room != null && !room.trim().isEmpty())
			result += ", room: " + room;
		return result;
	}

	public Speaker getSpeaker() {
		return this.speaker;
	}

	public void setSpeaker(final Speaker speaker) {
		this.speaker = speaker;
	}
}