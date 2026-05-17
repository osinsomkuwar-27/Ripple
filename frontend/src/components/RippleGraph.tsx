import { useMemo, useState } from "react";
import { riskColor, type ImpactFile, type GraphEdge } from "@/lib/mockData";

interface Props {
  selectedId: string | null;
  onSelect: (id: string) => void;
  filter: "all" | "high" | "med" | "low";
  preview?: boolean;
  seed?: number;
  files: ImpactFile[];
}

interface Pos {
  x: number;
  y: number;
}

const W = 1100;
const H = 680;
const PADDING = 32;

function mulberry32(seed: number) {
  return () => {
    let t = (seed += 0x6d2b79f5);
    t = Math.imul(t ^ (t >>> 15), t | 1);
    t ^= t + Math.imul(t ^ (t >>> 7), t | 61);
    return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
  };
}

function layout(files: ImpactFile[], seed = 1): Record<string, Pos> {
  const innerW = W - PADDING * 2;
  const innerH = H - PADDING * 2;
  const cx = PADDING + innerW / 2;
  const cy = PADDING + innerH / 2;
  const positions: Record<string, Pos> = {};
  const rand = mulberry32(seed);

  const byDepth = new Map<number, ImpactFile[]>();
  let maxDepth = 0;
  for (const f of files) {
    const d = Math.min(3, Math.max(0, f.cascade.length - 1));
    maxDepth = Math.max(maxDepth, d);
    if (!byDepth.has(d)) byDepth.set(d, []);
    byDepth.get(d)!.push(f);
  }

  const maxRing = Math.min(innerW, innerH) / 2 - 28;
  const desiredStep = 110;
  const ringStep = Math.min(desiredStep, maxRing / Math.max(1, maxDepth + 1));

  for (const [depth, group] of byDepth) {
    if (depth === 0) {
      positions[group[0].id] = { x: cx, y: cy };
      continue;
    }
    const r = ringStep * (depth + 1);
    const step = (Math.PI * 2) / group.length;
    const offset = depth % 2 === 0 ? 0 : step / 2;
    group.forEach((f, i) => {
      const a = i * step + offset - Math.PI / 2;
      const jitter = (rand() - 0.5) * 24;
      positions[f.id] = {
        x: cx + Math.cos(a) * r + jitter,
        y: cy + Math.sin(a) * r + jitter,
      };
    });
  }
  return positions;
}

/** Derive edges from cascade chains; fallback to star from origin */
function deriveEdges(files: ImpactFile[]): GraphEdge[] {
  const idByShortName = new Map<string, string>();
  for (const f of files) {
    idByShortName.set(f.shortName, f.id);
    if (f.cascade.length > 0) {
      idByShortName.set(f.cascade[f.cascade.length - 1], f.id);
    }
  }

  const edges: GraphEdge[] = [];
  const seen = new Set<string>();

  for (const f of files) {
    if (f.cascade.length < 2) continue;
    for (let i = 0; i < f.cascade.length - 1; i++) {
      const srcId = idByShortName.get(f.cascade[i]);
      const tgtId = idByShortName.get(f.cascade[i + 1]);
      if (!srcId || !tgtId || srcId === tgtId) continue;
      const key = `${srcId}→${tgtId}`;
      if (seen.has(key)) continue;
      seen.add(key);
      edges.push({ source: srcId, target: tgtId });
    }
  }

  if (edges.length === 0) {
    const origin = files.find((f) => f.risk === "origin");
    if (origin) {
      for (const f of files) {
        if (f.id !== origin.id) edges.push({ source: origin.id, target: f.id });
      }
    }
  }

  return edges;
}

function radius(f: ImpactFile) {
  if (f.risk === "origin") return 18;
  return 10 + Math.min(14, f.dependents * 1.6);
}

/** Smart label truncation */
function smartLabel(shortName: string): string {
  // Strip extensions
  let label = shortName.replace(/\.(java|html)$/, "");
  
  // Shorten common suffixes
  label = label.replace(/Controller$/, "Ctrl");
  label = label.replace(/Repository$/, "Repo");
  
  // For .properties files, extract just the locale part
  if (shortName.includes("messages_") && shortName.endsWith(".properties")) {
    const match = shortName.match(/messages_([a-z]{2})/);
    if (match) return `messages_${match[1]}`;
  }
  
  return label;
}

