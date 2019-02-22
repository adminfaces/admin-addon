package com.github.admin.addon.model;

import javax.persistence.Embeddable;
import java.io.Serializable;
import javax.persistence.Column;

@Embeddable
public class Address implements Serializable {

	@Column(length = 50, nullable = false)
	private String street;

	@Column(length = 50, nullable = false)
	private String city;

	@Column(length = 10, nullable = false)
	private Integer zipcode;

	@Column
	private String state;

	public String getStreet() {
		return street;
	}

	public void setStreet(String street) {
		this.street = street;
	}

	public String getCity() {
		return city;
	}

	public void setCity(String city) {
		this.city = city;
	}

	public Integer getZipcode() {
		return zipcode;
	}

	public void setZipcode(Integer zipcode) {
		this.zipcode = zipcode;
	}

	public String getState() {
		return state;
	}

	public void setState(String state) {
		this.state = state;
	}

	@Override
	public String toString() {
		String result = getClass().getSimpleName() + " ";
		if (street != null && !street.trim().isEmpty())
			result += "street: " + street;
		if (city != null && !city.trim().isEmpty())
			result += ", city: " + city;
		if (zipcode != null)
			result += ", zipcode: " + zipcode;
		if (state != null && !state.trim().isEmpty())
			result += ", state: " + state;
		return result;
	}
}