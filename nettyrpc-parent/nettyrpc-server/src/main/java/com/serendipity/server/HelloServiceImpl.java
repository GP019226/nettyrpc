package com.serendipity.server;

import com.serendipity.api.IHelloService;

public class HelloServiceImpl implements IHelloService{

	@Override
	public String sayHello(String name) {
		System.out.println("server response:Hello,"+ name);
		return "Hello,"+ name;
	}

}