export function RippleGraph({ selectedId, onSelect, filter, preview, seed = 1, files }: Props) {
  const [hoveredId, setHoveredId] = useState<string | null>(null);
  const positions = useMemo(() => layout(files, seed), [files, seed]);
  const edges = useMemo(() => deriveEdges(files), [files]);

  const visibleIds = useMemo(() => {
    if (filter === "all") return new Set(files.map((f) => f.id));
    return new Set(
      files.filter((f) => f.risk === filter || f.risk === "origin").map((f) => f.id),
    );
  }, [files, filter]);

  // Build adjacency for hover highlighting
  const adjacency = useMemo(() => {
    const adj = new Map<string, Set<string>>();
    for (const e of edges) {
      if (!adj.has(e.source)) adj.set(e.source, new Set());
      if (!adj.has(e.target)) adj.set(e.target, new Set());
      adj.get(e.source)!.add(e.target);
      adj.get(e.target)!.add(e.source);
    }
    return adj;
  }, [edges]);

  const connectedIds = useMemo(() => {
    if (!hoveredId) return new Set<string>();
    const connected = new Set<string>([hoveredId]);
    const neighbors = adjacency.get(hoveredId);
    if (neighbors) {
      neighbors.forEach((id) => connected.add(id));
    }
    return connected;
  }, [hoveredId, adjacency]);

  return (
    <svg
      viewBox={`0 0 ${W} ${H}`}
      className="h-full w-full"
      role="img"
      aria-label="Ripple impact graph"
    >
      <defs>
        <pattern id="grid" width="32" height="32" patternUnits="userSpaceOnUse">
          <path
            d="M 32 0 L 0 0 0 32"
            fill="none"
            stroke="var(--border-strong)"
            strokeWidth="0.8"
          />
        </pattern>
      </defs>
      <rect width={W} height={H} fill="url(#grid)" opacity={preview ? 0.55 : 0.85} />

      {/* Edges */}
      {edges.map((e, i) => {
        const a = positions[e.source];
        const b = positions[e.target];
        if (!a || !b) return null;
        if (!visibleIds.has(e.source) || !visibleIds.has(e.target)) return null;
        const target = files.find((f) => f.id === e.target);
        const edgeStroke = target ? riskColor(target.risk) : "var(--origin)";
        
        // Highlight edge if connected to hovered node or selected
        const isConnected = hoveredId 
          ? (e.source === hoveredId || e.target === hoveredId)
          : (e.source === selectedId || e.target === selectedId);
        
        return (
          <line
            key={i}
            x1={a.x}
            y1={a.y}
            x2={b.x}
            y2={b.y}
            stroke={edgeStroke}
            strokeOpacity={isConnected ? 0.75 : 0.15}
            strokeWidth={isConnected ? 2 : 1.2}
            className="graph-edge"
            style={{ 
              animationDelay: `${i * 60}ms`,
              transition: "stroke-opacity 0.15s ease, stroke-width 0.15s ease"
            }}
          />
        );
      })}

      {/* Nodes */}
      {files.map((f, i) => {
        const p = positions[f.id];
        if (!p) return null;
        if (!visibleIds.has(f.id)) return null;
        const r = radius(f);
        const isSelected = selectedId === f.id;
        const isHovered = hoveredId === f.id;
        const isConnected = hoveredId ? connectedIds.has(f.id) : false;
        const isUnrelated = hoveredId && !isConnected;
        const color = riskColor(f.risk);
        
        // Hover effects: grow +3px, dim unrelated nodes
        const nodeRadius = isHovered ? r + 3 : r;
        const nodeOpacity = isUnrelated ? 0.3 : 1;
        
        return (
          <g
            key={f.id}
            className="graph-node cursor-pointer"
            style={{ 
              animationDelay: `${i * 50}ms`,
              opacity: nodeOpacity,
              transition: "opacity 0.15s ease"
            }}
            onClick={() => onSelect(f.id)}
            onMouseEnter={() => setHoveredId(f.id)}
            onMouseLeave={() => setHoveredId(null)}
          >
            {isSelected && (
              <circle cx={p.x} cy={p.y} r={r + 10} fill={color} fillOpacity={0.18} />
            )}
            {f.risk === "origin" ? (
              <rect
                x={p.x - nodeRadius}
                y={p.y - nodeRadius}
                width={nodeRadius * 2}
                height={nodeRadius * 2}
                rx={3}
                fill={color}
                stroke="#fff"
                strokeWidth={isSelected ? 2 : 1.5}
                style={{ transition: "all 0.15s ease" }}
              />
            ) : (
              <circle
                cx={p.x}
                cy={p.y}
                r={nodeRadius}
                fill={color}
                stroke="#fff"
                strokeWidth={isSelected ? 2 : 1.5}
                style={{ transition: "all 0.15s ease" }}
              />
            )}
          </g>
        );
      })}

      {/* Labels - rendered after nodes so they sit on top */}
      {!preview && files.map((f) => {
        const p = positions[f.id];
        if (!p) return null;
        if (!visibleIds.has(f.id)) return null;
        const r = radius(f);
        const isHovered = hoveredId === f.id;
        
        // Show labels: always for origin/high, on hover for med/low
        const shouldShowLabel = f.risk === "origin" || f.risk === "high" || isHovered;
        if (!shouldShowLabel) return null;
        
        const label = smartLabel(f.shortName);
        const labelY = p.y + r + 14;
        
        // Pill background for origin and high risk
        const showPill = f.risk === "origin" || f.risk === "high";
        
        return (
          <g key={`label-${f.id}`}>
            {showPill && (
              <rect
                x={p.x - label.length * 3}
                y={labelY - 9}
                width={label.length * 6}
                height={14}
                rx={7}
                fill="white"
                stroke="var(--border-strong)"
                strokeWidth={0.5}
                opacity={0.95}
              />
            )}
            <text
              x={p.x}
              y={labelY}
              textAnchor="middle"
              className="font-mono"
              fontSize="9.5"
              fill="var(--text-2)"
              style={{ 
                pointerEvents: "none",
                transition: "opacity 0.15s ease",
                opacity: isHovered ? 1 : (showPill ? 0.9 : 0.8)
              }}
            >
              {label}
            </text>
          </g>
        );
      })}
    </svg>
  );
}

export const riskTextColor = (r: ImpactFile["risk"]) => {
  switch (r) {
    case "high":  return "var(--risk-high)";
    case "med":   return "var(--risk-med)";
    case "low":   return "var(--risk-low)";
    default:      return "var(--origin)";
  }
};

// Made with Bob
