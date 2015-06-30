package com.samsung.microbit.model;

import java.io.Serializable;

/**
 * Created by balbirs on 30/06/15.
 */
public class NameValuePair {
	private String name;
	private Serializable value;

	public NameValuePair(String name, Serializable value) {
		this.name = name;
		this.value = value;
	}


	public String getName() {
		return name;
	}

	public Serializable getValue() {
		return value;
	}
}
