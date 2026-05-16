export interface AnalyzeRequest {
  changedFile: string;
  changeDescription: string;
}

export async function analyzeRepo(req: AnalyzeRequest) {
  const res = await fetch("http://localhost:8081/api/analyze", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(req),
  });
  if (!res.ok) {
    const text = await res.text().catch(() => "");
    throw new Error(`Analysis failed (${res.status})${text ? `: ${text}` : ""}`);
  }
  return res.json();
}

export async function checkHealth(): Promise<boolean> {
  try {
    const res = await fetch("http://localhost:8081/api/health");
    return res.ok;
  } catch {
    return false;
  }
}