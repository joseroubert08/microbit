package com.samsung.microbit.data.model;

import java.io.Serializable;

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
