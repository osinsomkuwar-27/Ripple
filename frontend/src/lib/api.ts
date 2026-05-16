export interface AnalyzeRequest {
  changedFile: string;
  changeDescription: string;
}

export async function analyzeRepo(req: AnalyzeRequest) {
  const res = await fetch("http://localhost:8080/api/analyze", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(req),
  });
  if (!res.ok) throw new Error("Analysis failed");
  return res.json();
}