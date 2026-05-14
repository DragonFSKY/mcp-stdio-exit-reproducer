package com.example.mcpstdioexit;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.ServerParameters;
import io.modelcontextprotocol.client.transport.StdioClientTransport;
import io.modelcontextprotocol.json.McpJsonDefaults;

public final class ObservedBehaviorDemo {

	private static final Duration REQUEST_TIMEOUT = Duration.ofMillis(800);

	private ObservedBehaviorDemo() {
	}

	public static void main(String[] args) {
		FailureSnapshot snapshot = runOnce();

		System.out.println("MCP Java SDK version: 2.0.0-M2");
		System.out.println("Child process command: " + snapshot.command());
		System.out.println("Expected: fail quickly and include child process exit code 127");
		System.out.println("Actual elapsed millis: " + snapshot.elapsedMillis());
		System.out.println("Actual exception type: " + snapshot.exceptionType());
		System.out.println("Actual exception message: " + snapshot.exceptionMessage());
		System.out.println("Stack trace contains TimeoutException: " + snapshot.stackTrace().contains("TimeoutException"));
		System.out.println("Stack trace contains exit code 127: " + snapshot.stackTrace().contains("127"));
	}

	public static FailureSnapshot runOnce() {
		String javaCommand = Path.of(System.getProperty("java.home"), "bin", javaExecutableName()).toString();
		String classpath = System.getProperty("java.class.path");
		ServerParameters stdioParams = ServerParameters.builder(javaCommand)
			.args("-cp", classpath, ExitImmediatelyServer.class.getName())
			.build();

		StdioClientTransport transport = new StdioClientTransport(stdioParams, McpJsonDefaults.getMapper());
		McpSyncClient client = McpClient.sync(transport)
			.requestTimeout(REQUEST_TIMEOUT)
			.initializationTimeout(Duration.ofSeconds(3))
			.build();

		long startNanos = System.nanoTime();
		try {
			client.initialize();
			throw new AssertionError("Expected MCP initialization to fail");
		}
		catch (Throwable failure) {
			long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);
			return new FailureSnapshot(javaCommand, elapsedMillis, failure.getClass().getName(), failure.getMessage(),
					stackTraceOf(failure));
		}
		finally {
			try {
				client.closeGracefully();
			}
			catch (Throwable ignored) {
				// The reproducer only cares about the initialization failure.
			}
		}
	}

	private static String javaExecutableName() {
		return System.getProperty("os.name").toLowerCase().contains("win") ? "java.exe" : "java";
	}

	private static String stackTraceOf(Throwable failure) {
		StringWriter writer = new StringWriter();
		failure.printStackTrace(new PrintWriter(writer));
		return writer.toString();
	}

	public record FailureSnapshot(String command, long elapsedMillis, String exceptionType, String exceptionMessage,
			String stackTrace) {
	}

}

