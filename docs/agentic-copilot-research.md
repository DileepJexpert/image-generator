# Agentic Copilot — research notes (Cursor, Cline, OpenClaw)

Research to inform turning Katixo's chat-only Copilot into an agent that can *do
things* in the studio. Sources at the bottom. Date: 2026-06-19.

## TL;DR

All three tools are the **same core loop** with different surfaces:

> assemble context → call LLM with a list of tools → LLM emits tool call(s) →
> we execute them → feed results back → repeat until the LLM says "done".

What differs is **what the tools are**, **how much autonomy** the agent gets, and
**how changes are approved**. For Katixo the loop is cheap to build because every
action already exists as an internal endpoint — the tools are just thin wrappers
over our own API. The real design work is **autonomy/approval** and the **local
model**, not the loop.

## What each one actually is

| Tool | What it is | What it does |
|---|---|---|
| **Cursor** | Closed-source AI IDE (VS Code fork, by Anysphere) | Agent mode: searches the codebase, reads/edits many files, runs terminal cmds (with approval), iterates. Custom "Composer" model + a fast **apply** model for writing diffs. Codebase **index** (embeddings) for context. MCP for extra tools. Tool-call **budget** (~25, then a "Continue" checkpoint). |
| **Cline** | Open-source VS Code/JetBrains agent (~58k★) | Same loop, fully **client-side** — your API keys, code never leaves the machine. Distinctive **Plan mode → Act mode** split. **Every** file edit / command / browser action hits an **approval gate** (or flip auto-approve). Explores with read-only tools first. |
| **OpenClaw** | Open-source **always-on local** agent (was Clawdbot/Moltbot) | A long-lived Node daemon connecting an LLM to files/shell/browser/messaging (50+ apps). **AgentSkills** = portable `SKILL.md` files. A **heartbeat** scheduler wakes it every N min to act proactively. Model-agnostic router (cloud or local Ollama). |

## The universal architecture (what to borrow)

OpenClaw's own writeup says its loop "mirrors Claude Code." The reusable pieces:

1. **Agent loop** — system prompt + memory + tool list + history → LLM → parse
   tool calls → execute → append results as a `tool`-role message → loop.
2. **Tools as a registry** — each tool = name + description + JSON-schema params.
   Cursor/Cline expose file/terminal/browser; OpenClaw adds skills. **Ours would
   expose the studio**: `generate_image`, `image_to_video`, `remove_bg`,
   `upscale`, `scrape_leads`, `add_text_element`, `add_image_element`,
   `list_elements`, `select_element`, `export`.
3. **Plan vs Act** (Cline) / **Plan vs Agent** (Cursor) — let the model lay out a
   plan and get a thumbs-up *before* it mutates anything.
4. **Approval gates** (all three) — gate irreversible / outbound / costly actions
   behind a confirm. Cline gates *every* action by default; OpenClaw explicitly
   says gate "payments, deletions, external communication."
5. **Tool-call budget + checkpoints** (Cursor) — cap calls (~25), then pause for a
   "Continue" so a runaway loop can't burn the GPU unattended.
6. **Read-only-first exploration** (Cline) — cheap context-gathering tools
   (`list_elements`, read scene JSON) before any mutation.

## Local-model reality (this constrains us)

- **Ollama supports tool calling** via an OpenAI-style schema on `/api/chat`:
  tools are `{type:"function", function:{name, description, parameters}}`; the
  model replies with `message.tool_calls`; results go back as `role:"tool"`
  messages. We already use `/api/chat` — adding a `tools` array is incremental.
- **Streaming + tool calls together** was still maturing as of these posts (tool
  streaming and `tool_choice` enforcement were "coming"). Safe path: **buffered**
  chat for tool-using turns, keep token-streaming for plain replies.
