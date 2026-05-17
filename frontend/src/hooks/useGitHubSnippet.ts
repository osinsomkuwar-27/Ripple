import { useEffect, useState } from "react";

interface LineSnippet {
  line: number;
  snippet: string;
  githubUrl: string;
}

interface UseGitHubSnippetResult {
  snippets: Record<number, LineSnippet>;
  loading: boolean;
  error: string | null;
}

/** Convert a GitHub repo URL to raw content base URL */
function toRawBase(repoUrl: string, branch = "main"): string | null {
  const match = repoUrl
    .replace(/\.git$/, "")
    .replace(/\/$/, "")
    .match(/github\.com\/([^/]+\/[^/]+)/);
  if (!match) return null;
  return `https://raw.githubusercontent.com/${match[1]}/${branch}`;
}

/** Convert a GitHub repo URL to a GitHub file URL */
function toGitHubFileUrl(repoUrl: string, filePath: string, line: number, branch = "main"): string {
  const match = repoUrl
    .replace(/\.git$/, "")
    .replace(/\/$/, "")
    .match(/github\.com\/(.*)/);
  if (!match) return repoUrl;
  return `https://github.com/${match[1]}/blob/${branch}/${filePath}#L${line}`;
}

/**
 * Fetches a file from GitHub raw and returns snippets for given line numbers.
 * Each snippet includes 2 lines of context above and below the target line.
 */
export function useGitHubSnippet(
  repoUrl: string,
  filePath: string | null,
  lineNumbers: number[],
  branch = "main",
): UseGitHubSnippetResult {
  const [snippets, setSnippets] = useState<Record<number, LineSnippet>>({});
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!filePath || !repoUrl || lineNumbers.length === 0) {
      setSnippets({});
      return;
    }

    // Only fetch .java files — HTML/SQL snippets aren't useful
    if (!filePath.endsWith(".java")) {
      setSnippets({});
      return;
    }

    const rawBase = toRawBase(repoUrl, branch);
    if (!rawBase) {
      setError("Invalid GitHub URL");
      return;
    }

    const url = `${rawBase}/${filePath}`;
    let cancelled = false;

    async function fetchFile() {
      setLoading(true);
      setError(null);
      try {
        const res = await fetch(url);
        if (!res.ok) {
          // Try 'master' branch as fallback
          if (branch === "main") {
            const fallbackUrl = url.replace("/main/", "/master/");
            const fallbackRes = await fetch(fallbackUrl);
            if (!fallbackRes.ok) throw new Error(`${res.status}`);
            const text = await fallbackRes.text();
            if (!cancelled) processLines(text);
            return;
          }
          throw new Error(`${res.status}`);
        }
        const text = await res.text();
        if (!cancelled) processLines(text);
      } catch (e) {
        if (!cancelled) setError("Could not fetch file");
      } finally {
        if (!cancelled) setLoading(false);
      }
    }

    function processLines(text: string) {
      const allLines = text.split("\n");
      const result: Record<number, LineSnippet> = {};

      for (const lineNum of lineNumbers) {
        // Extract ±2 lines of context
        const start = Math.max(0, lineNum - 3);
        const end = Math.min(allLines.length - 1, lineNum + 1);
        const contextLines = allLines
          .slice(start, end + 1)
          .map((l, i) => {
            const ln = start + i + 1;
            const marker = ln === lineNum ? "→ " : "  ";
            return `${marker}${ln}: ${l}`;
          })
          .join("\n");

        result[lineNum] = {
          line: lineNum,
          snippet: contextLines,
          githubUrl: toGitHubFileUrl(repoUrl, filePath!, lineNum, branch),
        };
      }

      setSnippets(result);
    }

    fetchFile();
    return () => { cancelled = true; };
  }, [repoUrl, filePath, lineNumbers.join(","), branch]);

  return { snippets, loading, error };
}