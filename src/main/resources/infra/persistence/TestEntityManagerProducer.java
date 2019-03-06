package com.github.adminfaces.persistence.infra;


import com.github.database.rider.core.util.EntityManagerProvider;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.Specializes;
import javax.persistence.EntityManager;


@ApplicationScoped
@Specializes
public class TestEntityManagerProducer extends EntityManagerProducer {

    @Produces
    public EntityManager produce() {
        return  EntityManagerProvider.instance("testDB").em();
    }
}
