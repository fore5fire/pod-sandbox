package dev.podsandbox.executor;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/** Runs a Python snippet in the pod's workspace folder. */
public final class PythonRunner {

  private static final Path WORKSPACE = Path.of("/workspace");

  public Map<String, Object> run(String code) throws Exception {
    Files.writeString(WORKSPACE.resolve("main.py"), code);
    var process = new ProcessBuilder("sh", "-c", "cd " + WORKSPACE + " && python3 main.py").start();
    var stdout = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    var stderr = new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
    var exitCode = process.waitFor();
    return Map.of("stdout", stdout, "stderr", stderr, "exit_code", exitCode);
  }
}
