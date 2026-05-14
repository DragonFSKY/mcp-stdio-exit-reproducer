# StdioClientTransport does not propagate unexpected child process exit during initialization

**Bug description**

When a stdio MCP server process starts successfully and then exits before it can
reply to the `initialize` request, the client waits for the request timeout and
surfaces a timeout error. The child process exit and exit code are not surfaced
to the caller through the `initialize()` failure.

This is different from a `ProcessBuilder.start()` failure: the process is
created successfully, then exits shortly after startup.

This also seems distinct from #937, which is about bounded termination during
shutdown. The failure here happens during initialization, before the client gets
an initialize response.

This also appears to be the lower-level cause behind
spring-projects/spring-ai#5982, where Spring AI waits for its MCP client
initialization timeout instead of reporting the stdio process exit.

**Environment**

- Java 17+
- `io.modelcontextprotocol.sdk:mcp:2.0.0-M2`
- Stdio client transport

**Steps to reproduce**

I prepared a minimal reproducer:

https://github.com/DragonFSKY/mcp-stdio-exit-reproducer

The reproducer starts a Java child process through `StdioClientTransport`. The
child process starts successfully and then immediately exits with code `127`
before sending an MCP initialize response.

Run:

```bash
./mvnw test
```

The regression test is intentionally written for the expected behavior, so it
fails with the current SDK.

To print the observed behavior directly:

```bash
./mvnw -q -DskipTests exec:java
```

Observed output:

```text
Expected: fail quickly and include child process exit code 127
Actual elapsed millis: 826
Actual exception type: java.lang.RuntimeException
Actual exception message: Client failed to initialize by explicit API call
Stack trace contains TimeoutException: true
Error contains child exit code 127: false
```

**Expected behavior**

`McpClient.initialize()` should fail promptly when the stdio child process exits
unexpectedly during initialization. The failure should include enough context for
the caller to understand that the child process exited, ideally including the
command and exit code.

**Actual behavior**

`McpClient.initialize()` waits for the request timeout and reports a timeout. The
exit code is not available in the exception reported to the caller.

**Minimal Complete Reproducible example**

The reproducer above contains:

- a minimal child process that calls `System.exit(127)`
- a stdio MCP client using `StdioClientTransport`
- a failing JUnit test that demonstrates the timeout and missing exit code

From looking at the current implementation, `StdioClientTransport.connect()`
completes after `ProcessBuilder.start()` succeeds. If the process exits shortly
afterward, the pending initialize request remains pending until the request
timeout. The exit code is only observed later on close, not propagated to the
pending initialization request.

If this behavior and scope look valid, I would be happy to work on a focused PR.