- **Model size vs our hardware** ⚠️ — the consensus for *reliable* multi-step tool
  use is **14B–32B+**, and **OpenClaw recommends 24GB+ VRAM (32B+)** for
  dependable local agents. **Katixo targets a 12GB GPU.** So:
  - A 32B agent model won't fit comfortably alongside ComfyUI on 12GB.
  - Realistic local pick: **Qwen3 ~8–14B** (or GLM-4 / GPT-OSS class) with
    **temperature 0–0.2** and a **small, well-described toolset** — accuracy of
    tool selection drops with many tools, so keep the registry tight.
  - The agent model and a generation job won't run on the GPU at the *same* time
    on 12GB — fine, since generation is already serialized to 1 worker. The
    Copilot can run on CPU/partial-offload while idle, or we accept it stalls
    during a generation job.

## Security notes (relevant to what we already shipped)

- OpenClaw's cautionary data point: **26% of community skills had
  vulnerabilities** — i.e. don't blindly execute model-authored or third-party
  actions. Our agent should only ever call **our own fixed tool registry**, never
  arbitrary shell (a deliberate divergence from OpenClaw — we don't want
  shell/browser/email access).
- Two of our tools touch the outside world and should be **approval-gated** even
  in "auto" mode: `scrape_leads` (outbound fetches) and any future
  **send-outreach** tool (external communication). Generation/canvas edits are
  local and reversible → safe to auto-apply with an undo.

## Recommended shape for the Katixo agentic Copilot

Constrained, local, and within CLAUDE.md (no new services, async jobs, internal
API only):

1. **Tool registry** in the `copilot` package — each tool wraps an existing
   service/endpoint; described with JSON schema for Ollama.
2. **Agent loop** in `CopilotService`: buffered `/api/chat` with `tools`; execute
   returned `tool_calls` against the registry; loop with a **call budget** and a
   final-answer stop. Long actions (generate/scrape) still go through the **job
   queue** — the tool call returns a `jobId`, and the agent reports progress.
3. **Plan → Act toggle** in the panel (borrowed from Cline): plan first, confirm,
   then execute.
4. **Approval gate** for outbound/irreversible tools; auto-apply local canvas
   edits with undo.
5. **Context** = current project scene JSON + selected element(s), injected like
   Cursor's open-file/`@`-mention context.
6. **Step UI** in the Copilot panel: show each tool call + result as a row
   (Cursor/Cline style), reusing the streaming we already built for the text.

This gets us a "make a 1080×1080 promo with this product on a clean background,
then draft outreach to these shops" Copilot — without shell access, without the
cloud, and without violating the monolith/job-queue rules.

## Sources

- [How to Use Cursor Agent Mode (apidog)](https://apidog.com/blog/how-to-use-cursor-agent-mode/)
- [Cursor 2.0: Agent-First Architecture (digitalapplied)](https://www.digitalapplied.com/blog/cursor-2-0-agent-first-architecture-guide)
- [Cursor 3 Agent-First Interface (InfoQ)](https://www.infoq.com/news/2026/04/cursor-3-agent-first-interface/)
- [Cline — open-source coding agent (cline.bot)](https://cline.bot/)
- [Cline Guide (Onegen)](https://www.onegen.ai/project/cline-guide-the-open-source-autonomous-coding-agent-for-vs-code/)
- [What is OpenClaw (Built In)](https://builtin.com/articles/what-is-openclaw)
- [OpenClaw complete guide (Milvus)](https://milvus.io/blog/openclaw-formerly-clawdbot-moltbot-explained-a-complete-guide-to-the-autonomous-ai-agent.md)
- [Build a secure always-on local agent with OpenClaw (NVIDIA)](https://developer.nvidia.com/blog/build-a-secure-always-on-local-ai-agent-with-nvidia-nemoclaw-and-openclaw/)
- [Ollama tool support (ollama.com)](https://ollama.com/blog/tool-support)
- [Best Ollama models for coding agents (haimaker.ai)](https://haimaker.ai/blog/best-ollama-models-for-coding-agents/)
