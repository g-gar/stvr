package com.ggar.stvr.packages.streamlink.executor;

/**
 * Result of executing an external command.
 *
 * @param exitCode process exit code (0 indicates success)
 * @param stdout standard output captured from process
 * @param stderr standard error captured from process
 */
public record CommandResult(int exitCode, String stdout, String stderr) {

    public boolean isSuccess() {
        return exitCode == 0;
    }
}
