package com.example.mcpstdioexit;

public final class ExitImmediatelyServer {

	private ExitImmediatelyServer() {
	}

	public static void main(String[] args) {
		System.err.println("Exiting before MCP initialization with code 127");
		System.exit(127);
	}

}

