package com.example.mcpstdioexit;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StdioProcessExitReproducerTest {

	@Test
	void shouldSurfaceChildProcessExitInsteadOfTimingOut() {
		ObservedBehaviorDemo.FailureSnapshot snapshot = ObservedBehaviorDemo.runOnce();

		assertAll("stdio process exit should be visible to the caller",
				() -> assertFalse(snapshot.stackTrace().contains("TimeoutException"),
						"Expected the SDK to fail because the child process exited, but it failed by timeout.\n"
								+ describe(snapshot)),
				() -> assertTrue(snapshot.stackTrace().contains("127"),
						"Expected the SDK error to include the child process exit code 127.\n" + describe(snapshot)));
	}

	private static String describe(ObservedBehaviorDemo.FailureSnapshot snapshot) {
		return """
				command=%s
				elapsedMillis=%d
				exceptionType=%s
				exceptionMessage=%s
				""".formatted(snapshot.command(), snapshot.elapsedMillis(), snapshot.exceptionType(),
				snapshot.exceptionMessage());
	}

}

