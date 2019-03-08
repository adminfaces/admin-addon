package com.github.admin.addon.infra;

import java.util.Date;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.inject.Inject;

import com.github.adminfaces.addon.model.Address;
import com.github.admin.addon.model.Room;
import com.github.adminfaces.addon.model.Speaker;
import com.github.adminfaces.addon.model.Talk;
import com.github.adminfaces.persistence.service.CrudService;
import com.github.adminfaces.persistence.service.Service;

@Singleton
@Startup
public class InitDB {

	@Inject
	@Service
	private CrudService<Room, Long> roomService;
	private Set<Room> rooms;

	@Inject
	@Service
	private CrudService<Speaker, Long> speakerService;
	private Set<Speaker> speakers;

	@Inject
	@Service
	private CrudService<Talk, Long> talkService;
	private Set<Talk> talks;

	@PostConstruct
	public void init() {
		Room r1 = new Room();
		r1.setCapacity((short) 1);
		r1.setName("room1");
		r1.setHasWifi(true);
		roomService.insert(r1);

		Address address = new Address();
		address.setCity("Porto Alegre");
		address.setState("Rio Grande do sul");
		address.setStreet("Av. Assis brasil 42");
		address.setZipcode(1);
		Speaker s1 = new Speaker();
		s1.setAddress(address);
		s1.setFirstname("Rafael");
		s1.setSurname("Pestano");
		s1.setTwitter("@realpestano");
		speakerService.insert(s1);

		Speaker s2 = new Speaker();
		s2.setAddress(address);
		s2.setFirstname("Speaker2");
		s2.setSurname("BSDAH");
		s2.setTwitter("@abcd");
		speakerService.insert(s2);

		Talk t1 = new Talk();
		t1.setDate(new Date());
		t1.setRoom(r1);
		t1.setTitle("Enterprise Java testing with Arquillian");
		t1.setSpeaker(s1);
		talkService.insert(t1);
		Talk t2 = new Talk();
		t2.setDate(new Date());
		t2.setRoom(r1);
		t2.setTitle("Pragmatic Java Web Development with AdminFaces");
		t2.setSpeaker(s1);
		talkService.insert(t2);
	}
}
