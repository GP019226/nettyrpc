package com.serendipity.client;

import com.serendipity.api.IHelloService;
import com.serendipity.client.proxy.RpcProxy;

public class RpcClient {
	
	public static void main(String[] args) {
		IHelloService helloService = RpcProxy.create(IHelloService.class);

		System.out.println(helloService.sayHello("everyone"));
	}
}
