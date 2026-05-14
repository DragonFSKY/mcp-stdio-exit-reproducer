# MCP Java SDK stdio child process exit reproducer

This repository is a minimal reproducer for a stdio transport initialization
failure in the MCP Java SDK.

## Problem

When a stdio MCP server process starts successfully and then exits before it can
reply to the `initialize` request, the SDK waits for the request timeout and
surfaces a timeout error. The child process exit and exit code are not surfaced
to the caller.

This is different from a `ProcessBuilder.start()` failure: the child process is
created successfully, then exits shortly after startup.

## Environment

- Java 17+
- Maven
- `io.modelcontextprotocol.sdk:mcp:2.0.0-M2`

## Reproduce

Run the failing regression test:

```bash
./mvnw test
```

The test expects the SDK to fail because the child process exited with code
`127`. With the current SDK version, it fails because the caller observes a
timeout instead.

To print the observed behavior without relying on test output:

```bash
./mvnw -q -DskipTests exec:java
```

## Expected behavior

`McpClient.initialize()` should fail promptly when the stdio child process exits
unexpectedly during initialization, and the error should include the child
process exit code.

## Actual behavior

`McpClient.initialize()` waits for the request timeout and reports a timeout.
The exit code is not available in the exception reported to the caller.

Example observed output:

```text
Expected: fail quickly and include child process exit code 127
Actual elapsed millis: 826
Actual exception type: java.lang.RuntimeException
Actual exception message: Client failed to initialize by explicit API call
Stack trace contains TimeoutException: true
Stack trace contains exit code 127: false
```

The SDK logs the child process stderr and eventually observes the exit code
during graceful close, but the exception returned from `initialize()` still
contains only the timeout failure.

## Why this matters

Callers cannot distinguish a slow MCP server from a server process that has
already exited. In integrations such as Spring AI, this can hide the real
startup failure and make users wait for the initialization timeout instead of
seeing the actionable child process exit.
